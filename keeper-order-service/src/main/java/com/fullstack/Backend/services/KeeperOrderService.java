package com.fullstack.Backend.services;

import com.fullstack.Backend.models.KeeperOrder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface KeeperOrderService {
    public List<KeeperOrder> getListByDeviceId(int deviceId)
            throws InterruptedException, ExecutionException;

    public KeeperOrder findByDeviceIdAndKeeperId(int deviceId, int keeperId)
            throws InterruptedException, ExecutionException;

    public void create(KeeperOrder keeperOrder)
            throws InterruptedException, ExecutionException;

    public void update(KeeperOrder keeperOrder)
            throws InterruptedException, ExecutionException;

    public List<KeeperOrder> getAllKeeperOrders();

    public List<KeeperOrder> findByKeeperId(int keeperId);

    public void findByReturnedDevice(int deviceId);
}
