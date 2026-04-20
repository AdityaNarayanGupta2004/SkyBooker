# SkyBooker — Backend (Airline Ticket Booking System)

SkyBooker is a flight booking system built with Spring Boot microservices. Each part of the system — auth, flights, bookings, payments, seats, passengers, airlines — runs as a separate service with its own database. All frontend traffic goes through a single API Gateway.

---

## Tech Stack

Java 17 and Spring Boot 3.2.0 is used for all microservices. Routing is handled by Spring Cloud Gateway 2023.0.0. Security is built on Spring Security with JWT tokens signed using HMAC-SHA256, and passwords are hashed with BCrypt. Data layer uses Spring Data JPA with Hibernate on top of MySQL 8.0. Build tool is Maven and boilerplate is reduced using Lombok. API documentation is available through SpringDoc OpenAPI (Swagger).

---

## Services and Ports

| Service | Port | Database |
|---|---|---|
| API Gateway | 8080 | — |
| Auth Service | 8081 | skybooker_auth_db |
| Flight Service | 8082 | skybooker_flight_db |
| Booking Service | 8083 | skybooker_booking_db |
| Passenger Service | 8084 | skybooker_passenger_db |
| Payment Service | 8085 | skybooker_payment_db |
| Seat Service | 8086 | skybooker_seat_db |
| Airline Service | 8087 | skybooker_airline_db |

---

## Project Structure

```
SkyBooker/
├── api-gateway/
│   └── src/main/resources/application.yml       # routes + CORS config
│
├── auth-service/
│   └── src/main/java/com/skybooker/auth/
│       ├── controller/AuthController.java
│       ├── service/AuthServiceImpl.java
│       ├── security/JwtUtil.java
│       ├── security/JwtFilter.java
│       ├── entity/User.java
│       ├── dto/RegisterRequest.java
│       └── config/SecurityConfig.java
│
├── flight-service/
│   └── src/main/java/com/skybooker/flight/
│       ├── controller/FlightController.java
│       ├── service/FlightServiceImpl.java        # date validation + auto seat generation
│       └── entity/Flight.java
│
├── flight-booking/
│   └── src/main/java/com/skybooker/booking/
│       ├── controller/BookingController.java
│       └── service/BookingServiceImpl.java       # calls flight-service internally
│
├── passenger-service/
│   └── src/main/java/com/skybooker/passenger/
│       ├── controller/PassengerController.java
│       └── service/PassengerServiceImpl.java     # ticket number generation + validation
│
├── payment-service/
│   └── src/main/java/com/skybooker/payment/
│       ├── controller/PaymentController.java
│       └── service/PaymentServiceImpl.java       # transaction ID + refund logic
│
├── seat-service/
│   └── src/main/java/com/skybooker/seat/
│       ├── controller/SeatController.java
│       ├── service/SeatServiceImpl.java          # AVAILABLE → HELD → CONFIRMED + scheduler
│       └── config/SchedulingConfig.java
│
└── airline-service/
    └── src/main/java/com/skybooker/airline/
        ├── controller/AirlineController.java
        └── entity/Airline.java + Airport.java
```

Each service follows the same internal structure: `controller → service → repository → entity`, with its own `JwtFilter`, `JwtUtil`, and `SecurityConfig`.

---

## How JWT Works Here

Auth Service generates a JWT on login containing the user's email and role. This token is signed with HMAC-SHA256 using a secret key that is shared across all services. The API Gateway does not validate tokens — it just routes requests. Each service has its own `JwtFilter` that validates the token on every incoming request and sets the Spring `SecurityContext`. Role-based rules (`hasRole`, `hasAnyRole`) are then applied via each service's `SecurityConfig`.

Token validity is 24 hours. The same secret string must be present in every service's `application.properties`.

---

## Registration Security

Three roles exist: `PASSENGER`, `AIRLINE_STAFF`, and `ADMIN`.

