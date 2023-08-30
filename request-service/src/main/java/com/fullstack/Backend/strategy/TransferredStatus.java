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

import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import static com.fullstack.Backend.constant.constant.TRANSFERRED;

@Component
public class TransferredStatus implements RequestStatusStrategy {
    private final RequestRepository _requestRepository;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public TransferredStatus(RequestRepository requestRepository, WebClient.Builder webClientBuilder) {
        this._requestRepository = requestRepository;
        this.webClientBuilder = webClientBuilder;
    }


    @Override
    @Transactional
    public void updateStatus(Request request) {
        request.setTransferredDate(new Date());
        request.setRequestStatus(TRANSFERRED);
        _requestRepository.save(request);
        KeeperOrder[] lists = getListByDeviceId(request
                .getDevice()
                .getId()); /* Get a list of keeper orders of a device*/
        List<KeeperOrder> keeperOrderList = Arrays.asList(lists);

        int keeperNo = returnKeeperNo(keeperOrderList);
        KeeperOrder keeperOrder = new KeeperOrder();
        keeperOrder.setDevice_Id(request.getDevice().getId());
        keeperOrder.setKeeper_Id(request.getNextKeeper().getId());
        keeperOrder.setKeeperNo(keeperNo + 1);  /* By virtue of being a new keeper order, keeperNo is increased */
        keeperOrder.setBookingDate(request.getBookingDate());
        keeperOrder.setDueDate(request.getReturnDate());
        keeperOrder.setIsReturned(false);
        keeperOrder.setCreatedDate(new Date());
        keeperOrder.setUpdatedDate(new Date());
        createKeeperOrder(keeperOrder);
    }

    /* Get the latest keeper order's number */
    private int returnKeeperNo(List<KeeperOrder> keeperOrderList) {
        return keeperOrderList.size() > 0 ? keeperOrderList
                .stream()
                .max(Comparator.comparing(KeeperOrder::getKeeperNo))
                .map(KeeperOrder::getKeeperNo)
                .get() : 0;
    }

    private KeeperOrder[] getListByDeviceId(int deviceId) {
        return webClientBuilder
                .build()
                .get()
                .uri("http://keeper-order-service/api/keeper-orders/devices/{deviceId}", deviceId)
                .retrieve()
                .bodyToMono(KeeperOrder[].class)
                .block();
    }

    private void createKeeperOrder(KeeperOrder keeperOrder) {
        webClientBuilder
                .build()
                .post()
                .uri("http://keeper-order-service/api/keeper-orders")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(keeperOrder), KeeperOrder.class)
                .retrieve()
                .bodyToMono(Void.class)
                .block(); // Async
    }
}
