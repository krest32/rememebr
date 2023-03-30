package com.krest.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author Krest
 */
@EnableScheduling
@SpringBootApplication
public class BootApp2 {

    public static void main(String[] args) {
        SpringApplication.run(BootApp2.class, args);

    }
}
