# Order Service

A microservice for managing orders in an e-commerce system built with Spring Boot 3.2.1 (LTS).

## Technologies

- **Spring Boot 3.2.1** (Long-Term Support version)
- **Java 21**
- **Spring Data JPA**
- **H2 Database** (for development)
- **PostgreSQL** (for production)
- **Lombok**
- **Spring Boot Actuator**
- **Maven**

## Features

- Create new orders with multiple items
- Retrieve orders by ID, order number, customer ID, or status
- Update order status
- Cancel orders
- Delete orders
- Automatic order number generation
- Total amount calculation
- Comprehensive validation
- Global exception handling
- Health checks and metrics via Actuator

## Order Statuses

- `PENDING` - Order created, awaiting confirmation
- `CONFIRMED` - Order confirmed
- `PROCESSING` - Order being prepared
- `SHIPPED` - Order shipped to customer
- `DELIVERED` - Order delivered successfully
- `CANCELLED` - Order cancelled
- `REFUNDED` - Order refunded

## API Endpoints

### Create Order
```
POST /api/v1/orders
Content-Type: application/json

{
  "customerId": 1,
  "shippingAddress": "123 Main St, City, Country",
  "billingAddress": "123 Main St, City, Country",
  "items": [
    {
      "productId": 1,
      "productName": "Product Name",
      "quantity": 2,
      "unitPrice": 50.00
    }
  ]
}
```

### Get Order by ID
```
GET /api/v1/orders/{id}
```

### Get Order by Order Number
```
GET /api/v1/orders/order-number/{orderNumber}
```

### Get All Orders
```
GET /api/v1/orders
```

### Get Orders by Customer ID
```
GET /api/v1/orders/customer/{customerId}
```

### Get Orders by Status
```
GET /api/v1/orders/status/{status}
```

### Update Order Status
```
PATCH /api/v1/orders/{id}/status
Content-Type: application/json

{
  "status": "CONFIRMED"
}
```

### Cancel Order
```
POST /api/v1/orders/{id}/cancel
```

### Delete Order
```
DELETE /api/v1/orders/{id}
```

## Running the Application

### Prerequisites
- Java 21 or higher
- Maven 3.6+

### Development Mode (H2 Database)
```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### H2 Console
Access the H2 console at: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:orderdb`
- Username: `sa`
- Password: (leave empty)

### Production Mode (PostgreSQL)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

Make sure PostgreSQL is running and update the credentials in `application-prod.properties`.

## Building the Application

```bash
mvn clean package
```

The JAR file will be created in the `target` directory.

## Running Tests

```bash
mvn test
```

## Health Check

```
GET http://localhost:8080/actuator/health
```

## Project Structure

```
order-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/ecommerce/order/
│   │   │       ├── controller/
│   │   │       │   └── OrderController.java
│   │   │       ├── dto/
│   │   │       │   ├── CreateOrderRequest.java
│   │   │       │   ├── OrderItemRequest.java
│   │   │       │   ├── OrderResponse.java
│   │   │       │   ├── OrderItemResponse.java
│   │   │       │   └── UpdateOrderStatusRequest.java
│   │   │       ├── entity/
│   │   │       │   ├── Order.java
│   │   │       │   ├── OrderItem.java
│   │   │       │   └── OrderStatus.java
│   │   │       ├── exception/
│   │   │       │   ├── ErrorResponse.java
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   └── OrderNotFoundException.java
│   │   │       ├── repository/
│   │   │       │   └── OrderRepository.java
│   │   │       ├── service/
│   │   │       │   ├── OrderService.java
│   │   │       │   └── OrderServiceImpl.java
│   │   │       └── OrderServiceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       └── application-prod.properties
│   └── test/
│       └── java/
│           └── com/ecommerce/order/
│               ├── controller/
│               │   └── OrderControllerTest.java
│               └── service/
│                   └── OrderServiceImplTest.java
└── pom.xml
```

## Future Enhancements

- Integration with Product Service for inventory checks
- Integration with Payment Service
- Integration with Notification Service
- Event-driven architecture using message brokers
- API Gateway integration
- Service discovery
- Distributed tracing
- Rate limiting
- Caching
