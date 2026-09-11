package com.swefton.backend;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAdminServer
public class SweftonBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SweftonBackendApplication.class, args);
	}

}
