docker run -d --rm --name foliodb --sysctl net.ipv6.conf.all.disable_ipv6=1 -e POSTGRES_USER=folio -e POSTGRES_PASSWORD=folioadmin -p5432:5432 postgres:12 -N 1000
