package com.TrackFlow.identity.repositories;

import com.TrackFlow.identity.entities.Customer;
import com.TrackFlow.identity.entities.enums.IdType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByIdTypeAndIdNumber(IdType idType, String idNumber);
}
