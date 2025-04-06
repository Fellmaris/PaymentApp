#!/bin/bash

# Step 1: Run the Spring backend
echo "Running the Spring backend..."
cd "Payment API"
mvn spring-boot:run


# Step 2: Run the React frontend
echo "Running the React frontend..."
cd ../payment_client
npm start


echo "Opening browser on http://localhost:3000..."
# For macOS
open http://localhost:3000

echo "Application is running! Backend and frontend are both up."