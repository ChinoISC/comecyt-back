package com.example.proyecto.demo.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.example.proyecto.demo.security.JwtFilter;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity // por si usas @PreAuthorize
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        http
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

                // Opcional: CORS si pegas desde navegador
                .cors(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // Desarrollo: front local. Producción: front en Render (JWT en header Authorization)
        cfg.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://comecyt-front.onrender.com"
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*")); // Content-Type, Authorization, etc.
        cfg.setExposedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));
        cfg.setAllowCredentials(true); // necesario para enviar cookies si se usan; compatible con JWT en header

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
