# Use the official OpenJDK runtime as the base image
FROM openjdk:21-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the entire project
COPY . .

# Build the application JAR
RUN ./gradlew build -xmx1g --no-daemon

# Expose port 8080 to allow communication to/from the application
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "build/libs/tictactoe-0.0.1-SNAPSHOT.jar"]