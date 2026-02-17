package com.learn.microservices.exchange.config.cors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Autowired
    private CorsProperties cors;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(convertToList(cors.getAllowedOrigins()));
        corsConfiguration.setAllowedMethods(convertToList(cors.getAllowedMethods()));
        corsConfiguration.setAllowedHeaders(convertToList(cors.getAllowedHeaders()));
        corsConfiguration.setExposedHeaders(convertToList(cors.getExposedHeaders()));
        corsConfiguration.setAllowCredentials(cors.isAllowCredentials());
        corsConfiguration.setMaxAge(cors.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);

    }

    public List<String> convertToList(String input) {
        return Arrays.stream(input.split(",")).map(val -> val.trim()).toList();
    }

}
