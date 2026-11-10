package pro.dpsn.warehouse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import pro.dpsn.warehouse.config.JwtService;
import pro.dpsn.warehouse.dto.LoginRequest;
import pro.dpsn.warehouse.dto.LoginResponse;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhone(), request.getPin())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getPhone());
        String token = jwtService.generateToken(userDetails);

        return new LoginResponse(token);
    }
}