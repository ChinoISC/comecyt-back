package com.example.proyecto.demo.Service;

import com.example.proyecto.demo.Entity.AuthUser;
import com.example.proyecto.demo.Entity.Usuario;
import com.example.proyecto.demo.Repository.ArticuloRepository;
import com.example.proyecto.demo.Repository.AreaConocimientoRepository;
import com.example.proyecto.demo.Repository.AuthUserRepository;
import com.example.proyecto.demo.Repository.CongresoRepository;
import com.example.proyecto.demo.Repository.CursoRepository;
import com.example.proyecto.demo.Repository.DocumentoRepository;
import com.example.proyecto.demo.Repository.DivulgacionRepository;
import com.example.proyecto.demo.Repository.EstanciaRepository;
import com.example.proyecto.demo.Repository.HerramientaRepository;
import com.example.proyecto.demo.Repository.IdiomaRepository;
import com.example.proyecto.demo.Repository.IncidenciaSocialRepository;
import com.example.proyecto.demo.Repository.InstitucionRepository;
import com.example.proyecto.demo.Repository.InteresHabilidadRepository;
import com.example.proyecto.demo.Repository.LogroRepository;
import com.example.proyecto.demo.Repository.TrayectoriaAcademicaRepository;
import com.example.proyecto.demo.Repository.TrayectoriaProfesionalRepository;
import com.example.proyecto.demo.Repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UsuarioRepository usuarioRepository;
    private final AuthUserRepository authUserRepository;
    private final DocumentoRepository documentoRepository;
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
    private final IncidenciaSocialRepository incidenciaSocialRepository;
    private final HerramientaRepository herramientaRepository;

    /**
     * Elimina un usuario y todos sus datos asociados (documentos, trayectoria, auth). Registro1 y PerfilMigracion se eliminan en cascada al borrar Usuario.
     */
    @Transactional
    public void eliminarUsuario(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Long id = u.getId();
        documentoRepository.findByUsuarioId(id).forEach(documentoRepository::delete);
        areaConocimientoRepository.findByUsuarioId(id).forEach(areaConocimientoRepository::delete);
        institucionRepository.findByUsuarioId(id).forEach(institucionRepository::delete);
        trayectoriaAcademicaRepository.findByUsuarioId(id).forEach(trayectoriaAcademicaRepository::delete);
        idiomaRepository.findByUsuarioId(id).forEach(idiomaRepository::delete);
        trayectoriaProfesionalRepository.findByUsuarioId(id).forEach(trayectoriaProfesionalRepository::delete);
        estanciaRepository.findByUsuarioId(id).forEach(estanciaRepository::delete);
        cursoRepository.findByUsuarioId(id).forEach(cursoRepository::delete);
        congresoRepository.findByUsuarioId(id).forEach(congresoRepository::delete);
        divulgacionRepository.findByUsuarioId(id).forEach(divulgacionRepository::delete);
        articuloRepository.findByUsuarioId(id).forEach(articuloRepository::delete);
        logroRepository.findByUsuarioId(id).forEach(logroRepository::delete);
        interesHabilidadRepository.findByUsuarioId(id).ifPresent(interesHabilidadRepository::delete);
        incidenciaSocialRepository.findByUsuarioId(id).forEach(incidenciaSocialRepository::delete);
        herramientaRepository.findByUsuarioId(id).forEach(herramientaRepository::delete);

        AuthUser au = u.getAuthUser();
        if (au != null) {
            u.setAuthUser(null);
            usuarioRepository.save(u);
            authUserRepository.delete(au);
        }
        usuarioRepository.delete(u);
        log.info("Usuario {} eliminado correctamente", usuarioId);
    }
}
