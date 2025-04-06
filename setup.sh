#!/bin/bash

# Step 1: Install dependencies for the system (if not installed)
echo "Installing system dependencies..."

# For Windows: Make sure Node.js, Maven, Git, and Java are installed manually or via a package manager
# Example for Node.js installation in Windows
# If not installed, visit: https://nodejs.org/en/download/
# For Maven: https://maven.apache.org/install.html
# For JDK: https://adoptopenjdk.net/

# Step 2: Clone the repository (if not already cloned)
# echo "Cloning repository..."
# git clone https://github.com/your-username/your-repository.git
# cd your-repository

# Step 3: Navigate to Backend - Install and build Spring project
echo "Building the Spring backend..."
cd "Payment API"
mvn clean install


# Step 4: Frontend - Install React dependencies
echo "Installing React frontend dependencies..."
cd ../payment_client
npm install

# Step 5: Run the Spring backend
echo "Running the Spring backend..."
cd ../"Payment API"
mvn spring-boot:run

# Step 6: Run the React frontend
echo "Running the React frontend..."
cd ../payment_client
npm start

echo "Setup complete! Backend and frontend are running."
