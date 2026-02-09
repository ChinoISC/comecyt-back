package com.example.proyecto.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.example.proyecto.demo.security.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity // por si usas @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final CorsFilterExplicit corsFilterExplicit;

    @Bean
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
                // CORS antes de JWT (preflight OPTIONS y cabeceras en todas las respuestas)
                .addFilterBefore(corsFilterExplicit, JwtFilter.class)
                // API stateless: desactiva CSRF para permitir POST/PATCH/DELETE sin token CSRF
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))



                // Autorizaciones
                .authorizeHttpRequests(auth -> auth

                        // Preflight de navegadores
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Público
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/registro1").permitAll()
                        .requestMatchers(HttpMethod.POST, "/migracion").authenticated()


                        // Usuarios: exige estar autenticado (primero probamos así para descartar rol)
                        //.requestMatchers("/usuarios/**").authenticated()
                        //.anyRequest().authenticated()
                        .requestMatchers("/documentos/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuarios/investigadores").permitAll()
                        .requestMatchers(HttpMethod.GET, "/usuarios/me").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/usuarios/me").authenticated()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                // Filtro JWT antes del filtro de usuario/clave
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)

                // Diferenciar 401 (no autenticado) de 403 (sin permiso)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
                )

                // CORS: configurado en CorsConfig (comecyt-front.onrender.com + localhost)
                .cors(cors -> {});

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
