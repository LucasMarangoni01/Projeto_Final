package com.pbe.soda_caustica_flanges.model;

public enum TipoSensor {

    /*
     * Único tipo permitido para novos cadastros na nova fase do TCC.
     * Não implica detecção de soda cáustica ou validação industrial.
     */
    DETECCAO_GAS(
            "Sensor de gás (protótipo)",
            "Monitora um gás-alvo ainda a ser especificado. Um sensor de gás comum não detecta vazamentos de soda cáustica líquida."
    ),

    DETECCAO_VAZAMENTO(
            "Detecção de vazamento (NaOH)",
            "Identifica presença de líquido no ponto monitorado. O elemento sensor deve ser compatível com soda cáustica."
    ),
    TEMPERATURA_SUPERFICIE(
            "Temperatura superficial",
            "Acompanha a temperatura na região da flange e ajuda a identificar alterações térmicas."
    ),
    PRESSAO_PROCESSO(
            "Pressão do processo",
            "Acompanha a pressão da linha associada ao conjunto monitorado."
    ),
    PH_PROCESSO(
            "pH do processo",
            "Mede o pH no ponto instrumentado. Os limites de alarme devem ser definidos para o processo específico."
    ),
    CONDUTIVIDADE_LIQUIDO(
            "Condutividade / presença de líquido",
            "Detecta alteração de condutividade no elemento instalado; a compatibilidade química do elemento deve ser validada."
    ),
    PRESSAO_DIFERENCIAL(
            "Pressão diferencial",
            "Compara pressão entre pontos da linha para identificar quedas anormais conforme limites definidos pelo responsável técnico."
    ),
    VIBRACAO(
            "Vibração",
            "Ajuda a identificar vibrações anormais no conjunto de tubulação e flange."
    ),
    UMIDADE_CONDENSACAO(
            "Umidade / condensação",
            "Acompanha umidade no entorno e pode apoiar inspeções preventivas."
    );

    private final String descricao;
    private final String finalidade;

    TipoSensor(String descricao, String finalidade) {
        this.descricao = descricao;
        this.finalidade = finalidade;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getFinalidade() {
        return finalidade;
    }
}
