package gwdatacenter.mqservice;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import gwdatacenter.*;
import org.eclipse.paho.client.mqttv3.*;
import sharpSystem.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MqttProvider
{
	private MqttProvider()
	{
	}
	public static final MqttProvider Instance = new MqttProvider();
	private MqttClient _mqttClient;
	private MqttConnectOptions _aspOption;
	public final CopyOnWriteArrayList<Equip> EquipTableRows = new CopyOnWriteArrayList<>();

	public final void Init()
	{
		var username = System.getenv("MqUsername");
		var password = System.getenv("MqPassword");
		var gateWayId = System.getenv("InstanceId");
		var mqServer = "tcp://" + System.getenv("MqServer");

		var mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setServerURIs(new String[] { mqServer });
		mqttConnectOptions.setUserName(username);
		mqttConnectOptions.setPassword(password.toCharArray());
		mqttConnectOptions.setAutomaticReconnect(true);
		mqttConnectOptions.setKeepAliveInterval(10);

		String broker = mqServer + ":";
		var mqSslEnable = System.getenv("MqSslEnable");
		boolean mqSslEnableBool = Boolean.parseBoolean(mqSslEnable);

		// TODO: support tls connect
		if (mqSslEnableBool)
		{
			var mqSslPort = System.getenv("MqSslPort");
			var mqSslCert = System.getenv("MqSslCert");

			broker = broker + mqSslPort;
		}
		else
		{
			var mqPort = System.getenv("MqPort");
			broker = broker + mqPort;
		}

		_aspOption = mqttConnectOptions;
        try {
            _mqttClient = new MqttClient(broker, gateWayId);
			_mqttClient.setCallback(new MqttCallback() {
				public void messageArrived(String topic, MqttMessage message) throws RuntimeException {
					DataCenter.WriteLogFile("topic: " + topic);
					var payload = new String(message.getPayload());
					DataCenter.WriteLogFile("received:" + payload);

					ObjectMapper mapper = new ObjectMapper();
					mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
					mapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
					mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
					JavaTimeModule javaTimeModule = new JavaTimeModule();

					javaTimeModule.addSerializer(LocalDateTime.class,new LocalDateTimeSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					javaTimeModule.addSerializer(LocalDate.class,new LocalDateSerializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
					javaTimeModule.addSerializer(LocalTime.class,new LocalTimeSerializer(DateTimeFormatter.ofPattern("HH:mm:ss")));

					javaTimeModule.addDeserializer(LocalDateTime.class,new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
					javaTimeModule.addDeserializer(LocalDate.class,new LocalDateDeserializer(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
					javaTimeModule.addDeserializer(LocalTime.class,new LocalTimeDeserializer(DateTimeFormatter.ofPattern("HH:mm:ss")));
					mapper.registerModule(javaTimeModule);

					var topicCompare = org.eclipse.paho.client.mqttv3.MqttTopic.isMatched(topic, String.format("$sys/iotcenter/%1$s/java/down", _mqttClient.getClientId()));
					if (topicCompare)
					{
                        try {
							MqMessage desMsgObject = mapper.readValue(payload, MqMessage.class);
							if (desMsgObject.getFlowType() == 1 && !desMsgObject.getEquips().isEmpty())
							{
								for (int i = 0; i < desMsgObject.getEquips().size(); i++)
								{
									EquipTableRows.add(i, desMsgObject.getEquips().get(i));
								}

								if (EquipInited != null)
								{
									for (EventHandler<EventArgs> listener : EquipInited.listeners())
									{
										listener.invoke(this, EventArgs.Empty);
									}
								}
							}
                        } catch (JsonProcessingException e) {
							throw new RuntimeException(String.valueOf(e));
						} catch (Exception e) {
							throw new RuntimeException(String.valueOf(e));
                        }
                    }

					topicCompare = org.eclipse.paho.client.mqttv3.MqttTopic.isMatched(topic, String.format("$sys/iotcenter/%1$s/command/down", _mqttClient.getClientId()));
					if (topicCompare)
					{
                        try {
							MqCmdMessage desMsgObject = mapper.readValue(payload, MqCmdMessage.class);
							var equipItem = StationItem.GetEquipItemFromEquipNo(desMsgObject.getEquipNo());
							if (equipItem != null)
							{
								equipItem.AddSetItem(new SetItem(desMsgObject.getEquipNo(),
										desMsgObject.getMainInstruct(),
										desMsgObject.getMinorInstruct(),
										desMsgObject.getValue()));
							}
                        } catch (JsonProcessingException e) {
							throw new RuntimeException(String.valueOf(e));
						} catch (Exception e) {
							throw new RuntimeException(String.valueOf(e));
						}
					}

				}

				public void connectionLost(Throwable cause) {
					System.out.println("connectionLost: " + cause.getMessage());
					cause.printStackTrace();
				}

				public void deliveryComplete(IMqttDeliveryToken token) {
					System.out.println("deliveryComplete: " + token.isComplete());
				}
			});
			_mqttClient.connect(_aspOption);
			_mqttClient.subscribe(MqttTopic.TopicIotsysCommandDown);
			_mqttClient.subscribe(MqttTopic.getTopicIotsysEquipDown());
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }


	public final void PublishYcRtValueAsync(MqRtValueMessage msg) {
		var topic = String.format(MqttTopic.TOPIC_IOTSYS_MINIDCYC_DEVICE_DATA_REPORT, _mqttClient.getClientId());

		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(msg);
			var appMsg = new MqttMessage(json.getBytes());
			_mqttClient.publish(topic, appMsg);

		} catch (JsonProcessingException | MqttException e) {
			DataCenter.WriteLogFile(String.format("PublishYcRtValueAsync Exception: %1$s", e));
		}
	}

	public final void PublishYxRtValueAsync(MqRtValueMessage msg)
	{
		var topic = String.format(MqttTopic.TOPIC_IOTSYS_MINIDCYX_DEVICE_DATA_REPORT, _mqttClient.getClientId());

		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(msg);
			var appMsg = new MqttMessage(json.getBytes());
			_mqttClient.publish(topic, appMsg);

		} catch (JsonProcessingException | MqttException e) {
			DataCenter.WriteLogFile(String.format("PublishYxRtValueAsync Exception: %1$s", e));
		}
	}


	public final void PublishRtStateAsync(MqRtStateMessage msg)
	{
		var topic = String.format(MqttTopic.TOPIC_IOTSYS_DEVICE_STATE_REPORT, _mqttClient.getClientId());

		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(msg);
			var appMsg = new MqttMessage(json.getBytes());
			_mqttClient.publish(topic, appMsg);

		} catch (JsonProcessingException | MqttException e) {
			DataCenter.WriteLogFile(String.format("PublishRtStateAsync Exception: %1$s", e));
		}
	}

	public final void PublishEvtValueAsync(MqEvtMessage msg)
	{
		var topic = String.format(MqttTopic.TOPIC_IOTSYS_MINIDCYX_DEVICE_EVT_REPORT, _mqttClient.getClientId());

		try {
			ObjectMapper mapper = new ObjectMapper();
			String json = mapper.writeValueAsString(msg);
			var appMsg = new MqttMessage(json.getBytes());
			_mqttClient.publish(topic, appMsg);

		} catch (JsonProcessingException | MqttException e) {
			DataCenter.WriteLogFile(String.format("PublishEvtValueAsync Exception: %1$s", e));
		}
	}

	public Event<EventHandler<EventArgs>> EquipInited = new Event<EventHandler<EventArgs>>();
}