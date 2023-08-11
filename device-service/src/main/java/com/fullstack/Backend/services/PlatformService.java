package com.fullstack.Backend.services;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.fullstack.Backend.models.Platform;
import com.fullstack.Backend.utils.dropdowns.PlatformList;

public interface PlatformService {
	public CompletableFuture<List<String>> getPlatformNameList();

	public CompletableFuture<List<String>> getPlatformVersionList();

	public CompletableFuture<List<String>> getPlatformNameVersionList();
	public CompletableFuture<Boolean> doesPlatformExist(int id);

	public CompletableFuture<Platform> findByNameAndVersion(String name, String version);

	public CompletableFuture<List<PlatformList>> fetchPlatform();

}
