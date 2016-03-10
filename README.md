AMITaskServer
=============

Compiling AMITaskServer
=======================

1. Requierments

  Make sure that [Java 8](http://www.oracle.com/technetwork/java/javase/) and [Maven 3](http://maven.apache.org/) are installed:
	```bash
java -version
mvn -version
```

2. Compiling sources
	```bash
mvn install
```

Configuring AMITaskServer
=========================

Example of configuration file (~/.ami/AMI.xml):

	<?xml version="1.0" encoding="ISO-8859-1"?>

	<properties>
	  <property name="jdbc_url"><![CDATA[jdbc:mysql://localhost:3306/router]]></property>
	  <property name="router_user"><![CDATA[router_user]]></property>
	  <property name="router_pass"><![CDATA[router_pass]]></property>

	  <property name="server_name"><![CDATA[server_name]]></property>
	</properties>
