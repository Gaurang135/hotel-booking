package com.hotelbooking.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeTest {

    private static final LocalDate CHECK_IN = LocalDate.now(ZoneId.of("Asia/Kolkata")).plusDays(20);
    private static final LocalDate CHECK_OUT = CHECK_IN.plusDays(2);

    @Autowired
    private MockMvc mvc;

    private ResultActions postJson(String url, String body) throws Exception {
        return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions search() throws Exception {
        return mvc.perform(get("/api/properties/search")
                .param("city", "testville")
                .param("checkIn", CHECK_IN.toString())
                .param("checkOut", CHECK_OUT.toString())
                .param("guests", "2")
                .param("amenities", "WIFI,POOL"));
    }

    private static String read(ResultActions result, String path) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
    }

    /** Creates an owner with a one-room property in the given city; returns {propertyId, roomTypeId}. */
    private String[] oneRoomPropertyIn(String city) throws Exception {
        String ownerId = read(postJson("/api/owners", """
                {"name": "Flow Owner", "email": "flow@example.com"}
                """).andExpect(status().isCreated()), "$.id");
        ResultActions property = postJson("/api/owners/" + ownerId + "/properties", """
                {"name": "Test Inn", "type": "HOTEL", "city": "%s", "locality": "Centre", "starRating": 4,
                 "amenities": ["WIFI", "POOL"],
                 "roomTypes": [{"name": "Only room", "maxGuests": 2, "totalRooms": 1, "pricePerNight": 2500}]}
                """.formatted(city)).andExpect(status().isCreated()).andExpect(jsonPath("$.location.city").value(city));
        return new String[] {read(property, "$.id"), read(property, "$.roomTypes[0].id")};
    }

    private String bookingJson(String[] property, String extraFields) {
        return """
                {"propertyId": "%s", "roomTypeId": "%s", "checkIn": "%s", "checkOut": "%s", "guests": 2, "guestName": "Ravi"%s}
                """.formatted(property[0], property[1], CHECK_IN, CHECK_OUT, extraFields);
    }

    private ResultActions payWithUpi(String bookingId) throws Exception {
        return mvc.perform(post("/api/bookings/" + bookingId + "/pay")
                .header("Idempotency-Key", "key-" + bookingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"method": "UPI", "details": {"vpa": "ravi@okaxis"}}
                        """));
    }

    @Test
    void healthAndSwaggerAreServed() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void addSearchBookPayCancelFlow() throws Exception {
        String[] property = oneRoomPropertyIn("Testville");

        search().andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].location.locality").value("Centre"));

        String bookingId = read(postJson("/api/bookings", bookingJson(property, ""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.bookingType").value("FLEXIBLE")), "$.id");

        postJson("/api/bookings", bookingJson(property, ""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").exists());
        search().andExpect(jsonPath("$.length()").value(0));

        payWithUpi(bookingId).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(post("/api/bookings/" + bookingId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(5000.0));
        search().andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void nonRefundableBookingIsCancelledWithoutRefund() throws Exception {
        String[] property = oneRoomPropertyIn("Nonrefundville");

        String bookingId = read(postJson("/api/bookings", bookingJson(property, ", \"bookingType\": \"NON_REFUNDABLE\""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingType").value("NON_REFUNDABLE"))
                .andExpect(jsonPath("$.totalPrice").value(5000.0)), "$.id");
        payWithUpi(bookingId).andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(post("/api/bookings/" + bookingId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.refundAmount").value(0));
    }

    @Test
    void errorsExplainWhatWentWrong() throws Exception {
        mvc.perform(get("/api/properties/search")
                        .param("city", "Bengaluru")
                        .param("checkIn", CHECK_IN.toString())
                        .param("checkOut", CHECK_OUT.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("guests")));

        postJson("/api/bookings", """
                {"propertyId": "any-property", "roomTypeId": "any-room", "checkIn": "%s", "checkOut": "%s",
                 "guests": 2, "guestName": "Ravi"}
                """.formatted(CHECK_OUT, CHECK_IN))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("checkOut")));

        mvc.perform(get("/api/bookings/does-not-exist")).andExpect(status().isNotFound());

        mvc.perform(get("/api/properties/search")
                        .param("city", "Bengaluru")
                        .param("checkIn", CHECK_IN.toString())
                        .param("checkOut", CHECK_OUT.toString())
                        .param("guests", "2")
                        .param("amenities", "FOO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("amenities: invalid value 'FOO'"));

        mvc.perform(get("/api/properties/search")
                        .param("city", "Bengaluru")
                        .param("checkIn", CHECK_IN.toString())
                        .param("checkOut", CHECK_OUT.toString())
                        .param("guests", "2")
                        .param("minPrice", "6000")
                        .param("maxPrice", "4000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("minPrice")));

        mvc.perform(get("/api/properties/search")
                        .param("city", "Bengaluru")
                        .param("checkIn", CHECK_IN.toString())
                        .param("checkOut", CHECK_OUT.toString())
                        .param("guests", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("guests must be at least 1"));
    }

    @Test
    void malformedInputIsA400NotA500() throws Exception {
        mvc.perform(get("/api/properties/search")
                        .param("city", "Bengaluru")
                        .param("checkIn", CHECK_IN.toString())
                        .param("checkOut", CHECK_OUT.toString())
                        .param("guests", "2")
                        .param("amenities", "WIFI,"))
                .andExpect(status().isOk());

        String property = """
                {"name": "X", "type": "HOTEL", "city": "Pune", "starRating": 3, "roomTypes": %s}
                """;
        postJson("/api/owners/any-owner/properties", property.formatted("[null]"))
                .andExpect(status().isBadRequest());
        postJson("/api/owners/any-owner/properties",
                property.formatted("[{\"name\": \"R\", \"maxGuests\": 2, \"totalRooms\": 1, \"pricePerNight\": 1e999999999}]"))
                .andExpect(status().isBadRequest());

        postJson("/api/owners/any-owner/properties",
                property.formatted("[{\"name\": \"R\", \"maxGuests\": 0, \"totalRooms\": 1, \"pricePerNight\": 0}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("roomTypes[0].maxGuests")))
                .andExpect(jsonPath("$.detail", containsString("roomTypes[0].pricePerNight")));

        postJson("/api/owners/any-owner/properties", """
                {"name": "X", "type": "CASTLE", "city": "Pune", "starRating": 3,
                 "roomTypes": [{"name": "R", "maxGuests": 2, "totalRooms": 1, "pricePerNight": 100}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("type: invalid value"));
    }
}
