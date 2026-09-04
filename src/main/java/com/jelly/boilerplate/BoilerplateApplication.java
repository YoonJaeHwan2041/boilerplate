package com.jelly.boilerplate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // RefreshTokenCleanupScheduler 등 @Scheduled 활성화
public class BoilerplateApplication {

	public static void main(String[] args) {
		SpringApplication.run(BoilerplateApplication.class, args);
	}

}
