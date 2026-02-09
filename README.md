# Hygiene Store

Spring Boot backend with a simple storefront frontend for a hygiene shop.

## Features
- Product catalog with search
- Cart API and cart page
- Seeded hygiene products on startup
- Role-based access (USER/ADMIN)
- PostgreSQL database

## Prerequisites
- Java 25
- Maven
- PostgreSQL running on `localhost:5432`

## Configure
Update `src/main/resources/application.properties` with your Postgres credentials.

## Run
```bash
./mvnw spring-boot:run
```

App: `http://localhost:8081`  
Login: `http://localhost:8081/login`

## Demo Accounts
- USER: `user` / `user123`
- ADMIN: `admin` / `admin123`
