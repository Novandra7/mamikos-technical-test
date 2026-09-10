package com.mamikos.kostapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class KostApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(KostApiApplication.class, args);
    }
}
