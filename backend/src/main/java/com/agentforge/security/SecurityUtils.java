package com.agentforge.security;

import com.agentforge.entity.User;
import com.agentforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            // Default demo user ID if running unauthenticated
            return userRepository.findAll().stream().findFirst().map(User::getId).orElse(1L);
        }

        String email = auth.getName();
        return userRepository.findByEmail(email).map(User::getId).orElse(1L);
    }
}
