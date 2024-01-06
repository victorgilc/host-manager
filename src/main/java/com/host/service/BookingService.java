package com.host.service;

import com.host.model.Booking;

public class BookingService {
    public void update(final Long id,
                       final Booking toUpdateBooking){
        var dbBooking = Booking.findById(id);


    }
}
