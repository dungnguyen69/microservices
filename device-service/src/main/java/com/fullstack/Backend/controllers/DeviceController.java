package com.fullstack.Backend.controllers;

import com.fullstack.Backend.dto.device.AddDeviceDTO;
import com.fullstack.Backend.dto.device.FilterDeviceDTO;
import com.fullstack.Backend.dto.device.UpdateDeviceDTO;
import com.fullstack.Backend.dto.request.ReturnKeepDeviceDTO;
import com.fullstack.Backend.models.Device;
import com.fullstack.Backend.responses.device.*;
import com.fullstack.Backend.services.DeviceService;
import com.fullstack.Backend.utils.ErrorMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.fullstack.Backend.constant.constant.*;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    @Autowired
    DeviceService _deviceService;

    private final String user = "user";
    private final String fieldColumnDescription = """
            \n
            NAME = 0 \n
            PLATFORM_NAME = 1 \n
            PLATFORM_VERSION = 2 \n
            RAM = 3 \n
            SCREEN = 4 \n
            STORAGE = 5 \n
            OWNER = 6 \n
            INVENTORY_NUMBER = 7 \n
            SERIAL_NUMBER = 8 \n
            KEEPER = 9 \n
            """;
    private final String filterDescription = "Enter fields to filter";
    private final String keeperInfo = "Keeper Id";
    private final String ownerInfo = "Owner Id";
    private final String deviceInfo= "Device Id";
    private final String badRequest ="Invalid request";
    private final String adminOrMod = "hasRole('MODERATOR') or hasRole('ADMIN')";
    private final String allUsers = "hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')";
    @GetMapping(value = "/{id}")
    @Operation(summary = "REMOTE CALL in microservice: Get device details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device details", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Device.class))
            })
    })
    public ResponseEntity<Device> getDeviceById(@Parameter(description = deviceInfo) @PathVariable(
            value = "id") int id) {
        return ResponseEntity
                .ok()
                .body(_deviceService
                        .getDeviceById(id)
                        .orElse(null));
    }

    @PostMapping
    @Operation(summary = "REMOTE CALL in microservice: Save device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Save successfully", content = {
                    @Content(mediaType = "application/json")
            })
    })
    public void saveDevice(@RequestBody Device device) {
        _deviceService.saveDevice(device);
    }

    @GetMapping("/warehouse")
    @PreAuthorize(allUsers)
    @Operation(summary = "Get a list of devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of Devices", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeviceInWarehouseResponse.class))
            })
    })
    public DeviceInWarehouseResponse showDevicesWithPaging(@RequestParam(defaultValue = DEFAULT_PAGE_NUMBER,
            required = false) int pageNo, @RequestParam(defaultValue = DEFAULT_PAGE_SIZE,
            required = false) int pageSize, @RequestParam(defaultValue = DEFAULT_SORT_BY,
            required = false) String sortBy, @RequestParam(defaultValue = DEFAULT_SORT_DIRECTION,
            required = false) String sortDir, @Parameter(description = filterDescription) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE_TIME) FilterDeviceDTO deviceFilterDTO) throws InterruptedException, ExecutionException {
        return _deviceService.showDevicesWithPaging(pageNo, pageSize, sortBy, sortDir, deviceFilterDTO);
    }

    @GetMapping("/warehouse/{id}")
    @PreAuthorize(allUsers)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = user, fallbackMethod = "fallbackMethod")
    @Retry(name = user)
    @Operation(summary = "Get Device Details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device Details", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DetailDeviceResponse.class))
            })
    })
    public CompletableFuture<Object> getDetailDevice(@Parameter(description = deviceInfo) @PathVariable(
            value = "id") int deviceId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _deviceService.getDetailDevice(deviceId);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @PostMapping(value = "/warehouse", consumes = {"application/json"}, produces = {"application/json"})
    @ResponseBody
    @PreAuthorize(adminOrMod)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethodForResponseEntity")
    @TimeLimiter(name = user)
    @Retry(name = user)
    @Operation(summary = "Add Device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Add successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = AddDeviceResponse.class))
            }), @ApiResponse(responseCode = "400", description = badRequest, content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))
    })
    })
    public CompletableFuture<ResponseEntity<Object>> addDevice(@Valid @RequestBody AddDeviceDTO device) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                AddDeviceResponse response = _deviceService.addDevice(device);
                if(!response
                        .getErrorMessages()
                        .isEmpty())
                {
                    return new ResponseEntity<>(response, BAD_REQUEST);
                }
                return new ResponseEntity<>(response, OK);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @PutMapping("/warehouse/{id}")
    @ResponseBody
    @PreAuthorize(adminOrMod)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = user)
    @Retry(name = user)
    @Operation(summary = "Update Device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateDeviceResponse.class))
            }), @ApiResponse(responseCode = "400", description = badRequest, content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))
    })
    })
    public CompletableFuture<ResponseEntity<Object>> updateDevice(@Parameter(description = deviceInfo) @PathVariable(
            value = "id") int deviceId, @Valid @RequestBody UpdateDeviceDTO device) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UpdateDeviceResponse response = _deviceService.updateDevice(deviceId, device);
                if(response == null) return new ResponseEntity<>(null, NOT_FOUND);
                return new ResponseEntity<>(response, OK);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @GetMapping("/warehouse/suggestion")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Retrieve a list of keywords")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Keyword suggestion", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KeywordSuggestionResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Bad request", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> getSuggestKeywordDevices(@Parameter(
            description = fieldColumnDescription) @RequestParam(name = "column") int fieldColumn, @RequestParam(
            name = "keyword") String keyword, @Parameter(
            description = filterDescription) FilterDeviceDTO device) throws InterruptedException, ExecutionException {
        if(keyword
                .trim()
                .isBlank())
        {
            return ResponseEntity
                    .status(BAD_REQUEST)
                    .body("Keyword must be non-null");
        }
        KeywordSuggestionResponse response = _deviceService.getSuggestKeywordDevices(fieldColumn, keyword, device);
        return new ResponseEntity<>(response, OK);
    }

    @DeleteMapping("/warehouse/{id}")
    @ResponseBody
    @PreAuthorize(adminOrMod)
    @Operation(summary = "Delete Device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeleteDeviceResponse.class))
            }), @ApiResponse(responseCode = "404", description = badRequest, content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> deleteDevice(@Parameter(description = deviceInfo) @PathVariable(
            value = "id") int deviceId) throws InterruptedException, ExecutionException {
        DeleteDeviceResponse response = _deviceService.deleteDevice(deviceId);
        if(!response
                .getErrorMessage()
                .isEmpty())
        {
            return new ResponseEntity<>(response, NOT_FOUND);
        }
        return new ResponseEntity<>(response, OK);
    }

    @GetMapping("/warehouse/export")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Export to Excel")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export successfully", content = {
                    @Content(mediaType = "application/json")
            })
    })
    public void exportToExcel(HttpServletResponse response) throws IOException, ExecutionException, InterruptedException {
        _deviceService.exportToExcel(response);
    }

    @GetMapping("/warehouse/export/{id}")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Export to Excel for Owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Export successfully", content = {
                    @Content(mediaType = "application/json")
            })
    })
    public void exportToExcelForOwner(@Parameter(description = ownerInfo) @PathVariable(
            value = "id") int ownerId, HttpServletResponse response) throws IOException, ExecutionException, InterruptedException {
        _deviceService.exportToExcelForOwner(ownerId, response);
    }

    @GetMapping("/warehouse/download-template")
    @ResponseBody
    @PreAuthorize(adminOrMod)
    @Operation(summary = "Download a template for import")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Download successfully", content = {
                    @Content(mediaType = "application/json")
            })
    })
    public void downloadTemplateImport(HttpServletResponse response) throws IOException, InterruptedException, ExecutionException {
        _deviceService.downloadTemplate(response);
    }

    @PostMapping(value = "/warehouse/import/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseBody
    @PreAuthorize(adminOrMod)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethodForResponseEntity")
    @TimeLimiter(name = user)
    @Retry(name = user)
    @Operation(summary = "Import Device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Imported successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ImportDeviceResponse.class))
            }), @ApiResponse(responseCode = "404", description = badRequest, content = {
            @Content(mediaType = "application/json")
    })
    })
    public CompletableFuture<ResponseEntity<Object>> importFile(@Parameter(
            description = "Id of owner (Please fetch id after login)") @PathVariable(
            value = "id") int ownerId, @RequestParam("file") MultipartFile file) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return _deviceService.importToDb(ownerId, file);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @GetMapping("/warehouse/drop-down-values")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Retrieve Drop-down Value When Adding Device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DropdownValuesResponse.class))
            })
    })
    public DropdownValuesResponse getDropdownValues() throws InterruptedException, ExecutionException {
        return _deviceService.getDropDownValues();
    }

    @GetMapping("/owners/{id}")
    @ResponseBody
    @PreAuthorize(allUsers)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethodForResponseEntity")
    @TimeLimiter(name = user)
    @Retry(name = user, fallbackMethod = "fallbackMethodForResponseEntity")
    @Operation(summary = "Retrieve A List of Devices for Owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = OwnDeviceResponse.class))
            }), @ApiResponse(responseCode = "404", description = "User is not found", content = {
            @Content(mediaType = "application/json")
    })
    })
    public CompletableFuture<ResponseEntity<Object>> getDevicesOfOwner(@Parameter(
            description = ownerInfo) @PathVariable(value = "id") int ownerId, @RequestParam(
            defaultValue = DEFAULT_PAGE_NUMBER, required = false) int pageNo, @RequestParam(
            defaultValue = DEFAULT_PAGE_SIZE, required = false) int pageSize, @RequestParam(
            defaultValue = DEFAULT_SORT_BY, required = false) String sortBy, @RequestParam(
            defaultValue = DEFAULT_SORT_DIRECTION, required = false) String sortDir, @Parameter(
            description = filterDescription) @DateTimeFormat(
            pattern = "yyyy-MM-dd") FilterDeviceDTO deviceFilterDTO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                OwnDeviceResponse response = _deviceService.showOwnDevicesWithPaging(ownerId, pageNo, pageSize, sortBy,
                        sortDir, deviceFilterDTO);
                if(response == null) {
                    return new ResponseEntity<>("User does not exist", NOT_FOUND);
                }
                return new ResponseEntity<>(response, OK);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @GetMapping("/keepers/{id}")
    @ResponseBody
    @PreAuthorize(allUsers)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethodForResponseEntity")
    @TimeLimiter(name = user)
    @Retry(name = user)
    @Operation(summary = "Retrieve A List of Devices for Keeper")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KeepingDeviceResponse.class))
            }), @ApiResponse(responseCode = "404", description = "User is not found", content = {
            @Content(mediaType = "application/json")
    })
    })
    public CompletableFuture<ResponseEntity<Object>> getDevicesOfKeeper(@Parameter(
            description = keeperInfo) @PathVariable(value = "id") int keeperId, @RequestParam(
            defaultValue = DEFAULT_PAGE_NUMBER, required = false) int pageNo, @RequestParam(
            defaultValue = DEFAULT_PAGE_SIZE, required = false) int pageSize, @Parameter(
            description = filterDescription) @DateTimeFormat(
            pattern = "yyyy-MM-dd") FilterDeviceDTO deviceFilterDTO) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                KeepingDeviceResponse response = _deviceService.showKeepingDevicesWithPaging(keeperId, pageNo, pageSize,
                        deviceFilterDTO);
                if(response == null) {
                    return new ResponseEntity<>("User does not exist", NOT_FOUND);
                }
                return new ResponseEntity<>(response, OK);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @PutMapping("/owners/return")
    @ResponseBody
    @PreAuthorize(allUsers)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethodForResponseEntity")
    @TimeLimiter(name = user)
    @Retry(name = user)
    @Operation(summary = "Confirm A Device is Returned for Owner")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Owner Confirmed Successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReturnDeviceResponse.class))
            }), @ApiResponse(responseCode = "404", description = "The current device is in the latest order",
            content = {
                    @Content(mediaType = "application/json")
            })
    })
    public CompletableFuture<ResponseEntity<Object>> updateReturnOwnDevice(@Parameter(
            description = "Enter request info to confirm return") @RequestBody ReturnKeepDeviceDTO request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ReturnDeviceResponse response = _deviceService.updateReturnOwnDevice(request);
                if(response == null) {
                    return new ResponseEntity<>(NOT_FOUND);
                }
                return new ResponseEntity<>(response, OK);
            } catch (InterruptedException | ExecutionException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @PutMapping("/keepers/return")
    @ResponseBody
    @PreAuthorize(allUsers)
    @CircuitBreaker(name = user, fallbackMethod = "fallbackMethodForResponseEntity")
    @TimeLimiter(name = user)
    @Retry(name = user)
    @Operation(summary = "Confirm A Device is Returned for Keeper")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Keeper Confirmed Successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReturnDeviceResponse.class))
            }), @ApiResponse(responseCode = "404", description = "The current device is in the latest order",
            content = {
                    @Content(mediaType = "application/json")
            })
    })
    public CompletableFuture<ResponseEntity<Object>> updateReturnKeepDevice(@Parameter(
            description = "Enter request info to confirm return") @RequestBody ReturnKeepDeviceDTO request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ReturnDeviceResponse response = _deviceService.updateReturnKeepDevice(request);
                if(response == null) {
                    return new ResponseEntity<>(NOT_FOUND);
                }
                return new ResponseEntity<>(response, OK);
            } catch (InterruptedException | ExecutionException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @GetMapping("/owners/suggestion/{id}")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Retrieve a list of keywords from Own Devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Keyword suggestion", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KeywordSuggestionResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Bad request", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> getSuggestKeywordOwnDevices(@Parameter(description = ownerInfo) @PathVariable(
            value = "id") int ownerId, @Parameter(description = fieldColumnDescription) @RequestParam(
            name = "column") int fieldColumn, @Parameter(description = filterDescription) @RequestParam(
            name = "keyword") String keyword, FilterDeviceDTO device) throws InterruptedException, ExecutionException {
        if(keyword
                .trim()
                .isBlank())
        {
            return ResponseEntity
                    .status(BAD_REQUEST)
                    .body("Keyword must be non-null");
        }
        KeywordSuggestionResponse response = _deviceService.getSuggestKeywordOwnDevices(ownerId, fieldColumn, keyword,
                device);
        return new ResponseEntity<>(response, OK);
    }

    @GetMapping("/keepers/suggestion/{id}")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Retrieve a list of keywords from Keeping Devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Keyword suggestion", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KeywordSuggestionResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Bad request", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> getSuggestKeywordKeepingDevices(@Parameter(description = keeperInfo) @PathVariable(
            value = "id") int keeperId, @Parameter(description = fieldColumnDescription) @RequestParam(
            name = "column") int fieldColumn, @RequestParam(name = "keyword") String keyword, @Parameter(
            description = filterDescription) FilterDeviceDTO device) throws InterruptedException, ExecutionException {
        if(keyword
                .trim()
                .isBlank())
        {
            return ResponseEntity
                    .status(BAD_REQUEST)
                    .body("Keyword must be non-null");
        }
        KeywordSuggestionResponse response = _deviceService.getSuggestKeywordKeepingDevices(keeperId, fieldColumn,
                keyword, device);
        return new ResponseEntity<>(response, OK);
    }

    public CompletableFuture<Object> fallbackMethod(RuntimeException exception) {
        return CompletableFuture.supplyAsync(() -> "Something went wrong, please wait a few seconds!");
    }

    public CompletableFuture<ResponseEntity<Object>> fallbackMethodForResponseEntity(RuntimeException exception) {
        return CompletableFuture.supplyAsync(
                () -> new ResponseEntity<>("Something went wrong, please wait a few seconds!", BAD_REQUEST));
    }
}
