package com.learn.microservices.trade.processor;

import com.learn.microservices.trade.processor.model.Address;
import com.learn.microservices.trade.processor.model.User;
import com.learn.microservices.trade.processor.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;
import java.util.Optional;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties
public class TradeProcessorApplication {



	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(TradeProcessorApplication.class, args);
		UserService userService = context.getBean(UserService.class);
		userService.saveUserWithAddress();
		Optional<User> user = userService.findUser(1l);
		if(user.isPresent()) {
			System.out.println(user.get().getName());
			List<Address> addresses = user.get().getAddresses();
			System.out.println(addresses.get(0).getAddress());
		}
	}

}
