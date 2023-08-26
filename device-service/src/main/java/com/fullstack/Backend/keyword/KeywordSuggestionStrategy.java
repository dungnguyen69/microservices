package com.fullstack.Backend.keyword;

import com.fullstack.Backend.dto.device.ReadableDeviceDTO;

import java.util.List;
import java.util.stream.Stream;

public interface KeywordSuggestionStrategy {
    public Stream<String> suggestKeyword(List<? extends ReadableDeviceDTO> deviceList);
}
