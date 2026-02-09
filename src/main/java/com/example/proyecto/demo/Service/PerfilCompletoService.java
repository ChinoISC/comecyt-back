package com.example.proyecto.demo.Service;

import com.example.proyecto.demo.Entity.*;
import com.example.proyecto.demo.Repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerfilCompletoService {

    private final PerfilMigracionRepository perfilMigracionRepository;
    private final UsuarioRepository usuarioRepository;
    private final Registro1Repository registro1Repository;
    private final AreaConocimientoRepository areaConocimientoRepository;
    private final InstitucionRepository institucionRepository;
    private final TrayectoriaAcademicaRepository trayectoriaAcademicaRepository;
    private final IdiomaRepository idiomaRepository;
    private final TrayectoriaProfesionalRepository trayectoriaProfesionalRepository;
    private final EstanciaRepository estanciaRepository;
    private final CursoRepository cursoRepository;
    private final CongresoRepository congresoRepository;
    private final DivulgacionRepository divulgacionRepository;
    private final ArticuloRepository articuloRepository;
    private final LogroRepository logroRepository;
    private final InteresHabilidadRepository interesHabilidadRepository;

    @Transactional(rollbackFor = {IllegalStateException.class}, noRollbackFor = {IllegalArgumentException.class, NullPointerException.class})
    public PerfilMigracion guardarPerfilCompleto(Map<String, Object> datos) {
        PerfilMigracion perfilMigracion = null; // Declarar fuera del try para acceso en catch
        try {
            String migracionId = convertirAString(datos.get("migracionId"));
            
            // Si no se proporciona migracionId, generar uno automáticamente
            if (migracionId == null || migracionId.isBlank()) {
                // Obtener usuarioId para generar un ID único
                Long usuarioId = null;
                Object usuarioIdObj = datos.get("usuarioId");
                if (usuarioIdObj instanceof Long) {
                    usuarioId = (Long) usuarioIdObj;
                } else if (usuarioIdObj instanceof Integer) {
                    usuarioId = ((Integer) usuarioIdObj).longValue();
                } else if (usuarioIdObj instanceof String) {
                    try {
                        usuarioId = Long.parseLong((String) usuarioIdObj);
                    } catch (NumberFormatException e) {
                        log.warn(">>> No se pudo convertir usuarioId a Long: {}", usuarioIdObj);
                    }
                }
                
                // Generar migracionId automáticamente
                if (usuarioId != null) {
                    migracionId = "MIG_" + usuarioId + "_" + UUID.randomUUID().toString().substring(0, 8);
                } else {
                    migracionId = "MIG_" + UUID.randomUUID().toString();
                }
                log.info(">>> migracionId generado automáticamente: {}", migracionId);
            }
            
            log.info(">>> Iniciando guardado de perfil para migracionId: {}", migracionId);
            
            // 1. Buscar o crear PerfilMigracion
            perfilMigracion = perfilMigracionRepository.findByMigracionId(migracionId)
                    .orElse(PerfilMigracion.builder()
                            .migracionId(migracionId)
                            .build());
            
            log.info(">>> PerfilMigracion {} encontrado/creado", perfilMigracion.getId() != null ? perfilMigracion.getId() : "nuevo");
            
            // Actualizar campos del perfil de migración (tanto si es nuevo como existente)
            if (datos.get("perfilCvu") != null) {
                perfilMigracion.setCvu(convertirAString(datos.get("perfilCvu")));
            }
            if (datos.get("perfilLogin") != null) {
                perfilMigracion.setLogin(convertirAString(datos.get("perfilLogin")));
            }
            if (datos.get("perfilCorreoAlterno") != null) {
                perfilMigracion.setCorreoAlterno(convertirAString(datos.get("perfilCorreoAlterno")));
            }
            if (datos.get("perfilNivelAcademico") != null) {
                perfilMigracion.setNivelAcademico(convertirAString(datos.get("perfilNivelAcademico")));
            }
            if (datos.get("perfilTituloTratamiento") != null) {
                perfilMigracion.setTituloTratamiento(convertirAString(datos.get("perfilTituloTratamiento")));
            }
            if (datos.get("perfilFiltro") != null) {
                perfilMigracion.setFiltro(convertirAString(datos.get("perfilFiltro")));
            }
            if (datos.get("perfilInstitucionReceptora") != null) {
                perfilMigracion.setInstitucionReceptora(convertirAString(datos.get("perfilInstitucionReceptora")));
            }
            
            // Vincular con Usuario si se proporciona usuarioId y obtener usuarioId para actualizar Registro1
            Long usuarioIdFinal = null;
            Usuario usuarioFinal = null;
            if (datos.get("usuarioId") != null) {
                Object usuarioIdObj = datos.get("usuarioId");
                Long usuarioId = null;
                if (usuarioIdObj instanceof Long) {
                    usuarioId = (Long) usuarioIdObj;
                } else if (usuarioIdObj instanceof Integer) {
                    usuarioId = ((Integer) usuarioIdObj).longValue();
                } else if (usuarioIdObj instanceof String) {
                    try {
                        usuarioId = Long.parseLong((String) usuarioIdObj);
                    } catch (NumberFormatException e) {
                        log.warn(">>> Error al convertir usuarioId a Long: {}", usuarioIdObj);
                    }
                }
                
                usuarioIdFinal = usuarioId;
                final Long finalUsuarioId = usuarioId;
                if (finalUsuarioId != null) {
                    try {
                        usuarioFinal = usuarioRepository.findById(finalUsuarioId).orElse(null);
                        if (usuarioFinal != null) {
                            perfilMigracion.setUsuario(usuarioFinal);
                            log.info(">>> PerfilMigracion vinculado con Usuario: {}", finalUsuarioId);
                        } else {
                            log.warn(">>> Usuario no encontrado con ID: {}. Continuando sin usuario vinculado.", finalUsuarioId);
                        }
                    } catch (Exception e) {
                        log.error(">>> Error al buscar usuario con ID {}: {}", finalUsuarioId, e.getMessage());
                        // No lanzar excepción para evitar marcar la transacción para rollback
                        log.warn(">>> Continuando sin usuario debido a error en búsqueda");
                    }
                } else {
                    log.warn(">>> usuarioId es null después de la conversión");
                }
            } else {
                log.warn(">>> usuarioId no está presente en los datos");
            }
            
            // Si no hay usuario, intentar obtenerlo del PerfilMigracion existente o crear uno temporal
            if (usuarioFinal == null) {
                log.error(">>> No se pudo obtener el Usuario. Se requiere usuarioId en los datos.");
                // En lugar de lanzar excepción, intentar continuar sin usuario
                // El PerfilMigracion puede guardarse sin usuario inicialmente
                log.warn(">>> Continuando sin usuario vinculado. Se requerirá usuarioId válido.");
                // No lanzar excepción para evitar rollback
            }
            
            perfilMigracion = perfilMigracionRepository.save(perfilMigracion);
            log.info(">>> PerfilMigracion guardado exitosamente con ID: {}", perfilMigracion.getId());
            
            // Verificar que el usuario sigue disponible después de guardar (por si acaso)
            if (perfilMigracion.getUsuario() != null && usuarioFinal == null) {
                usuarioFinal = perfilMigracion.getUsuario();
                log.info(">>> Usuario obtenido del PerfilMigracion guardado");
            }

            // 1.5. Actualizar Registro1 si se proporcionan campos de genero o estadoCivil
            try {
                if (usuarioIdFinal != null) {
                    final Long finalUsuarioIdForRegistro1 = usuarioIdFinal;
                    registro1Repository.findById(finalUsuarioIdForRegistro1).ifPresent(registro1 -> {
                        try {
                            boolean actualizado = false;
                            
                            // Actualizar genero
                            if (datos.get("genero") != null) {
                                try {
                                    String generoStr = convertirAString(datos.get("genero"));
                                    if (generoStr != null && !generoStr.isBlank()) {
                                        Registro1.Genero genero = Registro1.Genero.valueOf(generoStr.toUpperCase());
                                        registro1.setGenero(genero);
                                        actualizado = true;
                                        log.info(">>> Genero actualizado en Registro1 para usuario {}: {}", finalUsuarioIdForRegistro1, genero);
                                    }
                                } catch (IllegalArgumentException e) {
                                    log.warn(">>> Valor de genero inválido: {}", datos.get("genero"));
                                }
                            }
                            
                            // Actualizar estadoCivil
                            if (datos.get("estadoCivil") != null) {
                                try {
                                    String estadoCivilStr = convertirAString(datos.get("estadoCivil"));
                                    if (estadoCivilStr != null && !estadoCivilStr.isBlank()) {
                                        Registro1.EstadoCivil estadoCivil = Registro1.EstadoCivil.valueOf(estadoCivilStr.toUpperCase());
                                        registro1.setEstadoCivil(estadoCivil);
                                        actualizado = true;
                                        log.info(">>> EstadoCivil actualizado en Registro1 para usuario {}: {}", finalUsuarioIdForRegistro1, estadoCivil);
                                    }
                                } catch (IllegalArgumentException e) {
                                    log.warn(">>> Valor de estadoCivil inválido: {}", datos.get("estadoCivil"));
                                }
                            }
                            
                            if (actualizado) {
                                registro1Repository.save(registro1);
                                log.info(">>> Registro1 actualizado exitosamente para usuario {}", finalUsuarioIdForRegistro1);
                            }
                        } catch (Exception e) {
                            log.error(">>> Error dentro del lambda al actualizar Registro1: {}", e.getMessage(), e);
                            // No propagar la excepción para evitar marcar la transacción para rollback
                        }
                    });
                }
            } catch (Exception e) {
                log.error(">>> Error al actualizar Registro1: {}", e.getMessage(), e);
                // No propagar la excepción para evitar marcar la transacción para rollback
            }

            // 2. Guardar Área de Conocimiento
            try {
                if (datos.get("areaNombre") != null && !datos.get("areaNombre").toString().isBlank()) {
                    log.info(">>> Guardando área de conocimiento");
                    guardarAreaConocimientoConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar área de conocimiento: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 3. Guardar Institución
            try {
                if (datos.get("instNombre") != null && !datos.get("instNombre").toString().isBlank()) {
                    log.info(">>> Guardando institución");
                    guardarInstitucionConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar institución: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 4. Guardar Trayectoria Académica
            try {
                if (datos.get("acadTitulo") != null && !datos.get("acadTitulo").toString().isBlank()) {
                    log.info(">>> Guardando trayectoria académica");
                    guardarTrayectoriaAcademicaConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar trayectoria académica: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 5. Guardar Idioma
            try {
                if (datos.get("idiomaNombre") != null && !datos.get("idiomaNombre").toString().isBlank()) {
                    log.info(">>> Guardando idioma");
                    guardarIdiomaConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar idioma: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 6. Guardar Trayectoria Profesional
            try {
                if (datos.get("trayProfNombramiento") != null && !datos.get("trayProfNombramiento").toString().isBlank()) {
                    log.info(">>> Guardando trayectoria profesional");
                    guardarTrayectoriaProfesionalConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar trayectoria profesional: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 7. Guardar Estancia
            try {
                if (datos.get("estanciaNombreProyecto") != null && !datos.get("estanciaNombreProyecto").toString().isBlank()) {
                    log.info(">>> Guardando estancia");
                    guardarEstanciaConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar estancia: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 8. Guardar Curso
            try {
                if (datos.get("cursoNombre") != null && !datos.get("cursoNombre").toString().isBlank()) {
                    log.info(">>> Guardando curso");
                    guardarCursoConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar curso: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 9. Guardar Congreso
            try {
                if (datos.get("congresoNombreEvento") != null && !datos.get("congresoNombreEvento").toString().isBlank()) {
                    log.info(">>> Guardando congreso");
                    guardarCongresoConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar congreso: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 10. Guardar Divulgación
            try {
                if (datos.get("divulgTitulo") != null && !datos.get("divulgTitulo").toString().isBlank()) {
                    log.info(">>> Guardando divulgación");
                    guardarDivulgacionConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar divulgación: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 11. Guardar Artículo
            try {
                if (datos.get("artTitulo") != null && !datos.get("artTitulo").toString().isBlank()) {
                    log.info(">>> Guardando artículo");
                    guardarArticuloConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar artículo: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 12. Guardar Logro
            try {
                if (datos.get("logroNombre") != null && !datos.get("logroNombre").toString().isBlank()) {
                    log.info(">>> Guardando logro");
                    guardarLogroConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar logro: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            // 13. Guardar Interés y Habilidad
            try {
                if ((datos.get("interesDescripcion") != null && !datos.get("interesDescripcion").toString().isBlank()) || 
                    (datos.get("habilidadDescripcion") != null && !datos.get("habilidadDescripcion").toString().isBlank())) {
                    log.info(">>> Guardando interés y habilidad");
                    guardarInteresHabilidadConTransaccion(usuarioFinal, datos);
                }
            } catch (Exception e) {
                log.error(">>> Error al guardar interés y habilidad: {}", e.getMessage(), e);
                // No propagar la excepción para no marcar la transacción para rollback
            }

            log.info(">>> PerfilMigracion completo guardado exitosamente");
            
            // Verificar que el perfil se guardó correctamente
            if (perfilMigracion == null || perfilMigracion.getId() == null) {
                log.error(">>> PerfilMigracion no se guardó correctamente");
                throw new IllegalStateException("No se pudo guardar el PerfilMigracion");
            }
            
            return perfilMigracion;
        } catch (IllegalStateException e) {
            // Esta excepción SÍ debe causar rollback
            log.error(">>> Error crítico de estado al guardar perfil completo: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            // Otras excepciones no críticas: loguear pero intentar retornar el perfil si existe
            log.error(">>> Error no crítico al guardar perfil completo: {}", e.getMessage(), e);
            // Intentar retornar el perfil si existe, aunque haya habido errores parciales
            if (perfilMigracion != null && perfilMigracion.getId() != null) {
                log.warn(">>> Retornando PerfilMigracion parcialmente guardado debido a errores no críticos");
                return perfilMigracion;
            }
            // Si no hay perfil guardado, lanzar IllegalStateException para rollback
            throw new IllegalStateException("Error al guardar perfil: " + e.getMessage(), e);
        }
    }

    private void guardarAreaConocimiento(Usuario usuario, Map<String, Object> datos) {
        // Eliminar áreas anteriores si existen
        areaConocimientoRepository.findByUsuarioId(usuario.getId()).forEach(areaConocimientoRepository::delete);
        
        AreaConocimiento area = AreaConocimiento.builder()
                .usuario(usuario)
                .areaId(convertirAString(datos.get("areaId")))
                .areaNombre(convertirAString(datos.get("areaNombre")))
                .areaClave(convertirAString(datos.get("areaClave")))
                .areaVersion(convertirAString(datos.get("areaVersion")))
                .campoId(convertirAString(datos.get("campoId")))
                .campoNombre(convertirAString(datos.get("campoNombre")))
                .campoClave(convertirAString(datos.get("campoClave")))
                .disciplinaId(convertirAString(datos.get("disciplinaId")))
                .disciplinaNombre(convertirAString(datos.get("disciplinaNombre")))
                .disciplinaClave(convertirAString(datos.get("disciplinaClave")))
                .subdisciplinaId(convertirAString(datos.get("subdisciplinaId")))
                .subdisciplinaNombre(convertirAString(datos.get("subdisciplinaNombre")))
                .subdisciplinaClave(convertirAString(datos.get("subdisciplinaClave")))
                .build();
        areaConocimientoRepository.save(area);
    }

    private void guardarInstitucion(Usuario usuario, Map<String, Object> datos) {
        // Eliminar instituciones anteriores si existen
        institucionRepository.findByUsuarioId(usuario.getId()).forEach(institucionRepository::delete);
        
        Institucion institucion = Institucion.builder()
                .usuario(usuario)
                .claveOficial(convertirAString(datos.get("instClaveOficial")))
                .nombre(convertirAString(datos.get("instNombre")))
                .tipoId(convertirAString(datos.get("instTipoId")))
                .tipoNombre(convertirAString(datos.get("instTipoNombre")))
                .paisNombre(convertirAString(datos.get("instPaisNombre")))
                .entidadNombre(convertirAString(datos.get("instEntidadNombre")))
                .nivelUnoNombre(convertirAString(datos.get("instNivelUnoNombre")))
                .nivelDosNombre(convertirAString(datos.get("instNivelDosNombre")))
                .build();
        institucionRepository.save(institucion);
    }

    private void guardarTrayectoriaAcademica(Usuario usuario, Map<String, Object> datos) {
        // Eliminar trayectorias anteriores si existen
        trayectoriaAcademicaRepository.findByUsuarioId(usuario.getId()).forEach(trayectoriaAcademicaRepository::delete);
        
        TrayectoriaAcademica trayectoria = TrayectoriaAcademica.builder()
                .usuario(usuario)
                .nivelNombre(convertirAString(datos.get("acadNivelNombre")))
                .titulo(convertirAString(datos.get("acadTitulo")))
                .estatusNombre(convertirAString(datos.get("acadEstatusNombre")))
                .cedulaProfesional(convertirAString(datos.get("acadCedulaProfesional")))
                .opcionTitulacion(convertirAString(datos.get("acadOpcionTitulacion")))
                .tituloTesis(truncarTextoLargo(datos.get("acadTituloTesis")))
                .fechaObtencion(convertirFecha(datos.get("acadFechaObtencion")))
                .build();
        trayectoriaAcademicaRepository.save(trayectoria);
    }

    private void guardarIdioma(Usuario usuario, Map<String, Object> datos) {
        // Eliminar idiomas anteriores si existen
        idiomaRepository.findByUsuarioId(usuario.getId()).forEach(idiomaRepository::delete);
        
        Idioma idioma = Idioma.builder()
                .usuario(usuario)
                .nombre(convertirAString(datos.get("idiomaNombre")))
                .dominioNombre(convertirAString(datos.get("idiomaDominioNombre")))
                .conversacion(convertirAString(datos.get("idiomaConversacion")))
                .lectura(convertirAString(datos.get("idiomaLectura")))
                .escritura(convertirAString(datos.get("idiomaEscritura")))
                .esCertificado((Boolean) datos.get("idiomaEsCertificado"))
                .certInstitucion(convertirAString(datos.get("idiomaCertInstitucion")))
                .certPuntuacion(convertirAString(datos.get("idiomaCertPuntuacion")))
                .vigenciaFin(convertirFecha(datos.get("idiomaVigenciaFin")))
                .build();
        idiomaRepository.save(idioma);
    }

    private void guardarTrayectoriaProfesional(Usuario usuario, Map<String, Object> datos) {
        // Eliminar trayectorias anteriores si existen
        trayectoriaProfesionalRepository.findByUsuarioId(usuario.getId()).forEach(trayectoriaProfesionalRepository::delete);
        
        // Obtener y truncar logros antes de construir
        Object logrosObj = datos.get("trayProfLogros");
        String logrosTruncado = truncarTextoLargo(logrosObj);
        if (logrosObj != null && logrosTruncado != null) {
            String original = convertirAString(logrosObj);
            if (original != null && original.length() > 16000) {
                log.info(">>> Logros de trayectoria profesional truncados de {} a {} caracteres", original.length(), logrosTruncado.length());
            }
        }
        
        TrayectoriaProfesional trayectoria = TrayectoriaProfesional.builder()
                .usuario(usuario)
                .nombramiento(convertirAString(datos.get("trayProfNombramiento")))
                .fechaInicio(convertirFecha(datos.get("trayProfFechaInicio")))
                .fechaFin(convertirFecha(datos.get("trayProfFechaFin")))
                .esActual((Boolean) datos.get("trayProfEsActual"))
                .logros(logrosTruncado)
                .build();
        trayectoriaProfesionalRepository.save(trayectoria);
    }

    private void guardarEstancia(Usuario usuario, Map<String, Object> datos) {
        // Eliminar estancias anteriores si existen
        estanciaRepository.findByUsuarioId(usuario.getId()).forEach(estanciaRepository::delete);
        
        Estancia estancia = Estancia.builder()
                .usuario(usuario)
                .nombreProyecto(truncarTextoLargo(datos.get("estanciaNombreProyecto")))
                .tipoNombre(convertirAString(datos.get("estanciaTipoNombre")))
                .logros(truncarTextoLargo(datos.get("estanciaLogros")))
                .fechaInicio(convertirFecha(datos.get("estanciaFechaInicio")))
                .fechaFin(convertirFecha(datos.get("estanciaFechaFin")))
                .institucionReceptora(convertirAString(datos.get("estanciaInstitucionReceptora")))
                .build();
        estanciaRepository.save(estancia);
    }

    private void guardarCurso(Usuario usuario, Map<String, Object> datos) {
        // Eliminar cursos anteriores si existen
        cursoRepository.findByUsuarioId(usuario.getId()).forEach(cursoRepository::delete);
        
        Curso curso = Curso.builder()
                .usuario(usuario)
                .nombre(convertirAString(datos.get("cursoNombre")))
                .programa(convertirAString(datos.get("cursoPrograma")))
                .horasTotales(convertirInteger(datos.get("cursoHorasTotales")))
                .fechaInicio(convertirFecha(datos.get("cursoFechaInicio")))
                .fechaFin(convertirFecha(datos.get("cursoFechaFin")))
                .institucion(convertirAString(datos.get("cursoInstitucion")))
                .nivelEscolaridad(convertirAString(datos.get("cursoNivelEscolaridad")))
                .build();
        cursoRepository.save(curso);
    }

    private void guardarCongreso(Usuario usuario, Map<String, Object> datos) {
        // Eliminar congresos anteriores si existen
        congresoRepository.findByUsuarioId(usuario.getId()).forEach(congresoRepository::delete);
        
        Congreso congreso = Congreso.builder()
                .usuario(usuario)
                .nombreEvento(convertirAString(datos.get("congresoNombreEvento")))
                .tituloTrabajo(convertirAString(datos.get("congresoTituloTrabajo")))
                .tipoParticipacionNombre(convertirAString(datos.get("congresoTipoPartNombre")))
                .fecha(convertirFecha(datos.get("congresoFecha")))
                .paisSede(convertirAString(datos.get("congresoPaisSede")))
                .build();
        congresoRepository.save(congreso);
    }

    private void guardarDivulgacion(Usuario usuario, Map<String, Object> datos) {
        // Eliminar divulgaciones anteriores si existen
        divulgacionRepository.findByUsuarioId(usuario.getId()).forEach(divulgacionRepository::delete);
        
        Divulgacion divulgacion = Divulgacion.builder()
                .usuario(usuario)
                .titulo(convertirAString(datos.get("divulgTitulo")))
                .tipoDivulgacionNombre(convertirAString(datos.get("divulgTipoDivNombre")))
                .medioNombre(convertirAString(datos.get("divulgMedioNombre")))
                .dirigidoA(convertirAString(datos.get("divulgDirigidoA")))
                .productoObtenidoNombre(convertirAString(datos.get("divulgProdObtenidoNombre")))
                .fecha(convertirFecha(datos.get("divulgFecha")))
                .institucionOrganizadora(convertirAString(datos.get("divulgInstitucionOrganizadora")))
                .build();
        divulgacionRepository.save(divulgacion);
    }

    private void guardarArticulo(Usuario usuario, Map<String, Object> datos) {
        // Eliminar artículos anteriores si existen
        articuloRepository.findByUsuarioId(usuario.getId()).forEach(articuloRepository::delete);
        
        Articulo articulo = Articulo.builder()
                .usuario(usuario)
                .idExterno(convertirAString(datos.get("artIdExterno")))
                .eje(convertirAString(datos.get("artEje")))
                .tipo(convertirAString(datos.get("artTipo")))
                .productoPrincipal((Boolean) datos.get("artProductoPrincipal"))
                .anio(convertirInteger(datos.get("artAnio")))
                .issn(convertirAString(datos.get("artIssn")))
                .issnElectronico(convertirAString(datos.get("artIssnElectronico")))
                .doi(convertirAString(datos.get("artDoi")))
                .nombreRevista(convertirAString(datos.get("artNombreRevista")))
                .titulo(convertirAString(datos.get("artTitulo")))
                .rolParticipacionNombre(convertirAString(datos.get("artRolPartNombre")))
                .estadoNombre(convertirAString(datos.get("artEstadoNombre")))
                .objetivoNombre(convertirAString(datos.get("artObjetivoNombre")))
                .fondoProgramaNombre(convertirAString(datos.get("artFondoProgNombre")))
                .autores(new java.util.ArrayList<>())
                .build();
        
        // Guardar el artículo primero para obtener su ID
        articulo = articuloRepository.save(articulo);
        
        // Guardar autores después de que el artículo tiene ID
        String autorNombre = convertirAString(datos.get("artAutorNombreCompleto"));
        if (autorNombre != null && !autorNombre.isBlank()) {
            AutorArticulo autor = AutorArticulo.builder()
                    .articulo(articulo)
                    .nombreCompleto(autorNombre)
                    .orcid(convertirAString(datos.get("artAutorOrcid")))
                    .orden(convertirInteger(datos.get("artAutorOrden")))
                    .build();
            articulo.getAutores().add(autor);
            articuloRepository.save(articulo);
        }
    }

    private void guardarLogro(Usuario usuario, Map<String, Object> datos) {
        // Eliminar logros anteriores si existen
        logroRepository.findByUsuarioId(usuario.getId()).forEach(logroRepository::delete);
        
        Logro logro = Logro.builder()
                .usuario(usuario)
                .tipo(convertirAString(datos.get("logroTipo")))
                .nombre(truncarTextoLargo(datos.get("logroNombre")))
                .anio(convertirInteger(datos.get("logroAnio")))
                .build();
        logroRepository.save(logro);
    }

    private void guardarInteresHabilidad(Usuario usuario, Map<String, Object> datos) {
        // Eliminar interés/habilidad anterior si existe
        interesHabilidadRepository.findByUsuarioId(usuario.getId())
                .ifPresent(interesHabilidadRepository::delete);
        
        InteresHabilidad interesHabilidad = InteresHabilidad.builder()
                .usuario(usuario)
                .fotoUri(convertirAString(datos.get("fotoUri")))
                .interesDescripcion(truncarTextoLargo(datos.get("interesDescripcion")))
                .habilidadDescripcion(truncarTextoLargo(datos.get("habilidadDescripcion")))
                .habilidadNivel(convertirAString(datos.get("habilidadNivel")))
                .build();
        interesHabilidadRepository.save(interesHabilidad);
    }

    // Métodos auxiliares para conversión
    private String convertirAString(Object valor) {
        if (valor == null) return null;
        String str = valor instanceof String ? (String) valor : valor.toString();
        // Retornar null si el string está vacío o solo contiene espacios
        return (str != null && !str.trim().isEmpty()) ? str.trim() : null;
    }
    
    /**
     * Trunca un texto a un tamaño máximo para evitar errores de "Data too long"
     * Para campos @Lob, MySQL TEXT puede almacenar hasta 65,535 caracteres
     * Usamos un límite conservador de 60,000 caracteres para dejar margen
     */
    private String truncarTexto(Object valor, int maxLength) {
        if (valor == null) return null;
        String str = convertirAString(valor);
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        log.warn(">>> Texto truncado de {} a {} caracteres", str.length(), maxLength);
        return str.substring(0, maxLength);
    }
    
    /**
     * Trunca campos de texto largo (Lob) a un tamaño seguro
     * Nota: Si la columna es VARCHAR en lugar de TEXT, el límite puede ser mucho menor
     * Usamos 16,000 caracteres como límite conservador (VARCHAR máximo en MySQL)
     */
    private String truncarTextoLargo(Object valor) {
        // MySQL TEXT puede almacenar hasta 65,535 caracteres
        // Pero si la columna es VARCHAR, el límite es mucho menor
        // Usamos 16,000 como límite conservador para evitar problemas
        return truncarTexto(valor, 16000);
    }

    private java.time.LocalDate convertirFecha(Object fecha) {
        if (fecha == null) return null;
        String fechaStr = convertirAString(fecha);
        if (fechaStr == null || fechaStr.isBlank()) return null;
        try {
            return java.time.LocalDate.parse(fechaStr);
        } catch (Exception e) {
            log.warn(">>> Error al convertir fecha: {}", fechaStr);
            return null;
        }
    }

    private Integer convertirInteger(Object valor) {
        if (valor == null) return null;
        if (valor instanceof Integer) return (Integer) valor;
        if (valor instanceof Long) {
            Long longVal = (Long) valor;
            if (longVal > Integer.MAX_VALUE || longVal < Integer.MIN_VALUE) {
                log.warn(">>> Valor Long {} fuera del rango de Integer, usando null", longVal);
                return null;
            }
            return longVal.intValue();
        }
        if (valor instanceof String) {
            try {
                String str = (String) valor;
                if (str.isBlank()) return null;
                // Intentar parsear como Long primero para manejar números grandes
                long longVal = Long.parseLong(str);
                if (longVal > Integer.MAX_VALUE || longVal < Integer.MIN_VALUE) {
                    log.warn(">>> Valor String '{}' fuera del rango de Integer, usando null", str);
                    return null;
                }
                return (int) longVal;
            } catch (NumberFormatException e) {
                log.warn(">>> No se pudo convertir '{}' a Integer: {}", valor, e.getMessage());
                return null;
            } catch (Exception e) {
                log.warn(">>> Error inesperado al convertir '{}' a Integer: {}", valor, e.getMessage());
                return null;
            }
        }
        if (valor instanceof Number) {
            try {
                return ((Number) valor).intValue();
            } catch (Exception e) {
                log.warn(">>> Error al convertir Number '{}' a Integer: {}", valor, e.getMessage());
                return null;
            }
        }
        return null;
    }

    // Métodos con transacciones anidadas para evitar que errores marquen la transacción principal para rollback
    // Nota: Los métodos deben ser públicos para que Spring pueda crear proxies
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarAreaConocimientoConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarAreaConocimiento(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarInstitucionConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarInstitucion(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarTrayectoriaAcademicaConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarTrayectoriaAcademica(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarIdiomaConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarIdioma(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarTrayectoriaProfesionalConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarTrayectoriaProfesional(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarEstanciaConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarEstancia(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarCursoConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarCurso(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarCongresoConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarCongreso(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarDivulgacionConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarDivulgacion(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarArticuloConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarArticulo(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarLogroConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarLogro(usuario, datos);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void guardarInteresHabilidadConTransaccion(Usuario usuario, Map<String, Object> datos) {
        guardarInteresHabilidad(usuario, datos);
    }
}
