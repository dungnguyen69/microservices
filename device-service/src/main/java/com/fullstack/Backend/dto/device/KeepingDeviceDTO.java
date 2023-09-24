package com.fullstack.Backend.dto.device;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fullstack.Backend.models.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KeepingDeviceDTO extends ReadableDeviceDTO {
    @JsonProperty("MaxExtendingReturnDate")
    public Date MaxExtendingReturnDate;
    @JsonProperty("isReturnable")
    public Boolean isReturnable = true;

    public KeepingDeviceDTO(Device device, KeeperOrder keeperOrder) {
        if (device.getOwner() == null)
            this.Owner = "";
        else
            this.Owner = device.getOwner().getUserName();

        this.Id = device.getId();
        this.DeviceName = device.getName();
        this.ItemType = device.getItemType().getName();
        this.Status = device.getStatus().name();
        this.PlatformName = device.getPlatform().getName();
        this.PlatformVersion = device.getPlatform().getVersion();
        this.RamSize = device.getRam().getSize();
        this.ScreenSize = device.getScreen().getSize();
        this.StorageSize = device.getStorage().getSize();
        this.InventoryNumber = device.getInventoryNumber();
        this.SerialNumber = device.getSerialNumber();
        this.Comments = device.getComments();
        this.Project = device.getProject().name();
        this.Origin = device.getOrigin().name();
        this.CreatedDate = device.getCreatedDate();
        this.UpdatedDate = device.getUpdatedDate();
        this.Keeper = keeperOrder.getKeeper().getUserName();
        this.keeperNumber = keeperOrder.getKeeperNo();
        this.BookingDate = keeperOrder.getBookingDate();
        this.ReturnDate = keeperOrder.getDueDate();
    }
}
