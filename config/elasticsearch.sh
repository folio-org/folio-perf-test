docker run -d --name elasticsearch -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "ELASTIC_PASSWORD=s3cret" docker.dev.folio.org/folio-elasticsearch:7.10.1
