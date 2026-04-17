#!/bin/bash
arg=${1:-BUILD}
DATE_TIME=`date '+%Y-%m-%d %H:%M:%S'`

# -n string - True if the string length is non-zero.
if [[ -n $arg ]] ; then
    arg_len=${#arg}
    # uppercase the argument
    arg=$(echo ${arg} | tr [a-z] [A-Z] | xargs)
    echo "User Action: ${arg}, and arg length: ${arg_len}"

    if [[ $arg == "BUILD" ]] ; then
        echo "Building the application at ${DATE_TIME}"
        ./mvnw clean package -DskipTests
    elif [[ $arg == "RUN" || $arg == "START" ]] ; then
        echo "Running the application at ${DATE_TIME}"
         ./mvnw spring-boot:run & 
        # java -jar target/spring-petclinic-*.jar
    elif [[ $arg == "STOP" || $arg == "CLEAN" ]] ; then
        echo "Stopping the application at ${DATE_TIME}"
        ./mvnw spring-boot:stop && kill -9 $(lsof -t -i:8080) && curl -X POST http://localhost:8080/actuator/shutdown 
        echo "Application stopped successfully."
    else
        echo "Invalid argument: ${arg}. Please use BUILD or RUN."
    fi
else
    echo "No argument provided. Please use BUILD or RUN."
fi
