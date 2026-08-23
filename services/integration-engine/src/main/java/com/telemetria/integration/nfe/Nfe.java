package com.telemetria.integration.nfe;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.ConsultaDFeEnum;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.PessoaEnum;
import com.telemetria.integration.nfe.dom.enuns.ServicosEnum;
import com.telemetria.integration.nfe.exception.NfeException;
import com.telemetria.integration.nfe.schemas.RetDistDFeInt;
import com.telemetria.integration.nfe.schemas.TEnviNFe;
import com.telemetria.integration.nfe.schemas.TInutNFe;
import com.telemetria.integration.nfe.schemas.TRetConsCad;
import com.telemetria.integration.nfe.schemas.TRetConsReciNFe;
import com.telemetria.integration.nfe.schemas.TRetConsSitNFe;
import com.telemetria.integration.nfe.schemas.TRetConsStatServ;
import com.telemetria.integration.nfe.schemas.TRetEnviNFe;
import com.telemetria.integration.nfe.schemas.TRetInutNFe;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoAtorInteressado;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamento;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoInsucessoEntrega;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCancelamentoSubstituicao;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoEpec;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoGenerico;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoInsucessoEntrega;
import com.telemetria.integration.nfe.schemas_eventos.TEnvEventoManifestacao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoAtorInteressado;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamento;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoInsucessoEntrega;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCancelamentoSubstituicao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoCartaCorrecao;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoConciliacaoFinanceira;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoEpec;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoGenerico;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoInsucessoEntrega;
import com.telemetria.integration.nfe.schemas_eventos.TRetEnvEventoManifestacao;
import com.telemetria.integration.nfe.util.ConfiguracoesUtil;

/**
 * @author Samuel Oliveira - samuel@swconsultoria.com.br - www.swconsultoria.com.br
 */
public class Nfe {

    /**
     * Construtor privado
     */
    private Nfe() {
    }

