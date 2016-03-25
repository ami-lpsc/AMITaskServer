#!/bin/bash

PORT=1357

#############################################################################

curl http://localhost:$PORT/?Command=StopServer &> /dev/null

#############################################################################

n=0

while [[ -n $(ps -ef | grep "net\.hep\.ami\.task\.Main") && $n -lt 30 ]]
do
  n=$((n+1))
  printf '.'
  sleep 1
done

#############################################################################

if [[ -n $(ps -ef | grep "net\.hep\.ami\.task\.Main") ]]
then
  ps -ef | grep "net\.hep\.ami\.task\.Main" | awk '{print $2}' | xargs kill

  echo 'Task server killed'
else
  echo 'Task server stopped'
fi

#############################################################################
