package com.microservice.notificationservice.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "keeper_order")
public class KeeperOrder extends BaseEntity {
    @OneToOne
    @JoinColumn(name = "device_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "device_Id_FK"))
    private Device device;
    @Column(name = "device_Id", nullable = false)
    private int device_Id;

    @OneToOne
    @JoinColumn(name = "keeper_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "keeper_Id_FK"))
    private User keeper;
    @Column(name = "keeper_Id", nullable = false)
    private int keeper_Id;

    @Column(nullable = false)
    private int keeperNo;

    @Column(nullable = false)
    private Boolean isReturned;

    @Column(nullable = false)
    private Date bookingDate;

    @Column(nullable = false)
    private Date dueDate;
}