    /**
     * Classe Reponsavel Por Consultar a Distribuiçao da NFE na SEFAZ
     *
     * @param tipoPessoa   Informar PessoaEnum.CPF ou PessoaEnum.CNPJ
     * @param cpfCnpj
     * @param tipoConsulta Informar ConsultaDFe.NSU ou ConsultaDFe.CHAVE
     * @param nsuChave
     * @return
     * @throws NfeException
     */
    public static RetDistDFeInt distribuicaoDfe(ConfiguracoesNfe configuracoesNfe, PessoaEnum tipoPessoa, String cpfCnpj,
                                                ConsultaDFeEnum tipoConsulta, String nsuChave) throws NfeException {

        return DistribuicaoDFe.consultaNfe(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, cpfCnpj), tipoPessoa, cpfCnpj, tipoConsulta, nsuChave);

    }

    /**
     * Metodo Responsavel Buscar o Status de Serviço do Servidor da Sefaz
     *
     * @param tipoDocumento informar DocumentoEnum.NFE ou DocumentoEnum.NFCE
     * @return TRetConsStatServ - objeto a mensagem de retorno da
     * transmissão.
     * @throws NfeException
     */
    public static TRetConsStatServ statusServico(ConfiguracoesNfe configuracoesNfe, DocumentoEnum tipoDocumento) throws NfeException {

        return Status.statusServico(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe), tipoDocumento);

    }

    /**
     * Classe Reponsavel Por Consultar o status da NFE na SEFAZ No tipo Informar
     *
     * @param chave
     * @param tipoDocumento USAR DocumentoEnum.NFE ou DocumentoEnum.NFCE
     * @return TRetConsSitNFe
     * @throws NfeException
     */
    public static TRetConsSitNFe consultaXml(ConfiguracoesNfe configuracoesNfe, String chave, DocumentoEnum tipoDocumento) throws NfeException {

        return ConsultaXml.consultaXml(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe), chave, tipoDocumento);

    }

    /**
     * Classe Reponsavel Por Consultar o cadastro do Cnpj/CPF na SEFAZ
     *
     * @param tipoPessoa Usar PessoaEnum.CNPJ ou PessoaEnum.CPF
     * @param cnpjCpf
     * @param estado
     * @return TRetConsCad
     * @throws NfeException
     */
    public static TRetConsCad consultaCadastro(ConfiguracoesNfe configuracoesNfe, PessoaEnum tipoPessoa, String cnpjCpf, EstadosEnum estado) throws NfeException {

        return ConsultaCadastro.consultaCadastro(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe), tipoPessoa, cnpjCpf, estado);

    }

    /**
     * Classe Reponsavel Por Consultar o retorno da NFE na SEFAZ No tipo Informar
     *
     * @param recibo
     * @param tipoDocumento USAR DocumentoEnum.NFE ou DocumentoEnum.NFCE
     * @return
     * @throws NfeException
     */
    public static TRetConsReciNFe consultaRecibo(ConfiguracoesNfe configuracoesNfe, String recibo, DocumentoEnum tipoDocumento) throws NfeException {
        return ConsultaRecibo.reciboNfe(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe), recibo, tipoDocumento);
    }

    /**
     * Classe Reponsavel Por Inutilizar a NFE na SEFAZ
     * No tipo Informar DocumentoEnum.NFE ou DocumentoEnum.NFCE
     *
     * @param tipoDocumento
     * @return
     * @throws NfeException
     */
    public static TRetInutNFe inutilizacao(ConfiguracoesNfe configuracoesNfe, TInutNFe inutNFe, DocumentoEnum tipoDocumento, boolean validar) throws NfeException {
        return Inutilizar.inutiliza(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, inutNFe.getInfInut().getCNPJ()), inutNFe, tipoDocumento, validar);
    }

    /**
     * Metodo para Montar a NFE
     *
     * @param enviNFe
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TEnviNFe montaNfe(ConfiguracoesNfe configuracoesNfe, TEnviNFe enviNFe, boolean valida) throws NfeException {

        return Enviar.montaNfe(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, enviNFe.getNFe().get(0).getInfNFe().getEmit().getCNPJ()), enviNFe, valida);

    }

    /**
     * Metodo para Enviar a NFE
     *
     * @param enviNFe
     * @param tipoDocumento No tipo Informar DocumentoEnum.NFE ou DocumentoEnum.NFCE
     * @return
     * @throws NfeException
     */
    public static TRetEnviNFe enviarNfe(ConfiguracoesNfe configuracoesNfe, TEnviNFe enviNFe, DocumentoEnum tipoDocumento) throws NfeException {

        return Enviar.enviaNfe(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, enviNFe.getNFe().get(0).getInfNFe().getEmit().getCNPJ()), enviNFe, tipoDocumento);

    }

    /**
     * Metodo para Cancelar a NFE
     * No tipo Informar DocumentoEnum.NFE ou DocumentoEnum.NFCE
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoCancelamento cancelarNfe(ConfiguracoesNfe configuracoesNfe, TEnvEventoCancelamento envEvento, boolean valida, DocumentoEnum tipoDocumento) throws NfeException {

        return Cancelar.eventoCancelamento(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, envEvento.getEvento().get(0).getInfEvento().getCNPJ()), envEvento, valida, tipoDocumento);

    }

    /**
     * Metodo para AtorInteressado da NFE
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoAtorInteressado atorInteressadoNFe(ConfiguracoesNfe configuracoesNfe, TEnvEventoAtorInteressado envEvento, boolean valida) throws NfeException {

        return AtorInteressado.eventoAtorInteressado(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, envEvento.getEvento().get(0).getInfEvento().getCNPJ()), envEvento, valida);

    }

    /**
     * Metodo para Enviar Evento Manual
     *
     * @param configuracoesNfe
     * @param xmlEvento
     * @param tipoEvento
     * @param valida
     * @param assina
     * @param tipoDocumento
     * @return
     * @throws NfeException
     */
    public static String enviarEnventoManual(ConfiguracoesNfe configuracoesNfe, String xmlEvento, ServicosEnum tipoEvento,
                                             boolean valida, boolean assina,
                                             DocumentoEnum tipoDocumento) throws NfeException {

        return Eventos.enviarEvento(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe), xmlEvento, tipoEvento, valida, assina, tipoDocumento);

    }

    /**
     * Metodo para Cancelar a NFCE em Substituicao
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoCancelamentoSubstituicao cancelarSubstituicaoNfe(ConfiguracoesNfe configuracoesNfe, TEnvEventoCancelamentoSubstituicao envEvento, boolean valida) throws NfeException {

        return Cancelar.eventoCancelamentoSubstituicao(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, envEvento.getEvento().get(0).getInfEvento().getCNPJ()), envEvento, valida);

    }

    /**
     * Metodo para Enviar o EPEC
     *
     * @param envEvento
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoEpec enviarEpec(ConfiguracoesNfe configuracoesNfe, TEnvEventoEpec envEvento, boolean valida) throws NfeException {

        return Epec.eventoEpec(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, envEvento.getEvento().get(0).getInfEvento().getCNPJ()), envEvento, valida);

    }

    /**
     * Metodo para Envio da Carta De Correção da NFE.
     *
     * @param evento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoCartaCorrecao cce(ConfiguracoesNfe configuracoesNfe, TEnvEventoCartaCorrecao evento, boolean valida) throws NfeException {
        return CartaCorrecao.eventoCCe(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, evento.getEvento().get(0).getInfEvento().getCNPJ()), evento, valida);
    }

    /**
     * Metodo para Manifestação da NFE.
     *
     * @param configuracoesNfe
     * @param evento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoManifestacao manifestacao(ConfiguracoesNfe configuracoesNfe, TEnvEventoManifestacao evento, boolean valida) throws NfeException {
        return ManifestacaoDestinatario.eventoManifestacao(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, evento.getEvento().get(0).getInfEvento().getCNPJ()), evento, valida);

    }

    /**
     * Metodo para Evento InsucessoEntrega da NFE
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoInsucessoEntrega insucessoEntrega(ConfiguracoesNfe configuracoesNfe,
                                                                 TEnvEventoInsucessoEntrega envEvento,
                                                                 boolean valida) throws NfeException {

        return InsucessoEntrega.eventoInsuccessoEntrega(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, envEvento.getEvento().get(0).getInfEvento().getCNPJ()),
                envEvento, valida);

    }

    /**
     * Metodo para Evento CancInsucessoEntrega da NFE
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoCancelamentoInsucessoEntrega cancelamentoInsucessoEntrega(ConfiguracoesNfe configuracoesNfe,
                                                                                         TEnvEventoCancelamentoInsucessoEntrega envEvento,
                                                                                         boolean valida) throws NfeException {

        return CancInsucessoEntrega.eventoCancInsuccessoEntrega(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe,
                        envEvento.getEvento().get(0).getInfEvento().getCNPJ()),
                envEvento, valida);

    }

    /**
     * Metodo para Evento ECONF da NFE
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoConciliacaoFinanceira econf(ConfiguracoesNfe configuracoesNfe,
                                                           TEnvEventoConciliacaoFinanceira envEvento,
                                                           DocumentoEnum documento,
                                                           boolean valida) throws NfeException {

        return ConciliacaoFinanceira.eventoEConf(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe, envEvento.getEvento().get(0).getInfEvento().getCNPJ()),
                envEvento, documento, valida);

    }

    /**
     * Metodo para Evento CancEConf da NFE
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoCancelamentoConciliacaoFinanceira cancelamentoEconf(ConfiguracoesNfe configuracoesNfe,
                                                                                   TEnvEventoCancelamentoConciliacaoFinanceira envEvento,
                                                                                   boolean valida) throws NfeException {

        return CancConciliacaoFinanceira.eventoEConf(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe,
                        envEvento.getEvento().get(0).getInfEvento().getCNPJ()),
                envEvento, valida);

    }

    /**
     * Metodo para Evento Generico da NFE
     *
     * @param envEvento
     * @param valida
     * @return
     * @throws NfeException
     */
    public static TRetEnvEventoGenerico eventoGenerico(ConfiguracoesNfe configuracoesNfe,
                                                       TEnvEventoGenerico envEvento,
                                                       boolean valida) throws NfeException {

        return EventoGenerico.evento(ConfiguracoesUtil.iniciaConfiguracoes(configuracoesNfe),
                envEvento, valida);

    }

}
