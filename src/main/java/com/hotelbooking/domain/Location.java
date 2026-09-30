package com.hotelbooking.domain;

public record Location(String city, String locality) {

    public Location {
        city = Require.text(city, "city");
        locality = locality == null || locality.isBlank() ? null : locality.trim();
    }

    public boolean matchesLocality(String wanted) {
        return wanted == null || wanted.isBlank() || wanted.trim().equalsIgnoreCase(locality);
    }
}
