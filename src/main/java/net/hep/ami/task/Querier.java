package net.hep.ami.task;

import java.sql.*;

public class Querier
{
	/*---------------------------------------------------------------------*/

	private final String m_url;
	private final String m_user;
	private final String m_pass;

	/*---------------------------------------------------------------------*/

	private Connection m_connection = null;

	/*---------------------------------------------------------------------*/

	public Querier(String url, String user, String pass) throws ClassNotFoundException, SQLException
	{
		/*-----------------------------------------------------------------*/
		/* SET INSTANCE VARIABLES                                          */
		/*-----------------------------------------------------------------*/

		m_url = url;
		m_user = user;
		m_pass = pass;

		/*-----------------------------------------------------------------*/
		/* LOAD JDBC DRIVER                                                */
		/*-----------------------------------------------------------------*/

		String clazz;

		/**/ if(url.startsWith("jdbc:oracle")) {
			clazz = "oracle.jdbc.driver.OracleDriver";
		}
		else if(url.startsWith("jdbc:postgresql")) {
			clazz = "org.postgresql.Driver";
		}
		else if(url.startsWith("jdbc:mysql")) {
			clazz = "com.mysql.cj.jdbc.Driver";
		}
		else if(url.startsWith("jdbc:mariadb")) {
			clazz = "org.mariadb.jdbc.Driver";
		}
		else if(url.startsWith("jdbc:sqlite")) {
			clazz = "org.sqlite.JDBC";
		}
		else {
			throw new ClassNotFoundException("unknown JDBC driver");
		}

		/*-----------------------------------------------------------------*/

		Class.forName(clazz);

		/*-----------------------------------------------------------------*/
	}

	/*---------------------------------------------------------------------*/

	public Connection createConnection() throws SQLException
	{
		if(m_connection == null || m_connection.isClosed() != false)
		{
			m_connection = DriverManager.getConnection(
				m_url,
				m_user,
				m_pass
			);

			m_connection.setAutoCommit(false);
		}

		return m_connection;
	}

	/*---------------------------------------------------------------------*/

	public void close() throws SQLException
	{
		if(m_connection != null && m_connection.isClosed() == false)
		{
			m_connection.close();
		}
	}

	/*---------------------------------------------------------------------*/
}
