package net.hep.ami.task;

import java.util.*;

import net.hep.ami.mini.*;

public class Main implements Handler
{
	/*---------------------------------------------------------------------*/

	private static final int s_max_tasks_default = 10;

	private static final float s_compression_default = 1.5f;

	/*---------------------------------------------------------------------*/

	private Scheduler m_scheduler;

	/*---------------------------------------------------------------------*/

	@Override
	public void init(Server server, Map<String, String> config) throws Exception
	{
		String s;

		/*-----------------------------------------------------------------*/
		/* GET CONNECTION INFORMATION                                      */
		/*-----------------------------------------------------------------*/

		String jdbc_url    = config.get("jdbc_url"   );
		String router_user = config.get("router_user");
		String router_pass = config.get("router_pass");
		String server_name = config.get("server_name");

		if(jdbc_url    == null
		   ||
		   router_user == null
		   ||
		   router_pass == null
		   ||
		   server_name == null
		 ) {
			throw new Exception("config error");
		}

		/*-----------------------------------------------------------------*/
		/* LOAD JDBC DRIVER                                                */
		/*-----------------------------------------------------------------*/

		String clazz;

		/**/ if(jdbc_url.startsWith("jdbc:mysql")) {
			clazz = "org.gjt.mm.mysql.Driver";
		}
		else if(jdbc_url.startsWith("jdbc:oracle")) {
			clazz = "oracle.jdbc.driver.OracleDriver";
		}
		else if(jdbc_url.startsWith("jdbc:postgresql")) {
			clazz = "org.postgresql.Driver";
		}
		else if(jdbc_url.startsWith("jdbc:sqlite")) {
			clazz = "org.sqlite.JDBC";
		}
		else {
			throw new Exception("unknown JDBC protocol");
		}

		Class.forName(clazz);

		/*-----------------------------------------------------------------*/
		/* SERVER INFORMATION                                              */
		/*-----------------------------------------------------------------*/

		s = config.get("max_tasks");

		int max_tasks = (s != null) ? Integer.parseInt(s) : s_max_tasks_default;

		if(max_tasks < 1)
		{
			throw new Exception("`max_tasks` out of range");
		}

		/*-----------------------------------------------------------------*/

		s = config.get("compression");

		Float compression = (s != null) ? Float.parseFloat(s) : s_compression_default;

		if(compression < 1.0)
		{
			throw new Exception("`compression` out of range");
		}

		/*-----------------------------------------------------------------*/
		/* RUN SCHEDULER                                                   */
		/*-----------------------------------------------------------------*/

		(m_scheduler = new Scheduler(jdbc_url, router_user, router_pass, server_name, max_tasks, compression)).start();

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	@Override
	public StringBuilder exec(Server server, Map<String, String> config, String command, Map<String, String> arguments, String ip) throws Exception
	{
		StringBuilder result = new StringBuilder();

		result.append("<info><![CDATA[Done with success]]></info>");

		/*-----------------------------------------------------------------*/
		/* GetSessionInfo                                                  */
		/*-----------------------------------------------------------------*/

		/**/ if(command.equals("GetSessionInfo"))
		{
			result.append("<rowset type=\"user\">")
			      .append("<row>")
			      .append("<field name=\"valid\"><![CDATA[true]]></field>")
			      .append("<field name=\"AMIUser\"><![CDATA[admin]]></field>")
			      .append("<field name=\"guestUser\"><![CDATA[guest]]></field>")
			      .append("<field name=\"lastName\"><![CDATA[admin]]></field>")
			      .append("<field name=\"firstName\"><![CDATA[admin]]></field>")
			      .append("<field name=\"email\"><![CDATA[none]]></field>")
			      .append("</row>")
			      .append("</rowset>")
			;
		}

		/*-----------------------------------------------------------------*/
		/* GetTaskStatus                                                   */
		/*-----------------------------------------------------------------*/

		else if(command.equals("GetTasksStatus"))
		{
			result.append("<rowset>");

			for(Map<String, String> map: m_scheduler.getTasksStatus())
			{
				result.append("<row>")
				      .append("<field name=\"id\"><![CDATA[" + map.get("id") + "]]></field>")
				      .append("<field name=\"name\"><![CDATA[" + map.get("name") + "]]></field>")
				      .append("<field name=\"description\"><![CDATA[" + map.get("description") + "]]></field>")
				      .append("<field name=\"running\"><![CDATA[" + map.get("running") + "]]></field>")
				      .append("<field name=\"success\"><![CDATA[" + map.get("success") + "]]></field>")
				      .append("</row>")
				;
			}

			result.append("</rowset>");
		}

		/*-----------------------------------------------------------------*/
		/* StopServer                                                      */
		/*-----------------------------------------------------------------*/

		else if(command.equals("StopServer"))
		{
			server.gracefulStop();
		}

		/*-----------------------------------------------------------------*/

		else
		{
			throw new Exception("Command not found");
		}

		return result;
	}

	/*---------------------------------------------------------------------*/

	@Override
	public StringBuilder help(Server server, Map<String, String> config, String command, Map<String, String> arguments, String ip) throws Exception
	{
		StringBuilder result = new StringBuilder();

		/*-----------------------------------------------------------------*/
		/* GetSessionInfo                                                  */
		/*-----------------------------------------------------------------*/

		/**/ if(command.equals("GetSessionInfo"))
		{
			result.append("Get session info");
		}

		/*-----------------------------------------------------------------*/
		/* GetTaskStatus                                                   */
		/*-----------------------------------------------------------------*/

		else if(command.equals("GetTasksStatus"))
		{
			result.append("Get task status");
		}

		/*-----------------------------------------------------------------*/
		/* StopServer                                                      */
		/*-----------------------------------------------------------------*/

		else if(command.equals("StopServer"))
		{
			result.append("Stop the server");
		}

		/*-----------------------------------------------------------------*/

		else
		{
			throw new Exception("Command not found");
		}

		return result;
	}

	/*---------------------------------------------------------------------*/

	public static void main(String[] args)
	{
		try
		{
			Server server = new Server(args.length == 1 ? Integer.parseInt(args[0]) : 1357, new Main());

			server.start();
			server.join();
		}
		catch(Exception e)
		{
			System.err.println(
				e.getMessage()
			);

			System.exit(1);
		}

		System.exit(0);
	}

	/*---------------------------------------------------------------------*/
}
