#!/bin/bash
arg=${1:-START}
DATE_TIME=`date '+%Y-%m-%d %H:%M:%S'`

APP_YAML="./k8s/ollama.yml"
NAMESPACE="ollama-ns"

ollama_deploy(){
    mkdir -p ~/ollama-models
    echo "Deploying the OLLAMA service at ${DATE_TIME}"

    kubectl apply -f ${APP_YAML} --record=true --validate=true
    minikube dashboard &

    # Check Job logs
    kubectl logs -n ollama-ns -f job/ollama-pull-qwen3.5

    # Verify it's on your Mac
    ls ~/ollama-models/models/manifests/registry.ollama.ai/library/qwen3.5/

    # Port-forward to use it
    kubectl port-forward -n ollama-ns svc/ollama-service 11434:11434

    # verify ollama is working
    curl http://localhost:11434/api/generate -d '{"model": "qwen3.5:0.8b", "prompt": "Hello"}'
}
service_info(){
    kubectl get sts,po,pvc,svc --namespace=${NAMESPACE}
}
ollama_stop(){
    echo "Stopping the OLLAMA service at ${DATE_TIME}"
    kubectl delete -f ${APP_YAML} --namespace=${NAMESPACE} --ignore-not-found --force
}
validate(){
    # Validate a single file
    kubectl apply --dry-run=client -f ${APP_YAML} --validate=true

    #Server-side dry run is stricter and catches more issues
    #kubectl apply --dry-run=server -f ${APP_YAML} --validate=true
}

# -n string - True if the string length is non-zero.
if [[ -n $arg ]] ; then
    arg_len=${#arg}
    # uppercase the argument
    arg=$(echo ${arg} | tr [a-z] [A-Z] | xargs)
    echo "User Action: ${arg}, and arg length: ${arg_len}"

    if [[ $arg == "DEPLOY" || $arg == "INSTALL" || $arg == "RUN" ]] ; then
        ollama_deploy
    elif [[ $arg == "STOP" || $arg == "CLEAN" || $arg == "UNINSTALL" ]] ; then
       ollama_stop
    elif [[ $arg == "VALIDATE" || $arg == "CHECK" || $arg == "VAL" ]] ; then
        validate
    elif [[ $arg == "INFO" || $arg == "STATUS" ]] ; then
        service_info
    else
        echo "Invalid argument: ${arg}. Please use BUILD or RUN."
    fi
else
    echo "No argument provided. Please use BUILD, RUN, START, RESTART, STOP, or CLEAN."
fi  