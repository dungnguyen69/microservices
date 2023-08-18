package com.microservice.notificationservice;

import com.microservice.notificationservice.models.Request;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestPlacedEvent {
    private List<Request> requests;
}
