# Odoo Instance Orchestration Platform

The Odoo Instance Orchestration Platform automates the provisioning of isolated Odoo environments on remote hosts. It uses a distributed architecture consisting of a Spring Boot control plane, a Kafka message broker, and a Python-based Orchestrator responsible for executing provisioning tasks such as container creation, database initialization, and environment configuration.

This project is published publicly for transparency and reproducibility. It was originally created as a small weekend prototype and is **not production-ready** as of the present release. It is shared openly in case it may be useful to others or serve as a reference.

---

## 🚀 Features

- Automated provisioning of Odoo instances  
- Asynchronous workflow using Kafka (`create_instance`, `job_updates`)  
- Callback mechanism with token validation  
- Persistent job and instance metadata (PostgreSQL)  
- Clear separation of control plane and execution layer  
- Containerized support scripts and modular architecture  

---

## 📁 Project Structure

```
spectre/
  spectrum/                         # Spring Boot control plane (API + job manager)
    src/main/java/com/spectrun/spectrum/
      controllers/                  # REST controllers
      services/                     # Business logic
      models/                       # JPA entities
      repositories/                 # Spring Data repositories
      config/                       # Security, Kafka, app config
    src/main/resources/
      application.properties        # Requires user-provided secrets

  InstanceControllerService/        # Python Orchestrator (FastAPI)
    app/
      api/                          # FastAPI route handlers
      config/                       # Application configuration
      dependencies/                 # DI modules
      KafkaManager/                 # Kafka consumer/producer logic
      Models/                       # Internal domain models
      schema/                       # Pydantic request/response schemas
      Scripts/                      # Environment/setup scripts
      services/                     # Provisioning + orchestration engine
      utils/                        # Docker, ports, logging, file ops
        kafkaResponse/              # Kafka response utilities

    env/                            # Environment variable files
    OdooConfigurationFiles/
      addons/                       # Odoo addons
      config/                       # Odoo configuration templates

    docker-compose.yml              # Kafka + Zookeeper stack
    .gitignore
```

---

## 🛠️ Prerequisites

- Docker & Docker Compose  
- Java 17  
- Python 3.10+  
- Maven or `./mvnw`  
- Git  
- A Fedora-based remote host (for provisioning)  
- SSH access to the host  

---

## 🔐 Sensitive Configuration Keys

Before running the system, configure the following keys in:

`spectrum/src/main/resources/application.properties`

```
spring.datasource.url
spring.datasource.username
spring.datasource.password

spring.security.jwt.secret_key
security.callback.shared-secret
security.jwt.expiration-time
```

These require your own secret values.  
**No real secrets are included in this repository.**

---

## 🔧 Configuration

### Spring Boot (Spectrum)

Create or modify:

`spectrum/src/main/resources/application.properties`

Example:

```
spring.datasource.url=jdbc:postgresql://<db-host>:5432/<db>
spring.datasource.username=<db-user>
spring.datasource.password=<db-password>

spring.security.jwt.secret_key=<random-secret>
security.jwt.expiration-time=<millis>
security.callback.shared-secret=<shared-secret>

spring.kafka.bootstrap-servers=localhost:9092
```

---

### FastAPI Worker (InstanceControllerService)

Reads settings from environment variables (`.env` or exported in shell):

```
HOST=localhost
KAFKA_PORT=9092

CREATE_CONTAINER=create_instance
PRODUCE_RESPONSE=create_response
INITIALIZE_HOST_TOPIC=initialize-server
```

Export them before running `uvicorn`, or place them in an `.env` file.

---

## ⚡ Quick Start (Local Development)

### 1. Clone the repository

```bash
git clone git@github.com:Marx0xD/spectre.git
cd spectre
```

### 2. Start Kafka, Zookeeper, and PostgreSQL

```bash
docker-compose up -d
```

### 3. Run the Spring Boot control plane

```bash
cd spectrum
./mvnw spring-boot:run
```

### 4. Run the FastAPI worker

```bash
cd ../InstanceControllerService
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

When both services are running, API calls to the Spring Boot service will publish Kafka messages that the FastAPI worker consumes to provision remote Odoo instances.

---

## ✅ Next Steps / Roadmap

- Improve security for remote execution (SSH keys, sudo hardening)  
- Implement retries, failure handling, and full idempotency  
- Add end-to-end integration tests  
- Document Kafka message schemas  
- Replace test routes with full production workflows  

---

## 📄 Notes

This project was built as an experimental orchestration engine.  
It requires additional hardening, security work, and testing before being used in real deployments.

