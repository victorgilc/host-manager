package com.host.service;

import com.host.exception.ResourceDoesNotExistException;
import com.host.model.Booking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Objects;

@ApplicationScoped
public class BookingService {
    @Transactional
    public void update(final Long id,
                       final Booking toUpdateBooking){
        final Booking dbBooking = Booking.findById(id);
        if(Objects.isNull(dbBooking)){
            throw new ResourceDoesNotExistException(id);
        }

        if(Objects.nonNull(toUpdateBooking.guestName)){
            dbBooking.guestName = toUpdateBooking.guestName;
        }

        if(Objects.nonNull(toUpdateBooking.start)){
            dbBooking.start = toUpdateBooking.start;
        }

        if(Objects.nonNull(toUpdateBooking.end)){
            dbBooking.end = toUpdateBooking.end;
        }
    }

    public void remove(final Long id) {
    }
}
