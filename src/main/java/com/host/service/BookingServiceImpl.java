package com.host.service;

import com.host.exception.PropertyAlreadyBookedException;
import com.host.exception.ResourceDoesNotExistException;
import com.host.model.Booking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Objects;

@ApplicationScoped
public class BookingServiceImpl implements BookingService{

    public void create(final Booking booking) {
        verifyAlreadyBooked(booking, Booking.findIfDateIsBooked(booking));
        booking.persistAndFlush();
    }
    @Transactional
    public void update(final Long id,
                       final Booking booking){
        final Booking dbBooking = get(id);
        verifyAlreadyBooked(booking, Booking.findIfDateIsBookedForUpdate(id, dbBooking));

        if(Objects.nonNull(booking.personId)){
            dbBooking.personId = booking.personId;
        }

        if(Objects.nonNull(booking.start)){
            dbBooking.start = booking.start;
        }

        if(Objects.nonNull(booking.end)){
            dbBooking.end = booking.end;
        }

        if(Objects.nonNull(booking.canceled)){
            dbBooking.canceled = booking.canceled;
        }
    }

    private static void verifyAlreadyBooked(Booking booking, Booking alreadyBooked) {
        if(Objects.nonNull(alreadyBooked)){
            throw new PropertyAlreadyBookedException(alreadyBooked, booking);
        }
    }

    @Transactional
    public void remove(final Long id) {
        get(id);
        Booking.deleteById(id);
    }

    public Booking get(final Long id) {
        final Booking dbBooking = Booking.findById(id);
        if(Objects.isNull(dbBooking)){
            throw new ResourceDoesNotExistException(id);
        }
        return dbBooking;
    }
}
