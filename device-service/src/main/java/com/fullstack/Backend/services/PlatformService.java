package com.fullstack.Backend.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fullstack.Backend.models.Platform;
import com.fullstack.Backend.utils.dropdowns.PlatformList;

public interface PlatformService {
	public List<String> getPlatformNameList();

	public List<String> getPlatformVersionList();

	public List<String> getPlatformNameVersionList();
	public Boolean doesPlatformExist(int id);

	public Platform findByNameAndVersion(String name, String version);

	public List<PlatformList> fetchPlatform();

}
