package gwdatacenter;

import gwdatacenter.database.*;
import gwdatacenter.mqservice.*;
import java.util.*;
import java.time.*;

public enum ChangedEquipState
{
	Add,
	Delete,
	Edit;

	public static final int SIZE = Integer.SIZE;

	public int getValue()
	{
		return this.ordinal();
	}

	public static ChangedEquipState forValue(int value)
	{
		return values()[value];
	}
}