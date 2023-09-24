package com.fullstack.Backend.strategy;

import com.fullstack.Backend.models.Request;
import com.fullstack.Backend.repositories.interfaces.RequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

import static com.fullstack.Backend.constant.constant.CANCELLED;

@Component
public class CancelledStatus implements RequestStatusStrategy{
    private final RequestRepository _requestRepository;
    @Autowired
    public CancelledStatus(RequestRepository requestRepository) {
        this._requestRepository = requestRepository;
    }
    @Override
    @Transactional
    public void updateStatus(Request request) {
        request.setApprovalDate(new Date());
        request.setRequestStatus(CANCELLED);
        _requestRepository.save(request);
    }
}
