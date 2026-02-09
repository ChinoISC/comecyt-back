package com.example.proyecto.demo.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.proyecto.demo.exception.ApiException;
import com.example.proyecto.demo.util.CurpValidator;

import com.example.proyecto.demo.Entity.AuthUser;
import com.example.proyecto.demo.Entity.Registro1;
import com.example.proyecto.demo.Entity.Usuario;
import com.example.proyecto.demo.Repository.AuthUserRepository;
import com.example.proyecto.demo.Repository.Registro1Repository;
import com.example.proyecto.demo.Repository.UsuarioRepository;
import com.example.proyecto.demo.dto.AuthResponse;
import com.example.proyecto.demo.dto.LoginRequest;
import com.example.proyecto.demo.dto.RegisterRequest;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository authUserRepo;
    private final UsuarioRepository usuarioRepo;
    private final Registro1Repository registro1Repo;
    private final PasswordEncoder encoder;
    private final SecretKey key;

    @Transactional
public AuthResponse register(RegisterRequest req) {

    // 1) Validaciones
    if (authUserRepo.existsByEmail(req.email())) {
        throw new ApiException(HttpStatus.CONFLICT, "Email ya registrado");
    }

    if (registro1Repo.existsByCurp(req.registro().curp())) {
        throw new ApiException(HttpStatus.CONFLICT, "CURP ya registrada");
    }

    // Validar formato de CURP
    if (!CurpValidator.validarFormato(req.registro().curp())) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "Formato de CURP inválido");
    }

    // Validar que los datos del CURP coincidan con los datos del usuario
    String generoStr = req.registro().genero().name();
    if (!CurpValidator.validarCoincidenciaDatos(
            req.registro().curp(),
            req.registro().apellidoPaterno(),
            req.registro().apellidoMaterno(),
            req.registro().nombre(),
            req.registro().fechaNacimiento(),
            generoStr,
            req.registro().entidadFederativa())) {
        // Mensaje específico cuando la entidad federativa no coincide (ej. CURP Oaxaca vs Estado de México)
        String curpEntidad = CurpValidator.obtenerNombreEntidadDesdeCurp(req.registro().curp());
        String codigoCurp = req.registro().curp().trim().toUpperCase().substring(11, 13);
        String codigoSel = CurpValidator.obtenerCodigoEntidad(req.registro().entidadFederativa());
        String mensaje = (codigoSel != null && !codigoCurp.equals(codigoSel) && curpEntidad != null)
            ? String.format("La entidad federativa del CURP (%s) no coincide con la seleccionada (%s). Verifique el estado de nacimiento.",
                curpEntidad, req.registro().entidadFederativa().trim())
            : "Los datos del CURP no coinciden con los datos proporcionados. Verifique nombre, apellidos, fecha de nacimiento, género y entidad federativa.";
        throw new ApiException(HttpStatus.BAD_REQUEST, mensaje);
    }

    // 2) Crear AuthUser
    AuthUser au = AuthUser.builder()
            .email(req.email().trim().toLowerCase())
            .username(req.email().trim().toLowerCase())
            .passwordHash(encoder.encode(req.password()))
            .enabled(true)
            .roles(new HashSet<>(List.of("ROLE_USER")))
            .build();

    // 3) Crear Usuario y setear AuthUser
    Usuario u = Usuario.builder()
            .nombre(req.registro().nombre().trim())
            .apellidoPaterno(req.registro().apellidoPaterno().trim())
            .apellidoMaterno(req.registro().apellidoMaterno().trim())
            .build();

    // 5) Crear Registro1 y asociar con Usuario
    Registro1.TipoPerfil tipoPerfil = req.registro().tipoPerfil() != null
            ? Registro1.TipoPerfil.valueOf(req.registro().tipoPerfil().name())
            : Registro1.TipoPerfil.INVESTIGADOR;
    Registro1 reg = Registro1.builder()
            .curp(req.registro().curp().trim().toUpperCase())
            .rfc(req.registro().rfc().trim().toUpperCase())
            .fechaNacimiento(req.registro().fechaNacimiento())
            .genero(Registro1.Genero.valueOf(req.registro().genero().name()))
            .nacionalidad(req.registro().nacionalidad().trim())
            .paisNacimiento(req.registro().paisNacimiento().trim())
            .entidadFederativa(req.registro().entidadFederativa().trim())
            .estadoCivil(Registro1.EstadoCivil.valueOf(req.registro().estadoCivil().name()))
            .tipoPerfil(tipoPerfil)
            .telefono(req.telefono() != null && !req.telefono().isBlank() ? req.telefono().trim() : null)
            .usuario(u)
            .build();
            

        // 4) Asociar Usuario con AuthUser
        au.setUsuario(u);
        u.setAuthUser(au);

        // 5) Asociar Registro1 con Usuario
        u.setRegistro1(reg);
        reg.setUsuario(u);

        // 6) Guardar AuthUser (Hibernate hará cascade de Usuario y Registro1)
        authUserRepo.save(au);

    // 9) Generar token JWT
    String token = Jwts.builder()
            .subject(String.valueOf(au.getId()))
            .claim("username", au.getUsername())
            .claim("roles", au.getRoles())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 3600_000))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();

    return new AuthResponse(au.getId(), au.getUsername(), token);
}


    public String login(LoginRequest req) {
        AuthUser au = authUserRepo.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (!encoder.matches(req.password(), au.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        au.setLastLoginAt(Instant.now());
        authUserRepo.save(au);

        String token = Jwts.builder()
                .subject(String.valueOf(au.getId()))
                .claim("username", au.getUsername())
                .claim("roles", au.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        return token;
    }

    /**
     * Login exclusivo para administradores. Valida credenciales y que el usuario tenga ROLE_ADMIN.
     */
    public String loginAdmin(LoginRequest req) {
        AuthUser au = authUserRepo.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (!encoder.matches(req.password(), au.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        if (!au.getRoles().contains("ROLE_ADMIN")) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Acceso solo para administradores");
        }
        au.setLastLoginAt(Instant.now());
        authUserRepo.save(au);
        return Jwts.builder()
                .subject(String.valueOf(au.getId()))
                .claim("username", au.getUsername())
                .claim("roles", au.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Transactional
    public void resetPassword(com.example.proyecto.demo.dto.ResetPasswordRequest req) {
        // Validar que tenga al menos 2 identificadores
        if (!req.hasEnoughIdentifiers()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Debe proporcionar al menos 2 identificadores válidos (email, CURP o RFC)");
        }

        // Buscar usuario por los identificadores proporcionados
        Usuario usuario = null;
        int matches = 0;
        ArrayList<Usuario> candidatos = new ArrayList<>();

        // Buscar por email
        if (req.email() != null && !req.email().trim().isEmpty()) {
            Optional<Usuario> usuarioPorEmail = usuarioRepo.findByAuthUser_Email(req.email().trim().toLowerCase());
            if (usuarioPorEmail.isPresent()) {
                candidatos.add(usuarioPorEmail.get());
            }
        }

        // Buscar por CURP
        if (req.curp() != null && !req.curp().trim().isEmpty()) {
            Optional<com.example.proyecto.demo.Entity.Registro1> registroPorCurp = 
                registro1Repo.findByCurp(req.curp().trim().toUpperCase());
            if (registroPorCurp.isPresent()) {
                candidatos.add(registroPorCurp.get().getUsuario());
            }
        }

        // Buscar por RFC
        if (req.rfc() != null && !req.rfc().trim().isEmpty()) {
            Optional<com.example.proyecto.demo.Entity.Registro1> registroPorRfc = 
                registro1Repo.findByRfc(req.rfc().trim().toUpperCase());
            if (registroPorRfc.isPresent()) {
                candidatos.add(registroPorRfc.get().getUsuario());
            }
        }

        // Verificar que todos los candidatos sean el mismo usuario
        if (candidatos.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, 
                "No se encontró un usuario con los identificadores proporcionados");
        }

        // Verificar que todos los candidatos sean el mismo usuario
        Long usuarioId = candidatos.get(0).getId();
        boolean todosIguales = candidatos.stream().allMatch(u -> u.getId().equals(usuarioId));
        
        if (!todosIguales) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Los identificadores proporcionados no corresponden al mismo usuario");
        }

        // Contar cuántos identificadores coincidieron
        matches = candidatos.size();
        if (matches < 2) {
            throw new ApiException(HttpStatus.BAD_REQUEST, 
                "Debe proporcionar al menos 2 identificadores válidos que coincidan con el mismo usuario");
        }

        usuario = candidatos.get(0);

        // Validar que el usuario tenga AuthUser
        if (usuario.getAuthUser() == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "El usuario no tiene una cuenta de autenticación asociada");
        }

        // Actualizar la contraseña
        AuthUser authUser = usuario.getAuthUser();
        authUser.setPasswordHash(encoder.encode(req.newPassword()));
        authUserRepo.save(authUser);
    }
}
