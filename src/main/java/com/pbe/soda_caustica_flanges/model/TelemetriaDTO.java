package com.pbe.soda_caustica_flanges.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TelemetriaDTO {

    @NotBlank(message = "O token do dispositivo é obrigatório.")
    @Size(max = 128)
    private String token;

    @NotBlank(message = "O identificador da mensagem é obrigatório.")
    @Pattern(regexp = "^[A-Za-z0-9_-]{8,80}$",
            message = "O identificador da mensagem precisa ter de 8 a 80 caracteres.")
    private String idMensagem;

    @NotNull(message = "A leitura de temperatura é obrigatória.")
    @DecimalMin(value = "-100.0", message = "Temperatura abaixo da faixa suportada.")
    @DecimalMax(value = "400.0", message = "Temperatura acima da faixa suportada.")
    private Double temperaturaC;

    // Pode ser enviado pelo firmware quando a própria lógica local detectar a condição.
    private Boolean vazamentoDetectado;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getIdMensagem() { return idMensagem; }
    public void setIdMensagem(String idMensagem) { this.idMensagem = idMensagem; }

    public Double getTemperaturaC() { return temperaturaC; }
    public void setTemperaturaC(Double temperaturaC) { this.temperaturaC = temperaturaC; }

    public Boolean getVazamentoDetectado() { return vazamentoDetectado; }
    public void setVazamentoDetectado(Boolean vazamentoDetectado) {
        this.vazamentoDetectado = vazamentoDetectado;
    }
}
