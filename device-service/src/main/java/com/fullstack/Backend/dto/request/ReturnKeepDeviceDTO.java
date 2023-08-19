package com.fullstack.Backend.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReturnKeepDeviceDTO {
    private int keeperNo;
    private int deviceId;
    @NotEmpty
    private int currentKeeperId;
}
