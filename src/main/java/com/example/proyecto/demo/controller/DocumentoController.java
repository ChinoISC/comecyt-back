package com.example.proyecto.demo.controller;

import com.example.proyecto.demo.Entity.Documento;
import com.example.proyecto.demo.Entity.Usuario;
import com.example.proyecto.demo.Repository.UsuarioRepository;
import com.example.proyecto.demo.Service.DocumentoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/documentos")
@RequiredArgsConstructor
@Slf4j
public class DocumentoController {

    private final DocumentoService documentoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping("/{documentoId}")
    public ResponseEntity<byte[]> descargarDocumento(@PathVariable Long documentoId) {
        try {
            var documento = documentoService.obtenerDocumento(documentoId)
                    .orElseThrow(() -> new RuntimeException("Documento no encontrado"));

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + documento.getNombreArchivo() + "\"")
                    .contentType(MediaType.parseMediaType(documento.getContentType()))
                    .body(documento.getContenido());
        } catch (Exception e) {
            log.error("Error al descargar documento {}: {}", documentoId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<List<Documento>> listarDocumentosPorUsuario(
            @PathVariable Long usuarioId) {
        var documentos = documentoService.obtenerDocumentosPorUsuario(usuarioId);
        return ResponseEntity.ok(documentos);
    }

    /**
     * Endpoint para guardar documentos del registro2 (sin perfil completo)
     * Solo guarda: CV, Fiscal PDF, Domicilio, Certificados
     */
    @PostMapping(value = "/registro2", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> guardarDocumentosRegistro2(
            Authentication auth,
            @RequestPart(value = "cvFile", required = false) MultipartFile cvFile,
            @RequestPart(value = "fiscalPdf", required = false) MultipartFile fiscalPdf,
            @RequestPart(value = "domicilio", required = false) MultipartFile domicilio,
            @RequestPart(value = "cert1", required = false) MultipartFile cert1,
            @RequestPart(value = "cert2", required = false) MultipartFile cert2) {
        
        try {
            // Obtener el usuario autenticado
            Long authUserId = (Long) auth.getPrincipal();
            Usuario usuario = usuarioRepository.findByAuthUserIdWithRegistro1(authUserId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
            
            Long usuarioId = usuario.getId();
            
            // Guardar los documentos
            List<Documento> documentosGuardados = documentoService.guardarDocumentos(
                    usuarioId,
                    cvFile,
                    fiscalPdf,
                    domicilio,
                    cert1,
                    cert2,
                    null // divulgArchivo no aplica para registro2
            );
            
            log.info("Documentos del registro2 guardados para usuario {}: {}", usuarioId, documentosGuardados.size());
            
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Documentos guardados correctamente",
                    "documentosGuardados", documentosGuardados.size(),
                    "usuarioId", usuarioId
            ));
            
        } catch (IOException e) {
            log.error("Error al guardar documentos del registro2: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Error al guardar documentos: " + e.getMessage()
                    ));
        } catch (Exception e) {
            log.error("Error inesperado al guardar documentos del registro2: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Error al procesar la solicitud: " + e.getMessage()
                    ));
        }
    }
}
