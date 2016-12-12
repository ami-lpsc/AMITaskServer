package net.hep.ami.task;

import java.sql.*;
import java.text.*;
import java.util.*;
import java.util.Map.*;
import java.util.logging.*;

public class Scheduler extends Thread
{
	/*---------------------------------------------------------------------*/

	private static final Logger s_logger = Logger.getLogger(
		Scheduler.class.getName()
	);

	/*---------------------------------------------------------------------*/

	private final String m_exclusionServerUrl;

	private final String m_serverName;

	private final Integer m_maxTasks;

	private final float m_compression;

	/*---------------------------------------------------------------------*/

	private final Querier m_querier;

	/*---------------------------------------------------------------------*/

	private int m_numberOfPriorities = 0x00;

	private List<Integer> m_priorityTable = null;

	private final Map<String, Task> m_runningTaskMap = new HashMap<>();

	/*---------------------------------------------------------------------*/

	public Scheduler(String jdbcUrl, String routerUser, String routerPass, String exclusionServerUrl, String serverName, int maxTasks, float compression) throws Exception
	{
		/*-----------------------------------------------------------------*/
		/* SUPER CONSTRUCTOR                                               */
		/*-----------------------------------------------------------------*/

		super(Scheduler.class.getName());

		/*-----------------------------------------------------------------*/
		/* SET INSTANCE VARIABLES                                          */
		/*-----------------------------------------------------------------*/

		m_exclusionServerUrl = exclusionServerUrl;

		m_serverName = serverName;

		m_maxTasks = maxTasks;

		m_compression = compression;

		/*-----------------------------------------------------------------*/
		/* CREATE AND CHECK QUERIER                                        */
		/*-----------------------------------------------------------------*/

		(m_querier = new Querier(jdbcUrl, routerUser, routerPass)).createConnection();

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
			Exclusion.unlockAll(m_exclusionServerUrl, m_serverName);
		}
		catch(Exception e)
		{
			s_logger.log(Level.SEVERE, e.getMessage(), e);
		}

		try
		{
			removeAllTasks();
		}
		catch(Exception e)
		{
			s_logger.log(Level.SEVERE, e.getMessage(), e);
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
					s_logger.log(Level.SEVERE, e.getMessage(), e);
				}

				try
				{
					Exclusion.unlockAll(m_exclusionServerUrl, m_serverName);
				}
				catch(Exception e)
				{
					s_logger.log(Level.SEVERE, e.getMessage(), e);
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

				Thread.sleep(1000L);

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
				s_logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private void buildPriorityTable() throws Exception
	{
		Connection connection = m_querier.createConnection();
		Statement statement = connection.createStatement();

		try
		{
			ResultSet resultSet = statement.executeQuery("SELECT MAX(priority) + 1 FROM router_task WHERE serverName = '" + m_serverName.replace("'", "''") + "'");

			try
			{
				m_numberOfPriorities = resultSet.next() ? resultSet.getInt(1)
				                                        : 0x00000000000000000
				;

				m_priorityTable = PriorityTableBuilder.build(
					m_numberOfPriorities
					,
					m_compression
				);
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
			s_logger.log(Level.WARNING, "Killing task `" + task.getName() + "`");

			task.destroy();
		}

		/*-----------------------------------------------------------------*/

		Connection connection = m_querier.createConnection();
		Statement statement = connection.createStatement();

		try
		{
			statement.executeUpdate("UPDATE router_task SET running = 0 WHERE serverName = '" + m_serverName.replace("'", "''") + "'");

			connection.commit();
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

		Connection connection = m_querier.createConnection();
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

				Exclusion.unlock(m_exclusionServerUrl, m_serverName, task.getCommaSeparatedLocks());

				s_logger.log(Level.INFO, "Task `" + task.getName() + "` finished");
			}

			connection.commit();
		}
		finally
		{
			statement.close();
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	private Random m_random = new Random();

	private volatile boolean m_schedulerLock = false;

	private SimpleDateFormat m_simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

	/*---------------------------------------------------------------------*/

	private void startTask() throws Exception
	{
		/*-----------------------------------------------------------------*/

		if(m_schedulerLock || m_numberOfPriorities == 0 || m_runningTaskMap.size() >= m_maxTasks)
		{
			return;
		}

		/*-----------------------------------------------------------------*/

		java.util.Date date = new java.util.Date();

		Connection connection = m_querier.createConnection();
		Statement statement = connection.createStatement();

		try
		{
			String id = "";
			String name = "";
			String command = "";
			String commaSeparatedLocks = "";

			/*-------------------------------------------------------------*/
			/* SELECT PRIORITY                                             */
			/*-------------------------------------------------------------*/

			ResultSet resultSet;

			boolean taskFound = false;

			for(int i = 0; i < 10; i++)
			{
				/*---------------------------------------------------------*/

				resultSet = statement.executeQuery("SELECT id, name, command, commaSeparatedLocks FROM router_task WHERE serverName = '" + m_serverName.replace("'", "''") + "' AND running = 0 AND priority = '" + m_priorityTable.get(m_random.nextInt(m_priorityTable.size())) + "' AND (lastRunTime + step) < '" + date.getTime() + "' ORDER BY RAND()");

				try
				{
					while(resultSet.next())
					{
						id = resultSet.getString(1);
						name = resultSet.getString(2);
						command = resultSet.getString(3);
						commaSeparatedLocks = resultSet.getString(4);

						if(Exclusion.lock(m_exclusionServerUrl, m_serverName, commaSeparatedLocks))
						{
							taskFound = true;

							break;
						}
					}
				}
				finally
				{
					resultSet.close();
				}

				/*---------------------------------------------------------*/

				Thread.sleep(100L);

				/*---------------------------------------------------------*/
			}

			/*-------------------------------------------------------------*/
			/* RUN TASK                                                    */
			/*-------------------------------------------------------------*/

			if(taskFound)
			{
				s_logger.log(Level.INFO, "Starting task `" + name + "`");

				m_runningTaskMap.put(id, new Task(id, name, command, commaSeparatedLocks));

				statement.executeUpdate("UPDATE router_task SET running = 1, success = 0, lastRunTime = '" + date.getTime() + "', lastRunDate = '" + m_simpleDateFormat.format(date) + "' WHERE id = '" + id + "'");

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
	/*---------------------------------------------------------------------*/

	public List<Map<String, String>> getTasksStatus() throws Exception
	{
		Connection connection = m_querier.createConnection();
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

	public void lock()
	{
		m_schedulerLock = true;

		try
		{
			Thread.sleep(500L);
		}
		catch(InterruptedException e)
		{
			/* IGNORE */
		}

		s_logger.log(Level.INFO, "Scheduler locked");
	}

	/*---------------------------------------------------------------------*/
	/*---------------------------------------------------------------------*/

	public void unlock()
	{
		m_schedulerLock = false;

		try
		{
			Thread.sleep(500L);
		}
		catch(InterruptedException e)
		{
			/* IGNORE */
		}

		s_logger.log(Level.INFO, "Scheduler unlocked");
	}

	/*---------------------------------------------------------------------*/
	/*---------------------------------------------------------------------*/
}
