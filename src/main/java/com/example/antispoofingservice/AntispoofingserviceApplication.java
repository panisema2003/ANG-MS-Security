package com.example.antispoofingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AntispoofingserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AntispoofingserviceApplication.class, args);
	}

}
