package com.vidhi.secureusermanagement.security;

import com.vidhi.secureusermanagement.entity.User;
import com.vidhi.secureusermanagement.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository
    ) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(
            String username
    ) throws UsernameNotFoundException {

        User user = userRepository.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "User not found"
                        )
                );

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(user.getEmail())
                .password(user.getPassword())

                // Convert database roles:
                // USER  -> ROLE_USER
                // ADMIN -> ROLE_ADMIN
                .roles(
                        user.getRoles()
                                .stream()
                                .map(role -> role.getName())
                                .toArray(String[]::new)
                )

                // true in database = locked
                // false in database = not locked
                .accountLocked(user.isAccountLocked())

                .build();
    }
}