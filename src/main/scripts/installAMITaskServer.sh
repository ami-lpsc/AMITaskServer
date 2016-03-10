#!/bin/bash

#############################################################################

THIS_SCRIPT=${BASH_SOURCE[0]:-$0}

while [[ -n $(readlink $THIS_SCRIPT) ]]
do
  THIS_SCRIPT=$(readlink $THIS_SCRIPT)
done

AMI_HOME=$(cd $(dirname $THIS_SCRIPT) && pwd)

#############################################################################

if [[ $(uname -s) == 'Linux' ]]
then
  ###########################################################################

  rm -f /etc/init.d/AMITaskServer

  ln -s $AMI_HOME/AMITaskServer /etc/init.d/AMITaskServer

  ###########################################################################

  chkconfig --add AMITaskServer
  chkconfig AMITaskServer on

  ###########################################################################
else
  echo 'Only for Linux'
fi

#############################################################################
