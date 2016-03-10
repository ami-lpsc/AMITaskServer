AMITaskServer
=============

The ATLAS Metadata Interface Task Server (AMITaskServer) is a generic high level task server. It was originally developed for the A Toroidal LHC ApparatuS (ATLAS) experiment, one of the two general-purpose detectors at the Large Hadron Collider (LHC).

Compiling AMITaskServer
=======================

1. Requierments

  Make sure that [Java 8](http://www.oracle.com/technetwork/java/javase/), [Maven 3](http://maven.apache.org/) and [**AMIMini**](https://github.com/ami-lpsc/AMIMini/) are installed:
	```bash
java -version
mvn -version
```

2. Compiling sources
	```bash
mvn assembly:assembly
```

Generated standalone bundle: *target/AMITaskServer-X.X.X-bundle.zip*

Configuring AMITaskServer
=========================

Example of configuration file (~/.ami/AMI.xml or java -Dami.conffile=path/AMI.xml ...):

	<?xml version="1.0" encoding="ISO-8859-1"?>

	<properties>
	  <property name="jdbc_url"><![CDATA[jdbc:mysql://localhost:3306/router]]></property>
	  <property name="router_user"><![CDATA[router_user]]></property>
	  <property name="router_pass"><![CDATA[router_pass]]></property>

	  <property name="server_name"><![CDATA[server_name]]></property>
	</properties>

Using AMITaskServer
===================

	./AMITaskServer --help
	./AMITaskServer status
	./AMITaskServer start
	./AMITaskServer stop

Install Linux Service
=====================

	./installAMITaskServer.sh
