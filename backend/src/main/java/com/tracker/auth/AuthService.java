package com.tracker.auth;

import com.tracker.auth.dto.RegisterRequest;
import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest  request)
    {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if(userRepository.existsByEmail(email))
        {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUsername(request.username().trim());

        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyExistsException(email);
        }
        String id = saved.getId().toString();
        return new UserResponse(id, saved.getEmail(), saved.getUsername());

    }

}
