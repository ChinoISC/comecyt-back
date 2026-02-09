package com.example.proyecto.demo.dto;

import java.util.List;

public record InvestigadorDTO(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String email,
        String telefono,
        String gradoAcademico,
        String semblanza,
        Long fotoDocumentoId,
        Long curriculumDocumentoId,
        List<CursoItemDTO> cursos,
        List<IdiomaItemDTO> idiomas,
        List<LogroItemDTO> logros,
        List<String> herramientas,
        List<ArticuloItemDTO> articulos
) {}
