[![][Build Status img]][Build Status]
[![][Dependency Status img]][Dependency Status]
[![][license img]][license]

AMITaskServer
=============

The ATLAS Metadata Interface Task Server (AMITaskServer) is a generic high level time-based job scheduler. It was originally developed for the A Toroidal LHC ApparatuS (ATLAS) experiment, one of the two general-purpose detectors at the Large Hadron Collider (LHC).

Compiling AMITaskServer
=======================

1. Requirements

  Make sure that [Java 8](http://www.oracle.com/technetwork/java/javase/), [Maven 3](http://maven.apache.org/) and [*AMIMini*](https://github.com/ami-team/AMIMini/) are installed:
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

AMITaskServer need a SQL table in order to store job definitions. Supported SGBD: MySQL, Oracle, PostgreSQL, SQLite.

Schema for MySQL 5.0.3 and later:

	CREATE TABLE `router_task` (
	  `id` INT(11) NOT NULL,
	  `name` VARCHAR(128) NOT NULL,
	  `command` VARCHAR(1024) NOT NULL,
	  `description` VARCHAR(512) DEFAULT NULL,
	  `commaSeparatedLocks` VARCHAR(512) DEFAULT NULL,
	  `serverName` VARCHAR(128) NOT NULL,
	  `running` INT(1) DEFAULT '0' NOT NULL,
	  `success` INT(1) DEFAULT '0' NOT NULL,
	  `priority` INT(3) DEFAULT '0' NOT NULL,
	  `step` BIGINT(20) DEFAULT '0' NOT NULL,
	  `lastRunTime` BIGINT(20) DEFAULT '0' NOT NULL,
	  `lastRunDate` DATETIME DEFAULT '1979-01-01 00:00:00' NOT NULL
	);

Schema for Oracle 11c and later:

	CREATE TABLE "router_task" (
	  "id" NUMBER(11) NOT NULL,
	  "name" VARCHAR2(128) NOT NULL,
	  "command" VARCHAR2(1024) NOT NULL,
	  "description" VARCHAR2(512) DEFAULT NULL,
	  "commaSeparatedLocks" VARCHAR2(512) DEFAULT NULL,
	  "serverName" VARCHAR2(128) NOT NULL,
	  "running" NUMBER(1) DEFAULT '0' NOT NULL,
	  "success" NUMBER(1) DEFAULT '0' NOT NULL,
	  "priority" NUMBER(3) DEFAULT '0' NOT NULL,
	  "step" NUMBER(20) DEFAULT '0' NOT NULL,
	  "lastRunTime" NUMBER(20) DEFAULT '0' NOT NULL,
	  "lastRunDate" TIMESTAMP(0) DEFAULT TO_TIMESTAMP('1979-01-01 00:00:00', 'YYYY-MM-DD HH24:MI:SS') NOT NULL
	);

Configuring AMITaskServer
=========================

Configuration file template (~/.ami/AMI.xml, /etc/ami/AMI.xml or java -Dami.conffile=path/AMI.xml ...):

	<?xml version="1.0" encoding="ISO-8859-1"?>

	<properties>
	  <property name="jdbc_url"><![CDATA[jdbc_url]]></property>
	  <property name="router_user"><![CDATA[router_user]]></property>
	  <property name="router_pass"><![CDATA[router_pass]]></property>

	  <property name="exclusion_server_url"><![CDATA[exclusion_server_url]]></property>
  
	  <property name="server_name"><![CDATA[server_name]]></property>

	  <!--
	  <property name="ips"><![CDATA[ip1, ip2, ...]]></property>
	  -->
	</properties>

Using AMITaskServer
===================

	./AMITaskServer start
	./AMITaskServer stop
	./AMITaskServer lock
	./AMITaskServer unlock
	./AMITaskServer status
	./AMITaskServer --help

Install as Linux service
========================

	./installAMITaskServer.sh

	service AMITaskServer start
	service AMITaskServer stop
	service AMITaskServer lock
	service AMITaskServer unlock
	service AMITaskServer status

[Build Status]:https://travis-ci.org/ami-lpsc/AMIExclusionServer/
[Build Status img]:https://api.travis-ci.org/ami-lpsc/AMIExclusionServer.svg?branch=master

[Dependency Status]:https://www.versioneye.com/user/projects/584f35a661ff4a003d3c3964/
[Dependency Status img]:https://www.versioneye.com/user/projects/584f35a661ff4a003d3c3964/badge.svg?style=flat

[license]:http://www.cecill.info/
[license img]:https://img.shields.io/badge/license-CeCILL-blue.svg
