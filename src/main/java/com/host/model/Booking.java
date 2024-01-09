package com.host.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.host.constants.BookingTypeEnum;
import com.host.validator.DateAfter;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@DateAfter
public class Booking extends PanacheEntity {
    public static final String PERSON_ID = "person_id";
    public static final String START = "start";
    public static final String PROPERTY_ID = "property_id";
    public static final String START_DATE = "start_date";
    public static final String END = "end";
    public static final String END_DATE = "end_date";
    public static final String OVERLAP_DATES_BY_PROPERTY_QUERY = "propertyId = ?1 and ((start < ?2 and end > ?2) or (start < ?3 and end > ?3) or (start > ?2 and end < ?3))";
    @NotNull
    @Column(name = PROPERTY_ID)
    @JsonProperty(PROPERTY_ID)
    public Long propertyId;
    @NotNull
    @Column(name = PERSON_ID)
    @JsonProperty(PERSON_ID)
    public Long personId;
    @NotNull
    @JsonProperty(START)
    @Column(name = START_DATE)
    public LocalDate start;
    @NotNull
    @JsonProperty(END)
    @Column(name = END_DATE)
    public LocalDate end;
    @NotNull
    public BookingTypeEnum type;
    public boolean canceled;

    public static Booking findIfDateIsBooked(final Booking newBooking){
        return find(OVERLAP_DATES_BY_PROPERTY_QUERY,
                newBooking.propertyId, newBooking.start, newBooking.end).firstResult();
    }

    public static Booking findIfDateIsBookedForUpdate(final Long bookingId, final Booking newBooking){
        return find("id != ?4 and "+OVERLAP_DATES_BY_PROPERTY_QUERY,
                newBooking.propertyId, newBooking.start, newBooking.end, bookingId).firstResult();
    }

}
