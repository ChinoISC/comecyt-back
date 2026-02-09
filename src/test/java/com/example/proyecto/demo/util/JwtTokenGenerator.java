package com.example.proyecto.demo.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

public class JwtTokenGenerator {
    public static void main(String[] args) {
        String base64Secret = "JCNFE4jsK5BTRTkU90gGJqOwB5YoPCTtpZwYGNCuAbQ="; // tu mismo secret
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));

        String jwt = Jwts.builder()
                .subject("123") // authUserId
                .claim("username", "carlos")
                .claim("roles", List.of("ROLE_USER", "ROLE_ADMIN"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000)) // 1 hora
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        System.out.println("JWT: " + jwt);
    }
}
