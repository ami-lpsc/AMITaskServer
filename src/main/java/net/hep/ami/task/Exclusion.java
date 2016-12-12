package net.hep.ami.task;

import java.io.*;
import java.net.*;

public class Exclusion
{
	/*---------------------------------------------------------------------*/

	private Exclusion() {}

	/*---------------------------------------------------------------------*/

	private static boolean exec(String spec) throws Exception
	{
		/*-----------------------------------------------------------------*/

		HttpURLConnection connection = (HttpURLConnection) new URL(spec).openConnection();

		connection.setRequestMethod("GET");

		connection.connect();

		/*-----------------------------------------------------------------*/

		try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
		{
			return connection.getResponseCode() == 200 && bufferedReader.readLine().equals("0");
		}
		finally
		{
			connection.disconnect();
		}

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	public static boolean lock(String endpoint, String server, String commaSeparatedLocks) throws Exception
	{
		if(endpoint.isEmpty()
		   ||
		   commaSeparatedLocks.isEmpty()
		 ) {
			return true;
		}

		return exec(endpoint + "/?Command=Lock," + server + "," + commaSeparatedLocks);
	}

	/*---------------------------------------------------------------------*/

	public static boolean unlock(String endpoint, String server, String commaSeparatedLocks) throws Exception
	{
		if(endpoint.isEmpty()
		   ||
		   commaSeparatedLocks.isEmpty()
		 ) {
			return true;
		}

		return exec(endpoint + "/?Command=Unlock," + server + "," + commaSeparatedLocks);
	}

	/*---------------------------------------------------------------------*/

	public static boolean unlockAll(String endpoint, String server) throws Exception
	{
		if(endpoint.isEmpty())
		{
			return true;
		}

		return exec(endpoint + "/?Command=UnlockAll," + server);
	}

	/*---------------------------------------------------------------------*/
}
