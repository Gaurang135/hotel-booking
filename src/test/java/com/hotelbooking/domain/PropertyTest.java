package com.hotelbooking.domain;

import com.hotelbooking.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyTest {

    private final RoomType room = new RoomType("r1", "Deluxe", 2, 3, new BigDecimal("1000"));
    private final Location location = new Location("Bengaluru", "Koramangala");

    @Test
    void rejectsStarRatingOutsideOneToFive() {
        assertThatThrownBy(() -> new Property("p1", "o1", "Hotel", PropertyType.HOTEL, location, 6, Set.of(), List.of(room)))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void needsAtLeastOneRoomType() {
        assertThatThrownBy(() -> new Property("p1", "o1", "Hotel", PropertyType.HOTEL, location, 4, Set.of(), List.of()))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void findsItsOwnRoomTypesOnly() {
        Property property = new Property("p1", "o1", "Hotel", PropertyType.HOTEL, location, 4, null, List.of(room));

        assertThat(property.findRoomType("r1")).contains(room);
        assertThat(property.findRoomType("other")).isEmpty();
        assertThat(property.amenities()).isEmpty();
    }

    @Test
    void roomTypeRejectsInvalidCapacityAndPrice() {
        assertThatThrownBy(() -> new RoomType("r", "Room", 0, 1, BigDecimal.TEN)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new RoomType("r", "Room", 1, 0, BigDecimal.TEN)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new RoomType("r", "Room", 1, 1, BigDecimal.ZERO)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new RoomType("r", "Room", 1, 1, new BigDecimal("0.004"))).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void localityMatchIgnoresCaseAndAcceptsAnyWhenNotGiven() {
        assertThat(location.matchesLocality("koramangala")).isTrue();
        assertThat(location.matchesLocality(null)).isTrue();
        assertThat(location.matchesLocality("Indiranagar")).isFalse();
    }

    @Test
    void ownerTreatsABlankEmailAsMissing() {
        assertThat(new Owner("o1", "Asha", "  ").email()).isNull();
        assertThat(new Owner("o1", "Asha", " asha@example.com ").email()).isEqualTo("asha@example.com");
    }
}
