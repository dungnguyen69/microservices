package com.fullstack.Backend.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Requests",
        indexes = {
                @Index(name = "request_id_idx", columnList = "id", unique = true),
                @Index(name = "requester_Id_idx", columnList = "requester_Id"),
                @Index(name = "currentKeeper_Id_Id_idx", columnList = "currentKeeper_Id"),
                @Index(name = "nextKeeper_Id_idx", columnList = "nextKeeper_Id"),
                @Index(name = "accepter_Id_idx", columnList = "accepter_Id"),
                @Index(name = "device_Id_idx", columnList = "device_Id")
        })
public class Request extends BaseEntity {
    @Column(nullable = false)
    private String requestId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "requester_Id_FK"))
    @JsonIgnore()
    private User requester;
    @Column(name = "requester_Id", nullable = false)
    private int requester_Id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currentKeeper_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "currentKeeper_Id_FK"))
    @JsonIgnore()
    private User currentKeeper;
    @Column(name = "currentKeeper_Id", nullable = false)
    private int currentKeeper_Id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nextKeeper_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "nextKeeper_Id_FK"))
    @JsonIgnore()
    private User nextKeeper;
    @Column(name = "nextKeeper_Id", nullable = false)
    private int nextKeeper_Id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accepter_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "accepter_Id_FK"))
    @JsonIgnore()
    private User accepter;
    @Column(name = "accepter_Id", nullable = false)
    private int accepter_Id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "device_Id_FK"))
    @JsonIgnore()
    private Device device;
    @Column(name = "device_Id", nullable = false)
    private int device_Id;

    @Column(nullable = false)
    private int requestStatus;

    @Column()
    private Date approvalDate;

    @Column()
    private Date transferredDate;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date bookingDate;

    @Column(nullable = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date returnDate;

    @Column()
    private Date cancelledDate;
}
