package com.host.exception;

import com.host.model.Booking;

public class PropertyAlreadyBookedException extends RuntimeException {
    public static final String ERROR_MESSAGE = "Property already booked for specified date range";
    public final Booking alreadyBooked;
    public final Booking triedToBook;
    public PropertyAlreadyBookedException(final Booking alreadyBooked, final Booking triedToBook){
        super(ERROR_MESSAGE);
        this.alreadyBooked = alreadyBooked;
        this.triedToBook = triedToBook;
    }
}
