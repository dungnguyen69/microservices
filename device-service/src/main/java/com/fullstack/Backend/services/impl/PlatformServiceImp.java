package com.fullstack.Backend.services.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fullstack.Backend.models.Platform;
import com.fullstack.Backend.repositories.interfaces.PlatformRepository;
import com.fullstack.Backend.services.PlatformService;
import com.fullstack.Backend.utils.dropdowns.PlatformList;

@Service
@CacheConfig(cacheNames = {"platform"})
public class PlatformServiceImp implements PlatformService {

    @Autowired
    PlatformRepository _platformRepository;

    @Override
    public List<String> getPlatformNameList() {
        return _platformRepository.findPlatformName();
    }

    @Override
    public List<String> getPlatformVersionList() {
        return _platformRepository.findPlatformVersion();
    }

    @Override
    public List<String> getPlatformNameVersionList() {
        return _platformRepository.findPlatformNameVersion();
    }

    @Override
    public Boolean doesPlatformExist(int id) {
        return _platformRepository.existsById((long) id);
    }

    @Override
    @Cacheable(key = "{#name, #version}")
    public Platform findByNameAndVersion(String name, String version) {
        return _platformRepository.findByNameAndVersion(name, version);
    }

    @Override
    public List<PlatformList> fetchPlatform() {
        return _platformRepository.fetchPlatform();
    }

}
