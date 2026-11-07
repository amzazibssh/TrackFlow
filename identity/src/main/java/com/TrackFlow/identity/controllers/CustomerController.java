package com.TrackFlow.identity.controllers;

import com.TrackFlow.identity.dtos.entities.CustomerDTO;
import com.TrackFlow.identity.dtos.response.CustomerResponse;
import com.TrackFlow.identity.entities.Customer;
import com.TrackFlow.identity.services.CustomerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/customer")
public class CustomerController {

    private final CustomerService customerServiceImpl;

    public CustomerController(CustomerService customerServiceImpl){
        this.customerServiceImpl = customerServiceImpl;
    }
    @PostMapping("/find-or-create")
    public ResponseEntity<CustomerResponse> findOrCreateCustomer(
            @RequestBody CustomerDTO customerDTO) {

        Customer customer = customerServiceImpl.findOrCreateCustomer(customerDTO);

        CustomerResponse response = new CustomerResponse(customer.getId());

        return ResponseEntity.ok(response);
    }
}
