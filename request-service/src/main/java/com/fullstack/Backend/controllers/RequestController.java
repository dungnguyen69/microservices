package com.fullstack.Backend.controllers;

import com.fullstack.Backend.dto.request.ExtendDurationRequestDTO;
import com.fullstack.Backend.dto.request.RequestFilterDTO;
import com.fullstack.Backend.dto.request.SubmitBookingRequestDTO;
import com.fullstack.Backend.dto.request.UpdateStatusRequestDTO;
import com.fullstack.Backend.models.Request;
import com.fullstack.Backend.responses.request.KeywordSuggestionResponse;
import com.fullstack.Backend.responses.request.MessageResponse;
import com.fullstack.Backend.responses.request.ShowRequestsResponse;
import com.fullstack.Backend.responses.request.SubmitBookingResponse;
import com.fullstack.Backend.services.RequestService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.fullstack.Backend.constant.constant.*;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final String allUsers = "hasRole('USER') or hasRole('MODERATOR') or hasRole('ADMIN')";
    private final String requestStatusDescription = """
                Request Status field:
                APPROVED = 0;
                CANCELLED = 2;
                TRANSFERRED = 3;
                PENDING = 4;
                RETURNED = 5;
                EXTENDING = 6;
            """;
    private final String fieldColumnDescription = """
                REQUEST_ID = 0;
                DEVICE_NAME = 1;
                DEVICE_SERIAL_NUMBER = 2;
                REQUESTER = 3;
                CURRENT_KEEPER = 4;
                NEXT_KEEPER = 5;
                APPROVER = 6;
            """;

    @Autowired
    RequestService _requestService;

    @GetMapping
    @Operation(summary = "REMOTE CALL IN MICROSERVICES: Find Request Based Upon Its Status and Device")
    public boolean findRequestBasedOnStatusAndDevice(
            @Parameter(description = "Device Id") @RequestParam int deviceId,
            @Parameter(description = requestStatusDescription) @RequestParam int requestStatus) {
        return _requestService.findRequestBasedOnStatusAndDevice(deviceId, requestStatus);
    }

    @DeleteMapping
    @Operation(summary = "REMOTE CALL IN MICROSERVICES: Delete Request Based On Its Status and Device")
    public void deleteRequestBasedOnStatusAndDevice(
            @Parameter(description = "Device Id") @RequestParam int deviceId,
            @Parameter(description = requestStatusDescription) @RequestParam int requestStatus) {
        _requestService.deleteRequestBasedOnStatusAndDevice(deviceId, requestStatus);
    }

    @GetMapping("/occupied-requests")
    @Operation(summary = "REMOTE CALL IN MICROSERVICES: Find An Occupied Request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Request.class))
            })
    })
    public Request findAnOccupiedRequest(
            @Parameter(description = "Next Keeper Id") @RequestParam int nextKeeperId,
            @Parameter(description = "Device Id") @RequestParam int deviceId)
            throws ParseException, ExecutionException, InterruptedException {
        return _requestService.findAnOccupiedRequest(nextKeeperId, deviceId);
    }

    @PutMapping("/occupied-requests")
    @Operation(summary = "REMOTE CALL IN MICROSERVICES: Update Request")
    public void updateRequest(@Parameter(description = requestStatusDescription) @RequestBody Request request)
            throws ExecutionException, InterruptedException {
        _requestService.updateRequest(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize(allUsers)
    @Operation(summary = "Retrieve a list of requests with pagination")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Retrieved successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ShowRequestsResponse.class))
            })
    })
    public ResponseEntity<Object> getRequestsWithPaging(
            @Parameter(description = "User Id") @PathVariable(value = "id") int userId,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER, required = false) int pageNo,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE, required = false) int pageSize,
            @RequestParam(defaultValue = DEFAULT_SORT_BY, required = false) String sortBy,
            @RequestParam(defaultValue = DEFAULT_SORT_DIRECTION, required = false) String sortDir,
            @Parameter(description = "Enter fields to filter or leave it empty")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) RequestFilterDTO requestFilterDTO)
            throws InterruptedException, ExecutionException {
        return _requestService.showRequestListsWithPaging(userId, pageNo, pageSize, sortBy, sortDir, requestFilterDTO);
    }

    @PostMapping("/submissions")
    @ResponseBody
    @PreAuthorize(allUsers)
    @CircuitBreaker(name = "user", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "user")
    @Retry(name = "user")
    @Operation(summary = "Submit a list of request with PENDING status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Submitted successfully", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = SubmitBookingResponse.class))
            })
    })
    public CompletableFuture<ResponseEntity<Object>> submitBookingRequest(@Valid @Parameter(
            description = "Enter a list of requests to submit. \n" + requestStatusDescription) @RequestBody
                                                                          SubmitBookingRequestDTO requests) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SubmitBookingResponse response = _requestService.submitBookingRequest(requests);
                if(!response.getFailedRequestsList().isEmpty()) {
                    return new ResponseEntity<>(response, BAD_REQUEST);
                }

                return new ResponseEntity<>(response, OK);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @GetMapping("/suggestions/{id}")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Retrieve a list of keywords")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful", content = {
                    @Content(mediaType = "application/json",
                            schema = @Schema(implementation = KeywordSuggestionResponse.class))
            }), @ApiResponse(responseCode = "404", description = "Keyword is null", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> getSuggestKeywordRequests(
            @Parameter(description = "User Id") @PathVariable(value = "id") int employeeId,
            @Parameter(description = fieldColumnDescription) @RequestParam(name = "column") int fieldColumn,
            @RequestParam(name = "keyword") String keyword,
            @Parameter(description = "Enter fields to filter or leave it empty") RequestFilterDTO request)
            throws InterruptedException, ExecutionException {
        if(keyword.trim().isBlank()) {
            return ResponseEntity.status(NOT_FOUND).body("Keyword must be non-null");
        }
        KeywordSuggestionResponse
                response
                = _requestService.getSuggestKeywordRequests(employeeId, fieldColumn, keyword, request);
        return new ResponseEntity<>(response, OK);
    }

    @PutMapping("/status-update")
    @ResponseBody
    @PreAuthorize(allUsers)
    @Operation(summary = "Update Request Status (RETURNED status belong to confirm returning device)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated Successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Boolean.class))
            }), @ApiResponse(responseCode = "400", description = "Bad Request", content = {
            @Content(mediaType = "application/json")
    })
    })
    public ResponseEntity<Object> updateRequestStatus(
            @Parameter(description = "Get existing request to update. \n" + requestStatusDescription) @RequestBody
            UpdateStatusRequestDTO request) throws InterruptedException, ExecutionException {
        return _requestService.updateRequestStatus(request);
    }

    @PostMapping("/keepers/extend-duration")
    @ResponseBody
    @PreAuthorize(allUsers)
    @CircuitBreaker(name = "user", fallbackMethod = "fallbackMethodForResponseEntity")
    @TimeLimiter(name = "user", fallbackMethod = "fallbackMethodForResponseEntity")
    @Retry(name = "user")
    @Operation(summary = "Send Extend Duration Request for Keeping Devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sent Successfully", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = MessageResponse.class))
            }), @ApiResponse(responseCode = "400", description = "Bad Request", content = {
            @Content(mediaType = "application/json")
    })
    })
    public CompletableFuture<ResponseEntity<Object>> extendDurationRequest(
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) @Parameter(
                    description = "Get existing request with EXTENDING status to update. Then confirm by using updateStatus with requestId and requestStatus = 6. \n" + requestStatusDescription)
            ExtendDurationRequestDTO request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return _requestService.extendDurationRequest(request);
            } catch (InterruptedException | ExecutionException | ParseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @ResponseStatus(BAD_REQUEST)
    public CompletableFuture<Object> fallbackMethod(RuntimeException exception) {
        return CompletableFuture.supplyAsync(() -> "Something went wrong, please wait a few seconds!");
    }

    @ResponseStatus(BAD_REQUEST)
    public CompletableFuture<ResponseEntity<Object>> fallbackMethodForResponseEntity(RuntimeException exception) {
        return CompletableFuture.supplyAsync(() -> new ResponseEntity<>("Something went wrong, please wait a few seconds!", BAD_REQUEST));
    }
}
