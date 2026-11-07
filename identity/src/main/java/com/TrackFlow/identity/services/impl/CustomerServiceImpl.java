package com.TrackFlow.identity.services.impl;

import com.TrackFlow.identity.dtos.entities.CustomerDTO;
import com.TrackFlow.identity.entities.Customer;
import com.TrackFlow.identity.repositories.CustomerRepository;
import com.TrackFlow.identity.services.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository){
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public Customer findOrCreateCustomer(CustomerDTO customerDTO){
        return customerRepository
                .findByIdTypeAndIdNumber(
                        customerDTO.idType(),
                        customerDTO.idNumber()
                )
                .orElseGet(() -> {
                    Customer customer = new Customer();

                    customer.setIdType(customerDTO.idType());
                    customer.setIdNumber(customerDTO.idNumber());
                    customer.setFirstName(customerDTO.firstName());
                    customer.setLastName(customerDTO.lastName());
                    customer.setPhone(customerDTO.phone());
                    customer.setEmail(customerDTO.email());

                    return customerRepository.save(customer);
                });
    }
}
