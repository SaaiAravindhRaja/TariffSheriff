#!/bin/bash

# TariffSheriff Application Startup Script
# This script starts both backend and frontend services

set -e

echo "🚀 Starting TariffSheriff Application..."
echo "=================================="

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

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

cleanup_ports() {
    print_status "Checking ports 8080 and 3000..."
    for p in 8080 3000; do
      if check_port $p; then
        print_warning "Port $p is in use. Attempting graceful stop..."
        lsof -ti:$p | xargs kill 2>/dev/null || true
        sleep 2
      fi
    done
}

# Function to check prerequisites
check_prerequisites() {
    print_status "Checking prerequisites..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        print_error "Java is not installed. Please install Java 17 or higher."
        exit 1
    fi
    
    # Check Maven
    if ! command -v mvn &> /dev/null; then
        print_error "Maven is not installed. Please install Maven."
        exit 1
    fi
    
    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js is not installed. Please install Node.js."
        exit 1
    fi
    
    # Check npm
    if ! command -v npm &> /dev/null; then
        print_error "npm is not installed. Please install npm."
        exit 1
    fi
    
    print_success "All prerequisites are installed."
}

# Function to install dependencies
install_dependencies() {
    print_status "Installing dependencies..."
    
    # Get current directory
    ORIGINAL_DIR=$(pwd)
    
    # Install frontend dependencies
    if [ ! -d "apps/frontend/node_modules" ]; then
        print_status "Installing frontend dependencies..."
        cd apps/frontend
        npm install
        cd "$ORIGINAL_DIR"
        print_success "Frontend dependencies installed."
    else
        print_status "Frontend dependencies already installed."
    fi
}

# Function to start backend
start_backend() {
    print_status "Starting backend service..."
    
    # Get current directory
    ORIGINAL_DIR=$(pwd)
    
    if [ ! -d "apps/backend" ]; then
        print_error "Backend directory not found. Make sure you're running this from the project root."
        exit 1
    fi
    
    cd apps/backend
    
    # Load environment variables
    if [ -f ".env" ]; then
        export $(cat .env | grep -v '^#' | xargs)
        print_success "Backend environment variables loaded."
    else
        print_warning "Backend .env file not found. Using default configuration."
    fi
    
    # Build backend quickly (skip tests)
    print_status "Building backend (skip tests)..."
    mvn -q -DskipTests package || { print_error "Backend build failed"; cd "$ORIGINAL_DIR"; exit 1; }
    
    # Start backend in background
    print_status "Starting Spring Boot application..."
    mvn spring-boot:run -Dspring-boot.run.arguments="--spring.flyway.validate-on-migrate=false" > ../../backend.log 2>&1 &
    BACKEND_PID=$!
    
    cd "$ORIGINAL_DIR"
    
    # Wait for backend to start
    print_status "Waiting for backend on port 8080..."
    for i in {1..60}; do
        if check_port 8080; then
            print_success "Backend started successfully! (PID: $BACKEND_PID)"
            break
        fi
        if [ $i -eq 60 ]; then
            print_error "Backend failed to start within 60 seconds."
            print_error "Check backend.log for details:"
            tail -20 backend.log
            exit 1
        fi
        sleep 1
        echo -n "."
    done
    echo ""
}

# Function to start frontend
start_frontend() {
    print_status "Starting frontend service..."
    
    # Get current directory
    ORIGINAL_DIR=$(pwd)
    
    if [ ! -d "apps/frontend" ]; then
        print_error "Frontend directory not found. Make sure you're running this from the project root."
        exit 1
    fi
    
    cd apps/frontend
    
    # Start frontend in background
    print_status "Starting Vite dev server..."
    npm run dev > ../../frontend.log 2>&1 &
    FRONTEND_PID=$!
    
    cd "$ORIGINAL_DIR"
    
    # Wait for frontend to start
    print_status "Waiting for frontend to start on port 3000..."
    for i in {1..30}; do
        if check_port 3000; then
            print_success "Frontend started successfully! (PID: $FRONTEND_PID)"
            break
        fi
        if [ $i -eq 30 ]; then
            print_error "Frontend failed to start within 30 seconds."
            print_error "Check frontend.log for details:"
            tail -20 frontend.log
            exit 1
        fi
        sleep 1
        echo -n "."
    done
    echo ""
}

# Function to display running services
show_services() {
    echo ""
    echo "🎉 TariffSheriff Application is now running!"
    echo "=========================================="
    echo ""
    echo "📱 Frontend:     http://localhost:3000"
    echo "🔧 Backend API:  http://localhost:8080/api"
    echo "📚 Swagger UI:   http://localhost:8080/swagger-ui.html"
    echo ""
    echo "📋 Process Information:"
    echo "   Backend PID:  $BACKEND_PID"
    echo "   Frontend PID: $FRONTEND_PID"
    echo ""
    echo "📝 Logs:"
    echo "   Backend:  tail -f backend.log"
    echo "   Frontend: tail -f frontend.log"
    echo ""
    echo "🛑 To stop the application:"
    echo "   ./stop-app.sh"
    echo "   or"
    echo "   kill $BACKEND_PID $FRONTEND_PID"
    echo ""
}

# Function to save PIDs for cleanup script
save_pids() {
    echo "BACKEND_PID=$BACKEND_PID" > .app_pids
    echo "FRONTEND_PID=$FRONTEND_PID" >> .app_pids
    print_success "Process IDs saved to .app_pids"
}

# Trap to handle script interruption
cleanup() {
    print_warning "Script interrupted. Cleaning up..."
    if [ ! -z "$BACKEND_PID" ]; then
        kill $BACKEND_PID 2>/dev/null || true
    fi
    if [ ! -z "$FRONTEND_PID" ]; then
        kill $FRONTEND_PID 2>/dev/null || true
    fi
    exit 1
}

trap cleanup INT TERM

# Function to check if we're in the right directory
check_project_structure() {
    if [ ! -d "apps/backend" ] || [ ! -d "apps/frontend" ]; then
        print_error "Project structure not found!"
        print_error "Make sure you're running this script from the TariffSheriff project root directory."
        print_error "Expected structure:"
        print_error "  - apps/backend/"
        print_error "  - apps/frontend/"
        exit 1
    fi
    
    if [ ! -f "apps/backend/pom.xml" ]; then
        print_error "Backend pom.xml not found. Invalid project structure."
        exit 1
    fi
    
    if [ ! -f "apps/frontend/package.json" ]; then
        print_error "Frontend package.json not found. Invalid project structure."
        exit 1
    fi
    
    print_success "Project structure validated."
}

# Main execution
main() {
    check_project_structure
    cleanup_ports
    check_prerequisites
    install_dependencies
    start_backend
    start_frontend
    save_pids
    show_services
    
    # Keep script running
    print_status "Application is running. Press Ctrl+C to stop."
    wait
}

# Run main function
main
