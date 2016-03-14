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
	private Set<String> m_lockSet;

	/*---------------------------------------------------------------------*/

	private Process m_process;

	/*---------------------------------------------------------------------*/

	public Task(String id, String name, String command, Set<String> lockSet) throws IOException
	{
		/*-----------------------------------------------------------------*/
		/* SET INSTANCE VARIABLES                                          */
		/*-----------------------------------------------------------------*/

		m_id = id;
		m_name = name;
		m_command = command;
		m_lockSet = lockSet;

		/*-----------------------------------------------------------------*/
		/* EXECUTE COMMAND                                                 */
		/*-----------------------------------------------------------------*/

		m_process = Runtime.getRuntime().exec(s_isWindows ? new String[] {("cmd.exe"), "/C", command}
		                                                  : new String[] {"/bin/bash", "-c", command}
		);

		/*-----------------------------------------------------------------*/
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

	public boolean isLocked(Set<String> n_lockSet)
	{
		for(String a: n_lockSet)
		{
			for(String b: m_lockSet)
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

	public void destroy()
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
}
