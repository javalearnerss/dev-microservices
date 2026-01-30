package com.learn.kube.deployment.booking.external.service.feign;


import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "userClient", url = "${user.base-url}")
public interface UserClient {

    /**
     * Validate user exists.
     * GET /users/U-1001
     */
    @GetMapping("/users/{userId}")
    Map<String, Object> getUser(@PathVariable("userId") String userId);
}

