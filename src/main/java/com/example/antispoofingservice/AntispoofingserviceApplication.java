package com.example.antispoofingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling
public class AntispoofingserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(AntispoofingserviceApplication.class, args);
	}

	@Bean // Define RestTemplate as a Spring Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
