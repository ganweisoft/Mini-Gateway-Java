package gwdatacenter;

import java.util.*;

public enum GWTcpType
{
	TS,
	TC,
	Other;

	public static final int SIZE = java.lang.Integer.SIZE;

	public int getValue()
	{
		return this.ordinal();
	}

	public static GWTcpType forValue(int value)
	{
		return values()[value];
	}
}