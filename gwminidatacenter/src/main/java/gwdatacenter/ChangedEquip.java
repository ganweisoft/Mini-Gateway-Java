package gwdatacenter;

import gwdatacenter.database.*;
import gwdatacenter.mqservice.*;
import java.util.*;
import java.time.*;

public class ChangedEquip
{
	public int iStaNo;
	public int iEqpNo;
	public ChangedEquipState State = ChangedEquipState.values()[0];
}