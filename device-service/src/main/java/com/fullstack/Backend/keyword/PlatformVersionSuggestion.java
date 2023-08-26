package com.fullstack.Backend.keyword;


import com.fullstack.Backend.dto.device.ReadableDeviceDTO;

import java.util.List;
import java.util.stream.Stream;

public class PlatformVersionSuggestion implements KeywordSuggestionStrategy{
    @Override
    public Stream<String> suggestKeyword(List<? extends ReadableDeviceDTO> deviceList) {
        return deviceList.stream()
                         .map(d -> (ReadableDeviceDTO) d)
                         .map(ReadableDeviceDTO::getPlatformVersion);
    }
}
