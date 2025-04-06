#!/bin/bash

# Script Name: run_apps.sh
# Description: This script stops and starts a Spring backend and a React frontend application.
# Author: Your Name
# Date: YYYY-MM-DD
# Usage: ./run_apps.sh

# Function to stop the Spring and React applications
stop_apps() {
  # Stop processes using port 3000 (React)
  echo "Stopping React on port 3000..."
  REACT_PIDS=$(netstat -ano | grep ":3000" | grep "LISTENING" | awk '{print $5}')
  if [ -n "$REACT_PIDS" ]; then
    for PID in $REACT_PIDS; do
      echo "Killing React process: $PID"
      taskkill //F //PID "$PID" 2>/dev/null
    done
  else
    echo "No process found on port 3000"
  fi

  # Stop processes using port 8080 (Spring)
  echo "Stopping Spring on port 8080..."
  SPRING_PIDS=$(netstat -ano | grep ":8080" | grep "LISTENING" | awk '{print $5}')
  if [ -n "$SPRING_PIDS" ]; then
    for PID in $SPRING_PIDS; do
      echo "Killing Spring process: $PID"
      taskkill //F //PID "$PID" 2>/dev/null
    done
  else
    echo "No process found on port 8080"
  fi

  # Additional check for npm and mvn processes
  echo "Stopping any remaining npm processes..."
  NPM_PIDS=$(ps -W | grep "npm" | awk '{print $1}')
  if [ -n "$NPM_PIDS" ]; then
    for PID in $NPM_PIDS; do
      # Don't kill this script's npm
      if [ "$PID" != "$$" ]; then
        echo "Killing npm process: $PID"
        taskkill //F //PID "$PID" 2>/dev/null
      fi
    done
  fi

  echo "Stopping any remaining mvn processes..."
  MVN_PIDS=$(ps -W | grep "mvn" | awk '{print $1}')
  if [ -n "$MVN_PIDS" ]; then
    for PID in $MVN_PIDS; do
      echo "Killing mvn process: $PID"
      taskkill //F //PID "$PID" 2>/dev/null
    done
  fi

  # Wait to ensure all processes are terminated
  sleep 2

  # Verify ports are free
  if netstat -ano | grep ":3000" | grep "LISTENING" > /dev/null; then
    echo "WARNING: Port 3000 is still in use!"
  else
    echo "Port 3000 is free."
  fi

  if netstat -ano | grep ":8080" | grep "LISTENING" > /dev/null; then
    echo "WARNING: Port 8080 is still in use!"
  else
    echo "Port 8080 is free."
  fi
}

# Trap signals to ensure cleanup on script termination
trap stop_apps EXIT INT TERM

# Call stop_apps at the start to ensure ports are clean
echo "Cleaning up any existing processes..."
stop_apps

# Step 1: Run the Spring backend in the background
echo "Starting Spring backend..."
cd "Payment API" || exit 1
mvn spring-boot:run > /dev/null 2>&1 &
sleep 5  # Give Spring some time to start

# Step 2: Run the React frontend in the background
echo "Starting React frontend..."
cd "../payment_client" || exit 1
npm start > /dev/null 2>&1 &

# Step 3: Wait until React frontend is fully loaded
echo "Waiting for applications to start..."
for i in {1..30}; do
  if curl --silent --head http://localhost:3000 2>/dev/null | grep "200 OK" > /dev/null 2>&1; then
    echo "React frontend is ready."
    break
  fi
  sleep 2
  if [ $i -eq 30 ]; then
    echo "Timed out waiting for React to start. It may still be starting..."
  fi
done

echo "Both the backend and frontend are running!"
echo "Spring backend is running at http://localhost:8080"
echo "React frontend is running at http://localhost:3000"
echo "Press Ctrl+C to stop both services."

# Wait indefinitely
while true; do
  sleep 5
  # Optional: periodically check if apps are still running
  if ! netstat -ano | grep ":3000" | grep "LISTENING" > /dev/null; then
    echo "React frontend is no longer running on port 3000. Restarting..."
    cd "../payment_client" || exit 1
    npm start > /dev/null 2>&1 &
  fi

  if ! netstat -ano | grep ":8080" | grep "LISTENING" > /dev/null; then
    echo "Spring backend is no longer running on port 8080. Restarting..."
    cd "../Payment API" || exit 1
    mvn spring-boot:run > /dev/null 2>&1 &
  fi
done