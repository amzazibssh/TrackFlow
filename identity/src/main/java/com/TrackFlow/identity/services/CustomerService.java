package com.TrackFlow.identity.services;

import com.TrackFlow.identity.dtos.entities.CustomerDTO;
import com.TrackFlow.identity.entities.Customer;

public interface CustomerService {
    public Customer findOrCreateCustomer(CustomerDTO customerDTO);
}
