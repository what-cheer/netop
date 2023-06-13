#!/usr/bin/bash

mkdir dev_pg_data

docker run --rm -d \
       --name dev-postgres \
       -e POSTGRES_PASSWORD=howdy \
       -e PGDATA=/var/lib/postgresql/data/pgdata         \
       -u $(id -u):$(id -g)       \
       -p 15432:5432 \
       -v $(pwd)/dev_pg_data:/var/lib/postgresql/data  \
       postgres:14
