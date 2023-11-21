package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DeadLetterApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeadLetterApplication.class, args);
        System.out.println("Hello world!");
    }
}