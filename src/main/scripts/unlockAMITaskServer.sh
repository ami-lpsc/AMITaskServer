#!/bin/bash

PORT=1357

#############################################################################

curl http://localhost:$PORT/?Command=UnlockScheduler &> /dev/null

#############################################################################

sleep 1

#############################################################################
