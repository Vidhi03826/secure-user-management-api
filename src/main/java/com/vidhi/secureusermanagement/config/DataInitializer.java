package com.vidhi.secureusermanagement.config;

import com.vidhi.secureusermanagement.entity.Role;
import com.vidhi.secureusermanagement.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initRoles(RoleRepository roleRepository) {

        return args -> {

            if (roleRepository.findByName("USER").isEmpty()) {
                roleRepository.save(new Role("USER"));
            }

            if (roleRepository.findByName("ADMIN").isEmpty()) {
                roleRepository.save(new Role("ADMIN"));
            }
        };
    }
}