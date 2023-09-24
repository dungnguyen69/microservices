package com.fullstack.Backend.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fullstack.Backend.models.Ram;
import com.fullstack.Backend.utils.dropdowns.RamList;

public interface RamService {
	public Ram findBySize(String size);
	public Boolean doesRamExist(int id);
	public List<String> getRamList();

	public List<RamList> fetchRams();
}
