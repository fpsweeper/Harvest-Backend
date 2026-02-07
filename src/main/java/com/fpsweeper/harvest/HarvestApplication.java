package com.fpsweeper.harvest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Enable async email sending
public class HarvestApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarvestApplication.class, args);
	}

}
