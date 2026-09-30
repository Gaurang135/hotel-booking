#!/usr/bin/env bash
# Loads 14 demo entries through the public API: 4 owners, 7 properties and 3 bookings.
set -euo pipefail

BASE_URL="${1:-http://localhost:8081}"
ENTRIES=0

command -v jq >/dev/null || { echo "seed.sh needs jq (brew install jq)" >&2; exit 1; }

post() {
    curl -sS --fail-with-body -X POST "$BASE_URL$1" -H 'Content-Type: application/json' -d "${2:-}"
}

day() {
    date -v+"$1"d +%F 2>/dev/null || date -d "+$1 days" +%F
}

show() {
    ENTRIES=$((ENTRIES + 1))
    printf '  %-9s %-40s %s\n' "$1" "$2" "$3"
}

add_owner() {  # <var> <name> <email>
    local response
    response=$(post /api/owners "$(jq -n --arg name "$2" --arg email "$3" '{name: $name, email: $email}')")
    printf -v "$1" '%s' "$(jq -r .id <<<"$response")"
    show owner "$2" "${!1}"
}

add_property() {  # <propertyVar> <firstRoomTypeVar> <ownerId> <json>
    local response
    response=$(post "/api/owners/$3/properties" "$4")
    printf -v "$1" '%s' "$(jq -r .id <<<"$response")"
    printf -v "$2" '%s' "$(jq -r '.roomTypes[0].id' <<<"$response")"
    show property "$(jq -r '"\(.name) (\(.location.city))"' <<<"$response")" "${!1}"
}

book() {  # <propertyId> <roomTypeId> <fromDay> <toDay> <guestName> <bookingType> -> prints booking id
    post /api/bookings "$(jq -n --arg property "$1" --arg room "$2" --arg checkIn "$(day "$3")" \
        --arg checkOut "$(day "$4")" --arg guest "$5" --arg type "$6" \
        '{propertyId: $property, roomTypeId: $room, checkIn: $checkIn, checkOut: $checkOut, guests: 2,
          guestName: $guest, bookingType: $type}')" \
        | jq -r .id
}

pay() {
    post "/api/bookings/$1/pay" '{"method": "UPI", "details": {"vpa": "guest@okaxis"}}' | jq -r .status
}

cancel() {
    post "/api/bookings/$1/cancel" | jq -r .status
}

for _ in $(seq 1 120); do
    curl -sf "$BASE_URL/actuator/health" >/dev/null 2>&1 && break
    sleep 1
done
curl -sf "$BASE_URL/actuator/health" >/dev/null || { echo "App is not reachable at $BASE_URL" >&2; exit 1; }

echo
echo "Seeding demo data into $BASE_URL"

add_owner SUNRISE "Sunrise Hotels" "ops@sunrise.example"
add_owner COASTAL "Coastal Stays" "hello@coastal.example"
add_owner ASHA "Asha Rao" "asha@homestay.example"
add_owner VIKRAM "Vikram Singh" "vikram@villa.example"

add_property SUNRISE_BLR SUNRISE_BLR_DELUXE "$SUNRISE" '{
  "name": "Sunrise Koramangala", "type": "HOTEL", "city": "Bengaluru", "locality": "Koramangala", "starRating": 4,
  "amenities": ["WIFI", "POOL", "BREAKFAST", "AC"],
  "roomTypes": [{"name": "Deluxe", "maxGuests": 2, "totalRooms": 5, "pricePerNight": 4500},
                {"name": "Suite", "maxGuests": 4, "totalRooms": 2, "pricePerNight": 8000}]}'
add_property SUNRISE_WF SUNRISE_WF_STANDARD "$SUNRISE" '{
  "name": "Sunrise Whitefield", "type": "HOTEL", "city": "Bengaluru", "locality": "Whitefield", "starRating": 3,
  "amenities": ["WIFI", "PARKING", "AC"],
  "roomTypes": [{"name": "Standard", "maxGuests": 2, "totalRooms": 8, "pricePerNight": 3200}]}'
