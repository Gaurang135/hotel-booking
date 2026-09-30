# Demo guide

1. [API reference with curl](#1-api-reference-with-curl) — every endpoint, a working `curl`, and a real response
2. [Run with demo data](#2-run-with-demo-data-make-run-seeded) — `make run-seeded` loads 14 entries for you
3. [5-minute guided demo](#3-5-minute-guided-demo) — the core flows end to end

Prefer clicking? Everything below also works from Swagger UI at http://localhost:8081/.

---

## 1. API reference with curl

### Setup

Start the app (`make run`, or `make run-seeded` for demo data — see [section 2](#2-run-with-demo-data-make-run-seeded)), then set these once in your terminal:

```bash
export BASE=http://localhost:8081
export CHECK_IN=$(date -v+30d +%F) CHECK_OUT=$(date -v+32d +%F)
```

On Linux use `date -d '+30 days' +%F` instead. The examples also use `OWNER_ID`, `PROPERTY_ID`, `ROOM_TYPE_ID` and `BOOKING_ID` — copy them from responses, or paste the `export …` line that `make run-seeded` prints. `jq` only pretty-prints; drop `| jq` if you don't have it.

| # | Method | Path | What it does |
|---|---|---|---|
| 1.1 | GET | `/actuator/health` | Health check |
| 1.2 | GET | `/` · `/v3/api-docs` | Swagger UI · OpenAPI JSON |
| 1.3 | POST | `/api/owners` | Create an owner account |
| 1.4 | POST | `/api/owners/{ownerId}/properties` | Onboard a property |
| 1.5 | GET | `/api/owners/{ownerId}/properties` | List an owner's properties |
| 1.6 | GET | `/api/properties/{propertyId}` | Get one property |
| 1.7 | GET | `/api/properties/search` | Search rooms available for dates |
| 1.8 | POST | `/api/bookings` | Book a room type (holds the room) |
| 1.9 | GET | `/api/bookings/{bookingId}` | Get a booking |
| 1.10 | POST | `/api/bookings/{bookingId}/pay` | Pay; success confirms, decline releases the room |
| 1.11 | POST | `/api/bookings/{bookingId}/cancel` | Cancel; refund by policy, room released |

### 1.1 Health

```bash
curl -s $BASE/actuator/health
```
```json
{"groups":["liveness","readiness"],"status":"UP"}
```

### 1.2 Swagger UI and OpenAPI

Open http://localhost:8081/ in a browser (redirects to `/swagger-ui.html`), or fetch the spec:

```bash
curl -s $BASE/v3/api-docs | jq .info
```
```json
{"title": "Hotel Booking API", "version": "v1"}
```

### 1.3 Create an owner

The same call creates a standalone owner or a chain — an owner simply has one or many properties.

```bash
curl -s -X POST $BASE/api/owners -H 'Content-Type: application/json' \
  -d '{"name": "Meera Iyer", "email": "meera@example.com"}' | jq
```
```json
{"id": "ea9257a7-…", "name": "Meera Iyer", "email": "meera@example.com"}
```

`201 Created`. Save the id for the next calls: `export OWNER_ID=<id>`. Errors: `400` if `name` is blank or `email` is invalid.

### 1.4 Onboard a property

```bash
curl -s -X POST $BASE/api/owners/$OWNER_ID/properties -H 'Content-Type: application/json' -d '{
  "name": "Meera Heritage Stay", "type": "HOMESTAY", "city": "Jaipur", "locality": "Civil Lines", "starRating": 4,
  "amenities": ["WIFI", "BREAKFAST", "PARKING"],
  "roomTypes": [{"name": "Heritage Room", "maxGuests": 2, "totalRooms": 3, "pricePerNight": 3500},
                {"name": "Family Room",   "maxGuests": 4, "totalRooms": 1, "pricePerNight": 5500}]}' | jq
```
```json
{
  "id": "ce00b3e2-…",
  "ownerId": "ea9257a7-…",
  "name": "Meera Heritage Stay",
  "type": "HOMESTAY",
  "location": {"city": "Jaipur", "locality": "Civil Lines"},
  "starRating": 4,
  "amenities": ["WIFI", "BREAKFAST", "PARKING"],
  "roomTypes": [
    {"id": "be0f6ee1-…", "name": "Heritage Room", "maxGuests": 2, "totalRooms": 3, "pricePerNight": 3500.00},
    {"id": "019f671c-…", "name": "Family Room",   "maxGuests": 4, "totalRooms": 1, "pricePerNight": 5500.00}
  ]
}
```

`201 Created`. Values: `type` = `HOTEL | RESORT | APARTMENT | VILLA | HOMESTAY`; `amenities` from `WIFI | POOL | PARKING | BREAKFAST | AC | GYM`; `starRating` 1–5; at least one room type; `pricePerNight` > 0 with at most 2 decimals.
Errors: `400` invalid body, `404` unknown owner.

### 1.5 List an owner's properties

```bash
curl -s $BASE/api/owners/$OWNER_ID/properties | jq '[.[] | {name, city: .location.city}]'
```
```json
[{"name": "Asha Indiranagar Homestay", "city": "Bengaluru"}]
```

A chain returns several (`$CHAIN_OWNER_ID` in the demo data has 3). Errors: `404` unknown owner.

### 1.6 Get a property

```bash
curl -s $BASE/api/properties/$PROPERTY_ID | jq
```
Returns the same shape as 1.4. Errors: `404` unknown property.

### 1.7 Search available rooms

Required: `city`, `checkIn`, `checkOut`, `guests`. Optional: `locality`, `minPrice` / `maxPrice` (per night), `amenities` (comma-separated, all must match), `minStars`. Only room types free on **every** night of the stay are returned.

```bash
curl -s "$BASE/api/properties/search?city=Bengaluru&checkIn=$CHECK_IN&checkOut=$CHECK_OUT&guests=2" \
  | jq '.[] | {name, offers: [.offers[] | {name, roomsLeft, totalPrice}]}'
```
```json
{"name": "Sunrise Whitefield", "offers": [{"name": "Standard", "roomsLeft": 8, "totalPrice": 6400.00}]}
{"name": "Asha Indiranagar Homestay", "offers": [{"name": "Entire home", "roomsLeft": 1, "totalPrice": 12000.00}]}
{"name": "Sunrise Koramangala", "offers": [{"name": "Deluxe", "roomsLeft": 5, "totalPrice": 9000.00},
                                           {"name": "Suite", "roomsLeft": 2, "totalPrice": 16000.00}]}
```

With every filter:

```bash
curl -s "$BASE/api/properties/search?city=Goa&locality=Candolim&checkIn=$CHECK_IN&checkOut=$CHECK_OUT&guests=2&minPrice=5000&maxPrice=10000&amenities=POOL,GYM&minStars=4" | jq
```
```json
[
  {
    "propertyId": "a4710d68-…",
    "name": "Coastal Candolim",
    "type": "RESORT",
    "location": {"city": "Goa", "locality": "Candolim"},
    "starRating": 5,
    "amenities": ["POOL", "GYM", "WIFI", "AC", "BREAKFAST"],
    "offers": [{"roomTypeId": "1063b0ed-…", "name": "Premium", "maxGuests": 2, "roomsLeft": 6, "totalPrice": 18000.00}]
  }
]
```

`totalPrice` is for the whole stay. An empty list `[]` means nothing is available. Errors: `400` missing parameter or past dates.

### 1.8 Book a room

```bash
curl -s -X POST $BASE/api/bookings -H 'Content-Type: application/json' -d @- <<EOF | jq
{"propertyId": "$PROPERTY_ID", "roomTypeId": "$ROOM_TYPE_ID", "checkIn": "$CHECK_IN", "checkOut": "$CHECK_OUT", "guests": 4, "guestName": "Ravi Kumar"}
EOF
```
```json
{
  "id": "d4c0a633-…",
  "propertyId": "00a7ee09-…",
  "roomTypeId": "3cb0e1b9-…",
  "guestName": "Ravi Kumar",
  "guests": 4,
  "checkIn": "2026-10-30",
  "checkOut": "2026-11-01",
  "nights": 2,
  "bookingType": "FLEXIBLE",
  "totalPrice": 12000.00,
  "status": "PENDING_PAYMENT",
  "paymentMethod": null,
  "paymentReference": null,
  "refundAmount": 0
}
```

`201 Created`. The room is now held until the booking is paid, declined or cancelled. Save the id: `export BOOKING_ID=<id>`.
Optional `"bookingType"`: `FLEXIBLE` (default — refundable by policy) or `NON_REFUNDABLE` (same price, no refund on cancel). Add it to the JSON, e.g. `"bookingType": "NON_REFUNDABLE"`.
Errors: `400` bad dates or more guests than the room fits, `404` unknown property/room type, `409` no room left for those dates.

### 1.9 Get a booking

```bash
curl -s $BASE/api/bookings/$BOOKING_ID | jq '{id, status, totalPrice}'
```
```json
{"id": "d4c0a633-…", "status": "PENDING_PAYMENT", "totalPrice": 12000.00}
```

Errors: `404` unknown booking.

### 1.10 Pay for a booking

The server charges the booking's `totalPrice`; the client never sends an amount. Details per method:

| `method` | `details` | Example |
|---|---|---|
| `UPI` | `vpa` (must contain `@`) | `{"vpa": "ravi@okaxis"}` |
| `CARD` | `cardToken` | `{"cardToken": "tok_visa_4242"}` |
| `WALLET` | `walletId` | `{"walletId": "paytm-98765"}` |

```bash
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/pay -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: pay-ravi-1' \
  -d '{"method": "UPI", "details": {"vpa": "ravi@okaxis"}}' | jq '{status, paymentMethod, paymentReference}'
```
```json
{"status": "CONFIRMED", "paymentMethod": "UPI", "paymentReference": "pay_d3ffc115-…"}
```

- **Idempotency** (optional header): repeating the call with the same `Idempotency-Key` returns the same result and does not charge again.
- **Declined payment**: any instrument starting with `fail` (e.g. `"vpa": "fail@upi"`) is declined by the mock gateway → `"status": "PAYMENT_FAILED"` and the room is released. A declined booking can't be paid again — book again instead.

Errors: `400` missing/invalid details, `404` unknown booking, `409` booking not payable (already paid, cancelled, failed, or past its check-in date).

### 1.11 Cancel a booking

```bash
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/cancel | jq '{status, totalPrice, refundAmount}'
```
```json
{"status": "CANCELLED", "totalPrice": 12000.00, "refundAmount": 12000.00}
```

The refund depends on the booking's type:

| `bookingType` | Refund |
|---|---|
| `FLEXIBLE` | 7+ days before check-in → 100 %, 1–6 days → 50 %, same day → 0 % |
| `NON_REFUNDABLE` | always 0 |

An unpaid booking refunds 0. Either way the room becomes bookable again immediately.
Errors: `404` unknown booking, `409` already cancelled / failed, or after the check-in date, `502` the payment provider declined the refund (the booking stays `CONFIRMED`; try again).

### Error format

Every error is an [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem that says what went wrong:

| Status | When | Example `detail` |
|---|---|---|
| `400` | Invalid input | `guests: must not be null` · `amenities: invalid value 'FOO'` · `minPrice cannot be greater than maxPrice` · `checkIn cannot be in the past` · `vpa must look like name@bank` · `Entire home fits at most 6 guests` |
| `404` | Unknown id | `Booking not found: does-not-exist` |
| `409` | Not allowed right now | `No rooms left for room type 3cb0e1b9-… on the requested dates` · `Booking d4c0a633-… is CANCELLED and cannot become CANCELLED` |
| `502` | Payment provider declined a refund | `The payment provider declined the refund for booking d4c0a633-…; the booking was not cancelled` |

```json
{"detail": "No rooms left for room type 3cb0e1b9-… on the requested dates", "instance": "/api/bookings", "status": 409, "title": "Conflict"}
```

---

## 2. Run with demo data: `make run-seeded`

```bash
make run-seeded
```

1. Starts the app exactly like `make run` (http://localhost:8081, logs in the foreground, `Ctrl-C` to stop).
2. In the background, [`scripts/seed.sh`](scripts/seed.sh) waits until `/actuator/health` is up.
3. It then creates **14 entries through the public API** — the same calls as section 1 — and prints them:

```
Seeding demo data into http://localhost:8081
  owner     Sunrise Hotels                           8dc6d6e4-…
  owner     Coastal Stays                            da9368ef-…
  owner     Asha Rao                                 abf07116-…
  owner     Vikram Singh                             60f07f54-…
  property  Sunrise Koramangala (Bengaluru)          f4b27238-…
  …
  booking   Ravi Kumar, Sunrise Koramangala Deluxe   CONFIRMED FLEXIBLE  25be543a-…
  booking   Priya Shah, Coastal Candolim Premium     PENDING_PAYMENT NON_REFUNDABLE  401f65df-…
  booking   Arjun Mehta, Sunrise Baga Garden Room    CANCELLED FLEXIBLE  ae237353-…
Done: 14 entries. Swagger UI: http://localhost:8081/

Paste this into your terminal to use the curl examples in DEMO.md:
  export BASE=http://localhost:8081 CHAIN_OWNER_ID=… OWNER_ID=… PROPERTY_ID=… ROOM_TYPE_ID=…
```

Paste that `export` line into your terminal and every curl above works as-is. Ids are generated fresh on each run.

### What gets created

| Owner | Kind | Property | City | Room types (guests · rooms · ₹/night) |
|---|---|---|---|---|
| Sunrise Hotels | chain | Sunrise Koramangala ★4 | Bengaluru | Deluxe (2 · 5 · 4,500), Suite (4 · 2 · 8,000) |
| | | Sunrise Whitefield ★3 | Bengaluru | Standard (2 · 8 · 3,200) |
| | | Sunrise Baga ★4 | Goa | Garden Room (3 · 10 · 3,000), Sea View (2 · 4 · 5,500) |
| Coastal Stays | chain | Coastal Candolim ★5 | Goa | Premium (2 · 6 · 9,000), Family Suite (5 · 2 · 14,000) |
| | | Coastal Colaba ★5 | Mumbai | Executive (2 · 10 · 11,000) |
| Asha Rao | standalone | Asha Indiranagar Homestay ★3 | Bengaluru | Entire home (6 · **1** · 6,000) |
| Vikram Singh | standalone | Lakeview Villa ★4 | Udaipur | Whole villa (8 · **1** · 15,000) |

Plus 3 bookings in different states: **CONFIRMED** (Sunrise Koramangala, in 14 days, flexible), **PENDING_PAYMENT** (Coastal Candolim, in 20 days, **non-refundable**) and **CANCELLED** with a full refund (Sunrise Baga, in 10 days, flexible).
`OWNER_ID` / `PROPERTY_ID` / `ROOM_TYPE_ID` in the export line point at Asha's homestay — it has a single room, which makes double-booking easy to see.

### Seeding an app that is already running

```bash
make seed                                            # local app on port 8081
make seed BASE_URL=https://hotel-booking-66so.onrender.com   # the live deployment
```

Notes:
- Data lives in memory, so it disappears on every restart — seed again after a restart.
- Seeding the same running app twice adds a second copy of the data.
- `make run-seeded` refuses to start if something is already running on the port, so it never seeds the wrong app.
- The script needs `curl` and `jq` (both preinstalled on recent macOS; `brew install jq` otherwise).

---

## 3. 5-minute guided demo

Run `make run-seeded`, paste the `export` line it prints, then run the setup lines from [section 1](#setup) (`CHECK_IN` / `CHECK_OUT`).

**1. A chain and a standalone owner use the same code path.**
```bash
curl -s $BASE/api/owners/$CHAIN_OWNER_ID/properties | jq 'length'   # 3
curl -s $BASE/api/owners/$OWNER_ID/properties | jq 'length'         # 1
```

**2. Search shows only rooms that are free for the dates.**
```bash
curl -s "$BASE/api/properties/search?city=Bengaluru&checkIn=$CHECK_IN&checkOut=$CHECK_OUT&guests=2" | jq '.[].name'
```

**3. Book Asha's homestay (1 room) → `PENDING_PAYMENT`, the room is held.**
```bash
export BOOKING_ID=$(curl -s -X POST $BASE/api/bookings -H 'Content-Type: application/json' -d @- <<EOF | jq -r .id
{"propertyId": "$PROPERTY_ID", "roomTypeId": "$ROOM_TYPE_ID", "checkIn": "$CHECK_IN", "checkOut": "$CHECK_OUT", "guests": 4, "guestName": "Ravi Kumar"}
EOF
)
curl -s $BASE/api/bookings/$BOOKING_ID | jq .status   # "PENDING_PAYMENT"
```

**4. Book the same dates again → `409`, no double-booking.**
```bash
curl -s -X POST $BASE/api/bookings -H 'Content-Type: application/json' -d @- <<EOF | jq .detail
{"propertyId": "$PROPERTY_ID", "roomTypeId": "$ROOM_TYPE_ID", "checkIn": "$CHECK_IN", "checkOut": "$CHECK_OUT", "guests": 2, "guestName": "Someone Else"}
EOF
```

**5. Pay → `CONFIRMED`; retrying with the same `Idempotency-Key` doesn't charge twice (same `paymentReference`).**
```bash
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/pay -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"method": "UPI", "details": {"vpa": "ravi@okaxis"}}' | jq '{status, paymentReference}'
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/pay -H 'Content-Type: application/json' -H 'Idempotency-Key: demo-1' \
  -d '{"method": "UPI", "details": {"vpa": "ravi@okaxis"}}' | jq '{status, paymentReference}'
```

**6. Cancel 30 days ahead → full refund, and the room is searchable again.**
```bash
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/cancel | jq '{status, refundAmount}'   # CANCELLED, 12000.00
curl -s "$BASE/api/properties/search?city=Bengaluru&locality=Indiranagar&checkIn=$CHECK_IN&checkOut=$CHECK_OUT&guests=2" | jq '.[].offers[].roomsLeft'   # 1
```

**7. A declined payment releases the room.**
```bash
export BOOKING_ID=$(curl -s -X POST $BASE/api/bookings -H 'Content-Type: application/json' -d @- <<EOF | jq -r .id
{"propertyId": "$PROPERTY_ID", "roomTypeId": "$ROOM_TYPE_ID", "checkIn": "$CHECK_IN", "checkOut": "$CHECK_OUT", "guests": 4, "guestName": "Ravi Kumar"}
EOF
)
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/pay -H 'Content-Type: application/json' \
  -d '{"method": "UPI", "details": {"vpa": "fail@upi"}}' | jq .status   # "PAYMENT_FAILED"
curl -s "$BASE/api/properties/search?city=Bengaluru&locality=Indiranagar&checkIn=$CHECK_IN&checkOut=$CHECK_OUT&guests=2" | jq '.[].offers[].roomsLeft'   # 1 again
```

**8. A non-refundable booking: same price, but cancelling it refunds 0 — and the room is still released.**
```bash
export BOOKING_ID=$(curl -s -X POST $BASE/api/bookings -H 'Content-Type: application/json' -d @- <<EOF | jq -r .id
{"propertyId": "$PROPERTY_ID", "roomTypeId": "$ROOM_TYPE_ID", "checkIn": "$CHECK_IN", "checkOut": "$CHECK_OUT", "guests": 4, "guestName": "Ravi Kumar", "bookingType": "NON_REFUNDABLE"}
EOF
)
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/pay -H 'Content-Type: application/json' \
  -d '{"method": "UPI", "details": {"vpa": "ravi@okaxis"}}' | jq .status   # "CONFIRMED"
curl -s -X POST $BASE/api/bookings/$BOOKING_ID/cancel | jq '{status, bookingType, totalPrice, refundAmount}'   # CANCELLED, NON_REFUNDABLE, 12000.00, 0
curl -s "$BASE/api/properties/search?city=Bengaluru&locality=Indiranagar&checkIn=$CHECK_IN&checkOut=$CHECK_OUT&guests=2" | jq '.[].offers[].roomsLeft'   # 1
```

That covers all five flows from the brief — discovery, onboarding (single and chain), booking with double-booking prevention, payment driving the booking state, and cancellation with a per-booking-type refund policy and inventory release.
