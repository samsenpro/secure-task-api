package com.example.secureapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SecureTaskApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecureTaskApiApplication.class, args);
    }
}
