package com.learn.microservices.trade.processor.repository;

import com.learn.microservices.trade.processor.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
