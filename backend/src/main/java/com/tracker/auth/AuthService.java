package com.tracker.auth;

import com.tracker.auth.dto.RegisterRequest;
import com.tracker.user.User;
import com.tracker.user.UserRepository;
import com.tracker.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService
{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest  request)
    {
        if(userRepository.ExistsByEmail(request.email()))
        {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setUsername(request.fullName());

        User saved = userRepository.save(user);
        String id = saved.getId().toString();
        return new UserResponse(id, saved.getEmail(), saved.getUsername());

    }

}

