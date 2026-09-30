package com.hotelbooking.service;

import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.repository.PropertyRepository;
import com.hotelbooking.service.pricing.PricingStrategy;
import com.hotelbooking.service.search.SearchCriteria;
import com.hotelbooking.service.search.SearchFilter;
import com.hotelbooking.service.search.SearchResult;
import com.hotelbooking.service.search.SearchResult.RoomOffer;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class SearchService {

    private final PropertyRepository properties;
    private final AvailabilityService availability;
    private final PricingStrategy pricing;
    private final List<SearchFilter> filters;
    private final Clock clock;

    public SearchService(PropertyRepository properties, AvailabilityService availability,
                         PricingStrategy pricing, List<SearchFilter> filters, Clock clock) {
        this.properties = properties;
        this.availability = availability;
        this.pricing = pricing;
        this.filters = filters;
        this.clock = clock;
    }

    public List<SearchResult> search(SearchCriteria criteria) {
        criteria.validate(LocalDate.now(clock));
        DateRange stay = criteria.stay();
        List<SearchResult> results = new ArrayList<>();
        for (Property property : properties.findByCity(criteria.city())) {
            if (!property.location().matchesLocality(criteria.locality())) {
                continue;
            }
            List<RoomOffer> offers = availableOffers(property, stay, criteria);
            if (!offers.isEmpty()) {
                results.add(SearchResult.of(property, offers));
            }
        }
        return results;
    }

    private List<RoomOffer> availableOffers(Property property, DateRange stay, SearchCriteria criteria) {
        List<RoomOffer> offers = new ArrayList<>();
        for (RoomType room : property.roomTypes()) {
            if (!room.canFit(criteria.guests()) || !passesFilters(property, room, criteria)) {
                continue;
            }
            int roomsLeft = availability.roomsLeft(room, stay);
            if (roomsLeft > 0) {
                offers.add(RoomOffer.of(room, roomsLeft, pricing.priceFor(room, stay)));
            }
        }
        return offers;
    }

    private boolean passesFilters(Property property, RoomType room, SearchCriteria criteria) {
        return filters.stream().allMatch(filter -> filter.matches(property, room, criteria));
    }
}
