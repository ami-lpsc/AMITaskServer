package net.hep.ami.task;

import java.sql.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;

public class Scheduler extends Thread
{
	/*---------------------------------------------------------------------*/

	private static final Pattern s_lockNameSplitPattern = Pattern.compile("[^a-zA-Z0-9_]");

	private static final int s_timeoutDelay = 1000;

	private static final int s_timeoutSteps = 10;

	/*---------------------------------------------------------------------*/

	private String m_jdbc_url;

	private String m_router_user;
	private String m_router_pass;

	private String m_server_name;

	/*---------------------------------------------------------------------*/

	private int m_max_tasks;

	private float m_compression;

	/*---------------------------------------------------------------------*/

	private Connection m_connection = null;

	private List<Integer> m_priorityTable = null;

	private final Map<String, Task> m_runningTaskMap = new HashMap<String, Task>();

	/*---------------------------------------------------------------------*/

	public Scheduler(String jdbc_url, String router_user, String router_pass, String server_name, int max_tasks, float compression) throws Exception
	{
		super();

		/*-----------------------------------------------------------------*/
		/* SET INSTANCE VARIABLES                                          */
		/*-----------------------------------------------------------------*/

		m_jdbc_url = jdbc_url;

		m_router_user = router_user;
		m_router_pass = router_pass;

		m_server_name = server_name;

		/*-----------------------------------------------------------------*/

		m_max_tasks = max_tasks;

		m_compression = compression;

		/*-----------------------------------------------------------------*/
		/* CHECK JDBC CONNECTION                                           */
		/*-----------------------------------------------------------------*/

		getRouterConnection();

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	@Override
	public void run()
	{
		/*-----------------------------------------------------------------*/

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

		/*-----------------------------------------------------------------*/

		long i = 0;

		Random random = new Random();

		for(;;)
		{
			/*-------------------------------------------------------------*/

			try { Thread.sleep(s_timeoutDelay); } catch(InterruptedException e) { /* IGNORE */ }

			/*-------------------------------------------------------------*/

			try
			{
				/*---------------------------------------------------------*/

				if(i++ % 30 == 0)
				{
					buildPriorityTable();
				}

				/*---------------------------------------------------------*/

				removeFinishTasks();

				startTask(random);

				/*---------------------------------------------------------*/
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.err.println(
					e.getMessage()
				);
			}

			/*-------------------------------------------------------------*/
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private Connection getRouterConnection() throws Exception
	{
		if(m_connection == null || m_connection.isClosed())
		{
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

	private void buildPriorityTable() throws Exception
	{
		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			ResultSet resultSet = statement.executeQuery("SELECT MAX(priority) + 1 FROM router_task WHERE serverName = '" + m_server_name.replace("'", "''") + "'");

			m_priorityTable = resultSet.next() ? PriorityTableBuilder.build(resultSet.getInt(1), m_compression)
			                                   : PriorityTableBuilder.build(0x00000000000000000, m_compression)
			;
		}
		finally
		{
			statement.close();
		}
	}

	/*---------------------------------------------------------------------*/

	private void removeAllTasks() throws Exception
	{
		/*-----------------------------------------------------------------*/

		m_runningTaskMap.clear();

		/*-----------------------------------------------------------------*/

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			if(statement.executeUpdate("UPDATE router_task SET status = (status & ~1) WHERE serverName = '" + m_server_name.replace("'", "''") + "'") > 0)
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

		for(Entry<String, Task> entry: m_runningTaskMap.entrySet())
		{
			if(entry.getValue().isAlive() == false)
			{
				toBeRemoved.add(entry.getKey());
			}
		}

		/*-----------------------------------------------------------------*/

		int nb;
		boolean status;

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			for(String taskId: toBeRemoved)
			{
				status = m_runningTaskMap.remove(taskId).getStatus();
System.out.println(status);
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

	private static class Tuple
	{
		public String id;
		public String command;
		public Set<String> lockNames;

		public Tuple(String _id, String _command, Set<String> _lockNames)
		{
			id = _id;
			command = _command;
			lockNames = _lockNames;
		}
	}

	/*---------------------------------------------------------------------*/

	private void startTask(Random random) throws Exception
	{
		/*-----------------------------------------------------------------*/

		if(m_runningTaskMap.size() >= m_max_tasks || m_priorityTable.isEmpty())
		{
			return;
		}

		/*-----------------------------------------------------------------*/

		java.util.Date date = new java.util.Date();

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			/*-------------------------------------------------------------*/
			/* SELECT TASK                                                 */
			/*-------------------------------------------------------------*/

			int i = 0;

			String a, b, c;

			ResultSet resultSet;

			Set<String> lockNames;

			List<Tuple> list = new ArrayList<Tuple>();

			do
			{
				/*---------------------------------------------------------*/

				if(i++ >= s_timeoutSteps)
				{
					return;
				}

				/*---------------------------------------------------------*/

				try { Thread.sleep(s_timeoutDelay / s_timeoutSteps); } catch(InterruptedException e) { /* IGNORE */ }

				/*---------------------------------------------------------*/

				resultSet = statement.executeQuery("SELECT id, command, lockNames FROM router_task WHERE serverName = '" + m_server_name.replace("'", "''") + "' AND priority = '" + m_priorityTable.get(random.nextInt(m_priorityTable.size())) + "' AND (lastRunTime + step) < '" + date.getTime() + "' AND (status & 1) = 0");

				while(resultSet.next())
				{
					lockNames = new HashSet<String>();

					a = resultSet.getString(1);
					b = resultSet.getString(2);
					c = resultSet.getString(3);

					if(c != null)
					{
						for(String lockName: s_lockNameSplitPattern.split(c))
						{
							lockNames.add(lockName);
						}
					}

					if(isLocked(lockNames) == false)
					{
						list.add(new Tuple(a, b, lockNames));
					}
				}

				/*---------------------------------------------------------*/

			} while(list.size() == 0);

			/*-------------------------------------------------------------*/

			Tuple tuple = list.get(random.nextInt(list.size()));

			/*-------------------------------------------------------------*/
			/* RUN TASK                                                    */
			/*-------------------------------------------------------------*/

			m_runningTaskMap.put(tuple.id, new Task(tuple.command, tuple.lockNames));

			if(statement.executeUpdate("UPDATE router_task SET status = (status | 1), lastRunTime = '" + date.getTime() + "', lastRunDate = '" + net.hep.ami.mini.JettyHandler.s_simpleDateFormat.format(date) + "' WHERE id = '" + tuple.id + "'") > 0)
			{
				connection.commit();
			}

			/*-------------------------------------------------------------*/
		}
		finally
		{
			statement.close();
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private boolean isLocked(Set<String> lockNames)
	{
		for(Task task: m_runningTaskMap.values())
		{
			if(task.isLocked(lockNames))
			{
				return true;
			}
		}

		return false;
	}

	/*---------------------------------------------------------------------*/
	/*---------------------------------------------------------------------*/

	public List<Map<String, String>> getTasksStatus() throws Exception
	{
		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		List<Map<String, String>> result = new ArrayList<Map<String, String>>();

		try
		{
			ResultSet resultSet = statement.executeQuery("SELECT id, name, description, status FROM router_task WHERE serverName = '" + m_server_name.replace("'", "''") + "'");

			Map<String, String> map;

			String id;
			String name;
			String description;

			int status;

			while(resultSet.next())
			{
				id = resultSet.getString(1);
				name = resultSet.getString(2);
				description = resultSet.getString(3);
				status = resultSet.getInt(4);

				map = new HashMap<String, String>();

				map.put("id"         , id          != null ? id          : "");
				map.put("name"       , name        != null ? name        : "");
				map.put("description", description != null ? description : "");

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
