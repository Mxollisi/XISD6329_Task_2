# Giant Auto Upholstery - Relational Database Documentation

## Architecture & Overview
The Giant Auto Upholstery Android application utilizes **Room (SQLite)** for robust local persistence, guaranteeing offline availability, transactional integrity, and relational data modeling. Version 2 of the database introduces an advanced relational logging schema (`api_sync_logs`) to track API synchronization events, request/response metadata, and HTTP status codes.

- **Database Name**: `giant_auto_upholstery.db`
- **Schema Version**: `2`
- **Export Schema**: Enabled (`app/schemas/`)

---

## Relational Entity Schema

### 1. `profiles`
Stores user profile attributes and preferences. Authentication credentials (passwords/hashes) are never stored locally and are managed entirely by Firebase Auth.
- `uid` (PRIMARY KEY, TEXT)
- `fullName` (TEXT)
- `phone` (TEXT, nullable)
- `address` (TEXT, nullable)
- `emailNotifications` (BOOLEAN)
- `pushNotifications` (BOOLEAN)
- `themeMode` (TEXT: system | light | dark)
- `createdAt` (INTEGER, epoch millis)
- `updatedAt` (INTEGER, epoch millis)

### 2. `services`
Stores the upholstery catalog items.
- `id` (PRIMARY KEY, TEXT)
- `name` (TEXT)
- `category` (TEXT)
- `priceCents` (INTEGER)
- `duration` (TEXT)
- `description` (TEXT)
- `createdAt` (INTEGER)
- `imageRef` (TEXT, nullable)

### 3. `bookings`
Stores user booking requests with strict relational foreign keys and indices.
- `id` (PRIMARY KEY, TEXT)
- `ownerUid` (TEXT, FOREIGN KEY referencing `profiles(uid)` ON DELETE CASCADE)
- `reference` (TEXT, UNIQUE)
- `serviceId` (TEXT, FOREIGN KEY referencing `services(id)`)
- `preferredDate` (TEXT: ISO yyyy-MM-dd)
- `preferredTime` (TEXT: HH:mm)
- `address` (TEXT)
- `description` (TEXT, nullable)
- `status` (TEXT: Pending | Confirmed | Completed | Cancelled)
- `submissionId` (TEXT, UNIQUE)
- `createdAt` (INTEGER)

### 4. `notifications`
Stores user inbox notifications.
- `id` (PRIMARY KEY, TEXT)
- `ownerUid` (TEXT, FOREIGN KEY referencing `profiles(uid)` ON DELETE CASCADE)
- `title` (TEXT)
- `body` (TEXT)
- `isRead` (BOOLEAN)
- `createdAt` (INTEGER)

### 5. `faqs`
Stores frequently asked questions.
- `id` (PRIMARY KEY, TEXT)
- `question` (TEXT)
- `answer` (TEXT)
- `displayOrder` (INTEGER)

### 6. `contact_messages`
Stores customer support contact submissions.
- `id` (PRIMARY KEY, TEXT)
- `ownerUid` (TEXT, FOREIGN KEY referencing `profiles(uid)` ON DELETE SET NULL, nullable)
- `name` (TEXT)
- `email` (TEXT)
- `subject` (TEXT)
- `message` (TEXT)
- `createdAt` (INTEGER)

### 7. `api_sync_logs` (New in Version 2)
Tracks all REST API synchronization events, endpoints called, HTTP methods, status codes, and execution durations.
- `id` (PRIMARY KEY, TEXT)
- `endpoint` (TEXT)
- `httpMethod` (TEXT: GET | POST)
- `statusCode` (INTEGER: 200, 201, 400, 500)
- `statusText` (TEXT)
- `requestPayload` (TEXT, nullable)
- `responsePayload` (TEXT, nullable)
- `durationMs` (INTEGER)
- `success` (BOOLEAN)
- `errorMessage` (TEXT, nullable)
- `timestamp` (INTEGER, epoch millis)

---

## Indices & Performance Optimization
To ensure high-performance querying and zero UI jank, indices are explicitly defined on foreign keys and frequently searched columns:
- `bookings`: `ownerUid`, `serviceId`, `status`, `(ownerUid, preferredDate)`, `reference` (unique), `submissionId` (unique).
- `notifications`: `ownerUid`, `(ownerUid, isRead)`.
- `api_sync_logs`: `timestamp`, `endpoint`, `statusCode`.

---

## Schema Versioning & Migrations
The database enforces strict migration compliance. `fallbackToDestructiveMigration()` is **never** used to protect user data.
- **Migration 1 -> 2 (`MIGRATION_1_2`)**:
  Creates the `api_sync_logs` table and indexing for optimized log retrieval.
  ```sql
  CREATE TABLE IF NOT EXISTS api_sync_logs (
      id TEXT NOT NULL PRIMARY KEY,
      endpoint TEXT NOT NULL,
      httpMethod TEXT NOT NULL,
      statusCode INTEGER NOT NULL,
      statusText TEXT NOT NULL,
      requestPayload TEXT,
      responsePayload TEXT,
      durationMs INTEGER NOT NULL,
      success INTEGER NOT NULL,
      errorMessage TEXT,
      timestamp INTEGER NOT NULL
  );
  CREATE INDEX IF NOT EXISTS index_api_sync_logs_timestamp ON api_sync_logs(timestamp);
  CREATE INDEX IF NOT EXISTS index_api_sync_logs_endpoint ON api_sync_logs(endpoint);
  CREATE INDEX IF NOT EXISTS index_api_sync_logs_statusCode ON api_sync_logs(statusCode);
  ```

---

## Transactions & Data Integrity
- Multi-step writes (such as booking creation, notification insertion, and API sync logging) are executed inside Room coroutine transactions (`db.withTransaction`) to guarantee ACID compliance and atomic rollbacks on failure.
