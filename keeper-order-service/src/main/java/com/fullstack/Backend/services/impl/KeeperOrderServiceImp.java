package com.fullstack.Backend.services.impl;

import com.fullstack.Backend.models.KeeperOrder;
import com.fullstack.Backend.repositories.interfaces.KeeperOrderRepository;
import com.fullstack.Backend.services.KeeperOrderService;
import jakarta.transaction.Transactional;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@Transactional
@Log
public class KeeperOrderServiceImp implements KeeperOrderService {
    @Autowired
    KeeperOrderRepository _keeperOrderRepository;

    @Override
    public List<KeeperOrder> getListByDeviceId(int deviceId) {
        List<KeeperOrder> lists = _keeperOrderRepository.findKeeperOrderByDeviceId(deviceId);
        log.info(String.valueOf(lists));
        return lists;
    }

    @Override
    public KeeperOrder findByDeviceIdAndKeeperId(int deviceId, int keeperId) {
        return _keeperOrderRepository.findByDeviceIdAndKeeperId(deviceId, keeperId);
    }

    @Override
    public void create(KeeperOrder keeperOrder) {
        _keeperOrderRepository.save(keeperOrder);
    }

    @Override
    public void update(KeeperOrder keeperOrder) {
        _keeperOrderRepository.save(keeperOrder);
    }

    @Override
    public List<KeeperOrder> getAllKeeperOrders() {
        return _keeperOrderRepository.findAll();
    }

    @Override
    public List<KeeperOrder> findByKeeperId(int keeperId) {
        return _keeperOrderRepository.findByKeeperId(keeperId);
    }

    @Override
    public void deleteReturnedDevice(int deviceId) {
        List<KeeperOrder> keeperOrderList = _keeperOrderRepository.findByReturnedDevice(deviceId);
        for (KeeperOrder keeperOrder : keeperOrderList) {
            _keeperOrderRepository.delete(keeperOrder);
        }
    }
}
