package com.fullstack.Backend.strategy;

import com.fullstack.Backend.models.Request;

public interface RequestStatusStrategy {
    public void updateStatus(Request request);
}
