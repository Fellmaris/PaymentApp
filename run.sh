#!/bin/bash

# Function to kill processes on ports 8080 and 3000
kill_processes() {
  echo "Killing processes on ports 8080 and 3000..."
  # Kill processes on port 8080 (Spring)
  lsof -i :8080 -t | xargs kill -9
  # Kill processes on port 3000 (React)
  lsof -i :3000 -t | xargs kill -9
}

# Trap EXIT signal to ensure cleanup
trap kill_processes EXIT

# Step 1: Run the Spring backend in the background
echo "Running the Spring backend..."
cd "Payment API"
mvn spring-boot:run &

# Step 2: Run the React frontend in the background
echo "Running the React frontend..."
cd ../payment_client
npm start &

# Step 3: Wait until React frontend is fully loaded (check if localhost:3000 is accessible)
echo "Waiting for React frontend to be available..."
until curl --silent --head http://localhost:3000 | grep "200 OK" > /dev/null; do
  echo "Waiting for React to be ready..."
  sleep 2
done

# Step 4: Open the browser for React
echo "Opening browser on http://localhost:3000..."
explorer http://localhost:3000

# Wait indefinitely so that the processes can continue running
echo "Application is running! Backend and frontend are both up."
echo "Press 'Ctrl + C' to stop or exit to terminate processes."

wait
