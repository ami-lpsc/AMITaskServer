package net.hep.ami.task;

import java.sql.*;

public class Querier
{
	/*---------------------------------------------------------------------*/

	private String m_url;
	private String m_user;
	private String m_pass;

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

		/**/ if(url.startsWith("jdbc:mysql")) {
			clazz = "org.gjt.mm.mysql.Driver";
		}
		else if(url.startsWith("jdbc:oracle")) {
			clazz = "oracle.jdbc.driver.OracleDriver";
		}
		else if(url.startsWith("jdbc:postgresql")) {
			clazz = "org.postgresql.Driver";
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

	public Connection getConnection() throws SQLException
	{
		if(m_connection == null || m_connection.isClosed())
		{
			m_connection = DriverManager.getConnection(
				m_url,
				m_user,
				m_pass
			);
		}

		return m_connection;
	}

	/*---------------------------------------------------------------------*/
}
