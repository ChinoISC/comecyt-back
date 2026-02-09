package com.example.proyecto.demo.controller;

import com.example.proyecto.demo.Entity.Documento;
import com.example.proyecto.demo.Entity.Registro1;
import com.example.proyecto.demo.Entity.Usuario;
import com.example.proyecto.demo.Repository.AuthUserRepository;
import com.example.proyecto.demo.Repository.UsuarioRepository;
import com.example.proyecto.demo.Service.AdminService;
import com.example.proyecto.demo.Service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final UsuarioRepository usuarioRepository;
    private final AuthUserRepository authUserRepository;
    private final DocumentoService documentoService;
    private final AdminService adminService;
    private final PasswordEncoder passwordEncoder;

    /**
     * KPIs y estadísticas para el dashboard de administración (incluye datos para gráficas).
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication auth) {
        List<Usuario> todos = usuarioRepository.findAllWithRelations();

        long totalUsuarios = todos.size();
        long investigadores = todos.stream()
                .filter(u -> u.getRegistro1() != null && u.getRegistro1().getTipoPerfil() == Registro1.TipoPerfil.INVESTIGADOR)
                .count();
        long innovadores = todos.stream()
                .filter(u -> u.getRegistro1() != null && u.getRegistro1().getTipoPerfil() == Registro1.TipoPerfil.INNOVADOR)
                .count();
        long cuentasActivas = todos.stream()
                .filter(u -> u.getAuthUser() != null && u.getAuthUser().isEnabled())
                .count();
        long cuentasSuspendidas = todos.stream()
                .filter(u -> u.getAuthUser() != null && !u.getAuthUser().isEnabled())
                .count();

        // Registros por mes (últimos 12 meses)
        YearMonth now = YearMonth.now();
        List<Map<String, Object>> registrosPorMes = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 11; i >= 0; i--) {
            YearMonth ym = now.minusMonths(i);
            String clave = ym.format(fmt);
            long cantidad = todos.stream()
                    .filter(u -> u.getRegistro1() != null && u.getRegistro1().getCreatedAt() != null)
                    .filter(u -> {
                        LocalDate d = u.getRegistro1().getCreatedAt();
                        return YearMonth.from(d).equals(ym);
                    })
                    .count();
            Map<String, Object> punto = new HashMap<>();
            punto.put("mes", clave);
            punto.put("etiqueta", ym.getMonthValue() + "/" + (ym.getYear() % 100));
            punto.put("cantidad", cantidad);
            registrosPorMes.add(punto);
        }

        // Por tipo de perfil (para gráfica circular)
        Map<String, Long> porTipoPerfil = new LinkedHashMap<>();
        porTipoPerfil.put("INVESTIGADOR", investigadores);
        porTipoPerfil.put("INNOVADOR", innovadores);

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsuarios", totalUsuarios);
        stats.put("investigadores", investigadores);
        stats.put("innovadores", innovadores);
        stats.put("cuentasActivas", cuentasActivas);
        stats.put("cuentasSuspendidas", cuentasSuspendidas);
        stats.put("registrosPorMes", registrosPorMes);
        stats.put("porTipoPerfil", porTipoPerfil);
        return ResponseEntity.ok(stats);
    }

    /**
     * Listado de registros (usuarios) para revisión.
     */
    @GetMapping("/registros")
    public ResponseEntity<List<Map<String, Object>>> getRegistros(Authentication auth) {
        List<Usuario> usuarios = usuarioRepository.findAllWithRelations();

        List<Map<String, Object>> list = usuarios.stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("nombre", u.getNombre());
            item.put("apellidoPaterno", u.getApellidoPaterno());
            item.put("apellidoMaterno", u.getApellidoMaterno());
            item.put("email", u.getAuthUser() != null ? u.getAuthUser().getEmail() : null);
            Instant lastLogin = u.getAuthUser() != null ? u.getAuthUser().getLastLoginAt() : null;
            item.put("lastLoginAt", lastLogin != null ? lastLogin.toString() : null);
            if (u.getRegistro1() != null) {
                Registro1 r = u.getRegistro1();
                item.put("curp", r.getCurp());
                item.put("tipoPerfil", r.getTipoPerfil() != null ? r.getTipoPerfil().name() : null);
                item.put("telefono", r.getTelefono());
                item.put("rfc", r.getRfc());
                item.put("fechaNacimiento", r.getFechaNacimiento() != null ? r.getFechaNacimiento().toString() : null);
                item.put("genero", r.getGenero() != null ? r.getGenero().name() : null);
                item.put("nacionalidad", r.getNacionalidad());
                item.put("paisNacimiento", r.getPaisNacimiento());
                item.put("entidadFederativa", r.getEntidadFederativa());
                item.put("estadoCivil", r.getEstadoCivil() != null ? r.getEstadoCivil().name() : null);
            }
            return item;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(list);
    }

    /**
     * Detalle de un usuario por ID (para el modal del panel admin). Incluye fotoDocumentoId para mostrar imagen.
     */
    @GetMapping("/registros/{id}")
    public ResponseEntity<Map<String, Object>> getRegistroDetalle(Authentication auth, @PathVariable Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Map<String, Object> item = new HashMap<>();
        item.put("id", u.getId());
        item.put("nombre", u.getNombre());
        item.put("apellidoPaterno", u.getApellidoPaterno());
        item.put("apellidoMaterno", u.getApellidoMaterno());
        item.put("visibilidadPerfil", u.getVisibilidadPerfil());
        Optional<Documento> fotoOpt = documentoService.obtenerDocumentoPorUsuarioYTipo(u.getId(), Documento.TipoDocumento.FOTO_PERFIL);
        item.put("fotoDocumentoId", fotoOpt.map(Documento::getId).orElse(null));
        if (u.getAuthUser() != null) {
            item.put("email", u.getAuthUser().getEmail());
            item.put("username", u.getAuthUser().getUsername());
            item.put("enabled", u.getAuthUser().isEnabled());
            item.put("locked", u.getAuthUser().isLocked());
            Instant last = u.getAuthUser().getLastLoginAt();
            item.put("lastLoginAt", last != null ? last.toString() : null);
        }
        if (u.getRegistro1() != null) {
            Registro1 r = u.getRegistro1();
            item.put("curp", r.getCurp());
            item.put("rfc", r.getRfc());
            item.put("tipoPerfil", r.getTipoPerfil() != null ? r.getTipoPerfil().name() : null);
            item.put("telefono", r.getTelefono());
            item.put("fechaNacimiento", r.getFechaNacimiento() != null ? r.getFechaNacimiento().toString() : null);
            item.put("genero", r.getGenero() != null ? r.getGenero().name() : null);
            item.put("nacionalidad", r.getNacionalidad());
            item.put("paisNacimiento", r.getPaisNacimiento());
            item.put("entidadFederativa", r.getEntidadFederativa());
            item.put("estadoCivil", r.getEstadoCivil() != null ? r.getEstadoCivil().name() : null);
            item.put("createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : null);
            item.put("updatedAt", r.getUpdatedAt() != null ? r.getUpdatedAt().toString() : null);
        }
        return ResponseEntity.ok(item);
    }

    /**
     * Suspender cuenta: enabled=false, locked=true.
     */
    @PatchMapping("/registros/{id}/suspender")
    public ResponseEntity<Map<String, String>> suspender(@PathVariable Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (u.getAuthUser() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Usuario sin cuenta de acceso"));
        }
        u.getAuthUser().setEnabled(false);
        u.getAuthUser().setLocked(true);
        authUserRepository.save(u.getAuthUser());
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Usuario suspendido"));
    }

    /**
     * Reactivar cuenta: enabled=true, locked=false.
     */
    @PatchMapping("/registros/{id}/reactivar")
    public ResponseEntity<Map<String, String>> reactivar(@PathVariable Long id) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (u.getAuthUser() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Usuario sin cuenta de acceso"));
        }
        u.getAuthUser().setEnabled(true);
        u.getAuthUser().setLocked(false);
        authUserRepository.save(u.getAuthUser());
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Usuario reactivado"));
    }

    /**
     * Restablecer contraseña. Body opcional: { "nuevaPassword": "xxx" }. Si no se envía, se genera una temporal.
     */
    @PostMapping("/registros/{id}/reset-password")
    public ResponseEntity<Map<String, Object>> restablecerPassword(@PathVariable Long id,
                                                                   @RequestBody(required = false) Map<String, String> body) {
        Usuario u = usuarioRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        if (u.getAuthUser() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Usuario sin cuenta de acceso"));
        }
        String nuevaPassword = body != null && body.containsKey("nuevaPassword") && body.get("nuevaPassword") != null && !body.get("nuevaPassword").isBlank()
                ? body.get("nuevaPassword")
                : "Temp" + (int) (Math.random() * 9000 + 1000) + "!";
        u.getAuthUser().setPasswordHash(passwordEncoder.encode(nuevaPassword));
        authUserRepository.save(u.getAuthUser());
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Contraseña actualizada", "nuevaPassword", nuevaPassword));
    }

    /**
     * Eliminar usuario y todos sus datos (cuenta, documentos, trayectoria, etc.).
     */
    @DeleteMapping("/registros/{id}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Long id) {
        adminService.eliminarUsuario(id);
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Usuario eliminado"));
    }
}
