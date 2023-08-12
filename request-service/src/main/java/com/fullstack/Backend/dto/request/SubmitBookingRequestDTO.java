package com.fullstack.Backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitBookingRequestDTO {
    List<RequestInput> requestsList;
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequestInput {
        @NotEmpty
        private int deviceId;
        @NotNull
        @NotEmpty
        private String requester;
        @NotNull
        @NotEmpty
        private String nextKeeper;
        private Date bookingDate;
        private Date returnDate;
    }
}
