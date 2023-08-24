package com.microservice.notificationservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "keeper_order",
                indexes = {
        @Index(name = "keeper_order_Id_idx", columnList = "id", unique = true),
        @Index(name = "keeper_Id_idx", columnList = "keeper_Id"),
        @Index(name = "device_Id_idx", columnList = "device_Id")
        })
public class KeeperOrder extends BaseEntity {
    public KeeperOrder(int Id, Date createdDate, Date updatedDate,  int device_Id, int keeper_Id, int keeperNo, Boolean isReturned, Date bookingDate, Date dueDate) {
        super(Id, createdDate, updatedDate);
        this.device_Id = device_Id;
        this.keeper_Id = keeper_Id;
        this.keeperNo = keeperNo;
        this.isReturned = isReturned;
        this.bookingDate = bookingDate;
        this.dueDate = dueDate;
    }

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