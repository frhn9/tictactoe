@echo off
REM Script to set up and run the Tic Tac Toe application with Docker

echo Setting up Multiplayer Tic Tac Toe with Docker...

REM Check if Docker is installed
docker --version >nul 2>&1
if errorlevel 1 (
    echo Error: Docker is not installed.
    exit /b 1
)

REM Check if Docker Compose is installed
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo Error: Docker Compose is not installed.
    exit /b 1
)

echo Starting PostgreSQL and Redis services...
docker-compose up -d postgres redis

echo Waiting for services to be ready...
timeout /t 10 /nobreak >nul

echo Starting the application...
gradlew.bat bootRun --args="--spring.profiles.active=docker"

echo Application should now be running on http://localhost:8080