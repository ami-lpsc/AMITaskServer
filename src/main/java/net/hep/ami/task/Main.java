package net.hep.ami.task;

import java.util.*;
import java.util.regex.*;

import net.hep.ami.mini.*;

public class Main implements Handler
{
	/*---------------------------------------------------------------------*/

	private static final int s_maxTasksDefault = 10;

	private static final float s_compressionDefault = 2.0f;

	private static final Pattern s_ipSplitPattern = Pattern.compile("[^0-9\\.]");

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

		String jdbcUrl    = config.get("jdbc_url"   );
		String routerUser = config.get("router_user");
		String routerPass = config.get("router_pass");

		String serverName = config.get("server_name");

		if(jdbcUrl    == null
		   ||
		   routerUser == null
		   ||
		   routerPass == null
		   ||
		   serverName == null
		 ) {
			throw new Exception("config error");
		}

		/*-----------------------------------------------------------------*/
		/* SERVER INFORMATION                                              */
		/*-----------------------------------------------------------------*/

		s = config.get("max_tasks");

		int maxTasks = (s != null) ? Integer.parseInt(s) : s_maxTasksDefault;

		if(maxTasks < 1)
		{
			throw new Exception("`max_tasks` out of range");
		}

		/*-----------------------------------------------------------------*/

		s = config.get("compression");

		Float compression = (s != null) ? Float.parseFloat(s) : s_compressionDefault;

		if(compression < 1.0)
		{
			throw new Exception("`compression` out of range");
		}

		/*-----------------------------------------------------------------*/
		/* RUN SCHEDULER                                                   */
		/*-----------------------------------------------------------------*/

		(m_scheduler = new Scheduler(jdbcUrl, routerUser, routerPass, serverName, maxTasks, compression)).start();

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
			checkIP(config, ip);

			result.append("<rowset>");

			for(Map<String, String> map: m_scheduler.getTasksStatus())
			{
				result.append("<row>")
				      .append("<field name=\"id\"><![CDATA[" + map.get("id") + "]]></field>")
				      .append("<field name=\"name\"><![CDATA[" + map.get("name") + "]]></field>")
				      .append("<field name=\"command\"><![CDATA[" + map.get("command") + "]]></field>")
				      .append("<field name=\"description\"><![CDATA[" + map.get("description") + "]]></field>")
				      .append("<field name=\"running\"><![CDATA[" + map.get("running") + "]]></field>")
				      .append("<field name=\"exitBit\"><![CDATA[" + map.get("exitBit") + "]]></field>")
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
			checkIP(config, ip);

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
			checkIP(config, ip);

			result.append("Get task status");
		}

		/*-----------------------------------------------------------------*/
		/* StopServer                                                      */
		/*-----------------------------------------------------------------*/

		else if(command.equals("StopServer"))
		{
			checkIP(config, ip);

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

	private void checkIP(Map<String, String> config, String ip) throws Exception
	{
		/*-----------------------------------------------------------------*/

		String ips = config.get("ips");

		if(ips == null)
		{
			return;
		}

		/*-----------------------------------------------------------------*/

		String[] IPS = s_ipSplitPattern.split(ips);

		for(String IP: IPS)
		{
			if(ip.equals(IP))
			{
				return;
			}
		}

		/*-----------------------------------------------------------------*/

		throw new Exception("User not allowed");

		/*-----------------------------------------------------------------*/
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
