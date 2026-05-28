#!/bin/bash
arg="${1:-RESTART}"
export OLLAMA_HOST="0.0.0.0"

start_ollama() {
    echo "Starting Ollama server..."
    ollama serve &
}
stop_ollama() {
    echo "Stopping Ollama server..."
    pkill -f "ollama serve"
    sleep 2
    kill -9 $(lsof -t -i:11434)
}
validate(){
    curl http://localhost:11434/api/tags
    sleep 2

    
}
info_ollama() {
    # https://docs.ollama.com/api/introduction
    echo "Ollama status"
    echo "-------------"
    if pgrep -f "ollama serve" >/dev/null 2>&1; then
        echo "Process: running"
    else
        echo "Process: not running"
    fi

    if lsof -iTCP:11434 -sTCP:LISTEN >/dev/null 2>&1; then
        echo "Port 11434: listening"
    else
        echo "Port 11434: not listening"
    fi

    echo "Host: http://localhost:11434"
    echo "Tags:"
    curl -s http://127.0.0.1:11434/api/tags
}

# uppercase the argument
arg=$(printf '%s' "$arg" | tr '[:lower:]' '[:upper:]' | xargs)
echo "User Action: ${arg}"

case $arg in
    START)
        start_ollama
        ;;
    STOP)
        stop_ollama
        ;;
    INFO)
        info_ollama
        ;;
    RESTART)
        stop_ollama
        sleep 2
        start_ollama
        ;;
    *)
        echo "Usage: $0 {start|stop|info|restart}"
        exit 1
        ;;
esac
