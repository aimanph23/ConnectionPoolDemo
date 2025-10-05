# Home Endpoint Guide

## Overview

The `/home` endpoint provides a comprehensive overview of all available endpoints in the Connection Pool Demo application. It serves as a central hub for discovering and understanding the application's capabilities.

## Endpoint Details

- **URL**: `GET http://localhost:8080/home`
- **Content-Type**: `application/json`
- **Description**: Returns a structured overview of all application endpoints, features, and quick start guide

## Response Structure

The response includes the following sections:

### 1. Application Information
```json
{
  "application": "Connection Pool Demo",
  "description": "A Spring Boot application demonstrating database connection pool management with HikariCP",
  "version": "1.0.0",
  "timestamp": "2025-10-05T19:00:02.01689",
  "baseUrl": "http://localhost:8080"
}
```

### 2. Endpoints Summary
The response categorizes all endpoints into four main groups:

#### Products Endpoints (`/api/products`)
- Product management with connection pool testing
- Includes both synchronous and asynchronous operations
- Main endpoint for testing connection pool behavior

#### Customers Endpoints (`/api/customers`)
- Customer management via Postman API 101 collection integration
- Full CRUD operations with both sync and async versions
- Integration with external Postman Echo API

#### Monitoring Endpoints (`/api/monitoring`)
- Real-time connection pool monitoring and metrics
- HikariCP statistics and detailed information
- Server-Sent Events for live monitoring

#### Dashboard Endpoints (`/dashboard`)
- Web-based monitoring dashboards
- Real-time connection pool visualization

### 3. Application Information
```json
{
  "applicationInfo": {
    "database": "H2 In-Memory Database",
    "connectionPool": "HikariCP",
    "maxPoolSize": "10",
    "minIdle": "5",
    "externalApis": [
      "JSONPlaceholder API (https://jsonplaceholder.typicode.com/posts/1)",
      "Postman Echo API (https://postman-echo.com)",
      "Postman Echo Delay API (https://postman-echo.com/delay/{seconds})"
    ],
    "features": [
      "Connection Pool Monitoring",
      "Async vs Sync API Processing",
      "External API Integration",
      "Real-time Metrics Dashboard",
      "Postman API 101 Collection Integration"
    ]
  }
}
```

### 4. Quick Start Guide
```json
{
  "quickStart": {
    "1": "Start the application: mvn spring-boot:run",
    "2": "Access the home endpoint: GET http://localhost:8080/home",
    "3": "Monitor connection pool: GET http://localhost:8080/api/monitoring/hikari",
    "4": "View real-time dashboard: http://localhost:8080/dashboard/hikari",
    "5": "Test product processing: POST http://localhost:8080/api/products/1/process",
    "6": "Test async processing: GET http://localhost:8080/api/products/v2/1",
    "7": "Test customer API: GET http://localhost:8080/api/customers"
  }
}
```

## Usage Examples

### Basic Request
```bash
curl http://localhost:8080/home
```

### Pretty-printed JSON
```bash
curl -s http://localhost:8080/home | python3 -m json.tool
```

### Using HTTP Client (VS Code/IntelliJ)
```http
GET http://localhost:8080/home
Accept: application/json
```

## Benefits

1. **Discovery**: Easy way to discover all available endpoints
2. **Documentation**: Self-documenting API with descriptions and details
3. **Quick Start**: Step-by-step guide for new users
4. **Reference**: Central reference for all application capabilities
5. **Testing**: Helps with API testing and exploration

## Integration with Other Tools

The home endpoint can be used with:
- API testing tools (Postman, Insomnia)
- Documentation generators
- Monitoring dashboards
- Development tools and IDEs
- Automated testing frameworks

## Response Format

Each endpoint in the response includes:
- **method**: HTTP method (GET, POST, PUT, DELETE)
- **path**: Full endpoint path
- **description**: Brief description of the endpoint
- **details**: Additional details about usage and parameters

## Example Response

```json
{
  "method": "POST",
  "path": "/api/products/{id}/process",
  "description": "Process product (DB + External API + Update)",
  "details": "Main endpoint for testing connection pool behavior"
}
```

This comprehensive home endpoint makes the application self-documenting and provides an excellent starting point for anyone exploring the API.