Anyone can register as a Passenger. Staff registration requires a secret key (`SkyStaff#2025`) that the airline administrator shares out-of-band. Admin registration requires a different key (`SkyAdmin#9999`) and is limited to a maximum of 5 admin accounts. Both keys are configured in `auth-service/src/main/resources/application.properties`.

```properties
app.admin.secret-key=SkyAdmin#9999
app.staff.secret-key=SkyStaff#2025
```

---

## Seat Hold System

When a user selects a seat, it gets marked as `HELD` for 15 minutes. If payment completes in time, the seat moves to `CONFIRMED`. If not, a `@Scheduled` job running every 2 minutes automatically releases expired holds back to `AVAILABLE`.

```
AVAILABLE → (user selects) → HELD → (payment done) → CONFIRMED
                                  → (15 min expire) → AVAILABLE
```

---

## Service-to-Service Calls

There are only two internal HTTP calls in the entire system.

When a booking is created, Booking Service calls Flight Service via `RestTemplate` to reduce the available seat count. The user's JWT is forwarded in this call. When a new flight is added, Flight Service calls Seat Service to auto-generate all seats. This uses a short-lived internal JWT (5 minutes, role=AIRLINE_STAFF) that the Flight Service generates itself.

No message queue is used — everything is synchronous REST.

---

## API Endpoints

**Auth** — no token needed
- `POST /auth/register`
- `POST /auth/login`

**Flights**
- `GET /flights/search?source=&destination=&date=` — public, no auth
- `GET /flights` — authenticated
- `POST /flights` — AIRLINE_STAFF or ADMIN
- `PUT /flights/{id}/reduce-seats?seats=` — authenticated

**Bookings**
- `POST /bookings` — PASSENGER only

**Passengers**
- `POST /passengers`
- `GET /passengers/booking/{bookingId}`
- `GET /passengers/ticket/{ticketNumber}`
- `PUT /passengers/{id}`
- `PUT /passengers/assign-seat`
- `DELETE /passengers/booking/{bookingId}`

**Payments**
- `POST /payments`
- `GET /payments/booking/{bookingId}`
- `GET /payments/user/{email}`
- `POST /payments/refund/{bookingId}`

**Seats**
- `GET /seats/flight/{flightId}/available`
- `PUT /seats/flight/{flightId}/hold/{seatNumber}`
- `PUT /seats/flight/{flightId}/confirm/{seatNumber}`
- `PUT /seats/flight/{flightId}/release/{seatNumber}`

**Airlines / Airports**
- `GET /airlines`, `POST /airlines`, `PUT /airlines/{id}/toggle-status`
- `GET /airports`, `POST /airports`, `GET /airports/search?keyword=`

---

## Setup

### 1. Create databases

```sql
CREATE DATABASE skybooker_auth_db;
CREATE DATABASE skybooker_flight_db;
CREATE DATABASE skybooker_booking_db;
CREATE DATABASE skybooker_passenger_db;
CREATE DATABASE skybooker_payment_db;
CREATE DATABASE skybooker_seat_db;
CREATE DATABASE skybooker_airline_db;
```

Tables are created automatically by Hibernate on first run.

### 2. Update credentials

In each service's `application.properties`, set your MySQL password:

```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

### 3. Start services

Open a separate terminal for each and run in this order:

```bash
cd auth-service       && mvn spring-boot:run
cd flight-service     && mvn spring-boot:run
cd flight-booking     && mvn spring-boot:run
cd passenger-service  && mvn spring-boot:run
cd payment-service    && mvn spring-boot:run
cd seat-service       && mvn spring-boot:run
cd airline-service    && mvn spring-boot:run
cd api-gateway        && mvn spring-boot:run   # start this last
```

Gateway health check: `http://localhost:8080/actuator/health`

---

## Swagger

Each service exposes Swagger UI at `/swagger-ui.html` on its own port. For example, Auth Service docs are at `http://localhost:8081/swagger-ui.html`.

 ---
Author  
Aditya Narayan Gupta
