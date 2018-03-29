#!/bin/sh
# chkconfig: 6 10 90
# description: start Service
nohup /usr/local/playframework/play run /usr/local/playframework/makefriends --%prod > output 2>&1 &