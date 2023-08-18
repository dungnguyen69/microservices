package com.microservice.notificationservice.models;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.microservice.notificationservice.enums.*;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity()
@Table(name = "Devices", uniqueConstraints = @UniqueConstraint(columnNames = "serialNumber", name = "serialNumber"))
public class Device extends BaseEntity {
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Status status;

    @ManyToOne()
    @JoinColumn(name = "platform_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "platform_Id_FK"))
    @JsonIgnore()
    private Platform platform;
    @Column(name = "platform_Id", nullable = false)
    private int platformId;

    @ManyToOne()
    @JoinColumn(name = "item_type_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "item_type_Id_FK"))
    @JsonIgnore()
    private ItemType itemType;
    @Column(name = "item_type_Id", nullable = false)
    private int itemTypeId;

    @ManyToOne()
    @JoinColumn(name = "ram_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "ram_Id_FK"))
    @JsonIgnore()
    private Ram ram;
    @Column(name = "ram_Id", nullable = false)
    private int ramId;

    @ManyToOne()
    @JoinColumn(name = "screen_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "screen_Id_FK"))
    @JsonIgnore()
    private Screen screen;
    @Column(name = "screen_Id", nullable = false)
    private int screenId;

    @ManyToOne()
    @JoinColumn(name = "storage_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "storage_Id_FK"))
    @JsonIgnore()
    private Storage storage;
    @Column(name = "storage_Id")
    private int storageId;

    @ManyToOne()
    @JoinColumn(name = "owner_Id", nullable = false, referencedColumnName = "id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "owner_Id_FK"))
    @JsonIgnore()
    private User owner;
    @Column(name = "owner_Id", nullable = false)
    private int ownerId;

    @Column(nullable = false)
    private String inventoryNumber;

    @Column(nullable = false)
    private String serialNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Origin origin;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Project project;

    @Column()
    private String comments;
}
