package org.poolc.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PoolcApplication {

    public static void main(String[] args) {
        SpringApplication.run(PoolcApplication.class, args);
    }

}
