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
		m_url = url;
		m_user = user;
		m_pass = pass;
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
