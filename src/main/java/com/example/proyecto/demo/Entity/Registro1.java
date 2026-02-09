package com.example.proyecto.demo.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
@Entity
@Table(
    name = "registro1",
    indexes = {
        @Index(name = "idx_registro1_curp", columnList = "curp", unique = true),
        @Index(name = "idx_registro1_rfc", columnList = "rfc", unique = true)
    }
)
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Registro1 {

    @Id
    private Long id;

    // 🔗 RELACIÓN COMPARTIENDO EL MISMO ID CON USUARIO
    @OneToOne
    @MapsId  // El id de Registro1 será el mismo que el de Usuario
    @JoinColumn(name = "id")  // Usa la columna 'id' como FK (que es también la PK)
    private Usuario usuario;
    



    @Pattern(regexp = "^[A-Z0-9]{18}$")
    @Column(length = 18, nullable = false, unique = true)
    private String curp;

    @Column(length = 13, unique = true)
    private String rfc;

    @Past
    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Genero genero;

    private String nacionalidad;
    private String paisNacimiento;
    private String entidadFederativa;

    @Enumerated(EnumType.STRING)
    private EstadoCivil estadoCivil;

    /** Tipo de perfil: INVESTIGADOR o INNOVADOR. Nullable para registros antiguos. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_perfil", length = 20)
    private TipoPerfil tipoPerfil;

    @Column(length = 20)
    private String telefono;

    @Column(updatable = false)
    private LocalDate createdAt;

    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
    }

    public enum Genero { MASCULINO, FEMENINO, OTRO }
    public enum EstadoCivil { SOLTERO, CASADO, DIVORCIADO, VIUDO, UNION_LIBRE }
    public enum TipoPerfil { INVESTIGADOR, INNOVADOR }
}
