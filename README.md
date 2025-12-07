# Odoo Instance Orchestration Platform

The Odoo Instance Orchestration Platform automates the provisioning of isolated Odoo environments on remote hosts. It uses a distributed architecture consisting of a Spring Boot control plane, a Kafka message broker, and a Python-based Orchestrator responsible for executing provisioning tasks such as container creation, database initialization, and environment configuration.

This project is published publicly to support transparency, reproducibility, and academic evaluation. It was originally developed as part of a technical submission for the Master of Information Technology programme at Brno University of Technology.

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
├── docker-compose.yml # Kafka, Zookeeper, PostgreSQL

---

## 🛠️ Prerequisites

Before running the platform, ensure the following tools are installed:

- Docker & Docker Compose  
- Java 17  
- Python 3.10+  
- Maven (or the included `mvnw`)  
- Git  

---

## ⚡ Quick Start

### 1. Clone the repository

```bash
git clone git@github.com:Marx0xD/spectre.git
cd  spectrum/

