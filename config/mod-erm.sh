docker run -d --rm --name ${modName} -e 'DB_HOST=${dbHost}' -e 'DB_DATABASE=folio' -e 'DB_USERNAME=folio' -e 'DB_PASSWORD=folioadmin' -p${port}:8080 folioci/${modName}:${modVer}
