package net.hep.ami.task;

import java.sql.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;
import java.util.concurrent.*;

/* status flag:
 *  - bit 0: 0 = pending, 1 = running
 *  - bit 1: 0 = success, 1 = error
 */

public class Scheduler extends Thread
{
	/*---------------------------------------------------------------------*/

	private static final Integer s_timeoutDelay = 1000;

	private static final Pattern s_lockNameSplitPattern = Pattern.compile("[^a-zA-Z0-9_]");

	/*---------------------------------------------------------------------*/

	private String m_jdbcUrl;
	private String m_routerUser;
	private String m_routerPass;

	private String m_serverName;

	/*---------------------------------------------------------------------*/

	private int m_maxTasks;

	private float m_compression;

	/*---------------------------------------------------------------------*/

	private int m_numberOfPriorities = 0x00;

	private List<Integer> m_priorityTable = null;

	private final Map<String, Task> m_runningTaskMap = new ConcurrentHashMap<String, Task>();

	/*---------------------------------------------------------------------*/

	public Scheduler(String jdbcUrl, String routerUser, String routerPass, String serverName, int maxTasks, float compression) throws Exception
	{
		/*-----------------------------------------------------------------*/
		/* SUPER CONSTRUCTOR                                               */
		/*-----------------------------------------------------------------*/

		super(Scheduler.class.getName());

		/*-----------------------------------------------------------------*/
		/* SET INSTANCE VARIABLES                                          */
		/*-----------------------------------------------------------------*/

		m_jdbcUrl = jdbcUrl;
		m_routerUser = routerUser;
		m_routerPass = routerPass;

		m_serverName = serverName;

		/*-----------------------------------------------------------------*/

		m_maxTasks = maxTasks;

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

		Runtime.getRuntime().addShutdownHook(new Thread(Scheduler.class.getName()) {

			@Override
			public void run()
			{
				try
				{
					removeAllTasks(false);
				}
				catch(Exception e)
				{
					warn(e.getMessage());
				}
			}
		});

		/*-----------------------------------------------------------------*/

		try
		{
			removeAllTasks(true);
		}
		catch(Exception e)
		{
			warn(e.getMessage());
		}

		/*-----------------------------------------------------------------*/

		long i = 0;

		for(;;)
		{
			/*-------------------------------------------------------------*/

			try
			{
				/*---------------------------------------------------------*/

				sleep(s_timeoutDelay);

				/*---------------------------------------------------------*/

				if(i++ % 30 == 0)
				{
					buildPriorityTable();
				}

				/*---------------------------------------------------------*/

				removeFinishTasks();

				startTask();

				/*---------------------------------------------------------*/
			}
			catch(Exception e)
			{
				warn(e.getMessage());
			}

			/*-------------------------------------------------------------*/
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private Connection m_connection = null;

	private Connection getRouterConnection() throws Exception
	{
		if(m_connection == null || m_connection.isClosed())
		{
			m_connection = DriverManager.getConnection(
				m_jdbcUrl,
				m_routerUser,
				m_routerPass
			);
		}

		return m_connection;
	}

	/*---------------------------------------------------------------------*/

	private void info(String msg)
	{
		org.eclipse.jetty.util.log.Log.getRootLogger().info(msg);
	}

	/*---------------------------------------------------------------------*/

	private void warn(String msg)
	{
		org.eclipse.jetty.util.log.Log.getRootLogger().warn(msg);
	}

	/*---------------------------------------------------------------------*/

	private void buildPriorityTable() throws Exception
	{
		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			ResultSet resultSet = statement.executeQuery("SELECT MAX(priority) + 1 FROM router_task WHERE serverName = '" + m_serverName.replace("'", "''") + "'");

			try
			{
				m_numberOfPriorities = resultSet.next() ? resultSet.getInt(1)
				                                        : 0x00000000000000000
				;

				m_priorityTable = PriorityTableBuilder.build(m_numberOfPriorities, m_compression);
			}
			finally
			{
				resultSet.close();
			}
		}
		finally
		{
			statement.close();
		}
	}

	/*---------------------------------------------------------------------*/

	private void removeAllTasks(boolean doNotChangeErrorBit) throws Exception
	{
		/*-----------------------------------------------------------------*/

		for(Task task: m_runningTaskMap.values())
		{
			warn("Killing task `" + task.getName() + "`");

			task.destroy();
		}

		/*-----------------------------------------------------------------*/

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			if(doNotChangeErrorBit)
			{
				statement.executeUpdate("UPDATE router_task SET status = ((status & ~0b01) | 0b00) WHERE serverName = '" + m_serverName.replace("'", "''") + "' AND (status & 0b01) = 0b01");
			}
			else
			{
				statement.executeUpdate("UPDATE router_task SET status = ((status & ~0b11) | 0b10) WHERE serverName = '" + m_serverName.replace("'", "''") + "' AND (status & 0b01) = 0b01");
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

		Task task;

		Connection connection = getRouterConnection();
		Statement statement = connection.createStatement();

		try
		{
			for(String taskId: toBeRemoved)
			{
				task = m_runningTaskMap.remove(taskId);

				if(task.isSuccess())
				{
					statement.executeUpdate("UPDATE router_task SET status = ((status & ~0b11) | 0b00) WHERE id = '" + taskId + "'");
				}
				else
				{
					statement.executeUpdate("UPDATE router_task SET status = ((status & ~0b11) | 0b10) WHERE id = '" + taskId + "'");
				}

				info("Task `" + task.getName() + "` finished");
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
		public String name;
		public String command;
		public Set<String> lockNames;

		public Tuple(String _id, String _name, String _command, Set<String> _lockNames)
		{
			id = _id;
			name = _name;
			command = _command;
			lockNames = _lockNames;
		}
	}

	/*---------------------------------------------------------------------*/

	private Random m_random = new Random();

	private void startTask() throws Exception
	{
		/*-----------------------------------------------------------------*/

		if(m_numberOfPriorities == 0 || m_runningTaskMap.size() >= m_maxTasks)
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

			String a, b;
			String c, d;

			ResultSet resultSet;

			Set<String> lockNames;

			List<Tuple> list = new ArrayList<Tuple>();

			do
			{
				/*---------------------------------------------------------*/

				if(i >= m_numberOfPriorities)
				{
					return;
				}

				i++;

				/*---------------------------------------------------------*/

				sleep(s_timeoutDelay / (2 * m_numberOfPriorities));

				/*---------------------------------------------------------*/

				resultSet = statement.executeQuery("SELECT id, name, command, lockNames FROM router_task WHERE serverName = '" + m_serverName.replace("'", "''") + "' AND priority = '" + m_priorityTable.get(m_random.nextInt(m_priorityTable.size())) + "' AND (lastRunTime + step) < '" + date.getTime() + "' AND (status & 0b01) = 0b00");

				try
				{
					while(resultSet.next())
					{
						lockNames = new HashSet<String>();

						a = resultSet.getString(1);
						b = resultSet.getString(2);
						c = resultSet.getString(3);
						d = resultSet.getString(4);

						if(d != null)
						{
							for(String lockName: s_lockNameSplitPattern.split(d))
							{
								lockNames.add(lockName);
							}
						}

						if(isLocked(lockNames) == false)
						{
							list.add(new Tuple(a, b, c, lockNames));
						}
					}
				}
				finally
				{
					resultSet.close();
				}

				/*---------------------------------------------------------*/

			} while(list.size() == 0);

			/*-------------------------------------------------------------*/

			Tuple tuple = list.get(m_random.nextInt(list.size()));

			/*-------------------------------------------------------------*/
			/* RUN TASK                                                    */
			/*-------------------------------------------------------------*/

			info("Starting task `" + tuple.name + "`");

			m_runningTaskMap.put(tuple.id, new Task(tuple.id, tuple.name, tuple.command, tuple.lockNames));

			statement.executeUpdate("UPDATE router_task SET status = ((status & ~0b11) | 0b01), lastRunTime = '" + date.getTime() + "', lastRunDate = '" + net.hep.ami.mini.JettyHandler.s_simpleDateFormat.format(date) + "' WHERE id = '" + tuple.id + "'");

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
			ResultSet resultSet = statement.executeQuery("SELECT id, name, command, description, status FROM router_task WHERE serverName = '" + m_serverName.replace("'", "''") + "'");

			Map<String, String> map;

			String id;
			String name;
			String command;
			String description;

			int status;

			try
			{
				while(resultSet.next())
				{
					map = new HashMap<String, String>();

					/*-----------------------------------------------------*/

					id = resultSet.getString(1);
					name = resultSet.getString(2);
					command = resultSet.getString(3);
					description = resultSet.getString(4);

					map.put("id"         , id          != null ? id          : "");
					map.put("name"       , name        != null ? name        : "");
					map.put("command"    , command     != null ? command     : "");
					map.put("description", description != null ? description : "");

					/*-----------------------------------------------------*/

					status = resultSet.getInt(5);

					map.put("running", Integer.toString((status >> 0) & 0b01));
					map.put("exitBit", Integer.toString((status >> 1) & 0b01));

					/*-----------------------------------------------------*/

					result.add(map);
				}
			}
			finally
			{
				resultSet.close();
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
