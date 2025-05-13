package com.docusign.env;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class EnvApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnvApplication.class, args);
	}

}
