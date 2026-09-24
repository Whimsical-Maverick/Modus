package com.tracker.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(@NotBlank @Email String email,
                              @NotBlank
                              @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
                              @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
                                      message = "Password must contain lowercase, uppercase, and a digit")
                              String password,
                              @NotBlank @Size(max = 255) String username) {}
