#!/bin/bash

# Simple launcher script for TariffSheriff
# This is the easiest way to start the application

echo "🚀 TariffSheriff Quick Launcher"
echo "=============================="
echo ""

# Check if we're on macOS/Linux or need to use Windows script
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" || "$OSTYPE" == "win32" ]]; then
    echo "Detected Windows environment. Starting Windows script..."
    ./start-app.bat
else
    echo "Detected Unix-like environment. Starting shell script..."
    ./start-app.sh
fi