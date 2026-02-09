package com.example.proyecto.demo.config;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Asegura que OPTIONS (preflight) reciba 200 con CORS y que todas las respuestas
 * (incluidas 404/5xx) lleven cabeceras CORS cuando el origen está permitido.
 * Así se evita "CORS Missing Allow Origin" cuando el backend devuelve error.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilterExplicit extends OncePerRequestFilter {

    /** Orígenes permitidos: front en Render (cualquiera de los dos nombres) y local. */
    private static final List<String> ALLOWED_ORIGINS = List.of(
            "https://comecyt-front.onrender.com",
            "https://comecyt-portal.onrender.com",
            "http://localhost:4200"
    );

    private static final String ALLOWED_METHODS = "GET, POST, PUT, PATCH, DELETE, OPTIONS";
    /** Authorization debe estar listado explícitamente; con "*" el navegador no lo permite. */
    private static final String ALLOWED_HEADERS = "Authorization, Content-Type, Accept, X-Requested-With, Origin";
    private static final String ALLOW_CREDENTIALS = "true";
    private static final String MAX_AGE = "3600";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", ALLOW_CREDENTIALS);
            response.setHeader("Access-Control-Allow-Methods", ALLOWED_METHODS);
            response.setHeader("Access-Control-Allow-Headers", ALLOWED_HEADERS);
            response.setHeader("Access-Control-Max-Age", MAX_AGE);
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
