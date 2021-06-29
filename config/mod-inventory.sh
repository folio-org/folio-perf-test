docker run -d --rm --name ${modName} --mount type=bind,source=/tmp/folio-conf,target=/folio-conf -e 'KAFKA_HOST=${dbHost}' -e 'KAFKA_PORT=9092' -e 'JAVA_OPTIONS=-Dorg.folio.metadata.inventory.storage.type=okapi' -p${port}:9403 folioci/${modName}:${modVer} db_connection=/folio-conf/pg.json
