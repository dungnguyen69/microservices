package com.fullstack.Backend.controllers;

import com.fullstack.Backend.models.KeeperOrder;
import com.fullstack.Backend.repositories.interfaces.KeeperOrderRepository;
import com.fullstack.Backend.services.KeeperOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/keeper-orders")
public class KeeperOrderController {

    @Autowired
    KeeperOrderService keeperOrderService;

    @GetMapping("/devices/{id}")
    public List<KeeperOrder> getListByDeviceId(@PathVariable(value = "id") int deviceId) throws ExecutionException, InterruptedException {
        return keeperOrderService.getListByDeviceId(deviceId);
    }

    @PutMapping
    public void updateKeeperOrder(KeeperOrder keeperOrder) throws ExecutionException, InterruptedException {
        keeperOrderService.update(keeperOrder);
    }

    @PostMapping
    public void createKeeperOrder(KeeperOrder keeperOrder) throws ExecutionException, InterruptedException {
        keeperOrderService.create(keeperOrder);
    }

    @DeleteMapping("{id}")
    public void deleteReturnedDevice(@PathVariable(value = "id") int deviceId) {
        keeperOrderService.deleteReturnedDevice(deviceId);
    }

    @GetMapping("/keepers/{id}")
    public List<KeeperOrder> findByKeeperId(@PathVariable(value = "id") int keeperId) {
        return keeperOrderService.findByKeeperId(keeperId);
    }

    @GetMapping
    public KeeperOrder findByDeviceIdAndKeeperId(int deviceId, int keeperId) throws ExecutionException, InterruptedException {
        return keeperOrderService.findByDeviceIdAndKeeperId(deviceId, keeperId);
    }
}
