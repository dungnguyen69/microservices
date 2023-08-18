package com.fullstack.Backend.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import com.fullstack.Backend.models.Storage;
import com.fullstack.Backend.utils.dropdowns.StorageList;

public interface StorageService {
	public Storage findBySize(String size);
	public Boolean doesStorageExist(int id);

	public List<String> getStorageList();
	
	public List<StorageList> fetchStorage();

}
