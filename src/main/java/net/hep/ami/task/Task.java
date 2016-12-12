package net.hep.ami.task;

import java.io.*;

public class Task
{
	/*---------------------------------------------------------------------*/

	private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().startsWith("windows");

	/*---------------------------------------------------------------------*/

	private String m_id;
	private String m_name;
	private String m_command;
	private String m_commaSeparatedLocks;

	/*---------------------------------------------------------------------*/

	private Process m_process;

	/*---------------------------------------------------------------------*/

	public Task(String id, String name, String command, String commaSeparatedLocks) throws IOException
	{
		/*-----------------------------------------------------------------*/
		/* SET INSTANCE VARIABLES                                          */
		/*-----------------------------------------------------------------*/

		m_id = id;
		m_name = name;
		m_command = command;
		m_commaSeparatedLocks = commaSeparatedLocks;

		/*-----------------------------------------------------------------*/
		/* CREATE PROCESS                                                  */
		/*-----------------------------------------------------------------*/

		m_process = Runtime.getRuntime().exec(IS_WINDOWS ? new String[] {("cmd.exe"), "/C", command}
		                                                 : new String[] {"/bin/bash", "-c", command}
		);

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/
	/* TASK                                                                */
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

	public String getCommaSeparatedLocks()
	{
		return m_commaSeparatedLocks;
	}

	/*---------------------------------------------------------------------*/
	/* PROCESS                                                             */
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
