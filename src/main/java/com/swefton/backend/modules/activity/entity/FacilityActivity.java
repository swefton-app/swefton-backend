package com.swefton.backend.modules.activity.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.swefton.backend.modules.facility.entity.Facility;
import com.swefton.backend.modules.facility.entity.FacilityStaff;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity @Table(name="facility_activity") @Getter @Setter
public class FacilityActivity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="facility_id",nullable=false) private Facility facility;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="responsible_staff_id") private FacilityStaff responsibleStaff;
    @Column(nullable=false,length=150) private String name;
    @Column(length=2000) private String description;
    @Column(nullable=false,length=80) private String category;
    @Column(nullable=false) private Integer capacity;
    @Column(nullable=false,length=30) private String status;
    @OneToMany(mappedBy="activity",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("dayOfWeek ASC, startTime ASC") private List<ActivitySchedule> schedules=new ArrayList<>();
    @Column(name="created_at",nullable=false,updatable=false) private LocalDateTime createdAt;
    @Column(name="updated_at") private LocalDateTime updatedAt;
    @Column(name="deleted_at") private LocalDateTime deletedAt;
    @PrePersist void create(){if(createdAt==null)createdAt=LocalDateTime.now();}
    @PreUpdate void update(){updatedAt=LocalDateTime.now();}
}
