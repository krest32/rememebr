package com.krest.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Krest
 */
@EnableScheduling
@SpringBootApplication
public class BootApp5 {

    public static void main(String[] args) {
        SpringApplication.run(BootApp5.class, args);

    }
}