add_property SUNRISE_GOA SUNRISE_GOA_GARDEN "$SUNRISE" '{
  "name": "Sunrise Baga", "type": "RESORT", "city": "Goa", "locality": "Baga", "starRating": 4,
  "amenities": ["POOL", "WIFI", "BREAKFAST", "PARKING"],
  "roomTypes": [{"name": "Garden Room", "maxGuests": 3, "totalRooms": 10, "pricePerNight": 3000},
                {"name": "Sea View", "maxGuests": 2, "totalRooms": 4, "pricePerNight": 5500}]}'
add_property COASTAL_GOA COASTAL_GOA_PREMIUM "$COASTAL" '{
  "name": "Coastal Candolim", "type": "RESORT", "city": "Goa", "locality": "Candolim", "starRating": 5,
  "amenities": ["POOL", "WIFI", "GYM", "BREAKFAST", "AC"],
  "roomTypes": [{"name": "Premium", "maxGuests": 2, "totalRooms": 6, "pricePerNight": 9000},
                {"name": "Family Suite", "maxGuests": 5, "totalRooms": 2, "pricePerNight": 14000}]}'
add_property COASTAL_MUM COASTAL_MUM_EXECUTIVE "$COASTAL" '{
  "name": "Coastal Colaba", "type": "HOTEL", "city": "Mumbai", "locality": "Colaba", "starRating": 5,
  "amenities": ["WIFI", "GYM", "AC", "BREAKFAST"],
  "roomTypes": [{"name": "Executive", "maxGuests": 2, "totalRooms": 10, "pricePerNight": 11000}]}'
add_property ASHA_HOME ASHA_HOME_ROOM "$ASHA" '{
  "name": "Asha Indiranagar Homestay", "type": "HOMESTAY", "city": "Bengaluru", "locality": "Indiranagar", "starRating": 3,
  "amenities": ["WIFI", "BREAKFAST"],
  "roomTypes": [{"name": "Entire home", "maxGuests": 6, "totalRooms": 1, "pricePerNight": 6000}]}'
add_property VIKRAM_VILLA VIKRAM_VILLA_ROOM "$VIKRAM" '{
  "name": "Lakeview Villa", "type": "VILLA", "city": "Udaipur", "locality": "Fateh Sagar", "starRating": 4,
  "amenities": ["POOL", "WIFI", "PARKING", "AC"],
  "roomTypes": [{"name": "Whole villa", "maxGuests": 8, "totalRooms": 1, "pricePerNight": 15000}]}'

BOOKING=$(book "$SUNRISE_BLR" "$SUNRISE_BLR_DELUXE" 14 16 "Ravi Kumar" FLEXIBLE)
show booking "Ravi Kumar, Sunrise Koramangala Deluxe" "$(pay "$BOOKING") FLEXIBLE  $BOOKING"
BOOKING=$(book "$COASTAL_GOA" "$COASTAL_GOA_PREMIUM" 20 23 "Priya Shah" NON_REFUNDABLE)
show booking "Priya Shah, Coastal Candolim Premium" "PENDING_PAYMENT NON_REFUNDABLE  $BOOKING"
BOOKING=$(book "$SUNRISE_GOA" "$SUNRISE_GOA_GARDEN" 10 12 "Arjun Mehta" FLEXIBLE)
pay "$BOOKING" >/dev/null
show booking "Arjun Mehta, Sunrise Baga Garden Room" "$(cancel "$BOOKING") FLEXIBLE  $BOOKING"

echo "Done: $ENTRIES entries. Swagger UI: $BASE_URL/"
echo
echo "Paste this into your terminal to use the curl examples in DEMO.md:"
echo "  export BASE=$BASE_URL CHAIN_OWNER_ID=$SUNRISE OWNER_ID=$ASHA PROPERTY_ID=$ASHA_HOME ROOM_TYPE_ID=$ASHA_HOME_ROOM"
echo
