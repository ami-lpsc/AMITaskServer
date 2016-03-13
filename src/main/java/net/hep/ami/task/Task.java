package net.hep.ami.task;

import java.io.*;
import java.util.*;

public class Task
{
	/*---------------------------------------------------------------------*/

	private static final boolean s_isWindows = System.getProperty("os.name").startsWith("Windows");

	/*---------------------------------------------------------------------*/

	private String m_id;
	private String m_name;
	private String m_command;

	private Process m_process;

	private Set<String> m_lockNames;

	/*---------------------------------------------------------------------*/

	public Task(String id, String name, String command, Set<String> lockNames) throws IOException
	{
		m_id = id;
		m_name = name;
		m_command = command;

		m_process = Runtime.getRuntime().exec(s_isWindows ? new String[] {("cmd.exe"), "/C", command}
		                                                  : new String[] {"/bin/bash", "-c", command}
		);

		m_lockNames = lockNames;
	}

	/*---------------------------------------------------------------------*/

	public String getId()
	{
		return m_id;
	}

	/*---------------------------------------------------------------------*/

	public String getName()
	{
		return m_name;
	}

	/*---------------------------------------------------------------------*/

	public String getCommand()
	{
		return m_command;
	}

	/*---------------------------------------------------------------------*/

	public void kill()
	{
		m_process.destroy();
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
