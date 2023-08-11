package com.fullstack.Backend.services.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fullstack.Backend.models.ItemType;
import com.fullstack.Backend.repositories.interfaces.ItemTypeRepository;
import com.fullstack.Backend.services.ItemTypeService;
import com.fullstack.Backend.utils.dropdowns.ItemTypeList;

@Service
@CacheConfig(cacheNames = {"itemType"})
public class ItemTypeServiceImp implements ItemTypeService {
	@Autowired
    ItemTypeRepository _itemTypeRepository;

	@Async
	@Override
	@Cacheable(key = "name")
	public CompletableFuture<ItemType> findByName(String name) {
		return CompletableFuture.completedFuture(_itemTypeRepository.findByName(name));
	}

	@Async
	@Override
	public CompletableFuture<Boolean> doesItemTypeExist(int id) {
		return CompletableFuture.completedFuture(_itemTypeRepository.existsById((long) id));
	}

	@Async
	@Override
	public CompletableFuture<List<String>> getItemTypeList() {
		return CompletableFuture.completedFuture(_itemTypeRepository.findItemTypeNames());
	}

	@Async
	@Override
	public CompletableFuture<List<ItemTypeList>> fetchItemTypes() {
		return CompletableFuture.completedFuture(_itemTypeRepository.fetchItemTypes());
	}

}
