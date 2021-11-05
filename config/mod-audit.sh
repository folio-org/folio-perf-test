docker run -d --rm --name ${modName} --mount type=bind,source=/tmp/folio-conf,target=/folio-conf -e 'OKAPI_URL=http://${okapiIp}:9130' -e 'ENV=${envName}' -e 'DB_HOST=${dbHost}' -e 'DB_PORT=5432' -e 'DB_DATABASE=folio' -e 'DB_USERNAME=folio' -e 'DB_PASSWORD=folioadmin' -e 'JAVA_OPTIONS=-Dorg.folio.metadata.inventory.storage.type=okapi' -p${port}:8081 folioci/${modName}:${modVer} db_connection=/folio-conf/pg.json 
