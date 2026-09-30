package com.hotelbooking.api;

import com.hotelbooking.api.dto.BookingResponse;
import com.hotelbooking.api.dto.CreateBookingRequest;
import com.hotelbooking.api.dto.PaymentRequest;
import com.hotelbooking.service.BookingService;
import com.hotelbooking.service.CancellationService;
import com.hotelbooking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
@Tag(name = "Bookings", description = "Book, pay for and cancel stays")
public class BookingController {

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final CancellationService cancellationService;

    public BookingController(BookingService bookingService, PaymentService paymentService,
                             CancellationService cancellationService) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.cancellationService = cancellationService;
    }

    @Operation(summary = "Book a room type for a date range (holds the room until paid)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        return BookingResponse.from(bookingService.create(request.propertyId(), request.roomTypeId(), request.stay(),
                request.guests(), request.guestName(), request.bookingType()));
    }

    @Operation(summary = "Get a booking")
    @GetMapping("/{bookingId}")
    public BookingResponse get(@PathVariable String bookingId) {
        return BookingResponse.from(bookingService.get(bookingId));
    }

    @Operation(summary = "Pay for a booking; success confirms it, a decline releases the room")
    @PostMapping("/{bookingId}/pay")
    public BookingResponse pay(@PathVariable String bookingId,
                               @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                               @Valid @RequestBody PaymentRequest request) {
        return BookingResponse.from(
                paymentService.pay(bookingId, request.method(), request.details(), idempotencyKey));
    }

    @Operation(summary = "Cancel a booking; refunds per policy and releases the room")
    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable String bookingId) {
        return BookingResponse.from(cancellationService.cancel(bookingId));
    }
}
