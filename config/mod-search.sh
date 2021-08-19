docker run -d --rm --name ${modName} -e 'DB_HOST=${dbHost}' -e 'ENV=folio' -e 'DB_PORT=5432' -e 'DB_DATABASE=folio' -e 'DB_USERNAME=folio' -e 'DB_PASSWORD=folioadmin' -e 'KAFKA_HOST=${dbHost}' -e 'KAFKA_PORT=9092' -e 'ELASTICSEARCH_HOST=${dbHost}' -e 'ELASTICSEARCH_PASSWORD=s3cret' -e 'OKAPI_URL=http://${okapiIp}:9130' -p${port}:8081 folioci/${modName}:${modVer}
