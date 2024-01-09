package com.host.exception;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public class OverlapMessageWrapper {
    public static final String START_BOOKED = "start_booked";
    public static final String END_BOOKED = "end_booked";
    public static final String START_TRY_TO_BOOK = "start_try_to_book";
    public static final String END_TRY_TO_BOOK = "end_try_to_book";

    public final String message;
    @JsonProperty(START_BOOKED)
    public final LocalDate startBooked;
    @JsonProperty(END_BOOKED)
    public final LocalDate endBooked;
    @JsonProperty(START_TRY_TO_BOOK)
    public final LocalDate startTryToBook;
    @JsonProperty(END_TRY_TO_BOOK)
    public final LocalDate endTryToBook;
}
