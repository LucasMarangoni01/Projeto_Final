package com.pbe.soda_caustica_flanges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * Imagens de perfil e flanges armazenadas em banco para containers efemeros.
 * A autorizacao para consultar cada imagem permanece nos controllers.
 */
@Entity
@Table(name = "imagem_privada")
public class ImagemPrivada {

    @Id
    @Column(name = "chave", length = 190, nullable = false)
    private String chave;

    @Column(name = "tipo_mime", length = 40, nullable = false)
    private String tipoMime;

    @Lob
    @Column(name = "conteudo", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] dados;

    protected ImagemPrivada() {
    }

    public ImagemPrivada(String chave, String tipoMime, byte[] dados) {
        this.chave = chave;
        this.tipoMime = tipoMime;
        this.dados = dados;
    }

    public String getChave() { return chave; }
    public String getTipoMime() { return tipoMime; }
    public byte[] getDados() { return dados; }
}
