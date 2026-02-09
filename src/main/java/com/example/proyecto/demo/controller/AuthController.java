package com.example.proyecto.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.proyecto.demo.Service.AuthService;
import com.example.proyecto.demo.Service.JwtService;
import com.example.proyecto.demo.dto.AuthResponse;
import com.example.proyecto.demo.dto.JwtResponse;
import com.example.proyecto.demo.dto.LoginRequest;
import com.example.proyecto.demo.dto.RegisterRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // Permite la conexión desde tu puerto de Angular
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    // --- Endpoints de Autenticación ---

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public JwtResponse login(@Valid @RequestBody LoginRequest req) {
        return new JwtResponse(authService.login(req));
    }

    @PostMapping("/login-admin")
    public JwtResponse loginAdmin(@Valid @RequestBody LoginRequest request) {
        String token = authService.loginAdmin(request);
        return new JwtResponse(token);
    }

    @PostMapping("/login-user")
    public String loginUser() {
        return jwtService.generateToken(2L, "usuario", List.of("ROLE_USER"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody com.example.proyecto.demo.dto.ResetPasswordRequest request) {
        try {
            authService.resetPassword(request);
            return ResponseEntity.ok().body(java.util.Map.of(
                "success", true,
                "message", "Contraseña actualizada correctamente"
            ));
        } catch (org.springframework.web.server.ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of(
                "success", false,
                "message", e.getReason()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of(
                "success", false,
                "message", "Error al actualizar la contraseña: " + e.getMessage()
            ));
        }
    }
    
}