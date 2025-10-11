#!/bin/bash

# TariffSheriff Application Stop Script
# This script stops both backend and frontend services

set -e

echo "🛑 Stopping TariffSheriff Application..."
echo "======================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to check if a process is running
is_process_running() {
    local pid=$1
    if ps -p $pid > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# Function to stop process gracefully
stop_process() {
    local pid=$1
    local name=$2
    
    if is_process_running $pid; then
        print_status "Stopping $name (PID: $pid)..."
        kill $pid
        
        # Wait for graceful shutdown
        for i in {1..10}; do
            if ! is_process_running $pid; then
                print_success "$name stopped successfully."
                return 0
            fi
            sleep 1
        done
        
        # Force kill if still running
        print_warning "Force killing $name..."
        kill -9 $pid 2>/dev/null || true
        sleep 1
        
        if ! is_process_running $pid; then
            print_success "$name force stopped."
        else
            print_error "Failed to stop $name."
        fi
    else
        print_warning "$name is not running."
    fi
}

# Function to kill processes by name
kill_by_name() {
    local process_name=$1
    local display_name=$2
    
    local pids=$(pgrep -f "$process_name" 2>/dev/null || true)
    
    if [ ! -z "$pids" ]; then
        print_status "Found $display_name processes: $pids"
        for pid in $pids; do
            stop_process $pid "$display_name"
        done
    else
        print_status "No $display_name processes found."
    fi
}

# Main cleanup function
cleanup_application() {
    # Try to read saved PIDs first
    if [ -f ".app_pids" ]; then
        print_status "Reading saved process IDs..."
        source .app_pids
        
        if [ ! -z "$BACKEND_PID" ]; then
            stop_process $BACKEND_PID "Backend"
        fi
        
        if [ ! -z "$FRONTEND_PID" ]; then
            stop_process $FRONTEND_PID "Frontend"
        fi
        
        # Remove PID file
        rm -f .app_pids
        print_success "Removed PID file."
    else
        print_warning "No saved PIDs found. Searching for processes..."
    fi
    
    # Kill any remaining processes by name
    print_status "Cleaning up any remaining processes..."
    
    # Kill backend processes
    kill_by_name "spring-boot:run" "Spring Boot Backend"
    kill_by_name "java.*backend" "Java Backend"
    
    # Kill frontend processes
    kill_by_name "vite" "Vite Frontend"
    kill_by_name "node.*vite" "Node Vite Frontend"
    
    # Clean up log files
    if [ -f "backend.log" ]; then
        print_status "Archiving backend.log..."
        mv backend.log "backend_$(date +%Y%m%d_%H%M%S).log"
    fi
    
    if [ -f "frontend.log" ]; then
        print_status "Archiving frontend.log..."
        mv frontend.log "frontend_$(date +%Y%m%d_%H%M%S).log"
    fi
    
    print_success "Application stopped successfully!"
    echo ""
    echo "📝 Archived log files are available for review."
    echo "🚀 To start the application again, run: ./start-app.sh"
}

# Main execution
cleanup_application