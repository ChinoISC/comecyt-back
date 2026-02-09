package com.example.proyecto.demo.controller;

import com.example.proyecto.demo.Entity.*;
import com.example.proyecto.demo.Repository.*;
import com.example.proyecto.demo.Service.DocumentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/trayectoria")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TrayectoriaController {

    private final UsuarioRepository usuarioRepository;
    private final CursoRepository cursoRepository;
    private final IdiomaRepository idiomaRepository;
    private final LogroRepository logroRepository;
    private final ArticuloRepository articuloRepository;
    private final DocumentoService documentoService;
    private final InteresHabilidadRepository interesHabilidadRepository;
    private final HerramientaRepository herramientaRepository;
    private final IncidenciaSocialRepository incidenciaSocialRepository;

    /**
     * Obtener todos los cursos del usuario autenticado
     */
    @GetMapping("/cursos")
    public ResponseEntity<List<Map<String, Object>>> getCursos(Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Curso> cursos = cursoRepository.findByUsuarioId(usuario.getId());
        List<Map<String, Object>> cursosDTO = cursos.stream().map(this::mapCursoToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(cursosDTO);
    }

    /**
     * Crear o actualizar un curso
     */
    @PostMapping("/cursos")
    public ResponseEntity<Map<String, Object>> saveCurso(
            Authentication auth,
            @RequestBody Map<String, Object> cursoData) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Curso curso;
        if (cursoData.containsKey("id") && cursoData.get("id") != null) {
            // Actualizar curso existente
            Long cursoId = Long.valueOf(cursoData.get("id").toString());
            curso = cursoRepository.findById(cursoId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
            if (!curso.getUsuario().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
            }
        } else {
            // Crear nuevo curso
            curso = new Curso();
            curso.setUsuario(usuario);
        }

        // Actualizar campos
        if (cursoData.containsKey("nombre")) curso.setNombre(cursoData.get("nombre").toString());
        if (cursoData.containsKey("programa")) curso.setPrograma(cursoData.get("programa").toString());
        if (cursoData.containsKey("horasTotales")) {
            Object horas = cursoData.get("horasTotales");
            if (horas != null) curso.setHorasTotales(Integer.valueOf(horas.toString()));
        }
        if (cursoData.containsKey("fechaInicio")) {
            String fechaStr = cursoData.get("fechaInicio").toString();
            if (!fechaStr.isEmpty()) curso.setFechaInicio(LocalDate.parse(fechaStr));
        }
        if (cursoData.containsKey("fechaFin")) {
            String fechaStr = cursoData.get("fechaFin").toString();
            if (!fechaStr.isEmpty()) curso.setFechaFin(LocalDate.parse(fechaStr));
        }
        if (cursoData.containsKey("institucion")) curso.setInstitucion(cursoData.get("institucion").toString());
        if (cursoData.containsKey("nivelEscolaridad")) curso.setNivelEscolaridad(cursoData.get("nivelEscolaridad").toString());

        curso = cursoRepository.save(curso);
        return ResponseEntity.ok(mapCursoToDTO(curso));
    }

    /**
     * Eliminar un curso
     */
    @DeleteMapping("/cursos/{id}")
    public ResponseEntity<Void> deleteCurso(Authentication auth, @PathVariable Long id) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Curso curso = cursoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Curso no encontrado"));
        if (!curso.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        cursoRepository.delete(curso);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener todos los idiomas del usuario
     */
    @GetMapping("/idiomas")
    public ResponseEntity<List<Map<String, Object>>> getIdiomas(Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Idioma> idiomas = idiomaRepository.findByUsuarioId(usuario.getId());
        List<Map<String, Object>> idiomasDTO = idiomas.stream().map(this::mapIdiomaToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(idiomasDTO);
    }

    /**
     * Crear o actualizar un idioma
     */
    @PostMapping("/idiomas")
    public ResponseEntity<Map<String, Object>> saveIdioma(
            Authentication auth,
            @RequestBody Map<String, Object> idiomaData) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Idioma idioma;
        if (idiomaData.containsKey("id") && idiomaData.get("id") != null) {
            Long idiomaId = Long.valueOf(idiomaData.get("id").toString());
            idioma = idiomaRepository.findById(idiomaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idioma no encontrado"));
            if (!idioma.getUsuario().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
            }
        } else {
            idioma = new Idioma();
            idioma.setUsuario(usuario);
        }

        if (idiomaData.containsKey("nombre")) idioma.setNombre(idiomaData.get("nombre").toString());
        if (idiomaData.containsKey("dominioNombre")) idioma.setDominioNombre(idiomaData.get("dominioNombre").toString());
        if (idiomaData.containsKey("conversacion")) idioma.setConversacion(idiomaData.get("conversacion").toString());
        if (idiomaData.containsKey("lectura")) idioma.setLectura(idiomaData.get("lectura").toString());
        if (idiomaData.containsKey("escritura")) idioma.setEscritura(idiomaData.get("escritura").toString());
        if (idiomaData.containsKey("esCertificado")) {
            idioma.setEsCertificado(Boolean.valueOf(idiomaData.get("esCertificado").toString()));
        }
        if (idiomaData.containsKey("certInstitucion")) idioma.setCertInstitucion(idiomaData.get("certInstitucion").toString());
        if (idiomaData.containsKey("certPuntuacion")) idioma.setCertPuntuacion(idiomaData.get("certPuntuacion").toString());
        if (idiomaData.containsKey("vigenciaFin")) {
            String fechaStr = idiomaData.get("vigenciaFin").toString();
            if (!fechaStr.isEmpty()) idioma.setVigenciaFin(LocalDate.parse(fechaStr));
        }

        idioma = idiomaRepository.save(idioma);
        return ResponseEntity.ok(mapIdiomaToDTO(idioma));
    }

    /**
     * Eliminar un idioma
     */
    @DeleteMapping("/idiomas/{id}")
    public ResponseEntity<Void> deleteIdioma(Authentication auth, @PathVariable Long id) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Idioma idioma = idiomaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Idioma no encontrado"));
        if (!idioma.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        idiomaRepository.delete(idioma);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener todos los logros del usuario
     */
    @GetMapping("/logros")
    public ResponseEntity<List<Map<String, Object>>> getLogros(Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Logro> logros = logroRepository.findByUsuarioId(usuario.getId());
        List<Map<String, Object>> logrosDTO = logros.stream().map(this::mapLogroToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(logrosDTO);
    }

    /**
     * Crear o actualizar un logro
     */
    @PostMapping("/logros")
    public ResponseEntity<Map<String, Object>> saveLogro(
            Authentication auth,
            @RequestBody Map<String, Object> logroData) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Logro logro;
        if (logroData.containsKey("id") && logroData.get("id") != null) {
            Long logroId = Long.valueOf(logroData.get("id").toString());
            logro = logroRepository.findById(logroId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logro no encontrado"));
            if (!logro.getUsuario().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
            }
        } else {
            logro = new Logro();
            logro.setUsuario(usuario);
        }

        if (logroData.containsKey("tipo")) logro.setTipo(logroData.get("tipo").toString());
        if (logroData.containsKey("nombre")) logro.setNombre(logroData.get("nombre").toString());
        if (logroData.containsKey("anio")) {
            Object anio = logroData.get("anio");
            if (anio != null) logro.setAnio(Integer.valueOf(anio.toString()));
        }

        logro = logroRepository.save(logro);
        return ResponseEntity.ok(mapLogroToDTO(logro));
    }

    /**
     * Eliminar un logro
     */
    @DeleteMapping("/logros/{id}")
    public ResponseEntity<Void> deleteLogro(Authentication auth, @PathVariable Long id) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Logro logro = logroRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Logro no encontrado"));
        if (!logro.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        logroRepository.delete(logro);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener certificaciones (documentos tipo CERTIFICADO y OTRO con nombre que empiece con CERTIFICADO_)
     */
    @GetMapping("/certificaciones")
    public ResponseEntity<List<Map<String, Object>>> getCertificaciones(Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Obtener todos los documentos de tipo CERTIFICADO y OTRO que sean certificaciones
        List<Documento> todosDocumentos = documentoService.obtenerDocumentosPorUsuario(usuario.getId());
        
        log.info("Total documentos del usuario {}: {}", usuario.getId(), todosDocumentos.size());
        
        List<Documento> certificados = todosDocumentos.stream()
                .filter(doc -> {
                    if (doc.getNombreArchivo() == null) {
                        log.debug("Documento {} sin nombre, descartado", doc.getId());
                        return false;
                    }
                    String nombre = doc.getNombreArchivo().toUpperCase();
                    // Incluir todos los tipos CERTIFICADO (CERTIFICADO_1, CERTIFICADO_2)
                    // y OTRO que tenga nombre que empiece con CERTIFICADO_, OTRO_CERTIFICADO_, o CERTIFICACIONES_
                    // También incluir cualquier documento cuyo nombre contenga CERTIFICADO o CERTIFICACIONES (más flexible)
                    boolean esCertificado = doc.getTipo() == Documento.TipoDocumento.CERTIFICADO_1 
                            || doc.getTipo() == Documento.TipoDocumento.CERTIFICADO_2
                            || (doc.getTipo() == Documento.TipoDocumento.OTRO 
                                && (nombre.startsWith("CERTIFICADO_") 
                                    || nombre.startsWith("OTRO_CERTIFICADO_")
                                    || nombre.startsWith("CERTIFICACIONES_")
                                    || nombre.contains("CERTIFICADO")
                                    || nombre.contains("CERTIFICACIONES")));
                    
                    if (esCertificado) {
                        log.debug("Certificado encontrado - ID: {}, Tipo: {}, Nombre: {}, ContentType: {}", 
                                doc.getId(), doc.getTipo(), doc.getNombreArchivo(), doc.getContentType());
                    } else {
                        log.debug("Documento descartado - ID: {}, Tipo: {}, Nombre: {}", 
                                doc.getId(), doc.getTipo(), doc.getNombreArchivo());
                    }
                    return esCertificado;
                })
                .sorted((a, b) -> {
                    // Ordenar por fecha de subida (más recientes primero)
                    if (a.getFechaSubida() != null && b.getFechaSubida() != null) {
                        return b.getFechaSubida().compareTo(a.getFechaSubida());
                    }
                    return 0;
                })
                .collect(Collectors.toList());
        
        log.info("Certificados encontrados para usuario {}: {}", usuario.getId(), certificados.size());
        certificados.forEach(cert -> {
            log.info("  - Certificado ID: {}, Tipo: {}, Nombre: {}, ContentType: {}", 
                    cert.getId(), cert.getTipo(), cert.getNombreArchivo(), cert.getContentType());
        });
        
        List<Map<String, Object>> certsDTO = certificados.stream().map(doc -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", doc.getId());
            // Limpiar el nombre para mostrar (remover prefijos CERTIFICACIONES_X_, OTRO_CERTIFICADO_, CERTIFICADO_X_)
            String nombreMostrar = doc.getNombreArchivo();
            String nombreUpper = nombreMostrar.toUpperCase();
            
            // Remover prefijo CERTIFICACIONES_X_ (nuevo formato)
            if (nombreUpper.startsWith("CERTIFICACIONES_")) {
                // Encontrar el número y el siguiente guion bajo
                int indice = nombreMostrar.indexOf('_', "CERTIFICACIONES_".length());
                if (indice > 0) {
                    int siguienteIndice = nombreMostrar.indexOf('_', indice + 1);
                    if (siguienteIndice > 0) {
                        nombreMostrar = nombreMostrar.substring(siguienteIndice + 1);
                    } else {
                        nombreMostrar = nombreMostrar.substring(indice + 1);
                    }
                }
            }
            // Remover prefijo OTRO_CERTIFICADO_
            else if (nombreUpper.startsWith("OTRO_CERTIFICADO_")) {
                nombreMostrar = nombreMostrar.substring("OTRO_CERTIFICADO_".length());
            } 
            // Remover prefijo CERTIFICADO_X_ (donde X puede ser 1, 2, 3, etc.)
            else if (nombreUpper.matches("^CERTIFICADO_\\d+_.*")) {
                // Encontrar el primer guion bajo después de CERTIFICADO_ y el número
                int indice = nombreMostrar.indexOf('_', "CERTIFICADO_".length());
                if (indice > 0) {
                    int siguienteIndice = nombreMostrar.indexOf('_', indice + 1);
                    if (siguienteIndice > 0) {
                        nombreMostrar = nombreMostrar.substring(siguienteIndice + 1);
                    } else {
                        // Si no hay más guiones bajos, solo remover hasta el primer número
                        nombreMostrar = nombreMostrar.substring(indice + 1);
                    }
                }
            }
            // Remover prefijo CERTIFICADO_ simple (sin número)
            else if (nombreUpper.startsWith("CERTIFICADO_")) {
                nombreMostrar = nombreMostrar.substring("CERTIFICADO_".length());
            }
            
            dto.put("nombre", nombreMostrar);
            dto.put("nombreCompleto", doc.getNombreArchivo());
            dto.put("tipo", doc.getTipo().name());
            dto.put("contentType", doc.getContentType());
            dto.put("fechaSubida", doc.getFechaSubida());
            dto.put("sizeBytes", doc.getSizeBytes());
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(certsDTO);
    }

    /**
     * Subir certificación (permite múltiples certificaciones)
     */
    @PostMapping("/certificaciones")
    public ResponseEntity<Map<String, Object>> uploadCertificacion(
            Authentication auth,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "nombre", required = false) String nombre) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        try {
            // Preservar la extensión del archivo original
            String nombreOriginal = file.getOriginalFilename();
            String extension = "";
            if (nombreOriginal != null && nombreOriginal.contains(".")) {
                extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
            } else {
                // Determinar extensión por contentType si no está en el nombre
                String contentType = file.getContentType();
                if (contentType != null) {
                    if (contentType.contains("pdf")) extension = ".pdf";
                    else if (contentType.contains("jpeg") || contentType.contains("jpg")) extension = ".jpg";
                    else if (contentType.contains("png")) extension = ".png";
                    else if (contentType.contains("doc")) extension = ".doc";
                    else if (contentType.contains("docx")) extension = ".docx";
                }
            }
            
            // Obtener todas las certificaciones existentes para determinar el siguiente número
            List<Documento> certsExistentes = documentoService.obtenerDocumentosPorUsuario(usuario.getId()).stream()
                    .filter(doc -> {
                        if (doc.getNombreArchivo() == null) return false;
                        String nombreArchivo = doc.getNombreArchivo().toUpperCase();
                        return doc.getTipo() == Documento.TipoDocumento.CERTIFICADO_1 
                                || doc.getTipo() == Documento.TipoDocumento.CERTIFICADO_2
                                || (doc.getTipo() == Documento.TipoDocumento.OTRO 
                                    && (nombreArchivo.startsWith("CERTIFICADO_") 
                                        || nombreArchivo.startsWith("OTRO_CERTIFICADO_")
                                        || nombreArchivo.startsWith("CERTIFICACIONES_")
                                        || nombreArchivo.contains("CERTIFICADO")));
                    })
                    .collect(Collectors.toList());
            
            // Determinar el siguiente número de certificación
            int siguienteNumero = certsExistentes.size() + 1;
            
            // Construir nombre del archivo con prefijo CERTIFICACIONES_ y número
            String nombreBase;
            if (nombre != null && !nombre.isEmpty()) {
                nombreBase = nombre;
                // Si el nombre personalizado no tiene extensión, agregarla
                if (!nombreBase.contains(".") && !extension.isEmpty()) {
                    nombreBase = nombreBase + extension;
                }
            } else {
                // Usar el nombre original completo (ya incluye extensión)
                nombreBase = nombreOriginal != null ? nombreOriginal : "certificado_" + System.currentTimeMillis() + extension;
            }
            
            // Asegurar que siempre tenga extensión
            if (!nombreBase.contains(".") && !extension.isEmpty()) {
                nombreBase = nombreBase + extension;
            }
            
            String nombreArchivo = "CERTIFICACIONES_" + siguienteNumero + "_" + nombreBase;
            
            log.info("Guardando certificación - Nombre original: {}, Nombre base: {}, Extensión: {}, Número: {}, Nombre final: {}, ContentType: {}", 
                    nombreOriginal, nombreBase, extension, siguienteNumero, nombreArchivo, file.getContentType());

            Documento documento = documentoService.guardarDocumento(
                    usuario.getId(),
                    file,
                    Documento.TipoDocumento.OTRO,
                    nombreArchivo,
                    false // NO eliminar anteriores, permitir múltiples
            );

            Map<String, Object> response = new HashMap<>();
            response.put("id", documento.getId());
            // Limpiar el nombre para mostrar (remover prefijos)
            String nombreMostrar = documento.getNombreArchivo();
            String nombreUpper = nombreMostrar.toUpperCase();
            
            // Remover prefijo CERTIFICACIONES_X_ (nuevo formato)
            if (nombreUpper.startsWith("CERTIFICACIONES_")) {
                int indice = nombreMostrar.indexOf('_', "CERTIFICACIONES_".length());
                if (indice > 0) {
                    int siguienteIndice = nombreMostrar.indexOf('_', indice + 1);
                    if (siguienteIndice > 0) {
                        nombreMostrar = nombreMostrar.substring(siguienteIndice + 1);
                    } else {
                        nombreMostrar = nombreMostrar.substring(indice + 1);
                    }
                }
            }
            // Remover prefijo OTRO_CERTIFICADO_
            else if (nombreUpper.startsWith("OTRO_CERTIFICADO_")) {
                nombreMostrar = nombreMostrar.substring("OTRO_CERTIFICADO_".length());
            }
            response.put("nombre", nombreMostrar);
            response.put("nombreCompleto", documento.getNombreArchivo());
            response.put("tipo", documento.getTipo().name());
            response.put("contentType", documento.getContentType());
            response.put("fechaSubida", documento.getFechaSubida());
            response.put("sizeBytes", documento.getSizeBytes());
            
            log.info("Certificación guardada exitosamente - ID: {}, Nombre: {}, ContentType: {}", 
                    documento.getId(), documento.getNombreArchivo(), documento.getContentType());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error al subir certificación", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al subir certificación: " + e.getMessage());
        }
    }

    /**
     * Eliminar certificación
     */
    @DeleteMapping("/certificaciones/{id}")
    public ResponseEntity<Void> deleteCertificacion(Authentication auth, @PathVariable Long id) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        documentoService.eliminarDocumento(id, usuario.getId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener herramientas del usuario
     */
    @GetMapping("/herramientas")
    public ResponseEntity<List<Map<String, Object>>> getHerramientas(Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Herramienta> herramientas = herramientaRepository.findByUsuarioId(usuario.getId());
        List<Map<String, Object>> herramientasDTO = herramientas.stream().map(h -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", h.getId());
            dto.put("nombre", h.getNombre());
            return dto;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(herramientasDTO);
    }

    /**
     * Guardar herramientas (reemplaza todas las existentes)
     */
    @PostMapping("/herramientas")
    public ResponseEntity<List<Map<String, Object>>> saveHerramientas(
            Authentication auth,
            @RequestBody List<String> herramientasNombres) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        // Validar máximo 12 herramientas
        if (herramientasNombres.size() > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Máximo 12 herramientas permitidas");
        }

        // Eliminar herramientas existentes
        List<Herramienta> herramientasExistentes = herramientaRepository.findByUsuarioId(usuario.getId());
        herramientaRepository.deleteAll(herramientasExistentes);

        // Guardar nuevas herramientas
        List<Herramienta> nuevasHerramientas = herramientasNombres.stream()
                .filter(nombre -> nombre != null && !nombre.trim().isEmpty())
                .map(nombre -> Herramienta.builder()
                        .usuario(usuario)
                        .nombre(nombre.trim())
                        .build())
                .collect(Collectors.toList());

        herramientaRepository.saveAll(nuevasHerramientas);

        // Retornar herramientas guardadas
        List<Map<String, Object>> herramientasDTO = nuevasHerramientas.stream().map(h -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", h.getId());
            dto.put("nombre", h.getNombre());
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(herramientasDTO);
    }

    /**
     * Agregar una herramienta individual
     */
    @PostMapping("/herramientas/agregar")
    public ResponseEntity<Map<String, Object>> agregarHerramienta(
            Authentication auth,
            @RequestBody Map<String, String> request) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String nombre = request.get("nombre");
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre de la herramienta es requerido");
        }

        // Verificar máximo 12 herramientas
        List<Herramienta> herramientasExistentes = herramientaRepository.findByUsuarioId(usuario.getId());
        if (herramientasExistentes.size() >= 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Máximo 12 herramientas permitidas");
        }

        // Verificar si ya existe
        herramientaRepository.findByUsuarioIdAndNombre(usuario.getId(), nombre.trim())
                .ifPresent(h -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta herramienta ya está registrada");
                });

        Herramienta herramienta = Herramienta.builder()
                .usuario(usuario)
                .nombre(nombre.trim())
                .build();

        herramienta = herramientaRepository.save(herramienta);

        Map<String, Object> dto = new HashMap<>();
        dto.put("id", herramienta.getId());
        dto.put("nombre", herramienta.getNombre());
        return ResponseEntity.ok(dto);
    }

    /**
     * Eliminar una herramienta
     */
    @DeleteMapping("/herramientas/{id}")
    public ResponseEntity<Void> eliminarHerramienta(Authentication auth, @PathVariable Long id) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Herramienta herramienta = herramientaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Herramienta no encontrada"));
        if (!herramienta.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        herramientaRepository.delete(herramienta);
        return ResponseEntity.noContent().build();
    }

    // ==================== INCIDENCIA SOCIAL ====================

    @GetMapping("/incidencia-social")
    public ResponseEntity<List<Map<String, Object>>> getIncidenciaSocial(Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<IncidenciaSocial> list = incidenciaSocialRepository.findByUsuarioId(usuario.getId());
        List<Map<String, Object>> dtoList = list.stream().map(this::mapIncidenciaSocialToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    @PostMapping("/incidencia-social")
    public ResponseEntity<Map<String, Object>> saveIncidenciaSocial(
            Authentication auth,
            @RequestBody Map<String, Object> data) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        String titulo = data.get("titulo") != null ? data.get("titulo").toString().trim() : null;
        if (titulo == null || titulo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es requerido");
        }

        IncidenciaSocial inc;
        if (data.containsKey("id") && data.get("id") != null) {
            Long id = Long.valueOf(data.get("id").toString());
            inc = incidenciaSocialRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia social no encontrada"));
            if (!inc.getUsuario().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
            }
        } else {
            inc = new IncidenciaSocial();
            inc.setUsuario(usuario);
        }

        inc.setTitulo(titulo);
        inc.setUbicacion(data.get("ubicacion") != null ? data.get("ubicacion").toString().trim() : null);
        inc.setDescripcion(data.get("descripcion") != null ? data.get("descripcion").toString() : null);
        if (data.get("fecha") != null && !data.get("fecha").toString().isEmpty()) {
            try {
                inc.setFecha(LocalDate.parse(data.get("fecha").toString()));
            } catch (Exception ignored) {}
        } else {
            inc.setFecha(null);
        }
        if (data.get("anio") != null && !data.get("anio").toString().isEmpty()) {
            try {
                inc.setAnio(Integer.valueOf(data.get("anio").toString()));
            } catch (Exception ignored) {}
        } else {
            inc.setAnio(null);
        }

        inc = incidenciaSocialRepository.save(inc);
        return ResponseEntity.ok(mapIncidenciaSocialToDTO(inc));
    }

    @DeleteMapping("/incidencia-social/{id}")
    public ResponseEntity<Void> deleteIncidenciaSocial(Authentication auth, @PathVariable Long id) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        IncidenciaSocial inc = incidenciaSocialRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidencia social no encontrada"));
        if (!inc.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }
        incidenciaSocialRepository.delete(inc);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> mapIncidenciaSocialToDTO(IncidenciaSocial inc) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", inc.getId());
        dto.put("titulo", inc.getTitulo());
        dto.put("ubicacion", inc.getUbicacion());
        dto.put("descripcion", inc.getDescripcion());
        dto.put("fecha", inc.getFecha() != null ? inc.getFecha().toString() : null);
        dto.put("anio", inc.getAnio());
        return dto;
    }

    // Métodos auxiliares para mapear entidades a DTOs
    private Map<String, Object> mapCursoToDTO(Curso curso) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", curso.getId());
        dto.put("nombre", curso.getNombre());
        dto.put("programa", curso.getPrograma());
        dto.put("horasTotales", curso.getHorasTotales());
        dto.put("fechaInicio", curso.getFechaInicio());
        dto.put("fechaFin", curso.getFechaFin());
        dto.put("institucion", curso.getInstitucion());
        dto.put("nivelEscolaridad", curso.getNivelEscolaridad());
        return dto;
    }

    private Map<String, Object> mapIdiomaToDTO(Idioma idioma) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", idioma.getId());
        dto.put("nombre", idioma.getNombre());
        dto.put("dominioNombre", idioma.getDominioNombre());
        dto.put("conversacion", idioma.getConversacion());
        dto.put("lectura", idioma.getLectura());
        dto.put("escritura", idioma.getEscritura());
        dto.put("esCertificado", idioma.getEsCertificado());
        dto.put("certInstitucion", idioma.getCertInstitucion());
        dto.put("certPuntuacion", idioma.getCertPuntuacion());
        dto.put("vigenciaFin", idioma.getVigenciaFin());
        return dto;
    }

    private Map<String, Object> mapLogroToDTO(Logro logro) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", logro.getId());
        dto.put("tipo", logro.getTipo());
        dto.put("nombre", logro.getNombre());
        dto.put("anio", logro.getAnio());
        return dto;
    }

    /**
     * Obtener todos los artículos del usuario autenticado
     */
    @GetMapping("/articulos")
    public ResponseEntity<List<Map<String, Object>>> getArticulos(Authentication auth) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        List<Articulo> articulos = articuloRepository.findByUsuarioId(usuario.getId());
        List<Map<String, Object>> articulosDTO = articulos.stream().map(this::mapArticuloToDTO).collect(Collectors.toList());
        return ResponseEntity.ok(articulosDTO);
    }

    /**
     * Crear o actualizar un artículo
     */
    @PostMapping("/articulos")
    public ResponseEntity<Map<String, Object>> saveArticulo(
            Authentication auth,
            @RequestBody Map<String, Object> articuloData) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Articulo articulo;
        if (articuloData.containsKey("id") && articuloData.get("id") != null) {
            // Actualizar artículo existente
            Long articuloId = Long.valueOf(articuloData.get("id").toString());
            articulo = articuloRepository.findById(articuloId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artículo no encontrado"));
            if (!articulo.getUsuario().getId().equals(usuario.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
            }
        } else {
            // Crear nuevo artículo
            articulo = new Articulo();
            articulo.setUsuario(usuario);
        }

        // Actualizar campos
        if (articuloData.containsKey("titulo")) articulo.setTitulo(articuloData.get("titulo").toString());
        if (articuloData.containsKey("revista")) articulo.setNombreRevista(articuloData.get("revista").toString());
        if (articuloData.containsKey("anio")) {
            Object anio = articuloData.get("anio");
            if (anio != null) articulo.setAnio(Integer.valueOf(anio.toString()));
        }
        if (articuloData.containsKey("doi")) articulo.setDoi(articuloData.get("doi").toString());
        if (articuloData.containsKey("url")) {
            // Si viene URL, podríamos guardarlo en algún campo adicional o ignorarlo
            // Por ahora lo ignoramos ya que la entidad no tiene campo URL
        }

        articulo = articuloRepository.save(articulo);
        return ResponseEntity.ok(mapArticuloToDTO(articulo));
    }

    /**
     * Eliminar un artículo
     */
    @DeleteMapping("/articulos/{id}")
    public ResponseEntity<Void> deleteArticulo(Authentication auth, @PathVariable Long id) {
        Long authUserId = (Long) auth.getPrincipal();
        Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Articulo articulo = articuloRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Artículo no encontrado"));
        if (!articulo.getUsuario().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No autorizado");
        }

        articuloRepository.delete(articulo);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> mapArticuloToDTO(Articulo articulo) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", articulo.getId());
        dto.put("titulo", articulo.getTitulo());
        dto.put("revista", articulo.getNombreRevista());
        dto.put("anio", articulo.getAnio());
        dto.put("doi", articulo.getDoi());
        // Construir URL si hay DOI
        if (articulo.getDoi() != null && !articulo.getDoi().isEmpty()) {
            dto.put("url", "https://doi.org/" + articulo.getDoi());
        }
        return dto;
    }
}
