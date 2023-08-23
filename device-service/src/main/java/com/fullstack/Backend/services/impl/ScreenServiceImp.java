package com.fullstack.Backend.services.impl;

import com.fullstack.Backend.models.Screen;
import com.fullstack.Backend.repositories.interfaces.ScreenRepository;
import com.fullstack.Backend.services.ScreenService;
import com.fullstack.Backend.utils.dropdowns.ScreenList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames = {"screen"})
public class ScreenServiceImp implements ScreenService {

    @Autowired
    ScreenRepository _screenRepository;

    @Override
    public Screen findBySize(String size) {
        return _screenRepository.findBySize(size);
    }

    @Override
    public Boolean doesScreenExist(int id) {
        return _screenRepository.existsById((long) id);
    }

    @Override
    public List<String> getScreenList() {
        return _screenRepository.findScreenSize();
    }

    @Override
    public List<ScreenList> fetchScreen() {
        return _screenRepository.fetchScreen();
    }

}
