package com.fullstack.Backend.services.impl;

import com.fullstack.Backend.models.KeeperOrder;
import com.fullstack.Backend.repositories.interfaces.KeeperOrderRepository;
import com.fullstack.Backend.services.KeeperOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class KeeperOrderServiceImp implements KeeperOrderService {
    @Autowired
    KeeperOrderRepository _keeperOrderRepository;

    @Async
    @Override
    public CompletableFuture<List<KeeperOrder>> getListByDeviceId(int deviceId) throws InterruptedException, ExecutionException {
        return CompletableFuture.completedFuture(_keeperOrderRepository.findKeeperOrderByDeviceId(deviceId));
    }

    @Async
    @Override
    public CompletableFuture<KeeperOrder> findByDeviceIdAndKeeperId(int deviceId, int keeperId) throws InterruptedException, ExecutionException {
        return CompletableFuture.completedFuture(_keeperOrderRepository.findByDeviceIdAndKeeperId(deviceId, keeperId));
    }

    @Async
    @Override
    public void create(KeeperOrder keeperOrder) throws InterruptedException, ExecutionException {
        _keeperOrderRepository.save(keeperOrder);
    }

    @Async
    @Override
    public void update(KeeperOrder keeperOrder) {
        _keeperOrderRepository.save(keeperOrder);
    }

    @Async
    @Override
    public CompletableFuture<List<KeeperOrder>> getAllKeeperOrders() {
        return CompletableFuture.completedFuture(_keeperOrderRepository.findAll());
    }

    @Async
    @Override
    public CompletableFuture<List<KeeperOrder>> findByKeeperId(int keeperId) {
        return CompletableFuture.completedFuture(_keeperOrderRepository.findByKeeperId(keeperId));
    }

    @Async
    @Override
    public void findByReturnedDevice(int deviceId) {
        List<KeeperOrder> keeperOrderList = _keeperOrderRepository.findByReturnedDevice(deviceId);
        for (KeeperOrder keeperOrder : keeperOrderList) {
            _keeperOrderRepository.delete(keeperOrder);
        }

    }
}
