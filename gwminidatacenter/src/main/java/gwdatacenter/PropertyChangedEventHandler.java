package gwdatacenter;

import java.util.*;
import java.io.*;
import java.nio.file.*;

@FunctionalInterface
public interface PropertyChangedEventHandler
{
	void invoke(Object sender, PropertyChangedEventArgs e);
}