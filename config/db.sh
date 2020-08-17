docker run -d --rm --name foliodb -e POSTGRES_USER=folio -e POSTGRES_PASSWORD=folioadmin -p5432:5432 postgres:10 -N 1000
