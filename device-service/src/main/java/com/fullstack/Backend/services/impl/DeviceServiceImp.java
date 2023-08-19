package com.fullstack.Backend.services.impl;

import java.text.ParseException;
import java.util.Date;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fullstack.Backend.dto.device.*;
import com.fullstack.Backend.dto.keeper_order.KeeperOrderListDTO;
import com.fullstack.Backend.dto.request.ReturnKeepDeviceDTO;
import com.fullstack.Backend.models.*;
import com.fullstack.Backend.mappers.DeviceMapper;
import com.fullstack.Backend.responses.device.*;
import com.fullstack.Backend.services.*;
import com.fullstack.Backend.utils.*;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.fullstack.Backend.constant.constant.*;
import static org.springframework.http.HttpStatus.*;

import com.fullstack.Backend.enums.Origin;
import com.fullstack.Backend.enums.Project;
import com.fullstack.Backend.enums.Status;
import com.fullstack.Backend.repositories.interfaces.DeviceRepository;
import com.fullstack.Backend.utils.dropdowns.ItemTypeList;
import com.fullstack.Backend.utils.dropdowns.OriginList;
import com.fullstack.Backend.utils.dropdowns.PlatformList;
import com.fullstack.Backend.utils.dropdowns.ProjectList;
import com.fullstack.Backend.utils.dropdowns.RamList;
import com.fullstack.Backend.utils.dropdowns.ScreenList;
import com.fullstack.Backend.utils.dropdowns.StatusList;
import com.fullstack.Backend.utils.dropdowns.StorageList;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.reactive.function.client.WebClient;


@Service
@CacheConfig(cacheNames = {"device"})
@RequiredArgsConstructor
@Transactional
public class DeviceServiceImp implements DeviceService {

    @Autowired
    DeviceRepository _deviceRepository;

    @Autowired
    ItemTypeService _itemTypeService;

    @Autowired
    RamService _ramService;

    @Autowired
    PlatformService _platformService;

    @Autowired
    ScreenService _screenService;

    @Autowired
    StorageService _storageService;

    @Autowired
    DeviceMapper deviceMapper;

    private final WebClient.Builder webClientBuilder;

    private final ObservationRegistry observationRegistry;

    final static Logger logger = LoggerFactory.getLogger(DeviceServiceImp.class);

    @Override
    public ResponseEntity<Object> showDevicesWithPaging(int pageIndex, int pageSize, String sortBy, String sortDir, FilterDeviceDTO deviceFilter) throws InterruptedException, ExecutionException {
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort
                .by(sortBy)
                .ascending() : Sort
                .by(sortBy)
                .descending();
        List<Device> devices = _deviceRepository.findAll(sort);
        List<DeviceDTO> deviceList = getAllDevices(devices, deviceFilter);
        /* Return lists for filtering purposes*/
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
        /* */
        int totalElements = deviceList.size();
        deviceList = getPage(deviceList, pageIndex, pageSize); /*Pagination*/
        /* Return the desired response*/
        logger.info("Device list after pagination: {}", deviceList);
        DeviceInWarehouseResponse deviceResponse = new DeviceInWarehouseResponse(deviceList, statusList, originList,
                projectList, itemTypeList, keeperNumberOptions, pageIndex, pageSize, totalElements,
                getTotalPages(pageSize, totalElements));
        return new ResponseEntity<>(deviceResponse, OK);
    }

    @Override
    @Transactional
    public ResponseEntity<Object> addDevice(AddDeviceDTO dto) throws ExecutionException, InterruptedException {
        List<ErrorMessage> errors = new ArrayList<>();
        checkFieldsWhenAddingDevice(errors, dto);
        if(errors.size() > 0) return new ResponseEntity<>(errors, BAD_REQUEST);
        Device newDevice = deviceMapper.addDeviceDtoToDevice(dto);
        newDevice.setOwnerId(findUserByName(dto.getOwner()).getId());
        logger.debug("New Device: {}", newDevice);
        saveDevice(newDevice);
        AddDeviceResponse addDeviceResponse = new AddDeviceResponse(newDevice, true);
        return new ResponseEntity<>(addDeviceResponse, OK);
    }

    private void doLongRunningTask() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveDevice(Device device) {
        _deviceRepository.save(device);
    }

    @Override
    @Cacheable(value = "detail_device", key = "#deviceId")
    public DetailDeviceResponse getDetailDevice(int deviceId) throws InterruptedException, ExecutionException {
//        System.out.print("fetching from DB! \n");
        DetailDeviceResponse response = new DetailDeviceResponse();
        Optional<Device> deviceDetail = getDeviceById(deviceId);

        if(deviceDetail.isEmpty()) return response;
        KeeperOrder[] keeperOrderList = getListByDeviceId(deviceDetail
                .get()
                .getId()); /* Get a list of keeper orders of a device*/
        List<KeeperOrder> list = Arrays.asList(keeperOrderList);
        List<KeeperOrderListDTO> showKeeperList = list
                .stream()
                .map(KeeperOrderListDTO::new)
                .toList();
        UpdateDeviceDTO dto = new UpdateDeviceDTO();
        dto.loadFromEntity(deviceDetail.get(), showKeeperList);
        if(!showKeeperList.isEmpty()) {/* Should a list be empty, we set a keeper value is a device's owner */
            Optional<KeeperOrder> keeperOrder = list
                    .stream()
                    .max(Comparator.comparing(KeeperOrder::getKeeperNo)); /* Get the latest keeper order of a device*/
            dto.setKeeper(keeperOrder
                    .get()
                    .getKeeper()
                    .getUserName()); /* Add keeper order information to devices*/
            dto.setBookingDate(keeperOrder
                    .get()
                    .getBookingDate());
            dto.setReturnDate(keeperOrder
                    .get()
                    .getDueDate());
        }
        dto.setKeeper(deviceDetail
                .get()
                .getOwner()
                .getUserName());

        User owner = findUserById(deviceDetail
                .get()
                .getOwnerId());
        dto.setOwner(owner.getUserName());

        response.setDetailDevice(dto);
        return response;
    }

