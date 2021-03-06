#!/bin/bash

PORT=1357
JAVA_HOME=/opt/java/latest

#############################################################################

THIS_SCRIPT=${BASH_SOURCE[0]:-$0}

while [[ -n $(readlink $THIS_SCRIPT) ]]
do
  THIS_SCRIPT=$(readlink $THIS_SCRIPT)
done

AMI_HOME=$(cd $(dirname $THIS_SCRIPT) && pwd)

#############################################################################

if [[ -z $(ps -ef | grep "net\.hep\.ami\.task\.Main") ]]
then
  ###########################################################################

  AMICLASSPATH=$AMI_HOME/classes

  for jar in $AMI_HOME/lib/*.jar
  do
    AMICLASSPATH=$AMICLASSPATH:$jar
  done

  ###########################################################################

  if [[ -f $AMI_HOME/log/AMITaskServer.out ]]
  then
    mv $AMI_HOME/log/AMITaskServer.out $AMI_HOME/log/AMITaskServer.$(date +%Y-%m-%d_%Hh%Mm%Ss).out
  fi

  ###########################################################################
  (

    cd $AMI_HOME

    $JAVA_HOME/bin/java -Dfile.encoding=UTF-8 -Djava.awt.headless=true -Djava.security.egd=file:/dev/./urandom -Dami.conffile=$AMI_HOME/AMI.xml -classpath $AMICLASSPATH net.hep.ami.task.Main $PORT &> $AMI_HOME/log/AMITaskServer.out &

  )
  ###########################################################################
fi

#############################################################################
