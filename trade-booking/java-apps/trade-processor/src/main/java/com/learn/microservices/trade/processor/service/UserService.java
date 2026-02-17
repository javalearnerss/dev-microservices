package com.learn.microservices.trade.processor.service;

import com.learn.microservices.trade.processor.model.Address;
import com.learn.microservices.trade.processor.model.User;
import com.learn.microservices.trade.processor.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepo;

    @Transactional
    public void saveUserWithAddress() {
        User user = new User();
        user.setId(1L);
        user.setName("Sandeep");

        Address address = new Address();
        address.setId(100L);
        address.setAddress("Bangalore");

        address.setUser(user);
        user.setAddresses(List.of(address));

        userRepo.save(user); // will now persist Address too due to cascade
    }

    public Optional<User> findUser(long id){
        Optional<User> user = userRepo.findById(id);
        /*if(user.isPresent())
            user.get().getAddresses().size();*/
        return user;
    }


}
