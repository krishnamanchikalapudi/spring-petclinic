#!/bin/bash
arg=${1:-RESTART}
clear
# DOCKER COMPOSE MANAGEMENT
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"


# Spring PetClinic
wait_for_postgres() {
    echo "Waiting for PostgreSQL on 127.0.0.1:5432..."
    for _ in $(seq 1 60); do
        if nc -z 127.0.0.1 5432 >/dev/null 2>&1; then
            echo "PostgreSQL is ready."
            return 0
        fi
        sleep 2
    done

    echo "PostgreSQL did not become ready in time."
    return 1
}

petclinic_start() {
    echo "Starting PostgreSQL container..."
    docker-compose -f "${SCRIPT_DIR}/docker-compose-postgresql.yml" up -d
    wait_for_postgres || exit 1
    echo "Starting Spring PetClinic application..."
        # env \
        #     POSTGRES_URL="${POSTGRES_URL:-jdbc:postgresql://127.0.0.1:5432/petclinic}" \
        #     POSTGRES_USER="${POSTGRES_USER:-petclinic}" \
        #     POSTGRES_PASS="${POSTGRES_PASS:-petclinic}" \
        #     "${ROOT_DIR}/mvnw" spring-boot:run -Dspring-boot.run.profiles=postgres &
}
petclinic_stop() {
    echo "Stopping Spring PetClinic application..."
        "${ROOT_DIR}/mvnw" spring-boot:stop && kill -9 $(lsof -t -i:8080)
    echo "Stopping PostgreSQL container..."
    docker-compose -f "${SCRIPT_DIR}/docker-compose-postgresql.yml" down
}

start(){
    petclinic_start
}
stop(){
    petclinic_stop
}
restart(){
    stop
    sleep 2
    start
}
# uppercase the argument
arg=$(printf '%s' "$arg" | tr '[:lower:]' '[:upper:]' | xargs)
echo "User Action: ${arg}"

case $arg in
    START)
        start
        ;;
    STOP)
        stop
        ;;
    RESTART)
        restart
        ;;
    *)
        echo "Usage: $0 {start|stop|restart}"
        exit 1
        ;;
esac