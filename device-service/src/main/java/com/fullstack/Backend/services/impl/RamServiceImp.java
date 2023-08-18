package com.fullstack.Backend.services.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
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

    @Override
    @Cacheable(key = "size")
    public Ram findBySize(String size) {
        return _ramRepository.findBySize(size);
    }

    @Override
    public Boolean doesRamExist(int id) {
        return _ramRepository.existsById((long) id);
    }

    @Override
    public List<String> getRamList() {
        return _ramRepository.findRamSize();
    }

    @Override
    public List<RamList> fetchRams() {
        return _ramRepository.fetchRams();
    }
}
