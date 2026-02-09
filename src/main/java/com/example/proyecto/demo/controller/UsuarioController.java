package com.example.proyecto.demo.controller;

import com.example.proyecto.demo.Entity.Articulo;
import com.example.proyecto.demo.Entity.Curso;
import com.example.proyecto.demo.Entity.Documento;
import com.example.proyecto.demo.Entity.Herramienta;
import com.example.proyecto.demo.Entity.Idioma;
import com.example.proyecto.demo.Entity.InteresHabilidad;
import com.example.proyecto.demo.Entity.Logro;
import com.example.proyecto.demo.Entity.Usuario;
import com.example.proyecto.demo.Repository.ArticuloRepository;
import com.example.proyecto.demo.Repository.CursoRepository;
import com.example.proyecto.demo.Repository.HerramientaRepository;
import com.example.proyecto.demo.Repository.IdiomaRepository;
import com.example.proyecto.demo.Repository.InteresHabilidadRepository;
import com.example.proyecto.demo.Repository.LogroRepository;
import com.example.proyecto.demo.Repository.TrayectoriaAcademicaRepository;
import com.example.proyecto.demo.Repository.UsuarioRepository;
import com.example.proyecto.demo.Service.DocumentoService;
import com.example.proyecto.demo.dto.ArticuloItemDTO;
import com.example.proyecto.demo.dto.CursoItemDTO;
import com.example.proyecto.demo.dto.IdiomaItemDTO;
import com.example.proyecto.demo.dto.InvestigadorDTO;
import com.example.proyecto.demo.dto.LogroItemDTO;
import com.example.proyecto.demo.dto.PerfilCompletoDTO;
import com.example.proyecto.demo.dto.UsuarioUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioRepository usuarioRepo;
    private final DocumentoService documentoService;
    private final TrayectoriaAcademicaRepository trayectoriaAcademicaRepository;
    private final InteresHabilidadRepository interesHabilidadRepository;
    private final CursoRepository cursoRepository;
    private final IdiomaRepository idiomaRepository;
    private final LogroRepository logroRepository;
    private final HerramientaRepository herramientaRepository;
    private final ArticuloRepository articuloRepository;

    @GetMapping("/me")
    public PerfilCompletoDTO getMe(Authentication auth) {
        try {
            if (auth == null || auth.getPrincipal() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado");
            }
            Object principal = auth.getPrincipal();
            Long authUserId = principal instanceof Long ? (Long) principal : Long.valueOf(principal.toString());
            Usuario u = usuarioRepo.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

            PerfilCompletoDTO dto = PerfilCompletoDTO.from(u);

            // Obtener IDs de foto y curriculum si existen
            Long fotoId = documentoService.obtenerDocumentoPorUsuarioYTipo(u.getId(), Documento.TipoDocumento.FOTO_PERFIL)
                    .map(Documento::getId)
                    .orElse(null);

            // Buscar curriculum: primero CURRICULUM, si no existe buscar CV (para sincronización)
            Long curriculumId = documentoService.obtenerDocumentoPorUsuarioYTipo(u.getId(), Documento.TipoDocumento.CURRICULUM)
                    .map(Documento::getId)
                    .orElseGet(() -> documentoService.obtenerDocumentoPorUsuarioYTipo(u.getId(), Documento.TipoDocumento.CV)
                            .map(Documento::getId)
                            .orElse(null));

            // Obtener el grado académico desde TrayectoriaAcademica (nivel de escolaridad)
            String gradoAcademico = null;
            try {
                gradoAcademico = trayectoriaAcademicaRepository.findByUsuarioId(u.getId())
                        .stream()
                        .findFirst()
                        .map(t -> t.getNivelNombre())
                        .orElse(null);
            } catch (Exception e) {
                log.warn("No se pudo obtener el grado académico para usuario {}: {}", u.getId(), e.getMessage());
            }

            return new PerfilCompletoDTO(
                    dto.id(),
                    dto.nombre(),
                    dto.apellidoPaterno(),
                    dto.apellidoMaterno(),
                    dto.email(),
                    dto.curp(),
                    dto.rfc(),
                    dto.genero(),
                    dto.fechaNacimiento(),
                    dto.nacionalidad(),
                    dto.paisNacimiento(),
                    dto.entidadFederativa(),
                    dto.estadoCivil(),
                    dto.tipoPerfil(),
                    dto.tienePerfilMigracion(),
                    dto.visibilidadPerfil(),
                    gradoAcademico,
                    fotoId,
                    curriculumId
            );
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error en GET /usuarios/me: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al cargar perfil: " + e.getMessage());
        }
    }

    @PatchMapping("/me")
    @Transactional
    public ResponseEntity<Map<String, String>> updateMe(@RequestBody UsuarioUpdateRequest req, Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario u = usuarioRepo.findByAuthUserIdWithRegistro1(authUserId).orElse(null);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "No se encontró su perfil. Asegúrese de haber completado al menos el registro inicial (crear cuenta)."));
        }

        if (req.nombre() != null) u.setNombre(req.nombre());
        if (req.apellidoPaterno() != null) u.setApellidoPaterno(req.apellidoPaterno());
        if (req.apellidoMaterno() != null) u.setApellidoMaterno(req.apellidoMaterno());
        if (req.visibilidadPerfil() != null && ("MINIMA".equals(req.visibilidadPerfil()) || "ESTANDAR".equals(req.visibilidadPerfil()) || "COMPLETA".equals(req.visibilidadPerfil()))) {
            u.setVisibilidadPerfil(req.visibilidadPerfil());
        }

        usuarioRepo.save(u);
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Perfil actualizado correctamente"));
    }

    @PostMapping(value = "/me/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFoto(
            @RequestPart("foto") MultipartFile foto,
            Authentication auth) {
        try {
            Long authUserId = (Long) auth.getPrincipal();
            Usuario u = usuarioRepo.findByAuthUserIdWithRegistro1(authUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            
            // Validar que sea una imagen
            if (!foto.getContentType().startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "El archivo debe ser una imagen"));
            }
            
            Documento documento = documentoService.guardarDocumentoUsuario(
                    u.getId(), 
                    foto, 
                    Documento.TipoDocumento.FOTO_PERFIL
            );
            
            log.info("Foto guardada para usuario {}: {}", u.getId(), documento.getId());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Foto actualizada correctamente",
                    "documentoId", documento.getId(),
                    "filename", foto.getOriginalFilename()
            ));
        } catch (Exception e) {
            log.error("Error al guardar foto: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @PostMapping(value = "/me/curriculum", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadCurriculum(
            @RequestPart("curriculum") MultipartFile curriculum,
            Authentication auth) {
        try {
            Long authUserId = (Long) auth.getPrincipal();
            Usuario u = usuarioRepo.findByAuthUserIdWithRegistro1(authUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            
            // Validar que sea un PDF
            if (!curriculum.getContentType().equals("application/pdf")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("status", "error", "message", "El archivo debe ser un PDF"));
            }
            
            Documento documento = documentoService.guardarDocumentoUsuario(
                    u.getId(), 
                    curriculum, 
                    Documento.TipoDocumento.CURRICULUM
            );
            
            log.info("Curriculum guardado para usuario {}: {}", u.getId(), documento.getId());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Curriculum actualizado correctamente",
                    "documentoId", documento.getId(),
                    "filename", curriculum.getOriginalFilename()
            ));
        } catch (Exception e) {
            log.error("Error al guardar curriculum: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping("/investigadores")
    @Transactional(readOnly = true)
    public ResponseEntity<List<InvestigadorDTO>> listarInvestigadores() {
        try {
            List<Usuario> usuarios = usuarioRepo.findAllWithRelations();
            
            List<InvestigadorDTO> investigadores = usuarios.stream()
                    .filter(u -> u.getAuthUser() != null) // Solo usuarios con autenticación
                    .map(u -> {
                        // Obtener email
                        String email = u.getAuthUser() != null ? u.getAuthUser().getEmail() : null;
                        
                        // Obtener teléfono desde Registro1
                        String telefono = null;
                        try {
                            if (u.getRegistro1() != null) {
                                telefono = u.getRegistro1().getTelefono();
                            }
                        } catch (Exception e) {
                            log.warn("No se pudo obtener el teléfono para usuario {}: {}", u.getId(), e.getMessage());
                        }
                        
                        // Obtener grado académico
                        String gradoAcademico = null;
                        try {
                            gradoAcademico = trayectoriaAcademicaRepository.findByUsuarioId(u.getId())
                                    .stream()
                                    .findFirst()
                                    .map(t -> t.getNivelNombre())
                                    .orElse(null);
                        } catch (Exception e) {
                            log.warn("No se pudo obtener el grado académico para usuario {}: {}", u.getId(), e.getMessage());
                        }
                        
                        // Obtener semblanza desde InteresHabilidad
                        String semblanza = null;
                        try {
                            Optional<InteresHabilidad> interesHabilidad = interesHabilidadRepository.findByUsuarioId(u.getId());
                            if (interesHabilidad.isPresent()) {
                                InteresHabilidad ih = interesHabilidad.get();
                                // Usar interesDescripcion como semblanza si está disponible
                                if (ih.getInteresDescripcion() != null && !ih.getInteresDescripcion().isBlank()) {
                                    semblanza = ih.getInteresDescripcion();
                                    log.debug("Semblanza obtenida desde InteresHabilidad para usuario {}: {}", u.getId(), semblanza.substring(0, Math.min(50, semblanza.length())));
                                }
                            } else {
                                log.debug("No se encontró InteresHabilidad para usuario {}", u.getId());
                            }
                        } catch (Exception e) {
                            log.warn("No se pudo obtener la semblanza para usuario {}: {}", u.getId(), e.getMessage());
                        }
                        
                        // Obtener foto
                        Long fotoId = null;
                        try {
                            fotoId = documentoService.obtenerDocumentoPorUsuarioYTipo(u.getId(), Documento.TipoDocumento.FOTO_PERFIL)
                                    .map(Documento::getId)
                                    .orElse(null);
                            if (fotoId != null) {
                                log.debug("Foto encontrada para usuario {}: documentoId={}", u.getId(), fotoId);
                            } else {
                                log.debug("No se encontró foto para usuario {}", u.getId());
                            }
                        } catch (Exception e) {
                            log.warn("Error al obtener foto para usuario {}: {}", u.getId(), e.getMessage());
                        }
                        
                        // Obtener CV/Curriculum
                        Long curriculumId = null;
                        try {
                            curriculumId = documentoService.obtenerDocumentoPorUsuarioYTipo(u.getId(), Documento.TipoDocumento.CURRICULUM)
                                    .map(Documento::getId)
                                    .orElseGet(() -> documentoService.obtenerDocumentoPorUsuarioYTipo(u.getId(), Documento.TipoDocumento.CV)
                                            .map(Documento::getId)
                                            .orElse(null));
                            if (curriculumId != null) {
                                log.debug("Curriculum encontrado para usuario {}: documentoId={}", u.getId(), curriculumId);
                            } else {
                                log.debug("No se encontró curriculum para usuario {}", u.getId());
                            }
                        } catch (Exception e) {
                            log.warn("Error al obtener curriculum para usuario {}: {}", u.getId(), e.getMessage());
                        }
                        
                        // Trayectoria: cursos, idiomas, logros, herramientas, artículos (según visibilidad)
                        List<CursoItemDTO> cursos = new ArrayList<>();
                        List<IdiomaItemDTO> idiomas = new ArrayList<>();
                        List<LogroItemDTO> logros = new ArrayList<>();
                        List<String> herramientas = new ArrayList<>();
                        List<ArticuloItemDTO> articulos = new ArrayList<>();
                        try {
                            cursos = cursoRepository.findByUsuarioId(u.getId()).stream()
                                    .map(c -> new CursoItemDTO(c.getNombre(), c.getPrograma(), c.getHorasTotales(), c.getInstitucion()))
                                    .collect(Collectors.toList());
                            idiomas = idiomaRepository.findByUsuarioId(u.getId()).stream()
                                    .map(i -> new IdiomaItemDTO(i.getNombre(), i.getDominioNombre() != null ? i.getDominioNombre() : (i.getConversacion() != null ? i.getConversacion() : "")))
                                    .collect(Collectors.toList());
                            logros = logroRepository.findByUsuarioId(u.getId()).stream()
                                    .map(l -> new LogroItemDTO(l.getTipo(), l.getNombre(), l.getAnio()))
                                    .collect(Collectors.toList());
                            herramientas = herramientaRepository.findByUsuarioId(u.getId()).stream()
                                    .map(Herramienta::getNombre)
                                    .collect(Collectors.toList());
                            articulos = articuloRepository.findByUsuarioId(u.getId()).stream()
                                    .map(a -> new ArticuloItemDTO(a.getTitulo(), a.getNombreRevista(), a.getAnio(), a.getDoi()))
                                    .collect(Collectors.toList());
                        } catch (Exception e) {
                            log.warn("Error al cargar trayectoria para usuario {}: {}", u.getId(), e.getMessage());
                        }
                        
                        // Aplicar visibilidad: qué se muestra en el módulo público
                        String visibilidad = u.getVisibilidadPerfil() != null ? u.getVisibilidadPerfil() : "ESTANDAR";
                        if ("MINIMA".equals(visibilidad)) {
                            email = null;
                            telefono = null;
                            semblanza = null;
                            fotoId = null;
                            curriculumId = null;
                            cursos = new ArrayList<>();
                            idiomas = new ArrayList<>();
                            logros = new ArrayList<>();
                            herramientas = new ArrayList<>();
                            articulos = new ArrayList<>();
                        } else if ("ESTANDAR".equals(visibilidad)) {
                            telefono = null;
                            semblanza = null;
                            curriculumId = null;
                            cursos = new ArrayList<>();
                            logros = new ArrayList<>();
                            articulos = new ArrayList<>();
                            // herramientas e idiomas sí se muestran en Estándar
                        }
                        // COMPLETA: se muestran todos los datos (ya cargados arriba)
                        
                        return new InvestigadorDTO(
                                u.getId(),
                                u.getNombre(),
                                u.getApellidoPaterno(),
                                u.getApellidoMaterno(),
                                email,
                                telefono,
                                gradoAcademico,
                                semblanza,
                                fotoId,
                                curriculumId,
                                cursos,
                                idiomas,
                                logros,
                                herramientas,
                                articulos
                        );
                    })
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(investigadores);
        } catch (Exception e) {
            log.error("Error al listar investigadores: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