    @Override
    @CacheEvict(value = "detail_device", key = "#deviceId")
    @Transactional
    public UpdateDeviceResponse updateDevice(int deviceId, UpdateDeviceDTO dto) throws ExecutionException, InterruptedException {
        UpdateDeviceResponse detailDeviceResponse = new UpdateDeviceResponse();
        List<ErrorMessage> errors = new ArrayList<>();
        ErrorMessage error = new ErrorMessage(NOT_FOUND, "Device does not exist",
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        Optional<Device> deviceDetail = getDeviceById(deviceId);
        if(deviceDetail.isEmpty()) {
            errors.add(error);
            detailDeviceResponse.setErrors(errors);
            return detailDeviceResponse;
        }
        checkFieldsWhenUpdatingDevice(errors, dto, deviceId);
        if(errors.size() > 0) {
            detailDeviceResponse.setErrors(errors);
            return detailDeviceResponse;
        }

        User response = findUserByName(dto.getOwner());

        int ownerId;
        deviceDetail = deviceMapper.updateDtoToDevice(deviceDetail.get(), dto);
        if(deviceDetail.isEmpty()) {
            errors.add(error);
            detailDeviceResponse.setErrors(errors);
            return detailDeviceResponse;
        }

        ownerId = response.getId();
        deviceDetail
                .get()
                .setOwnerId(ownerId);

        saveDevice(deviceDetail.get());
        detailDeviceResponse.setUpdatedDevice(deviceDetail.get());
        return detailDeviceResponse;
    }

    @Override
    @Caching(evict = {@CacheEvict(value = "detail_device", key = "#deviceId")})
    @Transactional
    public ResponseEntity<Object> deleteDevice(int deviceId) {
        DeleteDeviceResponse response = new DeleteDeviceResponse();

        if(doesDeviceExist(deviceId)) {
            response.setErrorMessage("Device is not existent");
            return new ResponseEntity<>(response, NOT_FOUND);
        }
        if(findRequestBasedOnStatusAndDevice(deviceId, PENDING)) {
            deleteRequestBasedOnStatusAndDevice(deviceId, PENDING);
        }
        if(findRequestBasedOnStatusAndDevice(deviceId, RETURNED)) {
            deleteRequestBasedOnStatusAndDevice(deviceId, RETURNED);
            deleteReturnedDevice(deviceId);
        }
        if(findRequestBasedOnStatusAndDevice(deviceId, APPROVED)) {
            response.setErrorMessage("Cannot delete by virtue of approved requests for this device");
            return new ResponseEntity<>(response, NOT_FOUND);
        }
        if(findRequestBasedOnStatusAndDevice(deviceId, TRANSFERRED)) {
            response.setErrorMessage("Cannot delete by virtue of transferred requests for this device");
            return new ResponseEntity<>(response, NOT_FOUND);
        }
        if(findRequestBasedOnStatusAndDevice(deviceId, EXTENDING)) {
            response.setErrorMessage("Cannot delete because someone uses this device");
            return new ResponseEntity<>(response, NOT_FOUND);
        }
        _deviceRepository.deleteById((long) deviceId);
        response.setIsDeletionSuccessful(true);
        return new ResponseEntity<>(response, OK);
    }

    @Async
    @Override
    public void exportToExcel(HttpServletResponse response) throws IOException, ExecutionException, InterruptedException {
        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());
        String headerKey = "Content-Disposition"; // ?
        String headerValue = "attachment; filename=ExportDevices_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);
        List<Device> devices = _deviceRepository.findAll();
        List<DeviceDTO> deviceList = convertEntityToDTO(devices);
        DeviceExcelExporter excelExporter = new DeviceExcelExporter(deviceList);
        excelExporter.export(response);
    }

    @Async
    @Override
    public void exportToExcelForOwner(int ownerId, HttpServletResponse response) throws IOException, ExecutionException, InterruptedException {
        response.setContentType("application/octet-stream");
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        String currentDateTime = dateFormatter.format(new Date());
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=ExportDevices_" + currentDateTime + ".xlsx";
        response.setHeader(headerKey, headerValue);
        Sort sort = Sort
                .by("id")
                .ascending();
        List<Device> devices = _deviceRepository.findByOwnerId(ownerId, sort);
        List<DeviceDTO> deviceList = convertEntityToDTO(devices);
        DeviceExcelExporter excelExporter = new DeviceExcelExporter(deviceList);
        excelExporter.export(response);
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) throws IOException, InterruptedException, ExecutionException {
        response.setContentType("application/octet-stream");
        String headerKey = "Content-Disposition";
        String headerValue = "attachment; filename=Template_Import.xlsx";
        response.setHeader(headerKey, headerValue);
        String[] statusList = Stream
                .of(Status.values())
                .map(Status::name)
                .toArray(String[]::new);
        String[] projectList = Stream
                .of(Project.values())
                .map(Project::name)
                .toArray(String[]::new);
        String[] originList = Stream
                .of(Origin.values())
                .map(Origin::name)
                .toArray(String[]::new);
        String[] itemTypeList = _itemTypeService
                .getItemTypeList()
                .toArray(String[]::new);
        String[] ramList = _ramService
                .getRamList()
                .toArray(String[]::new);
        String[] platformList = _platformService
                .getPlatformNameVersionList()
                .toArray(String[]::new);
        String[] screenList = _screenService
                .getScreenList()
                .toArray(String[]::new);
        String[] storageList = _storageService
                .getStorageList()
                .toArray(String[]::new);
        DropDownListsDTO dropDownListsDTO = new DropDownListsDTO();
        dropDownListsDTO.setStatusList(statusList);
        dropDownListsDTO.setProjectList(projectList);
        dropDownListsDTO.setOriginList(originList);
        dropDownListsDTO.setItemTypeList(itemTypeList);
        dropDownListsDTO.setRamList(ramList);
        dropDownListsDTO.setPlatformList(platformList);
        dropDownListsDTO.setScreenList(screenList);
        dropDownListsDTO.setStorageList(storageList);
        DeviceExcelTemplate deviceExcelTemplate = new DeviceExcelTemplate();
        deviceExcelTemplate.export(response, dropDownListsDTO);
    }

    /* Check if rows are empty*/
    @Override
    @Transactional
    public ResponseEntity<Object> importToDb(int ownerId, MultipartFile file) throws Exception {
        List<Device> deviceList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        String serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        String message;

        if(DeviceExcelImporter.hasExcelFormat(file)) {
            if(!file.isEmpty()) {
                XSSFWorkbook workBook = new XSSFWorkbook(file.getInputStream());
                XSSFSheet sheet = workBook.getSheet("Devices");

                if(sheet == null) {
                    ErrorMessage errorMessage = new ErrorMessage(NOT_FOUND, "Sheet \"Devices\" is nonexistent",
                            serverTime);
                    return new ResponseEntity<>(errorMessage, NOT_FOUND);
                }

                int numberOfNonEmptyRows = DeviceExcelImporter.getNumberOfNonEmptyCells(sheet, 0);

                if(numberOfNonEmptyRows == 0) {
                    ErrorMessage errorMessage = new ErrorMessage(NOT_FOUND, "Sheet must be not empty", serverTime);
                    return new ResponseEntity<>(errorMessage, BAD_REQUEST);
                }

                checkImportAndAddToList(ownerId, deviceList, errors, numberOfNonEmptyRows, sheet);

                if(!errors.isEmpty()) {
                    ImportError importError = new ImportError(errors);
                    return new ResponseEntity<>(importError, BAD_REQUEST);
                }

                workBook.close();
            }

            try {
                _deviceRepository.saveAll(deviceList);
                message = "Uploaded the file successfully: " + file.getOriginalFilename();
                ImportDeviceResponse importDevice = new ImportDeviceResponse(message);
                return new ResponseEntity<>(importDevice, OK);
            } catch (Exception e) {
                message = "Could not upload the file: " + e.getMessage() + "!";
                ImportDeviceResponse importDevice = new ImportDeviceResponse(message);
                return new ResponseEntity<>(importDevice, EXPECTATION_FAILED);
            }

        }
        message = "Please upload an excel file!"; /*If an imported file is non-excel*/
        ImportDeviceResponse importDevice = new ImportDeviceResponse(message);
        return new ResponseEntity<>(importDevice, NOT_FOUND);
    }

