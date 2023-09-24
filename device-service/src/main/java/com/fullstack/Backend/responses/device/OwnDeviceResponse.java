package com.fullstack.Backend.responses.device;

import com.fullstack.Backend.dto.device.DeviceDTO;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
public class OwnDeviceResponse {
    private List<DeviceDTO> devicesList;
    private List<String> statusList;
    private List<String> originList;
    private List<String> projectList;
    private List<String> itemTypeList;
    private List<String> keeperNumberOptions;
    private int pageNo;
    private int pageSize;
    private long totalElements;
    private int totalPages;
}
