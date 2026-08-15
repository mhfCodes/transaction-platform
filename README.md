# Transaction Processing Platform

A Spring Boot backend for handling money transfers between accounts.

The project focuses on transaction consistency, concurrent updates, asynchronous processing, caching, and application observability.

## Features

* Money transfers between accounts
* PostgreSQL transactions with optimistic locking
* Transactional Outbox Pattern with Kafka
* Asynchronous notification processing
* Redis caching for transaction lookups
* Application metrics with Prometheus and Grafana
* Centralized logging with ELK

## Architecture

```text
Client
  │
  ▼
Controller
  │
  ▼
Service
  │
  ├──────────────► PostgreSQL
  │
  └──────────────► Outbox
                       │
                       ▼
                    Kafka
                       │
                       ▼
              Notification Consumer
```

Transaction lookups use Redis as a cache in front of PostgreSQL.

```text
GET Transaction
      │
      ▼
    Redis
      │
   ┌──┴──┐
   │     │
  Hit   Miss
   │     │
   │     ▼
   │  PostgreSQL
   │     │
   └─────┘
```

## Tech Stack

* Java
* Spring Boot
* Spring Security
* Spring Data JPA
* PostgreSQL
* Kafka
* Redis
* Prometheus
* Grafana
* Elasticsearch
* Logstash
* Kibana
* Docker
* Maven
* Git

## Running the Application

### Requirements

* Java
* Maven
* Docker

### Start the infrastructure

```bash
docker compose up -d
```

Check the containers:

```bash
docker compose ps
```

### Start the application

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```bash
mvnw.cmd spring-boot:run
```

## Observability

The application exposes metrics through Spring Boot Actuator and Micrometer. Prometheus collects the metrics and Grafana is used to visualize them.

Application logs are sent through Logstash to Elasticsearch and can be viewed and searched in Kibana.

## Tests

The project includes tests covering the main application flows, including transaction processing, concurrent updates, caching, and Kafka-related functionality.
