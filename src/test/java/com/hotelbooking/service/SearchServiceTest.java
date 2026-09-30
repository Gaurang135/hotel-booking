package com.hotelbooking.service;

import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.PropertyType;
import com.hotelbooking.exception.InvalidInputException;
import com.hotelbooking.service.pricing.PricingStrategy;
import com.hotelbooking.service.search.PriceRangeFilter;
import com.hotelbooking.service.search.SearchFilter;
import com.hotelbooking.service.search.SearchResult;
import com.hotelbooking.service.search.SearchResult.RoomOffer;
import com.hotelbooking.support.TestApp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static com.hotelbooking.domain.Amenity.POOL;
import static com.hotelbooking.domain.Amenity.WIFI;
import static com.hotelbooking.domain.PropertyType.HOMESTAY;
import static com.hotelbooking.domain.PropertyType.HOTEL;
import static com.hotelbooking.support.TestApp.criteria;
import static com.hotelbooking.support.TestApp.room;
import static com.hotelbooking.support.TestApp.stay;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchServiceTest {

    private static final DateRange STAY = stay(2, 4);

    private final TestApp app = new TestApp();
    private final SearchService search = app.searchService();

    @BeforeEach
    void setUp() {
        app.addProperty("hotel", HOTEL, "Bengaluru", "Koramangala", 4, Set.of(WIFI, POOL),
                room("deluxe", 2, 1, "4500"), room("suite", 4, 1, "8000"));
        app.addProperty("home", HOMESTAY, "Bengaluru", "Indiranagar", 3, Set.of(WIFI),
                room("entire-home", 6, 1, "6000"));
        app.addProperty("goa", PropertyType.RESORT, "Goa", "Baga", 3, Set.of(POOL),
                room("goa-standard", 3, 5, "3000"));
    }

    private static List<String> propertyIds(List<SearchResult> results) {
        return results.stream().map(SearchResult::propertyId).toList();
    }

    private static List<String> offeredRooms(List<SearchResult> results) {
        return results.stream().flatMap(r -> r.offers().stream()).map(RoomOffer::roomTypeId).toList();
    }

    @Test
    void findsPropertiesInTheCityIgnoringCase() {
        List<SearchResult> results = search.search(criteria("bengaluru", STAY, 2));

        assertThat(propertyIds(results)).containsExactlyInAnyOrder("hotel", "home");
    }

    @Test
    void narrowsByLocality() {
        List<SearchResult> results = search.search(criteria("Bengaluru", "indiranagar", STAY, 2, null, null, Set.of(), null));

        assertThat(propertyIds(results)).containsExactly("home");
    }

    @Test
    void offersOnlyRoomTypesThatFitTheGuests() {
        List<SearchResult> results = search.search(criteria("Bengaluru", STAY, 3));

        assertThat(offeredRooms(results)).containsExactlyInAnyOrder("suite", "entire-home");
    }

    @Test
    void offersOnlyRoomTypesFreeForTheDates() {
        app.book("hotel", "deluxe", stay(1, 3));

        assertThat(offeredRooms(search.search(criteria("Bengaluru", stay(2, 4), 2))))
                .containsExactlyInAnyOrder("suite", "entire-home");
        assertThat(offeredRooms(search.search(criteria("Bengaluru", stay(3, 5), 2))))
                .containsExactlyInAnyOrder("deluxe", "suite", "entire-home");
    }

    @Test
    void dropsAPropertyWithNoFreeRoom() {
        app.book("home", "entire-home", STAY);

        assertThat(propertyIds(search.search(criteria("Bengaluru", STAY, 2)))).containsExactly("hotel");
    }

    @Test
    void quotesTheTotalPriceForTheStay() {
        List<SearchResult> results = search.search(criteria("Bengaluru", "Koramangala", STAY, 2, null, null, Set.of(), null));

        RoomOffer deluxe = results.getFirst().offers().stream()
                .filter(offer -> offer.roomTypeId().equals("deluxe")).findFirst().orElseThrow();
        assertThat(deluxe.totalPrice()).isEqualByComparingTo("9000");
        assertThat(deluxe.roomsLeft()).isEqualTo(1);
    }

    @Test
    void filtersByPricePerNight() {
        List<SearchResult> results = search.search(
                criteria("Bengaluru", null, STAY, 2, new BigDecimal("4000"), new BigDecimal("6000"), Set.of(), null));

        assertThat(offeredRooms(results)).containsExactlyInAnyOrder("deluxe", "entire-home");
    }

    @Test
    void priceFilterUsesTheQuotedPriceNotTheBaseRate() {
        PricingStrategy surcharge = (room, stay) -> app.pricing.priceFor(room, stay).multiply(new BigDecimal("1.2"));
        SearchService surchargedSearch = new SearchService(app.properties, app.availability, surcharge,
                List.of(new PriceRangeFilter(surcharge)), app.clock);

        List<SearchResult> results = surchargedSearch.search(
                criteria("Bengaluru", null, STAY, 2, null, new BigDecimal("5000"), Set.of(), null));

        assertThat(results).isEmpty();
    }

    @Test
    void requiresAllRequestedAmenities() {
        List<SearchResult> results = search.search(criteria("Bengaluru", null, STAY, 2, null, null, Set.of(WIFI, POOL), null));

        assertThat(propertyIds(results)).containsExactly("hotel");
    }

    @Test
    void filtersByMinimumStars() {
        List<SearchResult> results = search.search(criteria("Bengaluru", null, STAY, 2, null, null, Set.of(), 4));

        assertThat(propertyIds(results)).containsExactly("hotel");
    }

    @Test
    void aNewFilterPlugsInWithoutChangingSearchService() {
        SearchFilter homestaysOnly = (property, room, criteria) -> property.type() == HOMESTAY;

        List<SearchResult> results = app.searchService(List.of(homestaysOnly)).search(criteria("Bengaluru", STAY, 2));

        assertThat(propertyIds(results)).containsExactly("home");
    }

    @Test
    void rejectsDatesInThePast() {
        assertThatThrownBy(() -> search.search(criteria("Bengaluru", stay(-2, 1), 2)))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void rejectsMinPriceAboveMaxPrice() {
        assertThatThrownBy(() -> search.search(
                criteria("Bengaluru", null, STAY, 2, new BigDecimal("6000"), new BigDecimal("4000"), Set.of(), null)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessageContaining("minPrice");
    }

    @Test
    void rejectsInvalidGuestsStarsAndPrices() {
        assertThatThrownBy(() -> search.search(criteria("Bengaluru", STAY, 0)))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("guests");
        assertThatThrownBy(() -> search.search(criteria("Bengaluru", null, STAY, 2, null, null, Set.of(), 6)))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("minStars");
        assertThatThrownBy(() -> search.search(criteria("Bengaluru", null, STAY, 2, new BigDecimal("-1"), null, Set.of(), null)))
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("negative");
    }
}
