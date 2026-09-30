package com.hotelbooking.domain;

import com.hotelbooking.exception.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    private static final LocalDate DAY_1 = LocalDate.of(2026, 10, 1);

    private static DateRange range(int from, int to) {
        return new DateRange(DAY_1.plusDays(from), DAY_1.plusDays(to));
    }

    @Test
    void rejectsCheckOutOnOrBeforeCheckIn() {
        assertThatThrownBy(() -> range(0, 0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> range(1, 0)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void allowsAtMostThirtyNights() {
        assertThat(range(0, 30).nights()).isEqualTo(30);
        assertThatThrownBy(() -> range(0, 31)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void listsEachNightOfTheStay() {
        assertThat(range(0, 2).nights()).isEqualTo(2);
        assertThat(range(0, 2).eachNight()).containsExactly(DAY_1, DAY_1.plusDays(1));
    }

    @Test
    void checkOutDayIsNotANightOfTheStay() {
        DateRange stay = range(0, 2);
        assertThat(stay.contains(DAY_1)).isTrue();
        assertThat(stay.contains(DAY_1.plusDays(2))).isFalse();
    }

    @Test
    void backToBackStaysDoNotOverlap() {
        assertThat(range(0, 2).overlaps(range(2, 4))).isFalse();
        assertThat(range(2, 4).overlaps(range(0, 2))).isFalse();
    }

    @Test
    void sharedNightsOverlap() {
        assertThat(range(0, 2).overlaps(range(1, 3))).isTrue();
        assertThat(range(0, 5).overlaps(range(1, 2))).isTrue();
    }

    @Test
    void checkInCannotBeInThePast() {
        assertThatCode(() -> range(0, 2).ensureNotInPast(DAY_1)).doesNotThrowAnyException();
        assertThatThrownBy(() -> range(0, 2).ensureNotInPast(DAY_1.plusDays(1)))
                .isInstanceOf(InvalidInputException.class);
    }
}
