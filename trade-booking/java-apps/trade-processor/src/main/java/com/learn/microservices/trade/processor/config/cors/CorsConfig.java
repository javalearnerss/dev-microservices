package com.learn.microservices.trade.processor.config.cors;

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
    private CorsProperties props;

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(convertToList(props.getAllowedOrigins()));
        cors.setAllowedMethods(convertToList(props.getAllowedMethods()));
        cors.setAllowedHeaders(convertToList(props.getAllowedHeaders()));
        cors.setExposedHeaders(convertToList(props.getExposedHeaders()));
        cors.setMaxAge(props.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return new CorsFilter(source);

    }

    private List<String> convertToList(String input) {
        String[] values = input.split(",");
        return Arrays.asList(values).stream().map(val -> val.trim()).toList();
    }
}