    @Override
    public ResponseEntity<Object> getSuggestKeywordDevices(int fieldColumn, String keyword, FilterDeviceDTO deviceFilter) throws InterruptedException, ExecutionException {

        if(isKeywordInvalid(keyword)) return ResponseEntity
                .status(NOT_FOUND)
                .body("Keyword must be non-null");

        List<Device> devices = _deviceRepository.findAll();
        List<DeviceDTO> deviceList = getAllDevices(devices, deviceFilter);
        Set<String> keywordList = selectColumnForKeywordSuggestion(deviceList, keyword, fieldColumn);
        KeywordSuggestionResponse response = new KeywordSuggestionResponse();
        response.setKeywordList(keywordList);
        return new ResponseEntity<>(response, OK);
    }

    @Override
    public DropdownValuesResponse getDropDownValues() throws InterruptedException, ExecutionException {
        List<ItemTypeList> itemTypeList = _itemTypeService.fetchItemTypes();
        List<RamList> ramList = _ramService.fetchRams();
        List<PlatformList> platformList = _platformService.fetchPlatform();
        List<ScreenList> screenList = _screenService.fetchScreen();
        List<StorageList> storageList = _storageService.fetchStorage();
        List<StatusList> statusList = getStatusList();
        List<ProjectList> projectList = getProjectList();
        List<OriginList> originList = getOriginList();
        DropdownValuesResponse response = new DropdownValuesResponse();
        response.setItemTypeList(itemTypeList);
        response.setRamList(ramList);
        response.setPlatformList(platformList);
        response.setScreenList(screenList);
        response.setStorageList(storageList);
        response.setStatusList(statusList);
        response.setProjectList(projectList);
        response.setOriginList(originList);
        return response;
    }

    @Override
    @Transactional
    public ResponseEntity<Object> updateReturnKeepDevice(ReturnKeepDeviceDTO input) throws ExecutionException, InterruptedException, ParseException {
        /*  No 1: B borrowed A's from 1/6 - 1/10
         *  No 2: C borrowed B's from 1/7 - 1/9
         *  No 3: D borrowed C's from 1/8 - 15/8
         *  No 2 is able to confirm 3's device returned
         *  No 1 is able to confirm that 2 or 3 RETURNED THE DEVICE.
         *  Find orders (keeperOrderReturnList) whose keeper number > that of the input
         *  Find old requests based upon keeper and device of keeperOrderReturnList's keeper order
         *  Set current keeper to input's
         *  Set request status to RETURNED
         *  Set IsReturned to TRUE
         *  Set UpdatedDate to new date
         *  Display a list of old keepers
         */
        ReturnDeviceResponse response = new ReturnDeviceResponse();
        List<String> oldKeepers = new ArrayList<>();
        List<KeeperOrder> keeperOrderReturnList = Arrays
                .asList(getListByDeviceId(input.getDeviceId()))
                .stream()
                .filter(ko -> ko.getKeeperNo() > input.getKeeperNo())
                .toList();

        if(keeperOrderReturnList.size() == 0)
            return new ResponseEntity<>(response, NOT_FOUND);

        for (KeeperOrder keeperOrder : keeperOrderReturnList) {
            Request occupiedRequest = findAnOccupiedRequest(keeperOrder
                    .getKeeper()
                    .getId(), input.getDeviceId());
            occupiedRequest.setCurrentKeeper_Id(input.getCurrentKeeperId());
            occupiedRequest.setRequestStatus(RETURNED);
            keeperOrder.setIsReturned(true);
            keeperOrder.setUpdatedDate(new Date());
            oldKeepers.add(keeperOrder
                    .getKeeper()
                    .getUserName());
            updateRequest(occupiedRequest);
            updateKeeper(keeperOrder);
        }
        response.setKeepDeviceReturned(true);
        response.setOldKeepers(oldKeepers);
        return new ResponseEntity<>(response, OK);
    }

    @Async
    @Override
    @Transactional
    public CompletableFuture<ResponseEntity<Object>> updateReturnOwnedDevice(ReturnKeepDeviceDTO input) throws ExecutionException, InterruptedException, ParseException {
        /*  No 1: B borrowed A's from 1/6 - 1/10
         *  No 2: C borrowed B's from 1/7 - 1/9
         *  No 3: D borrowed C's from 1/8 - 15/8
         *  owner (as an input) is able to confirm that 1, 2 or 3 RETURNED THE DEVICE.
         *  Find orders (keeperOrderReturnList) of a device
         *  Find old requests based upon keeper and device of keeperOrderReturnList's keeper order
         *  Set current keeper to OWNER
         *  Set request status to RETURNED
         *  Set IsReturned to TRUE
         *  Set UpdatedDate to new date
         *  Set device status to VACANT
         *  Display a list of old keepers
         */
        List<KeeperOrder> keeperOrderReturnList = Arrays.asList(getListByDeviceId(input.getDeviceId()));
        ReturnDeviceResponse response = new ReturnDeviceResponse();

        if(keeperOrderReturnList.size() == 0)
            return CompletableFuture.completedFuture(new ResponseEntity<>(response, NOT_FOUND));

        List<String> oldKeepers = new ArrayList<>();
        for (KeeperOrder keeperOrder : keeperOrderReturnList) {
            Request occupiedRequest = findAnOccupiedRequest(keeperOrder
                    .getKeeper()
                    .getId(), input.getDeviceId());
            occupiedRequest.setCurrentKeeper_Id(input.getCurrentKeeperId());
            occupiedRequest.setRequestStatus(RETURNED);
            keeperOrder.setIsReturned(true);
            keeperOrder.setUpdatedDate(new Date());
            oldKeepers.add(keeperOrder
                    .getKeeper()
                    .getUserName());
            occupiedRequest.setUpdatedDate(new Date());
            updateRequest(occupiedRequest);
            updateKeeper(keeperOrder);
        }

        Optional<Device> device = getDeviceById(input.getDeviceId());
        if(device.isEmpty()) return CompletableFuture.completedFuture(new ResponseEntity<>(response, NOT_FOUND));

        device
                .get()
                .setStatus(Status.VACANT);
        saveDevice(device.get());
        response.setKeepDeviceReturned(true);
        response.setOldKeepers(oldKeepers);
        return CompletableFuture.completedFuture(new ResponseEntity<>(response, OK));
    }

    @Override
    public ResponseEntity<Object> showOwnedDevicesWithPaging(int ownerId, int pageIndex, int pageSize, String sortBy, String sortDir, FilterDeviceDTO deviceFilter) throws ExecutionException, InterruptedException {
        var userById = findUserById(ownerId);

        if(userById == null) return new ResponseEntity<>("User does not exist", NOT_FOUND);

        List<DeviceDTO> deviceList = getDevicesOfOwner(ownerId, deviceFilter, sortBy, sortDir);
        /* Return lists for filtering purposes*/
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

        /* */
        int totalElements = deviceList.size();
        deviceList = getPage(deviceList, pageIndex, pageSize); /*Pagination*/
        /* Return the desired response*/
        OwnedDeviceResponse response = new OwnedDeviceResponse();
        response.setDevicesList(deviceList);
        response.setPageNo(pageIndex);
        response.setPageSize(pageSize);
        response.setTotalElements(totalElements);
        response.setTotalPages(getTotalPages(pageSize, totalElements));
        response.setStatusList(statusList);
        response.setOriginList(originList);
        response.setProjectList(projectList);
        response.setItemTypeList(itemTypeList);
        response.setKeeperNumberOptions(keeperNumberOptions);
        return new ResponseEntity<>(response, OK);
    }

