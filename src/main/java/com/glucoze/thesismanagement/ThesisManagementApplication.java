package com.glucoze.thesismanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ThesisManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThesisManagementApplication.class, args);
    }
}