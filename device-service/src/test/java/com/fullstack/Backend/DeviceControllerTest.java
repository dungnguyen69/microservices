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
import com.fullstack.Backend.models.Storage;
import com.fullstack.Backend.responses.device.*;
import com.fullstack.Backend.services.DeviceService;
import com.fullstack.Backend.utils.dropdowns.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
public class DeviceControllerTest {
    private static final String END_POINT = "/api/devices";

    private MockMvc mockMvc;
    private final ObjectWriter ow = new ObjectMapper()
            .writer()
            .withDefaultPrettyPrinter();
    private final MediaType MEDIA_TYPE_JSON_UTF8 = new MediaType("application", "json", StandardCharsets.UTF_8);

    final static Logger logger = LoggerFactory.getLogger(DeviceControllerTest.class);
    @Mock
    private DeviceService deviceService;

    @InjectMocks
    private DeviceController deviceController;

    private final DeviceMapper deviceMapper = new DeviceMapperImp();

    public DeviceControllerTest() {
    }

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(deviceController)
                .build();
    }

    @Test
    @DisplayName("Should Show List of Devices using Pagination")
    public void shouldShowDevicesWithPagination() throws Exception {
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
    @DisplayName("Should Fail When Add Invalid Device")
    public void shouldReturnBadRequestWhenAddDevice() throws Exception {
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
    @DisplayName("Should Add Device")
    public void shouldReturn200WhenAddDevice() throws Exception {
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
    @DisplayName("Should Update Device")
    public void shouldReturn200WhenUpdateDevice() throws Exception {
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
        response.setUpdatedDevice(updatedDevice.orElse(null));
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
    @DisplayName("Should Delete Device Successfully")
    public void shouldReturn200WhenDeleteDevice() throws Exception {
        int deviceId = 0;
        DeleteDeviceResponse response = new DeleteDeviceResponse();
        response.setIsDeletionSuccessful(true);
        when(deviceService.deleteDevice(eq(deviceId))).thenReturn(new ResponseEntity<>(response, OK));

        mockMvc
                .perform(delete(END_POINT + "/warehouse/" + deviceId))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("Should Retrieve List of Keywords")
    public void shouldSuggestKeywordDevices() throws Exception {
        Set<String> keywordList = new HashSet<>(List.of("Air pod", "Mac air 11"));
        KeywordSuggestionResponse response = new KeywordSuggestionResponse();
        FilterDeviceDTO dto = new FilterDeviceDTO();
        response.setKeywordList(keywordList);

        when(deviceService.getSuggestKeywordDevices(eq(0), eq("a"), Mockito.any(FilterDeviceDTO.class))).thenReturn(
                new ResponseEntity<>(response, OK));

        mockMvc
                .perform(get(END_POINT + "/warehouse/suggestion")
                        .contentType(MEDIA_TYPE_JSON_UTF8)
                        .param("column", String.valueOf(0))
                        .param("keyword", "a")
                        .param("device", String.valueOf(dto))
                        .accept(MEDIA_TYPE_JSON_UTF8)
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andReturn();
    }

    @Test
    @DisplayName("Should Return Drop-down Values")
    public void shouldReturnDropdownValues() throws Exception {
        ProjectionFactory factory = new SpelAwareProxyProjectionFactory();
        PlatformList platform = factory.createProjection(PlatformList.class);
        platform.setId(1);
        platform.setName("Window");
        ItemTypeList itemType = factory.createProjection(ItemTypeList.class);
        itemType.setId(1);
        itemType.setName("PC");
        RamList ram = factory.createProjection(RamList.class);
        ram.setId(1);
        ram.setName("16GB");
        ScreenList screen = factory.createProjection(ScreenList.class);
        screen.setId(1);
        screen.setName("13 inch");
        StorageList storage = factory.createProjection(StorageList.class);
        storage.setId(1);
        storage.setName("512GB");
        StatusList status = new StatusList(1, "OCCUPIED");
        ProjectList project = new ProjectList(1, "Telsa");
        OriginList origin = new OriginList(1, "EXTERNAL");
        List<ItemTypeList> itemTypeList = List.of(itemType);
        List<RamList> ramList = List.of(ram);
        List<PlatformList> platformList = List.of(platform);
        List<ScreenList> screenList = List.of(screen);
        List<StorageList> storageList = List.of(storage);
        List<StatusList> statusList = List.of(status);
        List<ProjectList> projectList = List.of(project);
        List<OriginList> originList = List.of(origin);
        DropdownValuesResponse response = new DropdownValuesResponse(statusList, itemTypeList, originList, platformList,
                screenList, projectList, storageList, ramList);

        when(deviceService.getDropDownValues()).thenReturn(response);

        mockMvc
                .perform(get(END_POINT + "/warehouse/drop-down-values")
                        .contentType(MEDIA_TYPE_JSON_UTF8)
                        .accept(MEDIA_TYPE_JSON_UTF8)
                        .characterEncoding("utf-8"))
                .andDo(print())
                .andReturn();
    }
}