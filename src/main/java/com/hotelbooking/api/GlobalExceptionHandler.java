package com.hotelbooking.api;

import com.hotelbooking.exception.InvalidBookingStateException;
import com.hotelbooking.exception.InvalidInputException;
import com.hotelbooking.exception.NotFoundException;
import com.hotelbooking.exception.RefundFailedException;
import com.hotelbooking.exception.RoomNotAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;

import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidInputException.class)
    ProblemDetail invalidInput(InvalidInputException e) {
        return problem(BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidRequest(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + (error.isBindingFailure()
                        ? "invalid value '" + error.getRejectedValue() + "'"
                        : error.getDefaultMessage()))
                .collect(Collectors.joining(", "));
        return problem(BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail unreadable(HttpMessageNotReadableException e) {
        String field = e.getCause() instanceof JacksonException jackson
                ? jackson.getPath().stream().map(JacksonException.Reference::getPropertyName)
                        .filter(Objects::nonNull).collect(Collectors.joining("."))
                : "";
        return problem(BAD_REQUEST, field.isEmpty() ? "Malformed request body" : field + ": invalid value");
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(NotFoundException e) {
        return problem(NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({RoomNotAvailableException.class, InvalidBookingStateException.class})
    ProblemDetail conflict(RuntimeException e) {
        return problem(CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RefundFailedException.class)
    ProblemDetail refundFailed(RefundFailedException e) {
        return problem(BAD_GATEWAY, e.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String detail) {
        return ProblemDetail.forStatusAndDetail(status, detail);
    }
}
