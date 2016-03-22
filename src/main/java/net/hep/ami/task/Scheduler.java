package net.hep.ami.task;

import java.sql.*;
import java.util.*;
import java.util.Map.*;
import java.util.regex.*;
import java.util.concurrent.*;

public class Scheduler extends Thread
{
	/*---------------------------------------------------------------------*/

	private static final Pattern s_lockSplitPattern = Pattern.compile("[^a-zA-Z0-9_\\-]");

	/*---------------------------------------------------------------------*/

	private String m_serverName;

	private Integer m_maxTasks;

	private float m_compression;

	private Querier m_querier;

	/*---------------------------------------------------------------------*/

	private int m_numberOfPriorities = 0x00;

	private List<Integer> m_priorityTable = null;

	private final Map<String, Task> m_runningTaskMap = new ConcurrentHashMap<>();

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

		m_serverName = serverName;

		m_maxTasks = maxTasks;

		m_compression = compression;

		/*-----------------------------------------------------------------*/
		/* CREATE AND CHECK QUERIER                                        */
		/*-----------------------------------------------------------------*/

		(m_querier = new Querier(jdbcUrl, routerUser, routerPass)).getConnection();

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	@Override
	public void run()
	{
		/*-----------------------------------------------------------------*/
		/* INITIALIZE SCHEDULER                                            */
		/*-----------------------------------------------------------------*/

		try
		{
			removeAllTasks();
		}
		catch(Exception e)
		{
			warn(e.getMessage());
		}

		/*-----------------------------------------------------------------*/
		/* FINALIZE SCHEDULER                                              */
		/*-----------------------------------------------------------------*/

		Runtime.getRuntime().addShutdownHook(new Thread(Scheduler.class.getName()) {

			@Override
			public void run()
			{
				try
				{
					removeAllTasks();
				}
				catch(Exception e)
				{
					warn(e.getMessage());
				}
			}
		});

		/*-----------------------------------------------------------------*/
		/* SCHEDULER LOOP                                                  */
		/*-----------------------------------------------------------------*/

		long i = 0;

		for(;;)
		{
			try
			{
				/*---------------------------------------------------------*/

				sleep(1000L);

				/*---------------------------------------------------------*/

				if(i++ % 30 == 0)
				{
					buildPriorityTable();
				}

				removeFinishTasks();

				startTask();

				/*---------------------------------------------------------*/
			}
			catch(Exception e)
			{
				warn(e.getMessage());
			}
		}

		/*-----------------------------------------------------------------*/
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
		Connection connection = m_querier.getConnection();
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

	private void removeAllTasks() throws Exception
	{
		/*-----------------------------------------------------------------*/

		for(Task task: m_runningTaskMap.values())
		{
			warn("Killing task `" + task.getName() + "`");

			task.destroy();
		}

		/*-----------------------------------------------------------------*/

		Connection connection = m_querier.getConnection();
		Statement statement = connection.createStatement();

		try
		{
			statement.executeUpdate("UPDATE router_task SET running = 0 WHERE serverName = '" + m_serverName.replace("'", "''") + "'");
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

		List<String> toBeRemoved = new ArrayList<>();

		for(Entry<String, Task> entry: m_runningTaskMap.entrySet())
		{
			if(entry.getValue().isAlive() == false)
			{
				toBeRemoved.add(entry.getKey());
			}
		}

		/*-----------------------------------------------------------------*/

		Task task;

		Connection connection = m_querier.getConnection();
		Statement statement = connection.createStatement();

		try
		{
			for(String taskId: toBeRemoved)
			{
				task = m_runningTaskMap.remove(taskId);

				if(task.isSuccess())
				{
					statement.executeUpdate("UPDATE router_task SET running = 0, success = 1 WHERE id = '" + taskId + "'");
				}
				else
				{
					statement.executeUpdate("UPDATE router_task SET running = 0, success = 0 WHERE id = '" + taskId + "'");
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
		public Set<String> lockSet;

		public Tuple(String _id, String _name, String _command, Set<String> _lockSet)
		{
			id = _id;
			name = _name;
			command = _command;
			lockSet = _lockSet;
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

		Connection connection = m_querier.getConnection();
		Statement statement = connection.createStatement();

		try
		{
			/*-------------------------------------------------------------*/
			/* SELECT PRIORITY                                             */
			/*-------------------------------------------------------------*/

			int i = 0;

			String temp;

			Set<String> lockSet;

			java.sql.ResultSet resultSet;

			java.util.List<Tuple> tempTupleList = new ArrayList<>();

			do
			{
				/*---------------------------------------------------------*/

				if(i >= m_numberOfPriorities)
				{
					return;
				}

				i++;

				/*---------------------------------------------------------*/

				sleep(1000L / (2 * m_numberOfPriorities));

				/*---------------------------------------------------------*/

				resultSet = statement.executeQuery("SELECT id, name, command, commaSeparatedLocks FROM router_task WHERE serverName = '" + m_serverName.replace("'", "''") + "' AND running = 0 AND priority = '" + m_priorityTable.get(m_random.nextInt(m_priorityTable.size())) + "' AND (lastRunTime + step) < '" + date.getTime() + "'");

				try
				{
					while(resultSet.next())
					{
						lockSet = new HashSet<>();

						if((temp = resultSet.getString(4)) != null) Collections.addAll(lockSet, s_lockSplitPattern.split(temp));

						if(isLocked(lockSet) == false)
						{
							tempTupleList.add(new Tuple(
								resultSet.getString(1),
								resultSet.getString(2),
								resultSet.getString(3),
								lockSet
							));
						}
					}
				}
				finally
				{
					resultSet.close();
				}

				/*---------------------------------------------------------*/

			} while(tempTupleList.isEmpty());

			/*-------------------------------------------------------------*/
			/* SELECT TASK                                                 */
			/*-------------------------------------------------------------*/

			Tuple tuple = tempTupleList.get(m_random.nextInt(tempTupleList.size()));

			/*-------------------------------------------------------------*/
			/* RUN TASK                                                    */
			/*-------------------------------------------------------------*/

			info("Starting task `" + tuple.name + "`");

			m_runningTaskMap.put(tuple.id, new Task(tuple.id, tuple.name, tuple.command, tuple.lockSet));

			statement.executeUpdate("UPDATE router_task SET running = 1, success = 0, lastRunTime = '" + date.getTime() + "', lastRunDate = '" + net.hep.ami.mini.JettyHandler.s_simpleDateFormat.format(date) + "' WHERE id = '" + tuple.id + "'");

			/*-------------------------------------------------------------*/
		}
		finally
		{
			statement.close();
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private boolean isLocked(Set<String> lockSet)
	{
		for(Task task: m_runningTaskMap.values())
		{
			if(task.isLocked(lockSet))
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
		Connection connection = m_querier.getConnection();
		Statement statement = connection.createStatement();

		List<Map<String, String>> result = new ArrayList<>();

		try
		{
			ResultSet resultSet = statement.executeQuery("SELECT id, name, command, description, commaSeparatedLocks, running, success, priority, step, lastRunDate FROM router_task WHERE serverName = '" + m_serverName.replace("'", "''") + "'");

			Map<String, String> map;

			try
			{
				while(resultSet.next())
				{
					map = new HashMap<>();

					/*-----------------------------------------------------*/

					map.put("id", resultSet.getString(1));
					map.put("name", resultSet.getString(2));
					map.put("command", resultSet.getString(3));
					map.put("description", resultSet.getString(4));
					map.put("commaSeparatedLocks", resultSet.getString(5));
					map.put("running", resultSet.getString(6));
					map.put("success", resultSet.getString(7));
					map.put("priority", resultSet.getString(8));
					map.put("step", resultSet.getString(9));
					map.put("lastRunDate", resultSet.getString(10));

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
