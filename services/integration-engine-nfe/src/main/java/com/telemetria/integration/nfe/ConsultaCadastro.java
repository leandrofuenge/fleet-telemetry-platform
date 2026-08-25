package com.telemetria.integration.nfe;

import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.util.AXIOMUtil;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.kernel.http.HTTPConstants;

import com.telemetria.integration.nfe.codigo.gerado.schemas.TConsCad;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TRetConsCad;
import com.telemetria.integration.nfe.codigo.gerado.schemas.TUfCons;
import com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.CadConsultaCadastro4Stub;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.EstadosEnum;
import com.telemetria.integration.nfe.dom.enums.PessoaEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.util.ConstantesUtil;
import com.telemetria.integration.nfe.util.UtilitarioClienteAxis2;
import com.telemetria.integration.nfe.util.UtilitarioServicoWeb;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import br.com.swconsultoria.certificado.exception.CertificadoException;

import jakarta.xml.bind.JAXBException;

/**
 * Classe responsável por consultar o cadastro do contribuinte na SEFAZ.
 */
class ConsultaCadastro {

    private static final Logger log = Logger.getLogger(ConsultaCadastro.class.getName());
    private static final int QUANTIDADE_DIGITOS_VISIVEIS = 4;

    private ConsultaCadastro() {
    }

    /**
     * Consulta o cadastro de um contribuinte na SEFAZ.
     *
     * @param config configurações da NFe
     * @param tipoPessoa tipo da pessoa: física ou jurídica
     * @param cnpjCpf CPF ou CNPJ do contribuinte
     * @param estado estado da SEFAZ
     * @return retorno da consulta de cadastro
     * @throws ExcecaoNfe caso ocorra algum erro na validação,
     *                   comunicação ou processamento da resposta
     */
    static TRetConsCad consultaCadastro(
            ConfiguracoesNfe config,
            PessoaEnum tipoPessoa,
            String cnpjCpf,
            EstadosEnum estado) throws ExcecaoNfe {

        try {

            // ============================================================
            // 1. VALIDAÇÃO DOS PARÂMETROS
            // ============================================================

            validarParametros(config, tipoPessoa, cnpjCpf, estado);

            // ============================================================
            // 2. MONTA O OBJETO DE CONSULTA
            // ============================================================

            TConsCad consCad = new TConsCad();

            consCad.setVersao(
                    ConstantesUtil.VERSAO.CONSULTA_CADASTRO
            );

            TConsCad.InfCons infCons = new TConsCad.InfCons();

            if (PessoaEnum.JURIDICA.equals(tipoPessoa)) {
                infCons.setCNPJ(cnpjCpf);
            } else {
                infCons.setCPF(cnpjCpf);
            }

            infCons.setXServ("CONS-CAD");

            infCons.setUF(
                    TUfCons.valueOf(estado.toString())
            );

            consCad.setInfCons(infCons);

            // ============================================================
            // 3. CONVERTE O OBJETO PARA XML
            // ============================================================

            String xml = XmlNfeUtil.objectToXml(
                    consCad,
                    config.getEncode()
            );

            log.info("[XML-ENVIO]: " + xml);

            // ============================================================
            // 4. CONVERTE XML PARA OMElement
            // ============================================================

            OMElement ome = validarEConverterXmlEnvio(xml);

            // ============================================================
            // 5. CONFIGURAÇÃO ESPECÍFICA DA CONSULTA
            // ============================================================

            ConfiguracoesNfe configConsulta = new ConfiguracoesNfe();

            configConsulta.setContigenciaSVC(
                    config.isContigenciaSVC()
            );

            configConsulta.setEstado(estado);

            configConsulta.setAmbiente(
                    config.getAmbiente()
            );

            // ============================================================
            // 6. OBTÉM A URL DO WEBSERVICE
            // ============================================================

            String url = UtilitarioServicoWeb.getUrl(
                    configConsulta,
                    DocumentoEnum.NFE,
                    ServicosEnum.CONSULTA_CADASTRO
            );

            validarUrl(url, estado);

            // ============================================================
            // 7. CONSULTA ESPECÍFICA POR ESTADO
            // ============================================================

            if (EstadosEnum.MS.equals(estado)) {
                return consultarMatoGrossoDoSul(config, url, ome, estado);
            }
            if (EstadosEnum.MT.equals(estado)) {
                return consultarMatoGrosso(config, url, ome, estado);
            }
            return consultarDemaisEstados(config, url, ome, estado);

        } catch (RemoteException
                 | XMLStreamException
                 | JAXBException
                 | CertificadoException e) {

            log.log(
                    Level.SEVERE,
                    "Erro ao consultar cadastro na SEFAZ. "
                            + "Estado: " + estado
                            + ", Documento: " + mascararDocumento(cnpjCpf),
                    e
            );

            throw new ExcecaoNfe(
                    "Erro ao consultar cadastro na SEFAZ para o estado "
                            + estado + ".",
                    e
            );

        } catch (ExcecaoNfe e) {

            log.log(
                    Level.WARNING,
                    "Falha na consulta de cadastro. "
                            + "Estado: " + estado
                            + ", Documento: " + mascararDocumento(cnpjCpf)
                            + ", Motivo: " + e.getMessage()
            );

            throw e;
        }
    }

