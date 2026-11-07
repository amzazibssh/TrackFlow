package com.TrackFlow.shipment.services.implementations;

import com.TrackFlow.shipment.dtos.entitiesDTO.CustomerDTO;
import com.TrackFlow.shipment.dtos.response.CustomerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class IdentityClient {

    private final RestClient identityRestClient;

    public CustomerResponse getCustomerIdByIdNumber(CustomerDTO customerDTO) {

        return identityRestClient
                .post()
                .uri("/v1/api/customer/find-or-create")
                .body(customerDTO)
                .retrieve()
                .body(CustomerResponse.class);
    }
}