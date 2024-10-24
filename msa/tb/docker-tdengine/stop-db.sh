#!/bin/bash

ps ef | grep 'taos' | awk '{print $2}' | xargs kill

PG_CTL=$(find /usr/lib/postgresql/ -name pg_ctl)
echo "Stopping postgres."
${PG_CTL} stop