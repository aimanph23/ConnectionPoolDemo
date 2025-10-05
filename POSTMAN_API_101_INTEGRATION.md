# Postman API 101 Collection Integration

This document describes the integration of the `PostmanEchoService` with the [Postman API 101 collection](https://www.postman.com/postman/published-postman-templates/collection/07kihjr/api-101).

## Overview

The `PostmanEchoService` has been enhanced to integrate with the Postman API 101 collection, which provides a set of endpoints designed for learning API request basics. The integration includes both synchronous and asynchronous versions of all CRUD operations.

## API Endpoints

### Base URL
- **Postman Echo API**: `https://postman-echo.com`
- **Local API**: `http://localhost:8080/api/customers`

### Available Operations

#### 1. Retrieve All Customers
- **Synchronous**: `GET /api/customers`
- **Asynchronous**: `GET /api/customers/async`
- **External API**: `GET https://postman-echo.com/customers`

#### 2. Get One Customer
- **Synchronous**: `GET /api/customers/{id}`
- **Asynchronous**: `GET /api/customers/async/{id}`
- **External API**: `GET https://postman-echo.com/customers/{id}`

#### 3. Add New Customer
- **Synchronous**: `POST /api/customers`
- **Asynchronous**: `POST /api/customers/async`
- **External API**: `POST https://postman-echo.com/customers`

#### 4. Update Customer
- **Synchronous**: `PUT /api/customers/{id}`
- **External API**: `PUT https://postman-echo.com/customers/{id}`

#### 5. Remove Customer
- **Synchronous**: `DELETE /api/customers/{id}`
- **External API**: `DELETE https://postman-echo.com/customers/{id}`

## Customer Data Model

```json
{
  "id": "string",
  "name": "string",
  "email": "string",
  "phone": "string",
  "address": "string"
}
```

## Configuration

The integration is configured in `application.properties`:

```properties
# Postman API 101 Collection Configuration
postman.api.base-url=https://postman-echo.com
```

## Service Methods

### PostmanEchoService

The service provides the following methods:

#### Synchronous Methods
- `getAllCustomers()` - Retrieve all customers
- `getCustomerById(String id)` - Get customer by ID
- `addNewCustomer(Customer customer)` - Add new customer
- `updateCustomer(String id, Customer customer)` - Update customer
- `removeCustomer(String id)` - Remove customer

#### Asynchronous Methods
- `getAllCustomersAsync()` - Retrieve all customers asynchronously
- `getCustomerByIdAsync(String id)` - Get customer by ID asynchronously
- `addNewCustomerAsync(Customer customer)` - Add new customer asynchronously

## Error Handling

All methods include comprehensive error handling:
- Network timeouts and connection errors
- Invalid responses from external API
- Missing or invalid customer data
- Proper logging for debugging

## Testing

Use the provided `customer-api-requests.http` file to test all endpoints:

```bash
# Start the application
mvn spring-boot:run

# Test endpoints using the HTTP file in your IDE
# Or use curl commands:
curl http://localhost:8080/api/customers/health
curl http://localhost:8080/api/customers
curl http://localhost:8080/api/customers/async
```

## Benefits of Integration

1. **Learning Tool**: Perfect for understanding API request/response patterns
2. **Testing**: Provides a reliable external API for testing connection pool behavior
3. **Async Support**: Demonstrates both synchronous and asynchronous API patterns
4. **Error Handling**: Shows robust error handling for external API calls
5. **Connection Pool Testing**: Helps test how external API delays affect database connection pools

## Connection Pool Considerations

The integration is particularly useful for testing connection pool behavior because:

- **Synchronous calls** hold database connections during external API delays
- **Asynchronous calls** release database connections immediately, preventing pool exhaustion
- **External API delays** simulate real-world scenarios where external services are slow
- **Error scenarios** test how the application handles external API failures

This makes it an excellent tool for demonstrating the importance of asynchronous processing in microservices architectures.
