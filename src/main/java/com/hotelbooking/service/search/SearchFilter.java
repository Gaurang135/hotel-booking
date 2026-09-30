package com.hotelbooking.service.search;

import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.RoomType;

public interface SearchFilter {

    /** Must return true when this filter's criterion was not requested. */
    boolean matches(Property property, RoomType room, SearchCriteria criteria);
}
