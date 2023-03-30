package com.krest.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Krest
 */
@EnableScheduling
@SpringBootApplication
public class BootApp3 {

    public static void main(String[] args) {
        SpringApplication.run(BootApp3.class, args);

    }
}
