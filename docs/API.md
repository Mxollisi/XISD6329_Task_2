# Giant Auto Upholstery - REST API Documentation

## Architecture & Overview
The Giant Auto Upholstery Android application integrates with a robust asynchronous REST API client simulating live HTTP/REST endpoints. The API architecture is designed to reflect enterprise-grade cloud services, featuring standardized JSON response wrapping, comprehensive HTTP status codes (`200 OK`, `201 Created`, `400 Bad Request`, `500 Server Error`), custom request headers, timeout handling, error propagation, and mock latency simulation.

- **Base URL**: `https://api.giantautoupholstery.co.za/v1`
- **Protocol**: HTTPS (TLS 1.3)
- **Serialization**: JSON (strict schema mapping)

---

## Authentication & Headers
All requests sent by `UpholsteryApiClient` include enterprise transport headers:
- `Content-Type`: `application/json`
- `Accept`: `application/json`
- `User-Agent`: `GiantAutoUpholstery-Android/2.0`
- `X-Client-Version`: `2.0.0`
- `X-Environment`: `Production-Simulation`
- `Authorization`: `Bearer <token>` (for authenticated endpoints)
- `X-Request-Id`: `<UUID>` (for end-to-end request tracing)

---

## Standard Response Wrapper (`ApiResponse<T>`)
Every API response is encapsulated in a generic wrapper ensuring uniform parsing and error inspection:

```json
{
  "statusCode": 200,
  "statusText": "OK",
  "success": true,
  "message": "Successfully retrieved services from cloud API.",
  "data": { ... },
  "timestamp": 1747382400000,
  "headers": {
    "X-Request-Id": "req-9b1deb4d-3b7d",
    "X-RateLimit-Remaining": "98"
  }
}
```

---

## Endpoints

### 1. Get Services Catalog
- **Endpoint**: `GET /api/v1/services`
- **Description**: Retrieves the complete catalog of automotive and furniture upholstery services offered by Giant Auto Upholstery.
- **Headers**: `Authorization: Bearer <token>` (optional), `X-Request-Id`
- **Response (`200 OK`)**:
  ```json
  {
    "statusCode": 200,
    "statusText": "OK",
    "success": true,
    "message": "Successfully retrieved 5 upholstery services from cloud API.",
    "data": [
      {
        "id": "serv-1",
        "name": "Full Leather Re-upholstery",
        "category": "Leather",
        "priceCents": 450000,
        "duration": "3 days",
        "description": "Complete restoration of automotive leather interior with premium imported hides.",
        "imageRef": null
      }
    ]
  }
  ```

### 2. Get Frequently Asked Questions (FAQs)
- **Endpoint**: `GET /api/v1/faqs`
- **Description**: Fetches customer support FAQs and warranty guidelines.
- **Response (`200 OK`)**:
  ```json
  {
    "statusCode": 200,
    "statusText": "OK",
    "success": true,
    "message": "Successfully retrieved FAQs from cloud API.",
    "data": [
      {
        "id": "faq-1",
        "question": "How long does a full leather upholstery job take?",
        "answer": "Typically 2 to 4 working days depending on vehicle complexity.",
        "displayOrder": 1
      }
    ]
  }
  ```

### 3. Sync Offline Bookings
- **Endpoint**: `POST /api/v1/bookings/sync`
- **Description**: Synchronizes locally created offline/pending customer bookings with the cloud database.
- **Request Body**:
  ```json
  {
    "uid": "user-firebase-uid-12345",
    "submissions": [
      {
        "reference": "GAU-8923-01",
        "serviceId": "serv-1",
        "preferredDate": "2025-05-15",
        "preferredTime": "10:00",
        "address": "123 Main Road, Sandton, JHB",
        "status": "Pending"
      }
    ]
  }
  ```
- **Response (`201 Created`)**:
  ```json
  {
    "statusCode": 201,
    "statusText": "Created",
    "success": true,
    "message": "Cloud synchronization committed successfully.",
    "data": {
      "syncedCount": 1,
      "failedCount": 0,
      "syncToken": "sync-token-4a92c1",
      "message": "Successfully synchronized 1 booking(s) with cloud server."
    }
  }
  ```

### 4. Submit Support Ticket
- **Endpoint**: `POST /api/v1/support/tickets`
- **Description**: Submits customer inquiries or custom quotation requests directly to the support queue.
- **Request Body**:
  ```json
  {
    "name": "Thabo Mokoena",
    "email": "thabo@example.co.za",
    "subject": "Leather Re-upholstery Inquiry",
    "message": "I would like a quote for a 3-piece lounge suite.",
    "priority": "High"
  }
  ```
- **Response (`201 Created`)**:
  ```json
  {
    "statusCode": 201,
    "statusText": "Created",
    "success": true,
    "message": "Support ticket TICKET-4891 successfully submitted to customer support queue.",
    "data": {
      "ticketId": "TICKET-4891",
      "status": "Open",
      "estimatedResponseHours": 4,
      "createdAt": 1747382400000
    }
  }
  ```

---

## Error Handling & HTTP Status Codes
The API client gracefully handles and maps HTTP errors:
- **`200 OK` / `201 Created`**: Successful operation.
- **`400 Bad Request`**: Validation failure (e.g., missing mandatory payload fields).
- **`500 Server Error`**: Simulated backend database timeout or server failure.
- **Mock Latency**: All mock endpoints incorporate realistic network delays (`250ms` to `500ms`) using Kotlin coroutines `delay()`.
