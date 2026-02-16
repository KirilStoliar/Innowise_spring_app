# Ui

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 21.0.4.

# Launch UI without Docker

## Development server

To start a local development server, run:

```bash
npm install
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Backend Changes
Replace file `my_project/api-gateway/src/main/resources/application.yml` with followed:
``` bash
server:
  port: 8083

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      globalcors:
        add-to-simple-url-handler-mapping: true
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "http://localhost:4200"
              - "http://127.0.0.1:4200"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            exposedHeaders:
              - Authorization
            allowCredentials: true

gateway:
  auth:
    url: http://auth-service:8081
  user:
    url: http://user-service:8080
  order:
    url: http://order-service:8082
  payment:
    url: http://payment-service:8084

app:
  jwt:
    secret: ${JWT_SECRET}

# admin defaults
gateway-admin:
  email: ${GATEWAY_ADMIN_EMAIL:admin@example.com}
  password: ${GATEWAY_ADMIN_PASSWORD:admin123}

logging:
  level:
    org.springframework.cloud.gateway: INFO
    reactor.netty: INFO
    org.springframework.web: INFO
```
If you want, you can change file `application-docker.yml` with the same content

# Launch UI with Docker
!!!attention
Work in progress