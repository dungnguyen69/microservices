package com.fullstack.Backend;

import com.fasterxml.jackson.databind.*;
import com.fullstack.Backend.controllers.DeviceController;
import com.fullstack.Backend.dto.device.AddDeviceDTO;
import com.fullstack.Backend.dto.device.DeviceDTO;
import com.fullstack.Backend.dto.device.FilterDeviceDTO;
import com.fullstack.Backend.dto.device.UpdateDeviceDTO;
import com.fullstack.Backend.enums.Origin;
import com.fullstack.Backend.enums.Project;
import com.fullstack.Backend.enums.Status;
import com.fullstack.Backend.mappers.DeviceMapper;
import com.fullstack.Backend.mappers.DeviceMapperImp;
import com.fullstack.Backend.models.Device;
import com.fullstack.Backend.responses.device.AddDeviceResponse;
import com.fullstack.Backend.responses.device.DeleteDeviceResponse;
import com.fullstack.Backend.responses.device.DeviceInWarehouseResponse;
import com.fullstack.Backend.responses.device.UpdateDeviceResponse;
import com.fullstack.Backend.services.DeviceService;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/*https://thepracticaldeveloper.com/guide-spring-boot-controller-tests/#strategy-1-spring-mockmvc-example-in-standalone-mode*/
@ExtendWith(MockitoExtension.class)
public class DeviceApiControllerTest {
    private static final String END_POINT = "/api/devices";

    private MockMvc mockMvc;
    private final ObjectWriter ow = new ObjectMapper()
            .writer()
            .withDefaultPrettyPrinter();
    private final MediaType MEDIA_TYPE_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    final static Logger logger = LoggerFactory.getLogger(DeviceApiControllerTest.class);
    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DeviceController deviceController;

    private final DeviceMapper deviceMapper = new DeviceMapperImp();

