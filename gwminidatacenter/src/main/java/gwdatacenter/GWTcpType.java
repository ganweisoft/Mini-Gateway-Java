package gwdatacenter;

import java.util.*;

public enum GWTcpType
{
	TS,
	TC,
	Other;

	public static final int SIZE = Integer.SIZE;

	public int getValue()
	{
		return this.ordinal();
	}

	public static GWTcpType forValue(int value)
	{
		return values()[value];
	}
}