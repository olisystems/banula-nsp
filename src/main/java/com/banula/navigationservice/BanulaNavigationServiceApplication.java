package com.banula.navigationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = { "com.banula" })
public class BanulaNavigationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BanulaNavigationServiceApplication.class, args);
	}

}
