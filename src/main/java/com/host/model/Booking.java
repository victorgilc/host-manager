package com.host.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.host.validator.DateAfter;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Entity
@DateAfter(start = "start", end = "end")
public class Booking extends PanacheEntity {
    @NotNull
    @Column(name = "guest_name")
    @JsonProperty("guest_name")
    public String guestName;
    @NotNull
    @JsonProperty("start")
    @Column(name = "start_date")
    public LocalDate start;
    @NotNull
    @JsonProperty("end")
    @Column(name = "end_date")
    public LocalDate end;

}
