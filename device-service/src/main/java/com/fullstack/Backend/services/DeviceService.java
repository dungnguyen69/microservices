package com.fullstack.Backend.services;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;

import com.fullstack.Backend.dto.device.*;
import com.fullstack.Backend.dto.request.ReturnKeepDeviceDTO;
import com.fullstack.Backend.models.Device;
import com.fullstack.Backend.responses.device.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletResponse;

public interface DeviceService {

    public DeviceInWarehouseResponse showDevicesWithPaging(int pageIndex, int pageSize, String sortBy, String sortDir, FilterDeviceDTO dto) throws InterruptedException, ExecutionException;

    public AddDeviceResponse addDevice(AddDeviceDTO dto) throws ExecutionException, InterruptedException;

    public void saveDevice(Device device);

    public DetailDeviceResponse getDetailDevice(int deviceId) throws InterruptedException, ExecutionException;

    public UpdateDeviceResponse updateDevice(int deviceId, UpdateDeviceDTO device) throws ExecutionException, InterruptedException;

    public DeleteDeviceResponse deleteDevice(int deviceId) throws ExecutionException, InterruptedException;

    public void exportToExcel(HttpServletResponse response) throws IOException, ExecutionException, InterruptedException;

    public void exportToExcelForOwner(int ownerId, HttpServletResponse response) throws IOException, ExecutionException, InterruptedException;

    public void downloadTemplate(HttpServletResponse response) throws IOException, InterruptedException, ExecutionException;

    public ResponseEntity<Object> importToDb(int ownerId, MultipartFile file) throws Exception;

    public KeywordSuggestionResponse getSuggestKeywordDevices(int fieldColumn, String keyword, FilterDeviceDTO dto) throws InterruptedException, ExecutionException;

    public DropdownValuesResponse getDropDownValues() throws InterruptedException, ExecutionException;

    public KeywordSuggestionResponse getSuggestKeywordOwnDevices(int ownerId, int fieldColumn, String keyword, FilterDeviceDTO dto) throws InterruptedException, ExecutionException;

    public KeywordSuggestionResponse getSuggestKeywordKeepingDevices(int keeperId, int fieldColumn, String keyword, FilterDeviceDTO dto) throws InterruptedException, ExecutionException;

    public ReturnDeviceResponse updateReturnKeepDevice(ReturnKeepDeviceDTO request) throws ExecutionException, InterruptedException, ParseException;

    public ReturnDeviceResponse updateReturnOwnDevice(ReturnKeepDeviceDTO request) throws ExecutionException, InterruptedException, ParseException;

    public OwnDeviceResponse showOwnDevicesWithPaging(int ownerId, int pageIndex, int pageSize, String sortBy, String sortDir, FilterDeviceDTO dto) throws ExecutionException, InterruptedException;

    public KeepingDeviceResponse showKeepingDevicesWithPaging(int keeperId, int pageIndex, int pageSize, FilterDeviceDTO dto) throws ExecutionException, InterruptedException;

    public Device getDeviceById(int deviceId);
}
