# Odoo Instance Orchestration Platform

The Odoo Instance Orchestration Platform automates the provisioning of isolated Odoo environments on remote hosts. It uses a distributed architecture consisting of a Spring Boot control plane, a Kafka message broker, and a Python-based Orchestrator responsible for executing provisioning tasks such as container creation, database initialization, and environment configuration.

This project is published publicly for transparency and reproducibility. It was originally created as a small weekend prototype and is not a production-ready tool as of the present release. It is shared openly in case it may be useful to others or serve as a reference.
---

## 🚀 Features

- Automated provisioning of Odoo instances  
- Asynchronous workflow using Kafka (`create_instance`, `job_updates`)  
- Callback mechanism with token validation  
- Persistent job and instance metadata (PostgreSQL)  
- Clear separation of control plane and execution layer  
- Containerized support scripts and modular architecture  

---

## 📁 Repository Structure

project-root/
│
├── spring-control-plane/ # Spring Boot API & job lifecycle manager
├── python-orchestrator/ # Python service for provisioning tasks
│
├── docker-compose.yml # Kafka, Zookeeper

---

## 🛠️ Prerequisites

Before running the platform, ensure the following tools are installed:

- Docker & Docker Compose  
- Java 17  
- Python 3.10+  
- Maven (or the included `mvnw`)  
- Git
- virtual machine machine running with a fedora-server host

---
## 🔐 Sensitive Configuration Keys

Before running the system, configure the following properties with your own values in  
`spectrum/src/main/resources/application.properties`:
- spring.datasource.url
- spring.datasource.username
- spring.datasource.password

- spring.security.jwt.secret_key

- security.callback.shared-secret

- security.jwt.expiration-time

## ⚡ Quick Start

### 1. Clone the repository

```bash
git clone git@github.com:Marx0xD/spectre.git
cd  spectrum/
cd ..
docker-compose up -d
cd spectrum
./mvnw spring-boot:run
cd ../InstanceControllerService
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000



