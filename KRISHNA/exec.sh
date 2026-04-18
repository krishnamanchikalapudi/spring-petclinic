#!/bin/bash
arg=${1:-START}
DATE_TIME=`date '+%Y-%m-%d %H:%M:%S'`

stop_app(){
    echo "Stopping the application at ${DATE_TIME}"
    ./mvnw spring-boot:stop && kill -9 $(lsof -t -i:8080)
    LOCAL_HOST="http://localhost:8080/"
    SHUTDOWN_URL="${LOCAL_HOST}actuator/shutdown"
    curl -s -f  POST ${SHUTDOWN_URL} > /dev/null 2>&1
    # Capture the exit status
    EXIT_CODE=$?
    if [ $EXIT_CODE -eq 7 ]; then
        printf "\n ** Spring-Petclinic service OFFLINE \n ** Port 8080 is not in use\n"
    elif [ $EXIT_CODE -eq 0 ]; then
        printf "\n ** Spring-Petclinic service ONLINE \n ** Port 8080 is in use\n"
    else
        printf "\n ** An unexpected error occurred (Code: $EXIT_CODE) \n"
    fi
    echo " "
}
format_code(){
    echo "Formatting the application at ${DATE_TIME}"
    ./mvnw spring-javaformat:apply
}
clean_files(){
    find . -name ".DS_Store" -type f -delete
    find . -name "Thumbs.db" -type f -delete
}
run_app(){
    echo "Running the application at ${DATE_TIME}"
    ./mvnw spring-boot:run &
}
package_app(){
    echo "Packaging the application at ${DATE_TIME}"
    ./mvnw clean package -DskipTests
}

# -n string - True if the string length is non-zero.
if [[ -n $arg ]] ; then
    arg_len=${#arg}
    # uppercase the argument
    arg=$(echo ${arg} | tr [a-z] [A-Z] | xargs)
    echo "User Action: ${arg}, and arg length: ${arg_len}"

    if [[ $arg == "BUILD" ]] ; then
        package_app
    elif [[ $arg == "RUN" ]] ; then
        run_app
        # java -jar target/spring-petclinic-*.jar
    elif [[ $arg == "START" || $arg == "RESTART" ]] ; then
        format_code && stop_app
        package_app && run_app
    elif [[ $arg == "STOP" || $arg == "CLEAN" ]] ; then
        clean_files && stop_app
    elif [[ $arg == "FORMAT" ]] ; then
        format_code
    else
        echo "Invalid argument: ${arg}. Please use BUILD or RUN."
    fi
else
    echo "No argument provided. Please use BUILD or RUN."
fi
