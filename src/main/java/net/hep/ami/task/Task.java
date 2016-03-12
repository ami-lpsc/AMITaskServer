package net.hep.ami.task;

import java.io.*;
import java.util.*;

public class Task
{
	/*---------------------------------------------------------------------*/

	private static final boolean s_isWindows = System.getProperty("os.name").startsWith("Windows");

	/*---------------------------------------------------------------------*/

	private Process m_process;

	private Set<String> m_lockNames;

	/*---------------------------------------------------------------------*/

	public Task(String command, Set<String> lockNames) throws IOException
	{
		m_process = Runtime.getRuntime().exec(s_isWindows ? new String[] {("cmd.exe"), "/C", command}
		                                                  : new String[] {"/bin/bash", "-c", command}
		);

		m_lockNames = lockNames;
	}

	/*---------------------------------------------------------------------*/

	public boolean isAlive()
	{
		return m_process.isAlive();
	}

	/*---------------------------------------------------------------------*/

	public boolean isSuccess()
	{
		return m_process.exitValue() == 0;
	}

	/*---------------------------------------------------------------------*/

	public boolean isLocked(Set<String> lockNames)
	{
		for(String a: lockNames)
		{
			for(String b: m_lockNames)
			{
				if(a.equalsIgnoreCase(b))
				{
					return true;
				}
			}
		}

		return false;
	}

	/*---------------------------------------------------------------------*/
}
