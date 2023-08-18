package com.fullstack.Backend.event;

import com.fullstack.Backend.models.Request;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RequestPlacedEvent {
    private List<Request> requests;
}