    /**
     * Consulta específica para Mato Grosso do Sul.
     */
    private static TRetConsCad consultarMatoGrossoDoSul(
            ConfiguracoesNfe config,
            String url,
            OMElement ome,
            EstadosEnum estado)
            throws RemoteException, JAXBException, ExcecaoNfe, CertificadoException {

        com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub.NfeDadosMsg dadosMsg =
                new com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub.NfeDadosMsg();

        dadosMsg.setExtraElement(ome);

        com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub stub =
                new com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub(url);

        UtilitarioClienteAxis2.configuraHttpClient(
                stub,
                config,
                url
        );

        configurarTimeout(
                stub._getServiceClient(),
                config
        );

        com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub.NfeResultMsg result =
                stub.consultaCadastro(dadosMsg);

        String xmlRetorno = validarRetornoMS(
                result,
                estado
        );

        log.info("[XML-RETORNO]: " + xmlRetorno);

        return converterRetorno(xmlRetorno, estado);
    }

    /**
     * Consulta específica para Mato Grosso.
     */
    private static TRetConsCad consultarMatoGrosso(
            ConfiguracoesNfe config,
            String url,
            OMElement ome,
            EstadosEnum estado)
            throws RemoteException, JAXBException, ExcecaoNfe, CertificadoException {

        com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.ConsultaCadastro consultaCadastro =
                new com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.ConsultaCadastro();

        com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.NfeDadosMsg_type0 dadosMsg =
                new com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.NfeDadosMsg_type0();

        dadosMsg.setExtraElement(ome);

        consultaCadastro.setNfeDadosMsg(dadosMsg);

        com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub stub =
                new com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub(url);

        UtilitarioClienteAxis2.configuraHttpClient(
                stub,
                config,
                url
        );

        configurarTimeout(
                stub._getServiceClient(),
                config
        );

        com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.NfeResultMsg result =
                stub.consultaCadastro(consultaCadastro);

        String xmlRetorno = validarRetornoMT(
                result,
                estado
        );

        log.info("[XML-RETORNO]: " + xmlRetorno);

        return converterRetorno(xmlRetorno, estado);
    }

    /**
     * Consulta utilizada pelos demais estados.
     */
    private static TRetConsCad consultarDemaisEstados(
            ConfiguracoesNfe config,
            String url,
            OMElement ome,
            EstadosEnum estado)
            throws RemoteException, JAXBException, ExcecaoNfe, CertificadoException {

        CadConsultaCadastro4Stub.NfeDadosMsg dadosMsg =
                new CadConsultaCadastro4Stub.NfeDadosMsg();

        dadosMsg.setExtraElement(ome);

        CadConsultaCadastro4Stub stub =
                new CadConsultaCadastro4Stub(url);

        UtilitarioClienteAxis2.configuraHttpClient(
                stub,
                config,
                url
        );

        configurarTimeout(
                stub._getServiceClient(),
                config
        );

        CadConsultaCadastro4Stub.NfeResultMsg result =
                stub.consultaCadastro(dadosMsg);

        String xmlRetorno = validarRetornoPadrao(
                result,
                estado
        );

        log.info("[XML-RETORNO]: " + xmlRetorno);

        return converterRetorno(xmlRetorno, estado);
    }

