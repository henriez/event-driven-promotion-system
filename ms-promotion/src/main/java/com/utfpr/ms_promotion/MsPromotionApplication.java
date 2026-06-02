package com.utfpr.ms_promotion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MsPromotionApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsPromotionApplication.class, args);
	}
}
