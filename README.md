# LEAP-IO v3 - Backend for LEAP App Builder

LEAP-IO v3 is the backend service powering the LEAP App Builder. It provides robust APIs and backend logic for building, managing, and deploying applications via the LEAP platform.

## Table of Contents

- [Features](#features)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [API Reference](#api-reference)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

## Features

- Scalable Java-based backend
- RESTful API endpoints
- User authentication and authorization
- Data management and persistence
- Integration with LEAP frontend
- Extensible architecture for future modules

# About LEAP App Builder

**LEAP App Builder** is a web-based application development platform designed to streamline the creation of modern web apps. It is powered by three core components:

- **LEAP-IO** – The backend engine that powers data processing and API services  
- **LEAP-DESIGN** – A no-code editor for designing app logic and UI  
- **LEAP-RUN** – The runtime engine that executes and renders the app in real time

LEAP App Builder adopts a **building block** and **metadata-driven** approach, allowing apps to be constructed from reusable components. By leveraging a technology-agnostic metadata format, applications built with LEAP remain compatible and up-to-date with the evolving LEAP runtime — without requiring manual intervention from developers.

## Available Instances
<img width="240" height="356" alt="LEAP-based Platform" src="https://github.com/user-attachments/assets/9069bf7e-a24e-45d2-b16e-e780baab4f65" />

As of now, there are **six active instances** of the LEAP App Builder platform:


1. **LEAPMY** – Public and community instance  
2. **IA** – Privately managed by UNIMAS  
3. **AA** – Privately managed by UNIMAS  
4. **IREKA** – Commercial installation by UNIMAS  
5. **AppWizard** – Privately managed by Sarawak Skills  
6. **KBORNEO** – Privately managed by ICATS University College


## Getting Started

### Prerequisites

- Java 11 or later
- Maven (or Gradle)
- Database (e.g., PostgreSQL, MySQL)
- Clone this repository

### Setup

```bash
git clone https://github.com/benzourry/leap-io-v3.git
cd leap-io-v3
# Configure environment variables and database connection
# Build the project
mvn clean install
# Run the application
mvn spring-boot:run
