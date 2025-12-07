Tuyệt vời\! Đây là bản dịch tiếng Anh của file `README.md` cho dự án của bạn:

-----

# 🔋 EV Battery Swap Station Management System (Backend)

## 📝 Project Description

This project is a comprehensive backend system for managing Electric Vehicle (EV) Battery Swap Stations. The system handles complex processes including battery lifecycle management, swap transactions, a subscription package system, tiered overage billing, VNPay payment integration, battery reservation functionality, and user reputation management.

## 🛠️ Tech Stack

| Category | Technology | Details |
| :--- | :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.3.4 | Main framework |
| **Database** | PostgreSQL, JPA/Hibernate | Database and ORM |
| **Security** | Spring Security, JWT (jjwt-api) | Authentication and Authorization (USER, STAFF, ADMIN) |
| **Payment** | VNPay Integration | Handles payment for renewals and overages |
| **Utilities** | Lombok, Geodesy | Reduces boilerplate code, calculates distance/geolocation |
| **Docs** | Swagger/OpenAPI (springdoc-openapi) | Automatic API documentation |

## 💡 Core Features

The system supports the following main functionalities:

### 1\. Swap Transaction Management

  * **Swap Transaction:** Creates a swap transaction (`PENDING_CONFIRM`) and awaits Staff confirmation.
  * **Battery Lifecycle:** Tracks battery status (`AVAILABLE`, `IN_USE`, `RESERVED`, `MAINTENANCE`) and event history (`BatteryHistory`).
  * **Degradation:** Calculates battery State of Health (SoH) degradation and Equivalent Full Cycle (EFC) count after each swap.

### 2\. Subscription and Payment System

  * **Initial Payment:** Requires payment of the initial invoice (`SUBSCRIPTION_RENEWAL`) to activate the plan (`PENDING` → `ACTIVE`) and assign batteries to the vehicle.
  * **Auto Renewal:** A daily job runs to create renewal invoices when a subscription expires.
  * **Overage Billing (Tiered Pricing):** Automatically calculates overage fees (km/kWh) that exceed the base usage, using a progressive tiered pricing model.
  * **VNPay Integration:** Supports generating VNPay payment URLs and processing callback (IPN) from VNPay.
  * **Invoice Management:** Manages two types of invoices: `SWAP_OVERAGE` and `SUBSCRIPTION_RENEWAL`.

### 3\. Battery Reservation and Reputation

  * **Reservation:** Allows users to reserve batteries at a station for a **1-hour** window (`AVAILABLE` → `RESERVED`).
  * **Auto-Expire:** A cron job runs every minute to automatically cancel overdue reservations (`ACTIVE` → `EXPIRED`) and release the batteries.
  * **Reputation System:** Calculates a reputation score (max 6 points/month) based on reservation cancellations (-1 point) or expirations (-2 points). Users with 0 points are blocked from making new reservations.
  * **SoH Validation:** Ensures the selected/swapped batteries comply with the user's subscription plan's allowed State of Health (SoH) range.

### 4\. Location and Authorization Management

  * **Location:** Finds the nearest station (`/api/location/nearest`) and calculates distance based on GPS coordinates.
  * **User Roles:** Supports three roles (`USER`, `STAFF`, `ADMIN`) with varying access privileges (`@PreAuthorize`).
  * **Staff Assignment:** Assigns Staff members to a specific station to manage inventory and confirm swaps.

## 🚀 Setup and Launch

### 1\. Prerequisites

  * Java Development Kit (JDK) 17+
  * Maven
  * PostgreSQL Database (Neon.tech is configured by default)

### 2\. Database Configuration

Update the DB connection information in the `src/main/resources/application.properties` file:

```properties
spring.datasource.url=jdbc:postgresql://<your_host>/<your_db>?sslmode=require&channel_binding=require
spring.datasource.username=...
spring.datasource.password=...
# ...
spring.jpa.hibernate.ddl-auto=update  # Automatically runs migrations (Flyway/Liquibase is not used)
```