    /**
     * Configura os timeouts de conexão e leitura do cliente Axis2.
     *
     * O timeout é aplicado somente quando:
     * - o ServiceClient está disponível;
     * - as configurações estão disponíveis;
     * - o timeout foi informado;
     * - o timeout possui um valor válido.
     *
     * @param serviceClient cliente Axis2
     * @param config configurações da NFe
     */
    private static void configurarTimeout(
            ServiceClient serviceClient,
            ConfiguracoesNfe config) {

        if (serviceClient == null) {
            log.warning(
                    "Não foi possível configurar timeout: ServiceClient nulo."
            );
            return;
        }

        if (config == null) {
            log.warning(
                    "Não foi possível configurar timeout: ConfiguracoesNfe nula."
            );
            return;
        }

        Integer timeout = config.getTimeout();

        if (timeout == null) {
            log.fine(
                    "Timeout não configurado. "
                            + "Será utilizado o timeout padrão do Axis2."
            );
            return;
        }

        if (timeout <= 0) {
            log.warning(
                    "Timeout inválido: " + timeout
                            + ". Será utilizado o timeout padrão do Axis2."
            );
            return;
        }

        try {

            serviceClient
                    .getOptions()
                    .setProperty(
                            HTTPConstants.SO_TIMEOUT,
                            timeout
                    );

            serviceClient
                    .getOptions()
                    .setProperty(
                            HTTPConstants.CONNECTION_TIMEOUT,
                            timeout
                    );

            log.fine(
                    "Timeout configurado com sucesso: "
                            + timeout + " ms."
            );

        } catch (RuntimeException e) {

            log.log(
                    Level.WARNING,
                    "Não foi possível configurar o timeout "
                            + "do cliente Axis2.",
                    e
            );
        }
    }

    /**
     * Valida as configurações obrigatórias da NFe.
     *
     * @param config configurações da NFe
     * @throws ExcecaoNfe caso a configuração seja nula
     */
    private static void validarConfiguracao(
            ConfiguracoesNfe config)
            throws ExcecaoNfe {

        if (config == null) {
            throw new ExcecaoNfe(
                    "As configurações da NFe não foram informadas."
            );
        }
    }

    /**
     * Valida os parâmetros obrigatórios da consulta de cadastro.
     *
     * @param config configurações da NFe
     * @param tipoPessoa tipo da pessoa
     * @param cnpjCpf CPF ou CNPJ
     * @param estado estado da SEFAZ
     * @throws ExcecaoNfe caso algum parâmetro seja inválido
     */
    private static void validarParametros(
            ConfiguracoesNfe config,
            PessoaEnum tipoPessoa,
            String cnpjCpf,
            EstadosEnum estado)
            throws ExcecaoNfe {

        validarConfiguracao(config);

        if (tipoPessoa == null) {
            throw new ExcecaoNfe(
                    "Não foi informado o tipo de pessoa para a "
                            + "consulta de cadastro."
            );
        }

        if (cnpjCpf == null || cnpjCpf.isBlank()) {
            throw new ExcecaoNfe(
                    "Não foi informado o CPF/CNPJ para a "
                            + "consulta de cadastro."
            );
        }

        if (estado == null) {
            throw new ExcecaoNfe(
                    "Não foi informado o estado da SEFAZ "
                            + "para a consulta de cadastro."
            );
        }

        validarDocumento(cnpjCpf, tipoPessoa);
    }

