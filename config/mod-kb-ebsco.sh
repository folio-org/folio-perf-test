docker run -d --rm --name ${modName} -e 'EBSCO_RMAPI_BASE_URL=https://sandbox.ebsco.io'  -p${port}:8081 folioci/${modName}:${modVer}
