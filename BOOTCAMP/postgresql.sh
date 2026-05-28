#!/bin/bash
arg=${1:-start}

start(){
    # https://hub.docker.com/_/postgres
    # docker run --name postgres -e POSTGRES_DB=petclinic -e POSTGRES_USER=petclinic -e POSTGRES_PASSWORD=petclinic -d -p 5432:5432 -v ./BOOTCAMP/test-data/postgres.sql:/docker-entrypoint-initdb.d/postgres.sql postgres:18

    # https://hub.docker.com/r/pgvector/pgvector
    docker run --name pgvector -e POSTGRES_DB=petclinic -e POSTGRES_USER=petclinic -e POSTGRES_PASSWORD=petclinic -d -p 5432:5432 -v ./BOOTCAMP/test-data/postgres.sql:/docker-entrypoint-initdb.d/postgres.sql pgvector/pgvector:pg18

    
}
test(){ 
    docker exec -it pgvector psql -U petclinic -c "SELECT version();"
}
stop(){
    docker stop postgres
    docker stop pgvector
    docker rm -f postgres >/dev/null 2>&1 || true
    docker rm -f pgvector >/dev/null 2>&1 || true
}

case $arg in
    start)
        start
        sleep 5
        test
        ;;
    stop)
        stop
        ;;
    *)
        echo "Usage: $0 {start|stop}"
        exit 1
esac