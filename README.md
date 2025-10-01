# Multiplayer Tic Tac Toe

A real-time multiplayer Tic Tac Toe game built with Java and Spring Boot. The application leverages WebSocket technology for gameplay between two players, with Redis for active game states and PostgreSQL for persistent data.

## Prerequisites

- Java 21
- Docker and Docker Compose
- Redis server (when not using Docker)
- PostgreSQL database (when not using Docker)

## Quick Start with Docker

### 1. Clone the repository
```bash
git clone <repository-url>
cd tictactoe
```

### 2. Start PostgreSQL and Redis using Docker Compose
```bash
docker-compose up -d
```

This will start PostgreSQL and Redis services.

### 3. Run the application
```bash
./gradlew bootRun
```

The application will connect to the Docker services using the `application-docker.properties` profile.

### Alternative: Run everything with Docker Compose
If you want to run the entire application stack with Docker Compose, first build the application JAR:

```bash
./gradlew build
```

Then use the production compose file:

```bash
docker-compose -f docker-compose.yml up --build
```

This will build and run the entire application stack (PostgreSQL, Redis, and the application) in Docker containers.

## Manual Setup (without Docker)

### 1. Install and start PostgreSQL
- Download and install PostgreSQL
- Create a database called `tictactoe`
- Ensure PostgreSQL is running on default port 5432

### 2. Install and start Redis
- Download and install Redis
- Start Redis server on default port 6379

### 3. Run the application
```bash
./gradlew bootRun
```

The application will use the default `application.properties` which connects to PostgreSQL and Redis on localhost.

## Configuration

The application has two configuration profiles:
- `application.properties`: Uses H2 in-memory database for quick development
- `application-docker.properties`: Uses PostgreSQL and Redis services (as defined in docker-compose.yml)

To use the Docker configuration, you can specify the profile when running:
```bash
java -jar build/libs/tictactoe-0.0.1-SNAPSHOT.jar --spring.profiles.active=docker
```

Or in your IDE, add the VM option: `-Dspring.profiles.active=docker`

## Building the Project

### Build the JAR:
```bash
./gradlew build
```

### Run the JAR:
```bash
java -jar build/libs/tictactoe-0.0.1-SNAPSHOT.jar
```

## Docker Compose Services

The `docker-compose.yml` file includes:
- PostgreSQL database with persistent volume
- Redis cache for active game states
- Health checks for both services

## Project Structure

- `src/main/java/com/multiplayer/tictactoe`: Main application code
  - `config/`: Configuration classes
  - `controller/`: WebSocket and HTTP controllers
  - `dto/`: Data Transfer Objects
  - `entity/`: Entity classes (JPA and Redis)
  - `enums/`: Enumerations
  - `repository/`: Data repositories
  - `service/`: Service layer implementations
  - `utils/`: Utility classes
- `src/main/resources`: Configuration and static resources
  - `application.properties`: Default application configuration
  - `application-docker.properties`: Docker configuration
  - `static/`: Static assets
  - `templates/`: Thymeleaf templates (if any)

## Development

### Running Tests
```bash
./gradlew test
```

### Code Style
- Uses Lombok to reduce boilerplate code
- Uses MapStruct for object mapping
- Follows Spring Boot conventions

## Troubleshooting

### Common Issues
1. **Database Connection Issues**: Ensure PostgreSQL is running and credentials in application properties match
2. **Redis Connection Issues**: Ensure Redis server is running and accessible
3. **Port Conflicts**: Make sure ports 5432 (PostgreSQL) and 6379 (Redis) are available

### Docker-Specific Issues
1. **Docker Permission Issues**: Ensure you have proper Docker permissions
2. **Port Already Allocated**: Check if ports 5432 and 6379 are used by other services

## Architecture Overview

- **Real-time Gameplay**: Uses WebSocket technology with STOMP for messaging
- **Game State Storage**: Active games stored in Redis with TTL (Time To Live)
- **Persistent Storage**: Game history and other data stored in PostgreSQL
- **Scalable Board Size**: Supports configurable board sizes beyond traditional 3x3
- **Game Status Tracking**: Handles various game states (waiting, in-progress, won, draw, cancelled)

## Technologies Used

- Java 21
- Spring Boot 3.5.6
- Spring Web, WebSocket, Messaging
- Spring Data JPA, Spring Data Redis
- Thymeleaf
- PostgreSQL
- Redis
- Lombok
- MapStruct
- Docker and Docker Compose