package com.fullstack.Backend.services.impl;

import com.fullstack.Backend.models.ItemType;
import com.fullstack.Backend.repositories.interfaces.ItemTypeRepository;
import com.fullstack.Backend.services.ItemTypeService;
import com.fullstack.Backend.utils.dropdowns.ItemTypeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@CacheConfig(cacheNames = {"itemType"})
public class ItemTypeServiceImp implements ItemTypeService {
    @Autowired
    ItemTypeRepository _itemTypeRepository;

    @Override
    public ItemType findByName(String name) {
        return _itemTypeRepository.findByName(name);
    }

    @Override
    public Boolean doesItemTypeExist(int id) {
        return _itemTypeRepository.existsById((long) id);
    }

    @Override
    public List<String> getItemTypeList() {
        return _itemTypeRepository.findItemTypeNames();
    }

    @Override
    public List<ItemTypeList> fetchItemTypes() {
        return _itemTypeRepository.fetchItemTypes();
    }

}
