package gwdatacenter;

import gwdatacenter.database.*;

import java.time.OffsetTime;
import java.util.*;
import java.io.*;

public class SafeTimeSpan
{
	public OffsetTime tStart;
	public OffsetTime tEnd;
	public SafeTimeSpan(OffsetTime t, OffsetTime t1)
	{
		tStart = t;
		tEnd = t1;
	}
}