package gwdatacenter;

import gwdatacenter.database.*;
import gwdatacenter.mqservice.*;
import java.util.*;
import java.time.*;

public enum SafetyLevel
{
	Unsafety(1),
	Safety(2);

	public static final int SIZE = java.lang.Integer.SIZE;

	private int intValue;
	private static java.util.HashMap<Integer, SafetyLevel> mappings;
	private static java.util.HashMap<Integer, SafetyLevel> getMappings()
	{
		if (mappings == null)
		{
			synchronized (SafetyLevel.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, SafetyLevel>();
				}
			}
		}
		return mappings;
	}

	private SafetyLevel(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static SafetyLevel forValue(int value)
	{
		return getMappings().get(value);
	}
}