# Damn-API Setup Guide

This document outlines the steps to compile and deploy the `damn-api` Spring Boot project, which integrates with PostgreSQL and Neo4j databases running as separate Docker containers.

## Backend Compile Instructions

1. **Prerequisites**:
   - Java 21 installed.
   - Maven installed.
   - Docker and Docker Compose installed.

2. **Clone or Create Project**:
   - Either unzip the provided `damn-api.zip` file on your local machine or clone the project from GitHub:
     ```bash
     git clone https://github.com/damn-maps/damn-api.git
     ```

3. **Build the Project**:
   - Navigate to the project root (`damn-maps/`):
     ```bash
     cd damn-maps
     ```
   - Compile and package the project:
     ```bash
     mvn clean install
     ```

## Backend Deployment Instructions

1. **Start PostgreSQL and Neo4j**:
   - Create a Docker network for container communication:
     ```bash
     sudo docker network create damn-network
     ```
   - Start the Neo4j container:
     ```bash
     sudo docker run --name neo4j -p 7474:7474 -p 7687:7687 -e NEO4J_AUTH=neo4j/yourpassword -e NEO4J_dbms_security_procedures_unrestricted=apoc.* -e NEO4J_dbms_security_procedures_allowlist=apoc.* -e NEO4J_dbms_memory_transaction_total_max=4g -e NEO4J_dbms_memory_heap_initial__size=1g -e NEO4J_dbms_memory_heap_max__size=2g -e NEO4J_dbms_memory_pagecache_size=1g --memory="6g" --memory-swap="8g" -v C:/Users/Lenovo/Downloads/apoc-2025.02.0.jar:/var/lib/neo4j/plugins/apoc-2025.02.0.jar -v neo4j-data:/data -v neo4j-logs:/logs --network damn-network neo4j:2025.02.0
     ```
   - Pull the PostgreSQL image:
     ```bash
     sudo docker pull postgres
     ```
   - Start the PostgreSQL container:
     ```bash
     sudo docker run --name damn_map_postgres --network damn-network -e POSTGRES_USER=damn -e POSTGRES_PASSWORD=mypassword -e POSTGRES_DB=damn_maps -p 5432:5432 -d postgres
     ```

2. **Build and Run the Spring Boot App**:
   - From the project root (`damn-maps/`), build the Docker image and start the application:
     ```bash
     docker-compose up --build
     ```