    @Override
    public ResponseEntity<Object> showKeepingDevicesWithPaging(int keeperId, int pageIndex, int pageSize, FilterDeviceDTO deviceFilter) throws ExecutionException, InterruptedException {
        var userById = findUserById(keeperId);

        if(userById == null) return new ResponseEntity<>("User does not exist", NOT_FOUND);

        KeepingDeviceResponse response = new KeepingDeviceResponse();
        List<KeepingDeviceDTO> keepingDeviceList = getDevicesOfKeeper(keeperId, deviceFilter);
        if(keepingDeviceList == null) {
            return new ResponseEntity<>(response, OK);
        }
        List<String> statusList = keepingDeviceList
                .stream()
                .map(KeepingDeviceDTO::getStatus)
                .distinct()
                .toList();
        List<String> originList = keepingDeviceList
                .stream()
                .map(KeepingDeviceDTO::getOrigin)
                .distinct()
                .toList();
        List<String> projectList = keepingDeviceList
                .stream()
                .map(KeepingDeviceDTO::getProject)
                .distinct()
                .toList();
        List<String> itemTypeList = keepingDeviceList
                .stream()
                .map(KeepingDeviceDTO::getItemType)
                .distinct()
                .toList();
        List<String> keeperNumberOptions = List.of(new String[]{"LESS THAN 3", "EQUAL TO 3"});
        int totalElements = keepingDeviceList.size();
        keepingDeviceList = getPageForKeepingDevices(keepingDeviceList, pageIndex, pageSize); /*Pagination*/
        /* Return the desired response*/
        response.setDevicesList(keepingDeviceList);
        response.setPageNo(pageIndex);
        response.setPageSize(pageSize);
        response.setTotalElements(totalElements);
        response.setTotalPages(getTotalPages(pageSize, totalElements));
        response.setStatusList(statusList);
        response.setOriginList(originList);
        response.setProjectList(projectList);
        response.setItemTypeList(itemTypeList);
        response.setKeeperNumberOptions(keeperNumberOptions);
        return new ResponseEntity<>(response, OK);
    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> getSuggestKeywordOwnedDevices(int ownerId, int fieldColumn, String keyword, FilterDeviceDTO deviceFilter) throws InterruptedException, ExecutionException {
        if(isKeywordInvalid(keyword)) return CompletableFuture.completedFuture(ResponseEntity
                .status(NOT_FOUND)
                .body("Keyword must be non-null"));

        List<DeviceDTO> deviceList = getDevicesOfOwner(ownerId, deviceFilter, "id", "asc");
        Set<String> keywordList = selectColumnForKeywordSuggestion(deviceList, keyword, fieldColumn);

        KeywordSuggestionResponse response = new KeywordSuggestionResponse();
        response.setKeywordList(keywordList);
        return CompletableFuture.completedFuture(new ResponseEntity<>(response, OK));
    }

    @Async
    @Override
    public CompletableFuture<ResponseEntity<Object>> getSuggestKeywordKeepingDevices(int keeperId, int fieldColumn, String keyword, FilterDeviceDTO deviceFilter) throws InterruptedException, ExecutionException {
        if(isKeywordInvalid(keyword)) return CompletableFuture.completedFuture(ResponseEntity
                .status(NOT_FOUND)
                .body("Keyword must be non-null"));

        List<KeepingDeviceDTO> deviceList = getDevicesOfKeeper(keeperId, deviceFilter);
        Set<String> keywordList = selectColumnForKeepingDevicesKeywordSuggestion(deviceList, keyword, fieldColumn);

        KeywordSuggestionResponse response = new KeywordSuggestionResponse();
        response.setKeywordList(keywordList);
        return CompletableFuture.completedFuture(new ResponseEntity<>(response, OK));
    }

    private List<DeviceDTO> getAllDevices(List<Device> devices, FilterDeviceDTO deviceFilter) throws ExecutionException, InterruptedException {
        formatFilter(deviceFilter); /* Remove spaces and make input text become lowercase*/
        devices = fetchFilteredDevice(deviceFilter, devices); // List of devices after filtering
        List<DeviceDTO> deviceList = convertEntityToDTO(devices);
        deviceList = applyFilterBookingAndReturnDateForDevices(deviceFilter,
                deviceList); /*Apply booking date and return date filter after adding keeper orders to devices*/
        return deviceList;
    }

    private List<DeviceDTO> getPage(List<DeviceDTO> sourceList, int pageIndex, int pageSize) {
        final boolean isPageInvalid = pageSize <= 0 || pageIndex <= 0;
        if(isPageInvalid) return Collections.emptyList();

        int fromIndex = (pageIndex - 1) * pageSize;

        if(sourceList == null || sourceList.size() <= fromIndex) return Collections.emptyList();

        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

    private int getTotalPages(int pageSize, int listSize) {
        if(listSize == 0) return 1;

        if(listSize % pageSize == 0) return listSize / pageSize;

        return (listSize / pageSize) + 1;
    }

    private void formatFilter(FilterDeviceDTO deviceFilterDTO) {
        if(deviceFilterDTO.getName() != null) deviceFilterDTO.setName(deviceFilterDTO
                .getName()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getPlatformName() != null) deviceFilterDTO.setPlatformName(deviceFilterDTO
                .getPlatformName()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getPlatformVersion() != null) deviceFilterDTO.setPlatformVersion(deviceFilterDTO
                .getPlatformVersion()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getRam() != null) deviceFilterDTO.setRam(deviceFilterDTO
                .getRam()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getScreen() != null) deviceFilterDTO.setScreen(deviceFilterDTO
                .getScreen()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getStorage() != null) deviceFilterDTO.setStorage(deviceFilterDTO
                .getStorage()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getInventoryNumber() != null) deviceFilterDTO.setInventoryNumber(deviceFilterDTO
                .getInventoryNumber()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getSerialNumber() != null) deviceFilterDTO.setSerialNumber(deviceFilterDTO
                .getSerialNumber()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getOwner() != null) deviceFilterDTO.setOwner(deviceFilterDTO
                .getOwner()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getKeeper() != null) deviceFilterDTO.setKeeper(deviceFilterDTO
                .getKeeper()
                .trim()
                .toLowerCase());

        if(deviceFilterDTO.getKeeperNo() != null) deviceFilterDTO.setKeeperNo(deviceFilterDTO
                .getKeeperNo()
                .trim()
                .toLowerCase());
    }

    private List<Device> fetchFilteredDevice(FilterDeviceDTO deviceFilter, List<Device> devices) {
        if(deviceFilter.getName() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getName()
                        .toLowerCase()
                        .equals(deviceFilter.getName()))
                .collect(Collectors.toList());
        if(deviceFilter.getStatus() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getStatus()
                        .name()
                        .equalsIgnoreCase(deviceFilter.getStatus()))
                .collect(Collectors.toList());
        if(deviceFilter.getPlatformName() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getPlatform()
                        .getName()
                        .toLowerCase()
                        .equals(deviceFilter.getPlatformName()))
                .collect(Collectors.toList());
        if(deviceFilter.getPlatformVersion() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getPlatform()
                        .getVersion()
                        .toLowerCase()
                        .equals(deviceFilter.getPlatformVersion()))
                .collect(Collectors.toList());
        if(deviceFilter.getItemType() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getItemType()
                        .getName()
                        .equalsIgnoreCase(deviceFilter.getItemType()))
                .collect(Collectors.toList());
        if(deviceFilter.getRam() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getRam()
                        .getSize()
                        .equalsIgnoreCase(deviceFilter.getRam()))
                .collect(Collectors.toList());
        if(deviceFilter.getScreen() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getScreen()
                        .getSize()
                        .equalsIgnoreCase(deviceFilter.getScreen()))
                .collect(Collectors.toList());
        if(deviceFilter.getStorage() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getStorage()
                        .getSize()
                        .equalsIgnoreCase(deviceFilter.getStorage()))
                .collect(Collectors.toList());
        if(deviceFilter.getOwner() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getOwner()
                        .getUserName()
                        .toLowerCase()
                        .equals(deviceFilter.getOwner()))
                .collect(Collectors.toList());
        if(deviceFilter.getOrigin() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getOrigin()
                        .name()
                        .equalsIgnoreCase(deviceFilter.getOrigin()))
                .collect(Collectors.toList());
        if(deviceFilter.getInventoryNumber() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getInventoryNumber()
                        .toLowerCase()
                        .equals(deviceFilter.getInventoryNumber()))
                .collect(Collectors.toList());
        if(deviceFilter.getSerialNumber() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getSerialNumber()
                        .toLowerCase()
                        .equals(deviceFilter.getSerialNumber()))
                .collect(Collectors.toList());
        if(deviceFilter.getProject() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getProject()
                        .name()
                        .equalsIgnoreCase(deviceFilter.getProject()))
                .collect(Collectors.toList());
        return devices;
    }

    private List<StatusList> getStatusList() {
        Status[] statusCode = Status.values();
        List<StatusList> statusList = new ArrayList<>();

        for (int i = 0; i < statusCode.length; i++) {
            StatusList item = new StatusList(i, statusCode[i].toString());
            statusList.add(item);
        }

        return statusList;
    }

    private List<ProjectList> getProjectList() {
        Project[] projectCode = Project.values();
        List<ProjectList> projectList = new ArrayList<>();

        for (int i = 0; i < projectCode.length; i++) {
            ProjectList item = new ProjectList(i, projectCode[i].toString());
            projectList.add(item);
        }

        return projectList;
    }

    private List<OriginList> getOriginList() {
        Origin[] originCode = Origin.values();

        List<OriginList> originList = new ArrayList<>();
        for (int i = 0; i < originCode.length; i++) {
            OriginList item = new OriginList(i, originCode[i].toString());
            originList.add(item);
        }

        return originList;
    }

    private List<DeviceDTO> getDevicesOfOwner(int ownerId, FilterDeviceDTO deviceFilter, String sortBy, String sortDir) throws ExecutionException, InterruptedException {
        formatFilter(deviceFilter); /* Remove spaces and make input text become lowercase*/
        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort
                .by(sortBy)
                .ascending() : Sort
                .by(sortBy)
                .descending();
        List<Device> devices = _deviceRepository.findByOwnerId(ownerId, sort);
        devices = fetchFilteredDevice(deviceFilter, devices); /* List of devices after filtering */
        List<DeviceDTO> deviceList = convertEntityToDTO(devices);
        deviceList = applyFilterBookingAndReturnDateForDevices(deviceFilter,
                deviceList); /*Apply booking date and return date filter after adding keeper orders to devices*/
        return deviceList;
    }

    private boolean isKeywordInvalid(String keyword) {
        return keyword
                .trim()
                .isBlank();
    }

    private List<DeviceDTO> applyFilterBookingAndReturnDateForDevices(FilterDeviceDTO deviceFilter, List<DeviceDTO> devices) {
        if(deviceFilter.getBookingDate() != null) devices = devices
                .stream()
                .filter(device -> device.getBookingDate() != null)
                .filter(device -> device
                        .getBookingDate()
                        .after(deviceFilter.getBookingDate()))
                .collect(Collectors.toList());
        if(deviceFilter.getReturnDate() != null) devices = devices
                .stream()
                .filter(device -> device.getReturnDate() != null)
                .filter(device -> device
                        .getReturnDate()
                        .before(deviceFilter.getReturnDate()))
                .collect(Collectors.toList());
        if(deviceFilter.getKeeper() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getKeeper()
                        .equalsIgnoreCase(deviceFilter.getKeeper()))
                .collect(Collectors.toList());

        if(deviceFilter.getKeeperNo() != null) { //"LESS THAN 3", "EQUAL TO 3"
            if(deviceFilter
                    .getKeeperNo()
                    .equalsIgnoreCase("less than 3")) devices = devices
                    .stream()
                    .filter(device -> device.getKeeperNumber() < 3 && (device
                            .getStatus()
                            .equalsIgnoreCase("OCCUPIED") || device
                            .getStatus()
                            .equalsIgnoreCase("VACANT")))
                    .collect(Collectors.toList());
            else devices = devices
                    .stream()
                    .filter(device -> device.getKeeperNumber() == 3 && device
                            .getStatus()
                            .equalsIgnoreCase("OCCUPIED"))
                    .collect(Collectors.toList());
        }
        return devices;
    }

    private Set<String> selectColumnForKeywordSuggestion(List<DeviceDTO> deviceList, String keyword, int fieldColumn) {
        Set<String> keywordList = new HashSet<>();
        Stream<String> mappedDeviceList = null;
        switch (fieldColumn) { /*Fetch only one column*/
            case DEVICE_NAME_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getDeviceName);
            case DEVICE_PLATFORM_NAME_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getPlatformName);
            case DEVICE_PLATFORM_VERSION_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getPlatformVersion);
            case DEVICE_RAM_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getRamSize);
            case DEVICE_SCREEN_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getScreenSize);
            case DEVICE_STORAGE_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getStorageSize);
            case DEVICE_OWNER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getOwner);
            case DEVICE_INVENTORY_NUMBER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getInventoryNumber);
            case DEVICE_SERIAL_NUMBER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getSerialNumber);
            case DEVICE_KEEPER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(DeviceDTO::getKeeper);
        }
        if(mappedDeviceList != null) {
            keywordList = mappedDeviceList
                    .filter(element -> element
                            .toLowerCase()
                            .contains(keyword
                                    .strip()
                                    .toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toSet());
        }
        return keywordList;
    }

    private Set<String> selectColumnForKeepingDevicesKeywordSuggestion(List<KeepingDeviceDTO> deviceList, String keyword, int fieldColumn) {
        Set<String> keywordList = new HashSet<>();
        Stream<String> mappedDeviceList = null;
        switch (fieldColumn) { /*Fetch only one column*/
            case DEVICE_NAME_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getDeviceName);
            case DEVICE_PLATFORM_NAME_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getPlatformName);
            case DEVICE_PLATFORM_VERSION_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getPlatformVersion);
            case DEVICE_RAM_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getRamSize);
            case DEVICE_SCREEN_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getScreenSize);
            case DEVICE_STORAGE_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getStorageSize);
            case DEVICE_OWNER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getOwner);
            case DEVICE_INVENTORY_NUMBER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getInventoryNumber);
            case DEVICE_SERIAL_NUMBER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getSerialNumber);
            case DEVICE_KEEPER_COLUMN -> mappedDeviceList = deviceList
                    .stream()
                    .map(KeepingDeviceDTO::getKeeper);
        }
        if(mappedDeviceList != null) {
            keywordList = mappedDeviceList
                    .filter(element -> element
                            .toLowerCase()
                            .contains(keyword
                                    .strip()
                                    .toLowerCase()))
                    .limit(20)
                    .collect(Collectors.toSet());
        }
        return keywordList;
    }

    private Boolean useNonExistent(String owner) {
        return findUserByName(owner) == null;
    }

    private Boolean isSerialNumberExistent(String serialNumber) {
        return _deviceRepository.findBySerialNumber(serialNumber) != null;
    }

    private Boolean isSerialNumberExistentExceptUpdatedDevice(int deviceId, String serialNumber) {
        return _deviceRepository.findBySerialNumberExceptUpdatedDevice(deviceId, serialNumber) != null;
    }

    private Boolean isItemTypeInvalid(int itemTypeId) throws ExecutionException, InterruptedException {
        return !_itemTypeService.doesItemTypeExist(itemTypeId);
    }

    private Boolean isRamInvalid(int ramId) throws ExecutionException, InterruptedException {
        return !_ramService.doesRamExist(ramId);
    }

    private Boolean isStorageInvalid(int storageId) throws ExecutionException, InterruptedException {
        return !_storageService.doesStorageExist(storageId);
    }

    private Boolean isScreenInvalid(int screenId) throws ExecutionException, InterruptedException {
        return !_screenService.doesScreenExist(screenId);
    }

    private Boolean isPlatformInvalid(int platformId) throws ExecutionException, InterruptedException {
        return !_platformService.doesPlatformExist(platformId);
    }

    private Boolean isStatusInvalid(int statusId) {
        return Status
                .findByNumber(statusId)
                .isEmpty();
    }

    private Boolean isOriginInvalid(int originId) {
        return Origin
                .findByNumber(originId)
                .isEmpty();
    }

    private Boolean isProjectInvalid(int projectId) {
        return Project
                .findByNumber(projectId)
                .isEmpty();
    }

    private List<DeviceDTO> convertEntityToDTO(List<Device> devices) throws ExecutionException, InterruptedException {
        List<DeviceDTO> deviceList = new ArrayList<>();
        for (Device device : devices) {
            DeviceDTO deviceDTO = deviceMapper.deviceToDeviceDto(
                    device);  /* Convert fields that have an id value to a readable value */
            List<KeeperOrder> keeperOrderList = Arrays.asList(
                    getListByDeviceId(device.getId())); /* Get a list of keeper orders of a device*/

            if(keeperOrderList.isEmpty()) { /* Were a list empty, we would set a keeper value is a device's owner */
                deviceDTO.setKeeper(device
                        .getOwner()
                        .getUserName());
                deviceList.add(deviceDTO);
                continue;
            }

            Optional<KeeperOrder> keeperOrder = keeperOrderList
                    .stream()
                    .max(Comparator.comparing(KeeperOrder::getKeeperNo)); /* Get the latest keeper order of a device*/
            deviceDTO.setKeeper(keeperOrder
                    .get()
                    .getKeeper()
                    .getUserName()); /* Add keeper order information to devices*/
            deviceDTO.setKeeperNumber(keeperOrder
                    .get()
                    .getKeeperNo());
            deviceDTO.setBookingDate(keeperOrder
                    .get()
                    .getBookingDate());
            deviceDTO.setReturnDate(keeperOrder
                    .get()
                    .getDueDate());
            deviceList.add(deviceDTO);
        }
        return deviceList;
    }

    private List<KeepingDeviceDTO> getPageForKeepingDevices(List<KeepingDeviceDTO> sourceList, int pageIndex, int pageSize) {
        if(pageSize <= 0 || pageIndex <= 0) throw new IllegalArgumentException("invalid page size: " + pageSize);

        int fromIndex = (pageIndex - 1) * pageSize;

        if(sourceList == null || sourceList.size() <= fromIndex) return Collections.emptyList();

        return sourceList.subList(fromIndex, Math.min(fromIndex + pageSize, sourceList.size()));
    }

    private List<KeepingDeviceDTO> getDevicesOfKeeper(int keeperId, FilterDeviceDTO deviceFilter) throws ExecutionException, InterruptedException {
        formatFilter(deviceFilter); /* Remove spaces and make input text become lowercase*/
        List<KeeperOrder> keeperOrderList = findByKeeperId(keeperId);
        List<KeepingDeviceDTO> keepingDeviceList = new ArrayList<>();

        if(keeperOrderList == null) return null;

        for (KeeperOrder keeperOrder : keeperOrderList) {
            Optional<Device> device = getDeviceById(keeperOrder
                    .getDevice()
                    .getId());
            if(device.isEmpty()) {
                break;
            }
            List<KeeperOrder> allKeeperOrderList = Arrays.asList(getListByDeviceId(keeperOrder
                    .getDevice()
                    .getId()));
            KeeperOrder latestOrder = allKeeperOrderList
                    .stream()
                    .max(Comparator.comparing(KeeperOrder::getKeeperNo))
                    .orElse(null); /* Get the latest keeper order of a device*/
            KeeperOrder previousOrder = allKeeperOrderList
                    .stream()
                    .filter(k -> k.getKeeperNo() == keeperOrder.getKeeperNo() - 1)
                    .findFirst()
                    .orElse(null);
            KeepingDeviceDTO keepingDevice = new KeepingDeviceDTO(device.get(), keeperOrder);

            /* Set max extending date for keepers */
            if(previousOrder != null) {
                Calendar c = Calendar.getInstance();
                c.setTime(previousOrder.getDueDate());
                c.add(Calendar.DATE, -1);
                previousOrder.setDueDate(c.getTime());
                keepingDevice.setMaxExtendingReturnDate(previousOrder.getDueDate());
            }

            /* To make sure the latest keeper can not return, only previous keeper is able to confirm*/
            if(latestOrder != null) {
                if(latestOrder.getKeeperNo() == keeperOrder.getKeeperNo()) {
                    keepingDevice.setIsReturnable(false);
                }
            }

            keepingDeviceList.add(keepingDevice);
        }
        keepingDeviceList = fetchFilteredKeepingDevice(deviceFilter, keepingDeviceList);
        return keepingDeviceList;
    }

    private List<KeepingDeviceDTO> fetchFilteredKeepingDevice(FilterDeviceDTO deviceFilter, List<KeepingDeviceDTO> devices) {
        if(deviceFilter.getName() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getDeviceName()
                        .toLowerCase()
                        .equals(deviceFilter.getName()))
                .collect(Collectors.toList());
        if(deviceFilter.getStatus() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getStatus()
                        .equalsIgnoreCase(deviceFilter.getStatus()))
                .collect(Collectors.toList());
        if(deviceFilter.getPlatformName() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getPlatformName()
                        .toLowerCase()
                        .equals(deviceFilter.getPlatformName()))
                .collect(Collectors.toList());
        if(deviceFilter.getPlatformVersion() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getPlatformVersion()
                        .toLowerCase()
                        .equals(deviceFilter.getPlatformVersion()))
                .collect(Collectors.toList());
        if(deviceFilter.getItemType() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getItemType()
                        .toLowerCase()
                        .equals(deviceFilter.getItemType()))
                .collect(Collectors.toList());
        if(deviceFilter.getRam() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getRamSize()
                        .toLowerCase()
                        .equals(deviceFilter.getRam()))
                .collect(Collectors.toList());
        if(deviceFilter.getScreen() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getStorageSize()
                        .toLowerCase()
                        .equals(deviceFilter.getScreen()))
                .collect(Collectors.toList());
        if(deviceFilter.getStorage() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getStorageSize()
                        .toLowerCase()
                        .equals(deviceFilter.getStorage()))
                .collect(Collectors.toList());
        if(deviceFilter.getOwner() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getOwner()
                        .toLowerCase()
                        .equals(deviceFilter.getOwner()))
                .collect(Collectors.toList());
        if(deviceFilter.getKeeper() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getKeeper()
                        .toLowerCase()
                        .equals(deviceFilter.getKeeper()))
                .collect(Collectors.toList());
        if(deviceFilter.getOrigin() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getOrigin()
                        .equalsIgnoreCase(deviceFilter.getOrigin()))
                .collect(Collectors.toList());
        if(deviceFilter.getInventoryNumber() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getInventoryNumber()
                        .toLowerCase()
                        .equals(deviceFilter.getInventoryNumber()))
                .collect(Collectors.toList());
        if(deviceFilter.getSerialNumber() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getSerialNumber()
                        .toLowerCase()
                        .equals(deviceFilter.getSerialNumber()))
                .collect(Collectors.toList());
        if(deviceFilter.getProject() != null) devices = devices
                .stream()
                .filter(device -> device
                        .getProject()
                        .equalsIgnoreCase(deviceFilter.getProject()))
                .collect(Collectors.toList());
        if(deviceFilter.getBookingDate() != null) devices = devices
                .stream()
                .filter(device -> device.getBookingDate() != null)
                .filter(device -> device
                        .getBookingDate()
                        .after(deviceFilter.getBookingDate()))
                .collect(Collectors.toList());
        if(deviceFilter.getReturnDate() != null) devices = devices
                .stream()
                .filter(device -> device.getReturnDate() != null)
                .filter(device -> device
                        .getReturnDate()
                        .before(deviceFilter.getReturnDate()))
                .collect(Collectors.toList());
        if(deviceFilter.getKeeperNo() != null) { //"LESS THAN 3", "EQUAL TO 3"
            if(deviceFilter
                    .getKeeperNo()
                    .equalsIgnoreCase("less than 3")) devices = devices
                    .stream()
                    .filter(device -> device.getKeeperNo() < 3 && (device
                            .getStatus()
                            .equalsIgnoreCase("OCCUPIED") || device
                            .getStatus()
                            .equalsIgnoreCase("VACANT")))
                    .collect(Collectors.toList());
            else devices = devices
                    .stream()
                    .filter(device -> device.getKeeperNo() == 3 && device
                            .getStatus()
                            .equalsIgnoreCase("OCCUPIED"))
                    .collect(Collectors.toList());
        }
        return devices;
    }

    private void checkFieldsWhenAddingDevice(List<ErrorMessage> errors, AddDeviceDTO dto) throws ExecutionException, InterruptedException {
        String serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        if(useNonExistent(dto.getOwner())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Owner does not exist", serverTime);
            errors.add(error);
        }
        if(isSerialNumberExistent(dto.getSerialNumber())) {
            ErrorMessage error = new ErrorMessage(BAD_REQUEST, "Serial number value of this device is already existed",
                    serverTime);
            errors.add(error);
        }
        if(isItemTypeInvalid(dto.getItemTypeId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Item type value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
        if(isRamInvalid(dto.getRamId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Ram value of this device is non existent", serverTime);
            errors.add(error);
        }
        if(isStorageInvalid(dto.getStorageId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Storage value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
        if(isScreenInvalid(dto.getScreenId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Screen value of this device is non existent", serverTime);
            error.setMessage("Screen value of this device is non existent");
            errors.add(error);
        }
        if(isPlatformInvalid(dto.getPlatformId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Platform value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
        if(isStatusInvalid(dto.getStatusId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Status value of this device is non existent", serverTime);
            errors.add(error);
        }
        if(isOriginInvalid(dto.getOriginId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Origin value of this device is non existent", serverTime);
            errors.add(error);
        }
        if(isProjectInvalid(dto.getProjectId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Project value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
    }

    private void checkFieldsWhenUpdatingDevice(List<ErrorMessage> errors, UpdateDeviceDTO dto, int deviceId) throws ExecutionException, InterruptedException {
        String serverTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        if(useNonExistent(dto.getOwner())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Owner does not exist", serverTime);
            errors.add(error);
        }
        if(isSerialNumberExistentExceptUpdatedDevice(deviceId, dto.getSerialNumber())) {
            ErrorMessage error = new ErrorMessage(BAD_REQUEST, "Serial number value of this device is already existed",
                    serverTime);
            errors.add(error);
        }
        if(isItemTypeInvalid(dto.getItemTypeId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Item type value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
        if(isRamInvalid(dto.getRamId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Ram value of this device is non existent", serverTime);
            errors.add(error);
        }
        if(isStorageInvalid(dto.getStorageId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Storage value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
        if(isScreenInvalid(dto.getScreenId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Screen value of this device is non existent", serverTime);
            error.setMessage("Screen value of this device is non existent");
            errors.add(error);
        }
        if(isPlatformInvalid(dto.getPlatformId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Platform value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
        if(isStatusInvalid(dto.getStatusId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Status value of this device is non existent", serverTime);
            errors.add(error);
        }
        if(isOriginInvalid(dto.getOriginId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Origin value of this device is non existent", serverTime);
            errors.add(error);
        }
        if(isProjectInvalid(dto.getProjectId())) {
            ErrorMessage error = new ErrorMessage(NOT_FOUND, "Project value of this device is non existent",
                    serverTime);
            errors.add(error);
        }
    }

    private void checkImportAndAddToList(int ownerId, List<Device> deviceList, List<String> errors, int numberOfRows, XSSFSheet sheet) throws ExecutionException, InterruptedException {
        for (int rowIndex = 1; rowIndex <= numberOfRows; rowIndex++) {
            Row currentRow = sheet.getRow(rowIndex);
            String[] platformString = currentRow
                    .getCell(DEVICE_PLATFORM)
                    .toString()
                    .split(",");
            String name = String.valueOf(currentRow.getCell(DEVICE_NAME)), inventoryNumber = String.valueOf(
                    currentRow.getCell(DEVICE_INVENTORY_NUMBER)), serialNumber = String.valueOf(
                    currentRow.getCell(DEVICE_SERIAL_NUMBER)), comments = String.valueOf(
                    currentRow.getCell(DEVICE_COMMENTS));

            ItemType itemType = _itemTypeService.findByName(String.valueOf(currentRow.getCell(DEVICE_ITEM_TYPE)));
            Ram ram = _ramService.findBySize(String.valueOf(currentRow.getCell(DEVICE_RAM)));
            Screen screen = _screenService.findBySize(String.valueOf(currentRow.getCell(DEVICE_SCREEN)));
            Storage storage = _storageService.findBySize(String.valueOf(currentRow.getCell(DEVICE_STORAGE)));
            User owner = findUserById(ownerId);

            String statusString = String.valueOf(currentRow.getCell(DEVICE_STATUS)), originString = String.valueOf(
                    currentRow.getCell(DEVICE_ORIGIN)), projectString = String.valueOf(
                    currentRow.getCell(DEVICE_PROJECT));
            Device existDevice = _deviceRepository.findBySerialNumber(serialNumber);

            int rowInExcel = rowIndex + 1; /* Ignore the headers */

            if(platformString.length != 2) {
                errors.add("Platform at row " + rowInExcel + " is not valid");
                continue;
            }
            String platformName = platformString[0].strip(), platformVersion = platformString[1].strip();
            Platform platform = _platformService.findByNameAndVersion(platformName, platformVersion);
            if(platform == null) errors.add("Platform at row " + rowInExcel + " is not valid");
            if(name.isBlank()) errors.add("Name at row " + rowInExcel + " is not valid");
            if(inventoryNumber.isBlank()) errors.add("Inventory number at row " + rowInExcel + " is not valid");
            if(serialNumber.isBlank()) errors.add("Serial number at row " + rowInExcel + " is not valid");
            if(ram == null) errors.add("Ram at row " + rowInExcel + " is not valid");
            if(itemType == null) errors.add("Item type at row " + rowInExcel + " is not valid");
            if(screen == null) errors.add("Screen at row " + rowInExcel + " is not valid");
            if(storage == null) errors.add("Storage at row " + rowInExcel + " is not valid");
            if(owner == null) errors.add("Owner at row " + rowInExcel + " is not valid");
            if(projectString.isBlank()) errors.add("Project at row " + rowInExcel + " is not valid");
            if(originString.isBlank()) errors.add("Origin at row " + rowInExcel + " is not valid");
            if(statusString.isBlank()) errors.add("Status at row " + rowInExcel + " is not valid");
            /* Display list of error fields */
            if(!errors.isEmpty()) return;
            /* Create a new device */
            if(existDevice == null) {
                Device device = Device
                        .builder()
                        .name(name)
                        .status(Status.valueOf(statusString))
                        .ramId(ram.getId())
                        .platformId(platform.getId())
                        .screenId(screen.getId())
                        .storageId(storage.getId())
                        .ownerId(owner.getId())
                        .origin(Origin.valueOf(originString))
                        .project(Project.valueOf(projectString))
                        .comments(comments)
                        .itemTypeId(itemType.getId())
                        .inventoryNumber(inventoryNumber)
                        .serialNumber(serialNumber)
                        .build();
                device.setCreatedDate(new Date());
                deviceList.add(device);
                continue;
            }
            /* Update an existent device */
            existDevice.setName(name);
            existDevice.setStatus(Status.valueOf(statusString));
            existDevice.setInventoryNumber(inventoryNumber);
            existDevice.setProject(Project.valueOf(projectString));
            existDevice.setOrigin(Origin.valueOf(originString));
            existDevice.setPlatformId(platform.getId());
            existDevice.setRamId(ram.getId());
            existDevice.setItemTypeId(itemType.getId());
            existDevice.setStorageId(storage.getId());
            existDevice.setScreenId(screen.getId());
            existDevice.setComments(comments);
            existDevice.setOwnerId(owner.getId());
            existDevice.setUpdatedDate(new Date());
            deviceList.add(existDevice);
        }
    }

    @Override
    public Optional<Device> getDeviceById(int deviceId) {
        return _deviceRepository.findById(deviceId);
    }

    private Boolean doesDeviceExist(int deviceId) {
        return !_deviceRepository.existsById((long) deviceId);
    }

    private User findUserByName(String userName) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://user-service/api/users/user", uriBuilder -> uriBuilder
                        .queryParam("name", userName)
                        .build())
                .retrieve()
                .bodyToMono(User.class)
                .block();
    }

    private User findUserById(int id) {
        Observation userServiceObservation = Observation
                .createNotStarted("user-service-lookup", this.observationRegistry)
                .lowCardinalityKeyValue("call", "user-service");
        return userServiceObservation.observe(() -> webClientBuilder
                .build()
                .get()
                .uri("http://user-service/api/users/{id}", id)
                .retrieve()
                .bodyToMono(User.class)
                .block());
    }

    private boolean findRequestBasedOnStatusAndDevice(int deviceId, int requestStatus) {
        return Boolean.TRUE.equals(webClientBuilder
                .build()
                .get()
                .uri("http://request-service/api/requests", uriBuilder -> uriBuilder
                        .queryParam("deviceId", deviceId)
                        .queryParam("requestStatus", requestStatus)
                        .build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .block());
    }

    private void deleteRequestBasedOnStatusAndDevice(int deviceId, int requestStatus) {
        webClientBuilder
                .build()
                .delete()
                .uri("http://request-service/api/requests", uriBuilder -> uriBuilder
                        .queryParam("deviceId", deviceId)
                        .queryParam("requestStatus", requestStatus)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private Request findAnOccupiedRequest(int nextKeeperId, int deviceId) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://request-service/api/requests/occupied-requests", uriBuilder -> uriBuilder
                        .queryParam("nextKeeperId", nextKeeperId)
                        .queryParam("deviceId", deviceId)
                        .build())
                .retrieve()
                .bodyToMono(Request.class)
                .block();
    }

    private void updateRequest(Request request) {
        webClientBuilder
                .build()
                .put()
                .uri("http://request-service/api/requests/occupied-requests", uriBuilder -> uriBuilder
                        .queryParam("request", request)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private KeeperOrder[] getListByDeviceId(int deviceId) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://keeper-order-service/api/keeper-orders/devices/{deviceId}", deviceId)
                .retrieve()
                .bodyToMono(KeeperOrder[].class)
                .block();
    }

    private void deleteReturnedDevice(int deviceId) {
        webClientBuilder
                .build()
                .delete()
                .uri("http://keeper-order-service/api/keeper-orders/{deviceId}", deviceId)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private void updateKeeper(KeeperOrder keeperOrder) {
        webClientBuilder
                .build()
                .put()
                .uri("http://keeper-order-service/api/keeper-orders", uriBuilder -> uriBuilder
                        .queryParam("keeperOrder", keeperOrder)
                        .build())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    private List<KeeperOrder> findByKeeperId(int keeperId) {
        return (List<KeeperOrder>) webClientBuilder
                .build()
                .get()
                .uri("http://keeper-order/api/keeper-orders/keepers/{keeperId}", keeperId)
                .retrieve()
                .bodyToMono(Collection.class)
                .block();
    }
}