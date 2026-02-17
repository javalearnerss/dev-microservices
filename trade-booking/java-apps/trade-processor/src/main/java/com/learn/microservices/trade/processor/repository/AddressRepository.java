package com.learn.microservices.trade.processor.repository;

import com.learn.microservices.trade.processor.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, Long> {
}
