package com.fullstack.Backend.strategy;

import com.fullstack.Backend.dto.device.ReadableDeviceDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class KeywordSuggestion {
    private KeywordSuggestionStrategy strategy;
    private List<? extends ReadableDeviceDTO> list = new ArrayList<>();

    public void setStrategy(KeywordSuggestionStrategy strategy) {
        this.strategy = strategy;
    }

    public void add(List<? extends ReadableDeviceDTO> dtoList) {
        list = dtoList;
    }

    public Stream<String> suggest() {
        return strategy.suggestKeyword(list);
    }
}
