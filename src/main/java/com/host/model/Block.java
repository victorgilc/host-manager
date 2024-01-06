package com.host.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Block extends PanacheEntity {
    @Column(name = "start_date")
    LocalDate start;
    @Column(name = "end_date")
    LocalDate end;

}
