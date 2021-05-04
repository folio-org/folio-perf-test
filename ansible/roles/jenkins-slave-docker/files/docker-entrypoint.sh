#!/bin/bash

set -e

# Update the gid of the docker group so that we 
# have permission to write to the mounted socket.
groupmod -g $(stat -c "%g" /var/run/docker.sock) docker 

# in this case, we need the uid of the host system volume we want to 
# write to match the uid in the container. 
if [ -d /apt-repo/binary ]; then
  usermod -u $(stat -c "%u" /apt-repo/binary) $USER
fi

# fix perms on files in $USER's homedir
chown -R ${USER}.${USER} /home/${USER}

# start supervisord
/usr/bin/supervisord
