AMITaskServer
=============

The ATLAS Metadata Interface Task Server (AMITaskServer) is a generic high level task server. It was originally developed for the A Toroidal LHC ApparatuS (ATLAS) experiment, one of the two general-purpose detectors at the Large Hadron Collider (LHC).

Compiling AMITaskServer
=======================

1. Requirements

  Make sure that [Java 8](http://www.oracle.com/technetwork/java/javase/), [Maven 3](http://maven.apache.org/) and [*AMIMini*](https://github.com/ami-lpsc/AMIMini/) are installed:
	```bash
java -version
mvn -version
```

2. Compiling sources
	```bash
mvn assembly:assembly
```

Generated standalone bundle: *target/AMITaskServer-X.X.X-bundle.zip*

Task SQL table
==============

Supported SGBD: MySQL, Oracle, PostgreSQL, SQLite. Schema for MySQL:

	CREATE TABLE "router_task" (
	  "id" int(11) NOT NULL,
	  "name" varchar(128) NOT NULL,
	  "command" varchar(1024) NOT NULL,
	  "description" varchar(512) DEFAULT NULL,
	  "commaSeparatedLocks" varchar(512) DEFAULT NULL,
	  "serverName" varchar(128) NOT NULL,
	  "running" int(1) NOT NULL DEFAULT '0',
	  "success" int(1) NOT NULL DEFAULT '0',
	  "priority" int(3) NOT NULL DEFAULT '0',
	  "step" bigint(20) NOT NULL DEFAULT '0',
	  "lastRunTime" bigint(20) NOT NULL DEFAULT '0',
	  "lastRunDate" datetime NOT NULL DEFAULT '0000-00-00 00:00:00'
	);

Configuring AMITaskServer
=========================

Example of configuration file (~/.ami/AMI.xml, /etc/ami/AMI.xml or java -Dami.conffile=path/AMI.xml ...):

	<?xml version="1.0" encoding="ISO-8859-1"?>

	<properties>
	  <property name="jdbc_url"><![CDATA[jdbc:mysql://localhost:3306/router]]></property>
	  <property name="router_user"><![CDATA[router_user]]></property>
	  <property name="router_pass"><![CDATA[router_pass]]></property>

	  <property name="server_name"><![CDATA[server_name]]></property>

	  <!--
	  <property name="ips"><![CDATA[ip1, ip2, ...]]></property>
	  -->
	</properties>

Using AMITaskServer
===================

	./AMITaskServer --help
	./AMITaskServer status
	./AMITaskServer start
	./AMITaskServer stop

Install Linux service
=====================

	./installAMITaskServer.sh
