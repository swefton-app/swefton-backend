package com.swefton.backend.modules.activity.entity;

import java.time.DayOfWeek;
import java.time.LocalTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="activity_schedule") @Getter @Setter
public class ActivitySchedule {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="activity_id",nullable=false) private FacilityActivity activity;
    @Enumerated(EnumType.STRING) @Column(name="day_of_week",nullable=false,length=10) private DayOfWeek dayOfWeek;
    @Column(name="start_time",nullable=false) private LocalTime startTime;
    @Column(name="end_time",nullable=false) private LocalTime endTime;
}
