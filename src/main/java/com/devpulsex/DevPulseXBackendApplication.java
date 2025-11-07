package com.devpulsex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DevPulseXBackendApplication {

    public static void main(String[] args) {
        // Load .env file before starting the application
        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(e -> System.setProperty(e.getKey(), e.getValue()));
        } catch (io.github.cdimascio.dotenv.DotenvException e) {
            System.err.println("Warning: Could not load .env file: " + e.getMessage());
        }
        
        SpringApplication.run(DevPulseXBackendApplication.class, args);
    }

}