### 3\. VNPay Configuration

Update the VNPay merchant information in `application.properties`:

```properties
vnpay.tmn-code=YOUR_TMN_CODE_FROM_VNPAY
vnpay.hash-secret=YOUR_HASH_SECRET_FROM_VNPAY
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/api/payment/vnpay-return
```

*Note: For the Production environment, replace `sandbox.vnpayment.vn` with the official URL.*

### 4\. Launch

Use the Maven Wrapper to run the application:

```bash
./mvnw spring-boot:run
```

*The application will run on port `8080` (default Spring Boot).*

Tuyệt vời\! Đây là bản dịch tiếng Anh của file `README.md` cho dự án của bạn:

-----

# 🔋 EV Battery Swap Station Management System (Backend)

## 📝 Project Description

This project is a comprehensive backend system for managing Electric Vehicle (EV) Battery Swap Stations. The system handles complex processes including battery lifecycle management, swap transactions, a subscription package system, tiered overage billing, VNPay payment integration, battery reservation functionality, and user reputation management.

## 🛠️ Tech Stack

| Category | Technology | Details |
| :--- | :--- | :--- |
| **Backend** | Java 17, Spring Boot 3.3.4 | Main framework |
| **Database** | PostgreSQL, JPA/Hibernate | Database and ORM |
| **Security** | Spring Security, JWT (jjwt-api) | Authentication and Authorization (USER, STAFF, ADMIN) |
| **Payment** | VNPay Integration | Handles payment for renewals and overages |
| **Utilities** | Lombok, Geodesy | Reduces boilerplate code, calculates distance/geolocation |
| **Docs** | Swagger/OpenAPI (springdoc-openapi) | Automatic API documentation |

## 💡 Core Features

The system supports the following main functionalities:

### 1\. Swap Transaction Management

  * **Swap Transaction:** Creates a swap transaction (`PENDING_CONFIRM`) and awaits Staff confirmation.
  * **Battery Lifecycle:** Tracks battery status (`AVAILABLE`, `IN_USE`, `RESERVED`, `MAINTENANCE`) and event history (`BatteryHistory`).
  * **Degradation:** Calculates battery State of Health (SoH) degradation and Equivalent Full Cycle (EFC) count after each swap.

### 2\. Subscription and Payment System

  * **Initial Payment:** Requires payment of the initial invoice (`SUBSCRIPTION_RENEWAL`) to activate the plan (`PENDING` → `ACTIVE`) and assign batteries to the vehicle.
  * **Auto Renewal:** A daily job runs to create renewal invoices when a subscription expires.
  * **Overage Billing (Tiered Pricing):** Automatically calculates overage fees (km/kWh) that exceed the base usage, using a progressive tiered pricing model.
  * **VNPay Integration:** Supports generating VNPay payment URLs and processing callback (IPN) from VNPay.
  * **Invoice Management:** Manages two types of invoices: `SWAP_OVERAGE` and `SUBSCRIPTION_RENEWAL`.

### 3\. Battery Reservation and Reputation

  * **Reservation:** Allows users to reserve batteries at a station for a **1-hour** window (`AVAILABLE` → `RESERVED`).
  * **Auto-Expire:** A cron job runs every minute to automatically cancel overdue reservations (`ACTIVE` → `EXPIRED`) and release the batteries.
  * **Reputation System:** Calculates a reputation score (max 6 points/month) based on reservation cancellations (-1 point) or expirations (-2 points). Users with 0 points are blocked from making new reservations.
  * **SoH Validation:** Ensures the selected/swapped batteries comply with the user's subscription plan's allowed State of Health (SoH) range.