    /**
     * Valida CPF ou CNPJ de acordo com o tipo de pessoa.
     *
     * A validação contempla:
     * - remoção de máscara;
     * - quantidade de dígitos;
     * - rejeição de sequências repetidas;
     * - validação matemática dos dígitos verificadores.
     *
     * @param documento CPF ou CNPJ
     * @param tipoPessoa tipo da pessoa
     * @throws ExcecaoNfe caso o documento seja inválido
     */
    private static void validarDocumento(
            String documento,
            PessoaEnum tipoPessoa)
            throws ExcecaoNfe {

        String documentoLimpo = documento.replaceAll("\\D", "");

        if (PessoaEnum.JURIDICA.equals(tipoPessoa)) {

            if (documentoLimpo.length() != 14) {
                throw new ExcecaoNfe(
                        "CNPJ inválido. O CNPJ deve possuir 14 dígitos."
                );
            }

            if (!validarCnpj(documentoLimpo)) {
                throw new ExcecaoNfe(
                        "CNPJ inválido. Os dígitos verificadores não conferem."
                );
            }

        } else {

            if (documentoLimpo.length() != 11) {
                throw new ExcecaoNfe(
                        "CPF inválido. O CPF deve possuir 11 dígitos."
                );
            }

            if (!validarCpf(documentoLimpo)) {
                throw new ExcecaoNfe(
                        "CPF inválido. Os dígitos verificadores não conferem."
                );
            }
        }
    }

    /**
     * Valida matematicamente um CPF.
     *
     * @param cpf CPF contendo somente números
     * @return true se o CPF for válido
     */
    private static boolean validarCpf(String cpf) {

        // Rejeita CPFs como:
        // 00000000000
        // 11111111111
        // 22222222222
        // etc.
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        int primeiroDigito = calcularDigitoCpf(
                cpf.substring(0, 9)
        );

        int segundoDigito = calcularDigitoCpf(
                cpf.substring(0, 9) + primeiroDigito
        );

        return primeiroDigito == Character.digit(cpf.charAt(9), 10)
                && segundoDigito == Character.digit(cpf.charAt(10), 10);
    }

