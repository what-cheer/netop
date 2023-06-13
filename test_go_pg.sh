#!/usr/bin/bash

docker run --rm -d \
       --name test-postgres \
       -e POSTGRES_PASSWORD=howdy \
       -p 25432:5432 \
       postgres:14