### 4\. Location and Authorization Management

  * **Location:** Finds the nearest station (`/api/location/nearest`) and calculates distance based on GPS coordinates.
  * **User Roles:** Supports three roles (`USER`, `STAFF`, `ADMIN`) with varying access privileges (`@PreAuthorize`).
  * **Staff Assignment:** Assigns Staff members to a specific station to manage inventory and confirm swaps.

## 🚀 Setup and Launch

### 1\. Prerequisites

  * Java Development Kit (JDK) 17+
  * Maven
  * PostgreSQL Database (Neon.tech is configured by default)

### 2\. Database Configuration

Update the DB connection information in the `src/main/resources/application.properties` file:

```properties
spring.datasource.url=jdbc:postgresql://<your_host>/<your_db>?sslmode=require&channel_binding=require
spring.datasource.username=...
spring.datasource.password=...
# ...
spring.jpa.hibernate.ddl-auto=update  # Automatically runs migrations (Flyway/Liquibase is not used)
```

### 3\. VNPay Configuration

Update the VNPay merchant information in `application.properties`:

```properties
vnpay.tmn-code=YOUR_TMN_CODE_FROM_VNPAY
vnpay.hash-secret=YOUR_HASH_SECRET_FROM_VNPAY
vnpay.pay-url=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
vnpay.return-url=http://localhost:8080/api/payment/vnpay-return
```

*Note: For the Production environment, replace `sandbox.vnpayment.vn` with the official URL.*

### 4\. Launch

Use the Maven Wrapper to run the application:

```bash
./mvnw spring-boot:run
```

*The application will run on port `8080` (default Spring Boot).*

## 📡 Key API Endpoints

All APIs are protected by JWT and can be accessed via Swagger UI.

### 1\. Authentication and User

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/auth/register` | `POST` | `PUBLIC` | Register a new account |
| `/api/auth/login` | `POST` | `PUBLIC` | Log in and receive JWT |
| `/api/user/profile` | `GET/PUT` | `USER` | View/update personal information |

### 2\. Battery Swap (User)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/user/swap` | `POST` | `USER` | Create a swap request (return old battery, await Staff) |
| `/api/user/swap/history` | `GET` | `USER` | View swap transaction history |
| `/api/user/reservations` | `POST` | `USER` | Create a battery reservation |
| `/api/user/reservations/{id}` | `DELETE` | `USER` | Cancel a reservation |
| `/api/user/reputation` | `GET` | `USER` | Check reservation reputation score |

### 3\. Operation (Staff/Admin)

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/staff/swap/pending` | `GET` | `STAFF/ADMIN` | View list of pending swap transactions |
| `/api/staff/swap/{id}/confirm` | `PUT` | `STAFF/ADMIN` | Confirm a swap transaction (select new battery) |
| `/api/staff/batteries` | `GET` | `STAFF` | View batteries at the assigned station |
| `/api/admin/batteries/transfer` | `POST` | `ADMIN` | Transfer battery between stations |
| `/api/admin/users/staff/{id}/assign-station` | `PUT` | `ADMIN` | Assign a Staff member to a station |

### 4\. Payment

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/user/invoices` | `GET` | `USER` | Get user's invoice list |
| `/api/payment/create-vnpay-url` | `POST` | `USER` | Generate VNPay payment URL for an Invoice |
| `/api/payment/vnpay-return` | `GET` | `PUBLIC` | Callback/webhook to process VNPay result (updates Invoice/Subscription) |


## 📚 Reference Documentation

  * **VNPAY Integration Guide** [VNPAY\_INTEGRATION\_GUIDE.md]
  * **Subscription Renewal & Payment Flow** [SUBSCRIPTION\_RENEWAL\_PAYMENT.md]
  * **Reservation Feature & Logic** [BATTERY\_RESERVATION\_FEATURE.md]
  * **Reputation System Details** [REPUTATION\_SYSTEM\_GUIDE.md]
  * **Tiered Pricing Calculation** [TIERED\_PRICING\_CALCULATION.md]

-----
