package com.host.service;

import com.host.model.Booking;

public interface BookingService {
    void create(final Booking booking);
    void update(final Long id,
                       final Booking booking);
    void remove(final Long id);
    Booking get(final Long id);
}