    public DeviceApiControllerTest() {
    }

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(deviceController)
                .build();
    }

    @Test
    public void testListShouldReturn200() throws Exception {
        DeviceDTO mockDevice1 = DeviceDTO
                .builder()
                .Id(1)
                .DeviceName("Air pal")
                .Status("OCCUPIED")
                .RamSize("8 GB")
                .PlatformName("N/A")
                .PlatformVersion("N/A")
                .ItemType("Laptop")
                .ScreenSize("N/A")
                .StorageSize("N/A")
                .InventoryNumber("ABC1b4")
                .SerialNumber("t123")
                .Project("TELSA")
                .Origin("Internal")
                .Owner("valiance")
                .Keeper("valiance")
                .keeperNumber(0)
                .BookingDate(null)
                .ReturnDate(null)
                .CreatedDate(null)
                .UpdatedDate(null)
                .build();
        DeviceDTO mockDevice2 = DeviceDTO
                .builder()
                .Id(2)
                .DeviceName("Air pal")
                .Status("OCCUPIED")
                .RamSize("8 GB")
                .PlatformName("N/A")
                .PlatformVersion("N/A")
                .ItemType("Laptop")
                .ScreenSize("N/A")
                .StorageSize("N/A")
                .InventoryNumber("ABC1b4")
                .SerialNumber("t123")
                .Project("TELSA")
                .Origin("Internal")
                .Owner("valiance")
                .Keeper("valiance")
                .keeperNumber(0)
                .BookingDate(null)
                .ReturnDate(null)
                .CreatedDate(null)
                .UpdatedDate(null)
                .build();
        List<DeviceDTO> deviceList = List.of(mockDevice1, mockDevice2);
        List<String> statusList = deviceList
                .stream()
                .map(DeviceDTO::getStatus)
                .distinct()
                .collect(Collectors.toList());
        List<String> originList = deviceList
                .stream()
                .map(DeviceDTO::getOrigin)
                .distinct()
                .collect(Collectors.toList());
        List<String> projectList = deviceList
                .stream()
                .map(DeviceDTO::getProject)
                .distinct()
                .collect(Collectors.toList());
        List<String> itemTypeList = deviceList
                .stream()
                .map(DeviceDTO::getItemType)
                .distinct()
                .collect(Collectors.toList());
        List<String> keeperNumberOptions = List.of(new String[]{"LESS THAN 3", "EQUAL TO 3"});
        DeviceInWarehouseResponse deviceResponse = new DeviceInWarehouseResponse(deviceList, statusList, originList,
                projectList, itemTypeList, keeperNumberOptions, 1, 2, 2, 1);

        when(deviceService.showDevicesWithPaging(eq(1), eq(2), eq("id"), eq("desc"),
                Mockito.any(FilterDeviceDTO.class))).thenReturn(new ResponseEntity<>(deviceResponse, OK));

        String requestURI = END_POINT + "/warehouse?pageNo=1&pageSize=2";
        this.mockMvc
                .perform(get(requestURI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8"))
                .andDo(print());
    }

    @Test
    public void testAddShouldReturn400BadRequest() throws Exception {
        AddDeviceDTO newDevice = AddDeviceDTO
                .builder()
                .deviceName("Air pod")
                .statusId(2)
                .platformId(1)
                .itemTypeId(21)
                .ramId(3)
                .screenId(1)
                .storageId(1)
                .owner("valiance")
                .inventoryNumber("ABC1b4")
                .serialNumber("12345XT")
                .originId(0)
                .projectId(1)
                .comments(null)
                .build();

        when(deviceService.addDevice(refEq(newDevice))).thenReturn(
                new ResponseEntity<>(Mockito.any(ArrayList.class), BAD_REQUEST));
        String requestBody = ow.writeValueAsString(newDevice);

        MvcResult mvcResult = mockMvc
                .perform(post(END_POINT + "/warehouse")
                        .contentType(MEDIA_TYPE_JSON_UTF8)
                        .content(requestBody)
                        .accept(MEDIA_TYPE_JSON_UTF8)
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andReturn();

        mockMvc
                .perform(asyncDispatch(mvcResult))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    public void testAddShouldReturn200Ok() throws Exception {
        AddDeviceDTO newDevice = AddDeviceDTO
                .builder()
                .deviceName("Air pod")
                .statusId(2)
                .platformId(1)
                .itemTypeId(2)
                .ramId(3)
                .screenId(1)
                .storageId(1)
                .owner("valiance")
                .inventoryNumber("ABC1b4")
                .serialNumber("12345XT")
                .originId(0)
                .projectId(1)
                .comments(null)
                .build();

        Device device = deviceMapper.addDeviceDtoToDevice(newDevice);
        AddDeviceResponse addDeviceResponse = new AddDeviceResponse(device, true);
        when(deviceService.addDevice(refEq(newDevice))).thenReturn(ResponseEntity.ok(addDeviceResponse));
        String requestBody = ow.writeValueAsString(newDevice);

        MvcResult mvcResult = mockMvc
                .perform(post(END_POINT + "/warehouse")
                        .contentType(MEDIA_TYPE_JSON_UTF8)
                        .content(requestBody)
                        .accept(MEDIA_TYPE_JSON_UTF8)
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andReturn();

        mockMvc
                .perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void testUpdateShouldReturn200Ok() throws Exception {
        int deviceId = 0;
        Device device = Device
                .builder()
                .name("Air pod")
                .status(Status.VACANT)
                .platformId(2)
                .itemTypeId(2)
                .ramId(3)
                .screenId(1)
                .storageId(1)
                .ownerId(1)
                .inventoryNumber("ABC1b4")
                .serialNumber("12345XT")
                .origin(Origin.External)
                .project(Project.BMW)
                .comments(null)
                .build();
        device.setCreatedDate(new Date());
        device.setId(deviceId);
        UpdateDeviceDTO dto = UpdateDeviceDTO
                .builder()
                .name("Air pod")
                .statusId(2)
                .platformId(1)
                .itemTypeId(2)
                .ramId(3)
                .screenId(1)
                .storageId(1)
                .owner("valiance")
                .inventoryNumber("ABC1b4")
                .serialNumber("12345XT")
                .originId(0)
                .projectId(1)
                .comments(null)
                .build();

        Optional<Device> updatedDevice = deviceMapper.updateDtoToDevice(device, dto);
        UpdateDeviceResponse response = new UpdateDeviceResponse();
        response.setUpdatedDevice(updatedDevice.get());
        when(deviceService.updateDevice(eq(deviceId), refEq(dto))).thenReturn(response);
        String requestBody = ow.writeValueAsString(dto);

        logger.info("Response: {}", requestBody);
        MvcResult mvcResult = mockMvc
                .perform(put(END_POINT + "/warehouse/" + deviceId)
                        .contentType(MEDIA_TYPE_JSON_UTF8)
                        .content(requestBody)
                        .accept(MEDIA_TYPE_JSON_UTF8)
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andReturn();

        mockMvc
                .perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    public void testDeleteShouldReturn200OK() throws Exception {
        int deviceId = 0;
        DeleteDeviceResponse response = new DeleteDeviceResponse();
        response.setIsDeletionSuccessful(true);
        when(deviceService.deleteDevice(eq(deviceId))).thenReturn(new ResponseEntity<>(response, OK));

        mockMvc.perform(delete(END_POINT + "/warehouse/" + deviceId))
               .andExpect(status().isOk())
               .andDo(print());
    }
}