package com.example.proyecto.demo.dto;

import com.example.proyecto.demo.Entity.Registro1;

import java.time.LocalDate;

public record PerfilCompletoDTO(
        Long id,
        String nombre,
        String apellidoPaterno,
        String apellidoMaterno,
        String email,
        String curp,
        String rfc,
        String genero,
        LocalDate fechaNacimiento,
        String nacionalidad,
        String paisNacimiento,
        String entidadFederativa,
        String estadoCivil,
        String tipoPerfil,
        Boolean tienePerfilMigracion,
        String visibilidadPerfil,
        String gradoAcademico,
        Long fotoDocumentoId,
        Long curriculumDocumentoId
) {
    public static PerfilCompletoDTO from(com.example.proyecto.demo.Entity.Usuario usuario) {
        Registro1 reg = usuario.getRegistro1();
        boolean tienePerfil = usuario.getPerfilMigracion() != null;
        return new PerfilCompletoDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidoPaterno(),
                usuario.getApellidoMaterno(),
                usuario.getAuthUser() != null ? usuario.getAuthUser().getEmail() : null,
                reg != null ? reg.getCurp() : null,
                reg != null ? reg.getRfc() : null,
                reg != null && reg.getGenero() != null ? reg.getGenero().name() : null,
                reg != null ? reg.getFechaNacimiento() : null,
                reg != null ? reg.getNacionalidad() : null,
                reg != null ? reg.getPaisNacimiento() : null,
                reg != null ? reg.getEntidadFederativa() : null,
                reg != null && reg.getEstadoCivil() != null ? reg.getEstadoCivil().name() : null,
                reg != null && reg.getTipoPerfil() != null ? reg.getTipoPerfil().name() : null,
                tienePerfil,
                usuario.getVisibilidadPerfil() != null ? usuario.getVisibilidadPerfil() : "ESTANDAR",
                null, // gradoAcademico - se puede agregar después si existe en la BD
                null, // fotoDocumentoId - se obtiene desde el servicio
                null  // curriculumDocumentoId - se obtiene desde el servicio
        );
    }
}
