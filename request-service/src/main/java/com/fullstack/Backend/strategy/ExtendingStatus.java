package com.fullstack.Backend.strategy;

import com.fullstack.Backend.models.KeeperOrder;
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

import static com.fullstack.Backend.constant.constant.*;

@Component
public class ExtendingStatus implements RequestStatusStrategy {
    private final RequestRepository _requestRepository;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public ExtendingStatus(RequestRepository requestRepository, WebClient.Builder webClientBuilder) {
        this._requestRepository = requestRepository;
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    @Transactional
    public void updateStatus(Request request) {
        /* The request will change its status and update approval date */
        /* Accept extending */
        request.setApprovalDate(new Date());
        request.setRequestStatus(TRANSFERRED);
        _requestRepository.save(request);
        changeOldRequest(request);
        /* UPDATE order's due date */
        KeeperOrder keeperOrder = findByDeviceIdAndKeeperId(request.getDevice().getId(), request.getNextKeeper_Id());
        keeperOrder.setDueDate(request.getReturnDate());
        keeperOrder.setUpdatedDate(new Date());
        cancelRelatedExtendingRequest(request);
        updateKeeperOrder(keeperOrder);
    }

    /* The old transferred request's status will be changed to CANCELLED for EXTENDING CASE */
    @Transactional
    private void changeOldRequest(Request request) {
        List<Request>
                preExtendDurationRequest
                = _requestRepository.findDeviceRelatedApprovedRequest(request.getId(), request.getCurrentKeeper_Id(), request
                .getDevice()
                .getId(), TRANSFERRED);
        boolean isRequestListInvalid = preExtendDurationRequest != null;
        if(isRequestListInvalid) {
            for (Request relatedRequest : preExtendDurationRequest) {
                relatedRequest.setRequestStatus(CANCELLED);
                relatedRequest.setCancelledDate(new Date());
                _requestRepository.save(relatedRequest);
            }
        }
    }

    private KeeperOrder findByDeviceIdAndKeeperId(int deviceId, int keeperId) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://keeper-order-service/api/keeper-orders", uriBuilder -> uriBuilder
                        .queryParam("deviceId", deviceId)
                        .queryParam("keeperId", keeperId)
                        .build())
                .retrieve()
                .bodyToMono(KeeperOrder.class)
                .block();
    }

    /* Cancel all related extending requests except the SUBMITTED request */
    @Transactional
    private void cancelRelatedExtendingRequest(Request request) {
        List<Request>
                relatedRequests
                = _requestRepository.findDeviceRelatedApprovedRequest(request.getId(), request.getCurrentKeeper_Id(), request
                .getDevice()
                .getId(), EXTENDING);
        boolean isRequestListInvalid = relatedRequests != null;
        if(isRequestListInvalid) {
            for (Request relatedRequest : relatedRequests) {
                relatedRequest.setRequestStatus(CANCELLED);
                relatedRequest.setCancelledDate(new Date());
                _requestRepository.save(relatedRequest);
            }
        }
    }

    private void updateKeeperOrder(KeeperOrder keeperOrder) {
        webClientBuilder
                .build()
                .put()
                .uri("http://keeper-order-service/api/keeper-orders")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(keeperOrder), KeeperOrder.class)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
