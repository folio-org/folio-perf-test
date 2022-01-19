docker run -d --rm --name ${modName} --sysctl net.ipv6.conf.all.disable_ipv6=1 -e 'EBSCO_RMAPI_BASE_URL=https://sandbox.ebsco.io'  -p${port}:8081 folioci/${modName}:${modVer}
