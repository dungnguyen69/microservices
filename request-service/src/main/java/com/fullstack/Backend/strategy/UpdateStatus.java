package com.fullstack.Backend.strategy;

import com.fullstack.Backend.models.Request;

public class UpdateStatus {
    private RequestStatusStrategy strategy;
    private Request request;

    public void setStrategy(RequestStatusStrategy strategy) {
        this.strategy = strategy;
    }

    public void add(Request input) {
        request = input;
    }

    public void update() {
        strategy.updateStatus(request);
    }
}
