package com.fullstack.Backend.services.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fullstack.Backend.models.Ram;
import com.fullstack.Backend.repositories.interfaces.RamRepository;
import com.fullstack.Backend.services.RamService;
import com.fullstack.Backend.utils.dropdowns.RamList;

@Service
@CacheConfig(cacheNames = {"ram"})
public class RamServiceImp implements RamService {

    @Autowired
    RamRepository _ramRepository;

    @Async
    @Override
    @Cacheable(key = "size")
    public CompletableFuture<Ram> findBySize(String size) {
        return CompletableFuture.completedFuture(_ramRepository.findBySize(size));
    }

    @Async
    @Override
    public CompletableFuture<Boolean> doesRamExist(int id) {
        return CompletableFuture.completedFuture(_ramRepository.existsById((long) id));
    }

    @Async
    @Override
    public CompletableFuture<List<String>> getRamList() {
        return CompletableFuture.completedFuture(_ramRepository.findRamSize());
    }

    @Async
    @Override
    public CompletableFuture<List<RamList>> fetchRams() {
        return CompletableFuture.completedFuture(_ramRepository.fetchRams());
    }
}
