# Inventory Service

A Spring Boot 3.X microservice for managing product inventory in an e-commerce system.

## Features

- **CRUD Operations**: Create, read, update, and delete inventory items
- **Inventory Management**: Track product quantities, prices, and reservations
- **Reservation System**: Reserve inventory for orders with confirmation/release capabilities
- **Availability Checking**: Check if products are available in requested quantities
- **Stock Management**: Add stock to existing inventory items
- **Exception Handling**: Global exception handling with custom exceptions
- **Validation**: Request validation using Jakarta Validation
- **H2 Database**: In-memory database for development
- **PostgreSQL Support**: Production-ready PostgreSQL configuration

## Technology Stack

- Java 17
- Spring Boot 3.2.1
- Spring Data JPA
- Spring Web
- H2 Database (development)
- PostgreSQL (production)
- Lombok
- Jakarta Validation
- JUnit 5 & Mockito (testing)

## API Endpoints

### Inventory Management

- `POST /api/inventory` - Create new inventory item
- `GET /api/inventory/{id}` - Get inventory by ID
- `GET /api/inventory/product/{productId}` - Get inventory by product ID
- `GET /api/inventory` - Get all inventory items
- `PUT /api/inventory/{id}` - Update inventory item
- `DELETE /api/inventory/{id}` - Delete inventory item

### Inventory Operations

- `GET /api/inventory/check-availability?productId={productId}&quantity={quantity}` - Check product availability
- `POST /api/inventory/reserve` - Reserve inventory for an order
- `POST /api/inventory/release?productId={productId}&quantity={quantity}` - Release reservation
- `POST /api/inventory/confirm?productId={productId}&quantity={quantity}` - Confirm reservation (deducts from stock)
- `POST /api/inventory/add-stock?productId={productId}&quantity={quantity}` - Add stock to inventory

## Request Examples

### Create Inventory
```json
{
  "productId": "PROD-001",
  "productName": "Sample Product",
  "quantity": 100,
  "price": 99.99
}
```

### Reserve Inventory
```json
{
  "productId": "PROD-001",
  "quantity": 5
}
```

### Update Inventory
```json
{
  "productName": "Updated Product Name",
  "quantity": 150,
  "price": 149.99
}
```

## Response Example

```json
{
  "id": 1,
  "productId": "PROD-001",
  "productName": "Sample Product",
  "quantity": 100,
  "reservedQuantity": 10,
  "availableQuantity": 90,
  "price": 99.99,
  "createdAt": "2026-01-21T10:00:00",
  "updatedAt": "2026-01-21T10:00:00"
}
```

## Running the Application

### Development Mode (H2 Database)

```bash
mvn spring-boot:run
```

The application will start on port 8082. H2 Console is available at: http://localhost:8082/h2-console

### Production Mode (PostgreSQL)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Make sure PostgreSQL is running and configure the database credentials in `application-prod.properties`.

## Running Tests

```bash
mvn test
```

## Building the Application

```bash
mvn clean package
```

The executable JAR will be created in the `target` directory.

## Configuration

### Default Configuration (application.properties)
- Port: 8082
- Database: H2 (in-memory)
- H2 Console: Enabled
- Logging: DEBUG level for application

### Production Configuration (application-prod.properties)
- Port: 8082
- Database: PostgreSQL
- H2 Console: Disabled
- Logging: INFO level for application

## Business Logic

### Inventory States
- **Total Quantity**: Total stock available
- **Reserved Quantity**: Stock reserved for pending orders
- **Available Quantity**: Total - Reserved (available for new orders)

### Reservation Workflow
1. **Reserve**: Checks availability and reserves stock (increases reserved quantity)
2. **Release**: Cancels reservation (decreases reserved quantity)
3. **Confirm**: Finalizes reservation (decreases both reserved and total quantity)

## Error Handling

The service includes comprehensive error handling:
- `InventoryNotFoundException`: When inventory item is not found
- `InsufficientInventoryException`: When requested quantity exceeds available stock
- `InventoryAlreadyExistsException`: When attempting to create duplicate inventory
- `MethodArgumentNotValidException`: For validation errors

All errors return standardized error responses with timestamps, status codes, and descriptive messages.

## License

This project is part of the Turkcell AI E-commerce Chaos project.
