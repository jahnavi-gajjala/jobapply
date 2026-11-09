package com.jobapply.auth;

import com.jobapply.user.UserAccount;
import com.jobapply.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    record RegisterRequest(@NotBlank @Size(max=100) String fullName,
                           @Email @NotBlank String email,
                           @NotBlank @Size(min=8,max=72) String password) {}
    record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        UserAccount u = new UserAccount();
        u.setFullName(req.fullName().trim()); u.setEmail(email); u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);
        return Map.of("token", jwt.generate(email), "user", Map.of("id",u.getId(),"fullName",u.getFullName(),"email",u.getEmail()));
    }

    @PostMapping("/login")
    public Map<String,Object> login(@Valid @RequestBody LoginRequest req) {
        UserAccount u = users.findByEmail(req.email().trim().toLowerCase())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(req.password(), u.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        return Map.of("token", jwt.generate(u.getEmail()), "user", Map.of("id",u.getId(),"fullName",u.getFullName(),"email",u.getEmail()));
    }
}
