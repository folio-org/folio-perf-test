docker run -d --rm --name foliodb -e POSTGRES_USER=folio -e POSTGRES_PASSWORD=folioadmin -c max_connections=1000 -p5432:5432 postgres:10
