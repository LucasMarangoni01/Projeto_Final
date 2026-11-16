package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "flange")
public class Flange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "nome_flange", nullable = false)
    private String nomeFlange;

    @NotBlank
    @Column(nullable = false)
    private String localizacao;

    @NotNull
    @Column(name = "temp_ambiente", nullable = false)
    private Double tempAmbiente;

    @NotNull(message = "A data de entrada é obrigatória.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "data_entrada", nullable = false)
    private LocalDate dataEntrada;

    @Size(max = 180)
    private String foto;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private StatusFlange status = StatusFlange.NORMAL;

    // Dados construtivos e de integridade solicitados pela documentação.
    @Size(max = 80)
    @Column(name = "norma_tecnica", length = 80)
    private String normaTecnica = "ASME B16.5";

    @Size(max = 30)
    @Column(name = "tipo_flange", length = 30)
    private String tipoFlange;

    @Size(max = 30)
    @Column(name = "diametro_nominal", length = 30)
    private String diametroNominal;

    @Enumerated(EnumType.STRING)
    @Column(name = "classe_pressao", length = 20)
    private ClassePressaoFlange classePressao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_face", length = 20)
    private TipoFaceFlange tipoFace;

    @Enumerated(EnumType.STRING)
    @Column(name = "material_fabricacao", length = 40)
    private MaterialFlange materialFabricacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_junta", length = 40)
    private TipoJuntaFlange tipoJunta;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "ultima_inspecao_integridade")
    private LocalDate ultimaInspecaoIntegridade;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Column(name = "proxima_inspecao_integridade")
    private LocalDate proximaInspecaoIntegridade;

    @Size(max = 120)
    @Column(name = "profissional_habilitado", length = 120)
    private String profissionalHabilitado;

    @Size(max = 80)
    @Column(name = "numero_art", length = 80)
    private String numeroArt;

    // Coordenadas percentuais (0-100) da planta baixa. Opcionais para registros legados.
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "coordenada_x")
    private Double coordenadaX;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "coordenada_y")
    private Double coordenadaY;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    // Soft delete: registros arquivados deixam de operar, mas continuam no histórico.
    @Column(name = "ativo")
    private Boolean ativo = true;

    @Column(name = "desativada_em")
    private LocalDateTime desativadaEm;

    @Column(name = "desativada_por_email", length = 100)
    private String desativadaPorEmail;

    @Column(name = "motivo_desativacao", length = 500)
    private String motivoDesativacao;

    public Flange() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNomeFlange() { return nomeFlange; }
    public void setNomeFlange(String nomeFlange) { this.nomeFlange = nomeFlange; }

    // Compatibilidade temporária com templates e integrações existentes.
    public String getNome_flange() { return nomeFlange; }
    public void setNome_flange(String nomeFlange) { this.nomeFlange = nomeFlange; }

    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String localizacao) { this.localizacao = localizacao; }

    public Double getTempAmbiente() { return tempAmbiente; }
    public void setTempAmbiente(Double tempAmbiente) { this.tempAmbiente = tempAmbiente; }

    public Double getTemp_ambiente() { return tempAmbiente; }
    public void setTemp_ambiente(Double tempAmbiente) { this.tempAmbiente = tempAmbiente; }

    public LocalDate getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(LocalDate dataEntrada) { this.dataEntrada = dataEntrada; }

    public LocalDate getData_entrada() { return dataEntrada; }
    public void setData_entrada(LocalDate dataEntrada) { this.dataEntrada = dataEntrada; }

    public String getFoto() { return foto; }
    public void setFoto(String foto) { this.foto = foto; }

    public StatusFlange getStatus() {
        return status == null ? StatusFlange.NORMAL : status;
    }
    public void setStatus(StatusFlange status) { this.status = status; }

    public String getNormaTecnica() { return normaTecnica; }
    public void setNormaTecnica(String normaTecnica) { this.normaTecnica = normaTecnica; }

    public String getTipoFlange() { return tipoFlange; }
    public void setTipoFlange(String tipoFlange) { this.tipoFlange = tipoFlange; }

    public String getDiametroNominal() { return diametroNominal; }
    public void setDiametroNominal(String diametroNominal) { this.diametroNominal = diametroNominal; }

    public ClassePressaoFlange getClassePressao() { return classePressao; }
    public void setClassePressao(ClassePressaoFlange classePressao) { this.classePressao = classePressao; }

    public TipoFaceFlange getTipoFace() { return tipoFace; }
    public void setTipoFace(TipoFaceFlange tipoFace) { this.tipoFace = tipoFace; }

    public MaterialFlange getMaterialFabricacao() { return materialFabricacao; }
    public void setMaterialFabricacao(MaterialFlange materialFabricacao) { this.materialFabricacao = materialFabricacao; }

    public TipoJuntaFlange getTipoJunta() { return tipoJunta; }
    public void setTipoJunta(TipoJuntaFlange tipoJunta) { this.tipoJunta = tipoJunta; }

    public LocalDate getUltimaInspecaoIntegridade() { return ultimaInspecaoIntegridade; }
    public void setUltimaInspecaoIntegridade(LocalDate ultimaInspecaoIntegridade) { this.ultimaInspecaoIntegridade = ultimaInspecaoIntegridade; }

    public LocalDate getProximaInspecaoIntegridade() { return proximaInspecaoIntegridade; }
    public void setProximaInspecaoIntegridade(LocalDate proximaInspecaoIntegridade) { this.proximaInspecaoIntegridade = proximaInspecaoIntegridade; }

    public String getProfissionalHabilitado() { return profissionalHabilitado; }
    public void setProfissionalHabilitado(String profissionalHabilitado) { this.profissionalHabilitado = profissionalHabilitado; }

    public String getNumeroArt() { return numeroArt; }
    public void setNumeroArt(String numeroArt) { this.numeroArt = numeroArt; }

    public Double getCoordenadaX() { return coordenadaX; }
    public void setCoordenadaX(Double coordenadaX) { this.coordenadaX = coordenadaX; }

    public Double getCoordenadaY() { return coordenadaY; }
    public void setCoordenadaY(Double coordenadaY) { this.coordenadaY = coordenadaY; }

    public Unidade getUnidade() { return unidade; }
    public void setUnidade(Unidade unidade) { this.unidade = unidade; }

    public boolean isAtivo() { return ativo == null || ativo; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }

    public LocalDateTime getDesativadaEm() { return desativadaEm; }
    public void setDesativadaEm(LocalDateTime desativadaEm) { this.desativadaEm = desativadaEm; }

    public String getDesativadaPorEmail() { return desativadaPorEmail; }
    public void setDesativadaPorEmail(String desativadaPorEmail) { this.desativadaPorEmail = desativadaPorEmail; }

    public String getMotivoDesativacao() { return motivoDesativacao; }
    public void setMotivoDesativacao(String motivoDesativacao) { this.motivoDesativacao = motivoDesativacao; }
}
