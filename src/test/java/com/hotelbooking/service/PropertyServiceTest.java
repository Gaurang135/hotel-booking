package com.hotelbooking.service;

import com.hotelbooking.domain.Location;
import com.hotelbooking.domain.Owner;
import com.hotelbooking.domain.Property;
import com.hotelbooking.exception.NotFoundException;
import com.hotelbooking.support.TestApp;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.hotelbooking.domain.PropertyType.HOMESTAY;
import static com.hotelbooking.domain.PropertyType.HOTEL;
import static com.hotelbooking.support.TestApp.room;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertyServiceTest {

    private final TestApp app = new TestApp();
    private final PropertyService service = app.propertyService;

    private static Property propertyOf(String ownerId, String name) {
        return Property.create(ownerId, name, HOTEL, new Location("Bengaluru", null), 4, Set.of(), List.of(room(name + "-room", 2, 3, "1000")));
    }

    @Test
    void chainAndStandaloneOwnersUseTheSamePath() {
        Owner chain = service.addOwner(Owner.create("Sunrise Hotels", "ops@sunrise.example"));
        Owner standalone = service.addOwner(Owner.create("Asha", "asha@example.com"));

        service.addProperty(propertyOf(chain.id(), "Sunrise Koramangala"));
        service.addProperty(propertyOf(chain.id(), "Sunrise Whitefield"));
        service.addProperty(Property.create(standalone.id(), "Asha's Home", HOMESTAY, new Location("Goa", null), 3,
                Set.of(), List.of(room("home", 6, 1, "6000"))));

        assertThat(service.propertiesOf(chain.id())).hasSize(2);
        assertThat(service.propertiesOf(standalone.id())).hasSize(1);
    }

    @Test
    void unknownOwnerIsNotFound() {
        assertThatThrownBy(() -> service.addProperty(propertyOf("nobody", "Ghost Inn"))).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.propertiesOf("nobody")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownPropertyIsNotFound() {
        assertThatThrownBy(() -> service.getProperty("missing")).isInstanceOf(NotFoundException.class);
    }
}
