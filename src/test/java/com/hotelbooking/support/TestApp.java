package com.hotelbooking.support;

import com.hotelbooking.domain.Amenity;
import com.hotelbooking.domain.Booking;
import com.hotelbooking.domain.BookingType;
import com.hotelbooking.domain.DateRange;
import com.hotelbooking.domain.Location;
import com.hotelbooking.domain.Owner;
import com.hotelbooking.domain.PaymentMethod;
import com.hotelbooking.domain.Property;
import com.hotelbooking.domain.PropertyType;
import com.hotelbooking.domain.RoomType;
import com.hotelbooking.repository.inmemory.InMemoryBookingRepository;
import com.hotelbooking.repository.inmemory.InMemoryOwnerRepository;
import com.hotelbooking.repository.inmemory.InMemoryPropertyRepository;
import com.hotelbooking.service.AvailabilityService;
import com.hotelbooking.service.BookingService;
import com.hotelbooking.service.CancellationService;
import com.hotelbooking.service.InventoryLock;
import com.hotelbooking.service.JvmInventoryLock;
import com.hotelbooking.service.PaymentService;
import com.hotelbooking.service.PropertyService;
import com.hotelbooking.service.SearchService;
import com.hotelbooking.service.payment.CardPaymentProcessor;
import com.hotelbooking.service.payment.UpiPaymentProcessor;
import com.hotelbooking.service.payment.WalletPaymentProcessor;
import com.hotelbooking.service.pricing.PricingStrategy;
import com.hotelbooking.service.pricing.StandardPricingStrategy;
import com.hotelbooking.service.refund.FlexibleRefundPolicy;
import com.hotelbooking.service.refund.NonRefundableRefundPolicy;
import com.hotelbooking.service.search.AmenitiesFilter;
import com.hotelbooking.service.search.PriceRangeFilter;
import com.hotelbooking.service.search.SearchCriteria;
import com.hotelbooking.service.search.SearchFilter;
import com.hotelbooking.service.search.StarRatingFilter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The application wired by hand with in-memory repositories, a movable clock and a recording gateway. */
public class TestApp {

    public static final LocalDate TODAY = LocalDate.of(2026, 10, 1);
    public static final String OWNER_ID = "owner-1";

    public final MutableClock clock = new MutableClock(TODAY);
    public final InMemoryOwnerRepository owners = new InMemoryOwnerRepository();
    public final InMemoryPropertyRepository properties = new InMemoryPropertyRepository();
    public final InMemoryBookingRepository bookings = new InMemoryBookingRepository();
    public final RecordingPaymentGateway gateway = new RecordingPaymentGateway();
    public final InventoryLock lock = new JvmInventoryLock();
    public final PricingStrategy pricing = new StandardPricingStrategy();
    public final AvailabilityService availability = new AvailabilityService(bookings);
    public final PropertyService propertyService = new PropertyService(owners, properties);
    public final BookingService bookingService =
            new BookingService(properties, bookings, availability, pricing, lock, clock);
    public final PaymentService paymentService = new PaymentService(bookings,
            List.of(new CardPaymentProcessor(gateway), new UpiPaymentProcessor(gateway), new WalletPaymentProcessor(gateway)),
            lock, clock);
    public final CancellationService cancellationService =
            new CancellationService(bookings, List.of(new FlexibleRefundPolicy(), new NonRefundableRefundPolicy()),
                    gateway, lock, clock);

    public TestApp() {
        owners.save(new Owner(OWNER_ID, "Test Owner", "owner@test.example"));
    }

    public SearchService searchService() {
        return searchService(List.of(new PriceRangeFilter(pricing), new AmenitiesFilter(), new StarRatingFilter()));
    }

    public SearchService searchService(List<SearchFilter> filters) {
        return new SearchService(properties, availability, pricing, filters, clock);
    }

    public Property addProperty(String id, PropertyType type, String city, String locality, int stars,
                                Set<Amenity> amenities, RoomType... rooms) {
        return propertyService.addProperty(
                new Property(id, OWNER_ID, "Property " + id, type, new Location(city, locality), stars, amenities, List.of(rooms)));
    }

    public Booking book(String propertyId, String roomTypeId, DateRange stay) {
        return book(propertyId, roomTypeId, stay, BookingType.FLEXIBLE);
    }

    public Booking book(String propertyId, String roomTypeId, DateRange stay, BookingType type) {
        return bookingService.create(propertyId, roomTypeId, stay, 1, "Guest", type);
    }

    public Booking payWithUpi(Booking booking) {
        return paymentService.pay(booking.getId(), PaymentMethod.UPI, Map.of("vpa", "guest@okbank"), null);
    }

    public static RoomType room(String id, int maxGuests, int totalRooms, String pricePerNight) {
        return new RoomType(id, "Room " + id, maxGuests, totalRooms, new BigDecimal(pricePerNight));
    }

    /** Nights counted from TODAY, e.g. stay(1, 3) = tomorrow for 2 nights. */
    public static DateRange stay(int fromDay, int toDay) {
        return new DateRange(TODAY.plusDays(fromDay), TODAY.plusDays(toDay));
    }

    public static SearchCriteria criteria(String city, DateRange stay, int guests) {
        return criteria(city, null, stay, guests, null, null, Set.of(), null);
    }

    public static SearchCriteria criteria(String city, String locality, DateRange stay, int guests,
                                          BigDecimal minPrice, BigDecimal maxPrice, Set<Amenity> amenities, Integer minStars) {
        return new SearchCriteria(city, locality, stay.checkIn(), stay.checkOut(), guests, minPrice, maxPrice,
                amenities, minStars);
    }
}
