package com.fullstack.Backend.controllers;

import com.fullstack.Backend.models.KeeperOrder;
import com.fullstack.Backend.services.KeeperOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/keeper-orders")
public class KeeperOrderController {

    @Autowired
    KeeperOrderService keeperOrderService;

    @GetMapping("/devices/{id}") @Operation(summary = "Retrieve List of Keeper Orders By Device Id")
    public List<KeeperOrder> getListByDeviceId(@Parameter(description = "Device Id") @PathVariable(
            value = "id") int deviceId) throws ExecutionException, InterruptedException {
        return keeperOrderService.getListByDeviceId(deviceId);
    }

    @PutMapping @Operation(summary = "Update Keeper Order") public void updateKeeperOrder(@RequestBody(
            description = "Enter fields to update.") @org.springframework.web.bind.annotation.RequestBody KeeperOrder keeperOrder)
            throws ExecutionException, InterruptedException {
        keeperOrderService.update(keeperOrder);
    }

    @PostMapping @Operation(summary = "Create a New Keeper Order") public void createKeeperOrder(@Valid @RequestBody(
            description = "Enter fields to create.") @org.springframework.web.bind.annotation.RequestBody KeeperOrder keeperOrder)
            throws ExecutionException, InterruptedException {
        keeperOrderService.create(keeperOrder);
    }

    @DeleteMapping("{id}") @Operation(summary = "Delete A Returned Device")
    public void deleteReturnedDevice(@Parameter(description = "Device Id") @PathVariable(value = "id") int deviceId) {
        keeperOrderService.deleteReturnedDevice(deviceId);
    }

    @GetMapping("/keepers/{id}") @Operation(summary = "Retrieve List of Keeper Orders By Keeper Id")
    public List<KeeperOrder> findByKeeperId(@Parameter(description = "Keeper Id") @PathVariable(
            value = "id") int keeperId) {
        return keeperOrderService.findByKeeperId(keeperId);
    }

    @GetMapping @Operation(summary = "Find Keeper Order Based Upon Its Keeper Id and Device Id") @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200", description = "Retrieved successfully", content = {
                            @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = KeeperOrder.class))
                    })
            })
    public KeeperOrder findByDeviceIdAndKeeperId(@Parameter(description = "Device Id") @RequestParam int deviceId, @Parameter(
            description = "Keeper Id") @RequestParam int keeperId) throws ExecutionException, InterruptedException {
        return keeperOrderService.findByDeviceIdAndKeeperId(deviceId, keeperId);
    }
}