    /**
     * Calcula um dígito verificador do CPF.
     *
     * @param cpfParcial CPF com 9 ou 10 dígitos
     * @return dígito verificador calculado
     */
    private static int calcularDigitoCpf(String cpfParcial) {

        int soma = 0;
        int tamanho = cpfParcial.length();

        for (int i = 0; i < tamanho; i++) {

            int numero = Character.digit(
                    cpfParcial.charAt(i),
                    10
            );

            int peso = tamanho + 1 - i;

            soma += numero * peso;
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Valida matematicamente um CNPJ.
     *
     * @param cnpj CNPJ contendo somente números
     * @return true se o CNPJ for válido
     */
    private static boolean validarCnpj(String cnpj) {

        // Rejeita CNPJs como:
        // 00000000000000
        // 11111111111111
        // etc.
        if (cnpj.matches("(\\d)\\1{13}")) {
            return false;
        }

        int primeiroDigito = calcularPrimeiroDigitoCnpj(cnpj);

        int segundoDigito = calcularSegundoDigitoCnpj(
                cnpj,
                primeiroDigito
        );

        return primeiroDigito == Character.digit(cnpj.charAt(12), 10)
                && segundoDigito == Character.digit(cnpj.charAt(13), 10);
    }

    /**
     * Calcula o primeiro dígito verificador do CNPJ.
     */
    private static int calcularPrimeiroDigitoCnpj(String cnpj) {

        int[] pesos = {
                5, 4, 3, 2,
                9, 8, 7, 6,
                5, 4, 3, 2
        };

        int soma = 0;

        for (int i = 0; i < 12; i++) {

            int numero = Character.digit(
                    cnpj.charAt(i),
                    10
            );

            soma += numero * pesos[i];
        }

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Calcula o segundo dígito verificador do CNPJ.
     */
    private static int calcularSegundoDigitoCnpj(
            String cnpj,
            int primeiroDigito) {

        int[] pesos = {
                6, 5, 4, 3, 2,
                9, 8, 7, 6, 5, 4, 3, 2
        };

        int soma = 0;

        for (int i = 0; i < 12; i++) {

            int numero = Character.digit(
                    cnpj.charAt(i),
                    10
            );

            soma += numero * pesos[i];
        }

        soma += primeiroDigito * pesos[12];

        int resto = soma % 11;

        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Valida e converte o XML gerado para OMElement.
     *
     * @param xml XML da consulta
     * @return OMElement correspondente ao XML
     * @throws ExcecaoNfe caso o XML seja inválido
     * @throws XMLStreamException caso o XML esteja malformado
     */
    private static OMElement validarEConverterXmlEnvio(
            String xml)
            throws ExcecaoNfe, XMLStreamException {

        if (xml == null) {
            throw new ExcecaoNfe(
                    "Não foi possível gerar o XML da consulta de cadastro: "
                            + "XML nulo."
            );
        }

        String xmlNormalizado = xml.trim();

        if (xmlNormalizado.isEmpty()) {
            throw new ExcecaoNfe(
                    "Não foi possível gerar o XML da consulta de cadastro: "
                            + "XML vazio."
            );
        }

        try {

            OMElement elementoRaiz =
                    AXIOMUtil.stringToOM(xmlNormalizado);

            if (elementoRaiz == null) {
                throw new ExcecaoNfe(
                        "Não foi possível gerar o XML da consulta de cadastro: "
                                + "elemento raiz não encontrado."
                );
            }

            return elementoRaiz;

        } catch (XMLStreamException e) {

            log.log(
                    Level.WARNING,
                    "XML da consulta de cadastro está malformado.",
                    e
            );

            throw new ExcecaoNfe(
                    "O XML da consulta de cadastro está malformado.",
                    e
            );
        }
    }

    /**
     * Valida a URL do WebService da SEFAZ.
     *
     * @param url URL do WebService
     * @param estado estado da SEFAZ
     * @throws ExcecaoNfe caso a URL seja inválida
     */
    private static void validarUrl(
            String url,
            EstadosEnum estado)
            throws ExcecaoNfe {

        if (estado == null) {
            throw new ExcecaoNfe(
                    "Não foi possível validar a URL do WebService: "
                            + "estado não informado."
            );
        }

        if (url == null || url.isBlank()) {
            throw new ExcecaoNfe(
                    "URL do WebService de Consulta de Cadastro "
                            + "não encontrada para o estado "
                            + estado + "."
            );
        }

        String urlNormalizada = url.trim();

        try {

            java.net.URI uri =
                    java.net.URI.create(urlNormalizada);

            String scheme = uri.getScheme();

            if (scheme == null
                    || (!"http".equalsIgnoreCase(scheme)
                    && !"https".equalsIgnoreCase(scheme))) {

                throw new ExcecaoNfe(
                        "URL do WebService inválida para o estado "
                                + estado
                                + ". O protocolo deve ser HTTP ou HTTPS."
                );
            }

            if (uri.getHost() == null
                    || uri.getHost().isBlank()) {

                throw new ExcecaoNfe(
                        "URL do WebService inválida para o estado "
                                + estado
                                + ". Host não informado."
                );
            }

        } catch (IllegalArgumentException e) {

            throw new ExcecaoNfe(
                    "URL do WebService inválida para o estado "
                            + estado + ".",
                    e
            );
        }
    }

    /**
     * Valida o retorno específico de MS.
     */
    private static String validarRetornoMS(
            com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.ms.CadConsultaCadastro4Stub.NfeResultMsg result,
            EstadosEnum estado)
            throws ExcecaoNfe {

        if (result == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ não retornou uma resposta para o estado "
                            + estado + "."
            );
        }

        if (result.getExtraElement() == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ retornou uma resposta vazia para o estado "
                            + estado + "."
            );
        }

        String xmlRetorno =
                result.getExtraElement().toString();

        validarXmlRetorno(xmlRetorno, estado);

        return xmlRetorno;
    }

    /**
     * Valida o retorno específico de MT.
     */
    private static String validarRetornoMT(
            com.telemetria.integration.nfe.codigo.gerado.wsdl.CadConsultaCadastro.rs.CadConsultaCadastro4Stub.NfeResultMsg result,
            EstadosEnum estado)
            throws ExcecaoNfe {

        if (result == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ não retornou uma resposta para o estado "
                            + estado + "."
            );
        }

        if (result.getConsultaCadastroResult() == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ retornou uma resposta vazia para o estado "
                            + estado + "."
            );
        }

        if (result.getConsultaCadastroResult().getExtraElement() == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ retornou um XML vazio para o estado "
                            + estado + "."
            );
        }

        String xmlRetorno =
                result.getConsultaCadastroResult()
                        .getExtraElement()
                        .toString();

