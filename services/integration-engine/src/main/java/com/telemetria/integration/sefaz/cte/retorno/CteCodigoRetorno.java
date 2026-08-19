package com.telemetria.integration.sefaz.cte.retorno;

import java.util.Arrays;
import java.util.EnumSet;

/** Catálogo dos códigos que alteram o estado da integração CT-e. */
public enum CteCodigoRetorno {
    AUTORIZADO(100, CteResultadoCategoria.SUCESSO, CteOperacao.AUTORIZACAO, CteOperacao.CONSULTA),
    CANCELAMENTO_HOMOLOGADO(101, CteResultadoCategoria.CANCELADO, CteOperacao.CONSULTA),
    LOTE_RECEBIDO(103, CteResultadoCategoria.PROCESSANDO, CteOperacao.AUTORIZACAO),
    LOTE_PROCESSADO(104, CteResultadoCategoria.SUCESSO, CteOperacao.AUTORIZACAO),
    USO_DENEGADO(110, CteResultadoCategoria.DENEGADO, CteOperacao.AUTORIZACAO, CteOperacao.CONSULTA),
    SERVICO_EM_OPERACAO(107, CteResultadoCategoria.SUCESSO, CteOperacao.STATUS),
    SERVICO_PARALISADO_MOMENTANEAMENTE(108, CteResultadoCategoria.INDISPONIVEL, CteOperacao.STATUS),
    SERVICO_PARALISADO_SEM_PREVISAO(109, CteResultadoCategoria.INDISPONIVEL, CteOperacao.STATUS),
    SVC_EM_PROCESSO_DESATIVACAO(113, CteResultadoCategoria.SUCESSO, CteOperacao.STATUS),
    LOTE_EVENTO_PROCESSADO(128, CteResultadoCategoria.PROCESSANDO, CteOperacao.EVENTO),
    EVENTO_REGISTRADO_VINCULADO(135, CteResultadoCategoria.SUCESSO, CteOperacao.EVENTO),
    EVENTO_REGISTRADO_NAO_VINCULADO(136, CteResultadoCategoria.SUCESSO, CteOperacao.EVENTO),
    CANCELAMENTO_REGISTRADO(155, CteResultadoCategoria.CANCELADO, CteOperacao.EVENTO),
    CT_E_NAO_LOCALIZADO(217, CteResultadoCategoria.REJEICAO, CteOperacao.CONSULTA),
    DUPLICIDADE_DENEGADA(205, CteResultadoCategoria.DENEGADO, CteOperacao.AUTORIZACAO),
    USO_DENEGADO_IRREGULARIDADE_EMITENTE(301, CteResultadoCategoria.DENEGADO, CteOperacao.AUTORIZACAO),
    USO_DENEGADO_IRREGULARIDADE_DESTINATARIO(302, CteResultadoCategoria.DENEGADO, CteOperacao.AUTORIZACAO),
    ERRO_TECNICO_LOCAL(999, CteResultadoCategoria.ERRO_TECNICO,
            CteOperacao.AUTORIZACAO, CteOperacao.CONSULTA, CteOperacao.EVENTO, CteOperacao.STATUS);

    private final int codigo;
    private final CteResultadoCategoria categoria;
    private final EnumSet<CteOperacao> operacoes;

    CteCodigoRetorno(int codigo, CteResultadoCategoria categoria, CteOperacao... operacoes) {
        this.codigo = codigo;
        this.categoria = categoria;
        this.operacoes = EnumSet.copyOf(Arrays.asList(operacoes));
    }

    public int codigo() { return codigo; }
    public CteResultadoCategoria categoria() { return categoria; }

    public static CteResultadoCategoria classificar(CteOperacao operacao, int codigo) {
        return Arrays.stream(values())
                .filter(item -> item.codigo == codigo && item.operacoes.contains(operacao))
                .map(CteCodigoRetorno::categoria)
                .findFirst()
                .orElse(codigo >= 200 && codigo <= 999
                        ? CteResultadoCategoria.REJEICAO : CteResultadoCategoria.DESCONHECIDO);
    }
}
