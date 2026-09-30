# Hotel Booking System

Backend for a hotel booking platform: discover properties, onboard single properties or chains, book rooms, pay, and cancel.
Java 21 · Spring Boot 4.1.1 · Maven · in-memory storage · Swagger UI · Docker · Render.

> **Live demo:** https://hotel-booking-66so.onrender.com (Swagger UI) · health: https://hotel-booking-66so.onrender.com/actuator/health
> Free Render instances sleep after 15 min idle — the first request can take ~1 minute. Data is in memory and resets on every restart; load demo data with `make seed BASE_URL=https://hotel-booking-66so.onrender.com`.
>
> **Trying the API?** [DEMO.md](DEMO.md) has every endpoint with a working `curl`, how to run with demo data, and a 5-minute guided demo.

## Run locally

Requires JDK 21 (Maven is provided by the wrapper) and Docker for the image targets. Run `make` to list commands:

| Command | What it does |
|---|---|
| `make run` | Start the app locally → http://localhost:8081 (Swagger UI), empty |
| `make run-seeded` | Same, and loads 14 demo entries once it's up ([details](DEMO.md#2-run-with-demo-data-make-run-seeded)) |
| `make seed [BASE_URL=…]` | Load the demo entries into an already running app (local or Render) |
| `make test` | Run all 91 tests |
| `make test-coverage` | Run tests with JaCoCo coverage; prints a per-package line/branch table and writes `target/site/jacoco/index.html` |
| `make build` | Build `target/app.jar` |
| `make docker-run` | Build the image and run it → http://localhost:8081 |
| `make docker-push DOCKER_USER=<you> [TAG=v1]` | Build for `linux/amd64` and push to Docker Hub (run `docker login` first) |
| `make clean` | Remove build output |

Without make: `./mvnw spring-boot:run`, `./mvnw test`, `docker build -t hotel-booking . && docker run --rm -p 8081:8081 hotel-booking`.

| URL | What |
|---|---|
| `/` or `/swagger-ui.html` | Swagger UI |
| `/v3/api-docs` | OpenAPI JSON |
| `/actuator/health` | Health check (used by Render) |

## Architecture

```
api        → controllers, request/response DTOs (the API never returns domain objects), GlobalExceptionHandler (only place that knows HTTP codes)
service    → use cases: PropertyService, SearchService, AvailabilityService, BookingService, PaymentService,
             CancellationService, InventoryLock (JvmInventoryLock)
  search/  → SearchFilter + PriceRange/Amenities/StarRating filters
  payment/ → PaymentProcessor (Card/Upi/Wallet) + PaymentGateway (MockPaymentGateway)
  refund/  → RefundPolicy per booking type (FlexibleRefundPolicy, NonRefundableRefundPolicy)
  pricing/ → PricingStrategy (StandardPricingStrategy)
repository → OwnerRepository, PropertyRepository, BookingRepository + in-memory implementations
domain     → Owner, Property, RoomType, Booking, DateRange, Location, Payment + enums (plain Java, no Spring)
exception  → NotFound (404), InvalidInput (400), RoomNotAvailable (409), InvalidBookingState (409), RefundFailed (502)
```

Dependency rule: `api → service → repository → domain → exception`.

## Key design decisions

- **Inventory is computed, not stored.** Free rooms = `totalRooms` minus rooms taken on the *busiest night* of the requested stay. No counter to drift; cancelling or a declined payment frees the room with no extra code.
- **Double-booking prevention.** "Check availability, then save" runs under a lock per room type (`InventoryLock`). Pay and cancel use the same lock and re-read the booking inside it.
- **Booking lifecycle** is an enum with an exhaustive `switch`; `Booking` has no status setter:
  ```
  PENDING_PAYMENT ──pay ok──► CONFIRMED ──cancel──► CANCELLED
        │ └──────────────cancel──────────────────────▲
        └──pay declined──► PAYMENT_FAILED
  ```
  Rooms are held only in `PENDING_PAYMENT` and `CONFIRMED`. Every other move returns 409.
- **Extension points** — each new behaviour is one new class, no service changes:

  | Interface | Add a… |
  |---|---|
  | `SearchFilter` | new search filter (Spring injects every `@Component` filter) |
  | `PaymentProcessor` | new payment method (+1 enum constant) |
  | `PaymentGateway` | real provider instead of the mock |
  | `RefundPolicy` | new booking type with its own refund rule (+1 `BookingType` constant); the app won't start if a type has no policy |
  | `PricingStrategy` | new pricing rule, e.g. weekend or seasonal rates (`@Primary`) |
  | `*Repository` | database instead of memory |
  | `InventoryLock` | database / distributed lock instead of the in-process `JvmInventoryLock` |

- **Single property vs chain.** An owner has 1..N properties; a standalone property is simply an owner with one. No flag, no branching.

## Assumptions

- Dates are a half-open range `[checkIn, checkOut)`: a checkout and a check-in on the same day don't clash. 1–30 nights; check-in can't be in the past. "Today" is the `Asia/Kolkata` date.
- One booking = one room of one room type; `guests ≤ maxGuests` of that room type.
- Search needs `city` (locality narrows within it). Price filter is per night; amenities must all match.
- Money is `BigDecimal` INR; price = rate × nights, fixed on the booking at creation.
- One payment attempt per booking; the server decides the amount. A decline is final (book again). An optional `Idempotency-Key` header makes a retried payment return the same result without charging twice.
- Pay or cancel is allowed up to and including the check-in date. If the payment provider declines a refund, the cancel fails with 502 and the booking stays confirmed.
- No auth, no UI, no property edits (out of scope). "property/maid kinds" in the brief is read as property kinds (`PropertyType`).
- Booking types are chosen per booking (`bookingType`, default `FLEXIBLE`) and decide only the refund:
  - `FLEXIBLE` — refund 100 % if cancelled 7+ days before check-in, 50 % at 1–6 days, 0 % on the day.
  - `NON_REFUNDABLE` — no refund; cancelling still releases the room.
  - Unpaid bookings refund 0, and a refund is capped at the amount paid whatever a policy returns. Every type pays the same price (price per night × nights); the brief sets no discounts.
  - A new booking type = 1 `BookingType` constant + 1 `RefundPolicy` class.

## Trade-offs

- The lock lives in one JVM — correct for a single instance (Render free). Scaling out needs a database row lock (`SELECT … FOR UPDATE`): a new `InventoryLock` implementation plus database repositories; services don't change.
- In-memory data is lost on restart.
- An unpaid booking holds its room until paid, declined or cancelled (no hold expiry yet).
- The mock gateway is called inside the lock; a real, slow gateway would need an async `PAYMENT_IN_PROGRESS` step.

## With more time

Postgres + row locks · hold expiry (15-min hold, localised to `Booking.holdsRoom()`) · multi-room bookings · webhooks for async payments · pagination and sorting in search · CI running `./mvnw test`.

## Deploy to Render

`render.yaml` defines a free Docker web service with `healthCheckPath: /actuator/health`.
Push to GitHub → Render dashboard → **New → Blueprint** → select the repo → **Apply**. Render injects `PORT`; the app binds to it.
