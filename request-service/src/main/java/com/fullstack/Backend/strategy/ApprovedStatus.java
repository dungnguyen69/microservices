package com.fullstack.Backend.strategy;

import com.fullstack.Backend.enums.Status;
import com.fullstack.Backend.models.Device;
import com.fullstack.Backend.models.Request;
import com.fullstack.Backend.repositories.interfaces.RequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.fullstack.Backend.constant.constant.*;

@Component
public class ApprovedStatus implements RequestStatusStrategy{
    private final RequestRepository _requestRepository;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public ApprovedStatus(RequestRepository requestRepository, WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
        this._requestRepository = requestRepository;
    }

    @Override
    @Transactional
    public void updateStatus(Request request) {
        request.setApprovalDate(new Date());
        request.setRequestStatus(APPROVED);
        _requestRepository.save(request);
        cancelRelatedPendingRequest(request);
        changeDeviceStatusToOccupied(request);
    }

    /* Cancel all related pending requests except the SUBMITTED request */
    @Transactional
    private void cancelRelatedPendingRequest(Request request) {
        List<Request>
                relatedRequests
                = _requestRepository.findDeviceRelatedApprovedRequest(request.getId(), request.getCurrentKeeper_Id(), request
                .getDevice()
                .getId(), PENDING);
        boolean isRequestListInvalid = relatedRequests != null;
        if(isRequestListInvalid) {
            for (Request relatedRequest : relatedRequests) {
                relatedRequest.setRequestStatus(CANCELLED);
                relatedRequest.setCancelledDate(new Date());
                _requestRepository.save(relatedRequest);
            }
        }
    }

    /* Change device status to OCCUPIED when a request is approved */
    @Transactional
    private void changeDeviceStatusToOccupied(Request request) {
        Optional<Device> device = Optional.ofNullable(findDeviceById(request.getDevice().getId()));
        if(device.isEmpty()) {
            return;
        }
        if(device.get().getStatus() != Status.OCCUPIED) {
            device.get().setStatus(Status.OCCUPIED);
            saveDevice(device.get());
        }
    }

    private Device findDeviceById(int id) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://device-service/api/devices/{id}", id)
                .retrieve()
                .bodyToMono(Device.class)
                .block();
    }

    private void saveDevice(Device device) {
        webClientBuilder
                .build()
                .post()
                .uri("http://device-service/api/devices")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(device), Device.class)
                .retrieve()
                .bodyToMono(Device.class)
                .block();
    }
}