        validarXmlRetorno(xmlRetorno, estado);

        return xmlRetorno;
    }

    /**
     * Valida o retorno dos demais estados.
     */
    private static String validarRetornoPadrao(
            CadConsultaCadastro4Stub.NfeResultMsg result,
            EstadosEnum estado)
            throws ExcecaoNfe {

        if (result == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ não retornou uma resposta para o estado "
                            + estado + "."
            );
        }

        if (result.getExtraElement() == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ retornou uma resposta vazia para o estado "
                            + estado + "."
            );
        }

        String xmlRetorno =
                result.getExtraElement().toString();

        validarXmlRetorno(xmlRetorno, estado);

        return xmlRetorno;
    }

    /**
     * Valida o XML retornado pela SEFAZ.
     *
     * @param xmlRetorno XML retornado pelo WebService
     * @param estado estado responsável pela consulta
     * @throws ExcecaoNfe caso o retorno seja inválido
     */
    private static void validarXmlRetorno(
            String xmlRetorno,
            EstadosEnum estado)
            throws ExcecaoNfe {

        if (estado == null) {
            throw new ExcecaoNfe(
                    "Não foi possível validar o retorno da SEFAZ: "
                            + "estado não informado."
            );
        }

        if (xmlRetorno == null) {
            throw new ExcecaoNfe(
                    "A SEFAZ não retornou nenhum XML para o estado "
                            + estado + "."
            );
        }

        String xmlNormalizado = xmlRetorno.trim();

        if (xmlNormalizado.isEmpty()) {
            throw new ExcecaoNfe(
                    "A SEFAZ retornou um XML vazio para o estado "
                            + estado + "."
            );
        }

        if (!xmlNormalizado.startsWith("<")) {
            throw new ExcecaoNfe(
                    "A SEFAZ retornou um conteúdo inválido para o estado "
                            + estado + ". O retorno não possui formato XML."
            );
        }

        try {

            OMElement elementoRaiz =
                    AXIOMUtil.stringToOM(xmlNormalizado);

            if (elementoRaiz == null) {
                throw new ExcecaoNfe(
                        "A SEFAZ retornou um XML inválido para o estado "
                                + estado + ". Elemento raiz não encontrado."
                );
            }

        } catch (XMLStreamException e) {

            log.log(
                    Level.WARNING,
                    "XML de retorno inválido ou malformado. Estado: " + estado,
                    e
            );

            throw new ExcecaoNfe(
                    "A SEFAZ retornou um XML malformado para o estado "
                            + estado + ".",
                    e
            );
        }
    }

    /**
     * Converte o XML de retorno para o objeto TRetConsCad.
     */
    private static TRetConsCad converterRetorno(
            String xmlRetorno,
            EstadosEnum estado)
            throws JAXBException, ExcecaoNfe {

        TRetConsCad retorno = XmlNfeUtil.xmlToObject(xmlRetorno, TRetConsCad.class);
        if (retorno == null) {
            throw new ExcecaoNfe(
                    "Não foi possível converter o retorno da SEFAZ para TRetConsCad. Estado: " + estado
            );
        }
        return retorno;
    }

    /**
     * Mascara CPF/CNPJ para evitar exposição do documento completo nos logs.
     *
     * Mantém somente os últimos 4 dígitos do documento.
     *
     * Exemplos:
     * CPF 12345678901  -> ********8901
     * CNPJ 12345678000199 -> ********0199
     *
     * @param documento CPF ou CNPJ
     * @return documento mascarado
     */
    private static String mascararDocumento(
            String documento) {

        if (documento == null || documento.isBlank()) {
            return "NÃO INFORMADO";
        }

        String somenteNumeros =
                documento.replaceAll("\\D", "");

        if (somenteNumeros.isEmpty()) {
            return "********";
        }

        if (somenteNumeros.length() <= QUANTIDADE_DIGITOS_VISIVEIS) {
            return "********";
        }

        String ultimosDigitos =
                somenteNumeros.substring(
                        somenteNumeros.length()
                                - QUANTIDADE_DIGITOS_VISIVEIS
                );

        return "********" + ultimosDigitos;
    }
}