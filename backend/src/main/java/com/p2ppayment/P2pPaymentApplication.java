package com.p2ppayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class P2pPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(P2pPaymentApplication.class, args);
    }
}