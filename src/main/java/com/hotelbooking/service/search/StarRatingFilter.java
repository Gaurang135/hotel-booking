package com.hotelbooking.service.search;

import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.RoomType;
import org.springframework.stereotype.Component;

@Component
public class StarRatingFilter implements SearchFilter {

    @Override
    public boolean matches(Property property, RoomType room, SearchCriteria criteria) {
        return criteria.minStars() == null || property.starRating() >= criteria.minStars();
    }
}
