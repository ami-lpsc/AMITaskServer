package net.hep.ami.task;

import java.sql.*;
import java.util.*;
import java.util.Map.*;

public class Scheduler extends Thread
{
	/*---------------------------------------------------------------------*/

	private String m_jdbc_url;
	private String m_router_user;
	private String m_router_pass;
	private String m_server_name;

	/*---------------------------------------------------------------------*/

	private int m_max_tasks;

	private int[] m_priorityTable;

	/*---------------------------------------------------------------------*/

	private static int s_timeout = 1000;

	private Connection m_connection = null;

	private final Map<String, Process> m_taskMap = new HashMap<String, Process>();

	/*---------------------------------------------------------------------*/

	public Scheduler(String jdbc_url, String router_user, String router_pass, String server_name, int max_tasks, int[] priorityTable)
	{
		super();

		m_jdbc_url = jdbc_url;
		m_router_user = router_user;
		m_router_pass = router_pass;
		m_server_name = server_name;

		m_max_tasks = max_tasks;
		m_priorityTable = priorityTable;
	}

	/*---------------------------------------------------------------------*/

	@Override
	public void run()
	{
		try
		{
			removeAllTasks();
		}
		catch(Exception e)
		{
			System.err.println(
				e.getMessage()
			);
		}

		Random random = new Random();

		for(;;)
		{
			/*-------------------------------------------------------------*/

			try { Thread.sleep(s_timeout); } catch(InterruptedException e) { /* IGNORE */ }

			/*-------------------------------------------------------------*/

			try
			{
				removeFinishTasks();

				if(m_taskMap.size() <= m_max_tasks)
				{
					startTask(random);
				}
			}
			catch(Exception e)
			{
				System.err.println(
					e.getMessage()
				);
			}

			/*-------------------------------------------------------------*/
		}
	}

	/*---------------------------------------------------------------------*/

	private Connection getRouterConnection() throws Exception
	{
		if(m_connection == null
		   ||
		   m_connection.isClosed()
		 ) {
			m_connection = DriverManager.getConnection(
				m_jdbc_url,
				m_router_user,
				m_router_pass
			);

			m_connection.setAutoCommit(false);
		}

		return m_connection;
	}

	/*---------------------------------------------------------------------*/

	private void removeAllTasks() throws Exception
	{
		/*-----------------------------------------------------------------*/

		m_taskMap.clear();

		/*-----------------------------------------------------------------*/

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			if(statement.executeUpdate("UPDATE router_task SET status = (status & ~1)") > 0)
			{
				connection.commit();
			}
		}
		finally
		{
			statement.close();
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private void removeFinishTasks() throws Exception
	{
		/*-----------------------------------------------------------------*/

		List<String> toBeRemoved = new ArrayList<String>();

		for(Entry<String, Process> entry: m_taskMap.entrySet())
		{
			if(entry.getValue().isAlive() == false)
			{
				toBeRemoved.add(entry.getKey());
			}
		}

		/*-----------------------------------------------------------------*/

		int nb;
		Boolean status;

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			for(String taskId: toBeRemoved)
			{
				status = m_taskMap.remove(taskId).exitValue() == 0;

				nb = status ? statement.executeUpdate("UPDATE router_task SET status = ((status & ~3) | 2) WHERE id = '" + taskId + "'")
				            : statement.executeUpdate("UPDATE router_task SET status = ((status & ~3) | 0) WHERE id = '" + taskId + "'")
				;

				if(nb > 0)
				{
					connection.commit();
				}
			}
		}
		finally
		{
			statement.close();
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private class Task
	{
		public String id;
		public String command;

		public Task(String _id, String _command)
		{
			id = _id;
			command = _command;
		}
	}

	/*---------------------------------------------------------------------*/

	private void startTask(Random random) throws Exception
	{
		int i = 0;

		ResultSet resultSet;

		List<Task> list = new ArrayList<Task>();

		java.util.Date date = new java.util.Date();

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			/*-------------------------------------------------------------*/

			do
			{
				if(i++ < 10)
				{
					try { Thread.sleep(s_timeout / 10); } catch(InterruptedException e) { /* IGNORE */ }
				}
				else
				{
					return;
				}

				resultSet = statement.executeQuery("SELECT id, command FROM router_task WHERE serverName = '" + m_server_name.replace("'", "''") + "' AND priority = '" + m_priorityTable[random.nextInt(m_priorityTable.length)] + "' AND (lastRunTime + step) < '" + date.getTime() + "' AND (status & 1) = 0");

				while(resultSet.next())
				{
					list.add(new Task(
						resultSet.getString(1)
						,
						resultSet.getString(2)
					));
				}

			} while(list.size() == 0);

			/*-------------------------------------------------------------*/

			Task task = list.get(random.nextInt(list.size()));

			m_taskMap.put(task.id, Runtime.getRuntime().exec("/bin/sh -c \"" + task.command.replace("\"", "\\\"") + "\""));

			if(statement.executeUpdate("UPDATE router_task SET status = (status | 1), lastRunTime = '" + date.getTime() + "', lastRunDate = '" + net.hep.ami.mini.JettyHandler.s_simpleDateFormat.format(date) + "' WHERE id = '" + task.id + "'") > 0)
			{
				connection.commit();
			}

			/*-------------------------------------------------------------*/
		}
		finally
		{
			statement.close();
		}
	}

	/*---------------------------------------------------------------------*/
	/*---------------------------------------------------------------------*/

	public List<Map<String, String>> getTasksStatus() throws Exception
	{
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			ResultSet resultSet = statement.executeQuery("SELECT name, status FROM router_task WHERE serverName = '" + m_server_name.replace("'", "''") + "'");

			Map<String, String> map;

			int status;

			while(resultSet.next())
			{
				map = new HashMap<String, String>();

				status = resultSet.getInt(2);

				map.put("name", resultSet.getString(1));
				map.put("running", Integer.toString((status >> 0) & 0x01));
				map.put("success", Integer.toString((status >> 1) & 0x01));

				result.add(map);
			}
		}
		finally
		{
			statement.close();
		}

		return result;
	}

	/*---------------------------------------------------------------------*/
	/*---------------------------------------------------------------------*/
}
