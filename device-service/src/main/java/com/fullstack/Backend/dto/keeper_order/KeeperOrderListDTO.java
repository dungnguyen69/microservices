package com.fullstack.Backend.dto.keeper_order;

import com.fullstack.Backend.models.KeeperOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class KeeperOrderListDTO {
    private String keeperName;
    private int keeperNo;
    private Date bookingDate;
    private Date dueDate;

    public KeeperOrderListDTO(KeeperOrder keeperOrder){
        this.keeperName = keeperOrder.getKeeper().getUserName();
        this.keeperNo = keeperOrder.getKeeperNo();
        this.bookingDate = keeperOrder.getBookingDate();
        this.dueDate = keeperOrder.getDueDate();
    }
}
