package com.ooad.mhs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of VCogniRule, the rule-based mental health screening system.
 *
 * {@code @SpringBootApplication} turns on Spring Boot: it scans the com.ooad.mhs package for
 * controllers, services, repositories and other components, creates one object of each, connects
 * them through their constructors (dependency injection) and starts the embedded web server
 * (http://localhost:8080).
 */
@SpringBootApplication
public class MentalHealthApp {

    public static void main(String[] args) {
        SpringApplication.run(MentalHealthApp.class, args);
    }
}