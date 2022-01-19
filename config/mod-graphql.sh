docker run -d --rm --name ${modName} --sysctl net.ipv6.conf.all.disable_ipv6=1 -p${port}:3001 folioci/${modName}:${modVer}
