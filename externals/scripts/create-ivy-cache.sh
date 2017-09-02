#!/bin/bash
[[ -e /usr/share/ivy/cache ]] && { echo "Cache directory already exists. Exiting." ; exit 0 ; }
mkdir -p /usr/share/ivy/cache
chown ${USER}: /usr/share/ivy/cache
