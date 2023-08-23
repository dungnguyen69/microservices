package com.fullstack.Backend.services.impl;

import com.fullstack.Backend.models.Storage;
import com.fullstack.Backend.repositories.interfaces.StorageRepository;
import com.fullstack.Backend.services.StorageService;
import com.fullstack.Backend.utils.dropdowns.StorageList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames = {"storage"})
public class StorageServiceImp implements StorageService {

    @Autowired
    StorageRepository _storageRepository;

    @Override
    public Storage findBySize(String size) {
        return _storageRepository.findBySize(size);
    }

    @Override
    public Boolean doesStorageExist(int id) {
        return _storageRepository.existsById((long) id);
    }

    @Override
    public List<String> getStorageList() {
        return _storageRepository.findStorageSize();
    }

    @Override
    public List<StorageList> fetchStorage() {
        return _storageRepository.fetchStorage();
    }
}
