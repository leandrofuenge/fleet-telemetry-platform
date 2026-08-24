package com.telemetria.integration.nfe.util;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;

import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;

/**
 * Classe Responsavel Por Carregar as informações do Certificado Digital
 * 
 * 
 */
public class ConfiguracoesUtil {
    
     /**
     * Recebe como parâmetro um objeto ConfiguracoesNfe e Inicializa as COnfigurações e retorna um objeto
     * ConfiguracoesNfe.
     * 
     * <p>
     * Semelhante ao método iniciaConfiguracoes(), o Certificado Digital será 
     * validado e inicializado.Caso ocorrá algum prolema será disparado um 
     * ExcecaoNfe
     * </p>
     * 
     * @param configuracoesNfe
     * @return ConfiguracoesWebNfe
     * @throws ExcecaoNfe
     * @see CertificadoException
     * @see ConfiguracoesWebNfe
     */
    public static ConfiguracoesNfe iniciaConfiguracoes(ConfiguracoesNfe configuracoesNfe) throws ExcecaoNfe {


        return iniciaConfiguracoes(configuracoesNfe, null);
    }

     /**
     * Recebe como parâmetro um objeto ConfiguracoesNfe e Inicializa as COnfigurações e retorna um objeto
     * ConfiguracoesNfe.
     *
     * <p>
     * Semelhante ao método iniciaConfiguracoes(), o Certificado Digital será
     * validado e inicializado.Caso ocorrá algum prolema será disparado um
     * ExcecaoNfe
     * </p>
     *
     * @param configuracoesNfe
     * @param cpfCnpj
     * @return ConfiguracoesWebNfe
     * @throws ExcecaoNfe
     * @see CertificadoException
     * @see ConfiguracoesWebNfe
     */
    public static ConfiguracoesNfe iniciaConfiguracoes(ConfiguracoesNfe configuracoesNfe, String cpfCnpj) throws ExcecaoNfe {

        ObjetoUtil.verifica(configuracoesNfe).orElseThrow( () -> new ExcecaoNfe("Configurações não foram criadas"));

        try {
            if (!configuracoesNfe.getCertificado().isValido()) {
                throw new CertificadoException("Certificado vencido ou inválido.");
            }

            if (configuracoesNfe.isValidacaoDocumento() && cpfCnpj != null && !configuracoesNfe.getCertificado().getCnpjCpf().substring(0,8).equals(cpfCnpj.substring(0,8))) {
                throw new CertificadoException("Documento do Certificado("+configuracoesNfe.getCertificado().getCnpjCpf()+") não equivale ao Documento do Emissor("+cpfCnpj+")");
            }

            if( ObjetoUtil.verifica(configuracoesNfe.getCacert()).isPresent()){
                CertificadoService.inicializaCertificado(configuracoesNfe.getCertificado(),configuracoesNfe.getCacert());
            }else{
                CertificadoService.inicializaCertificado(configuracoesNfe.getCertificado());
            }
        } catch (CertificadoException e) {
            throw new ExcecaoNfe(e.getMessage(),e);
        }

        return configuracoesNfe;
    }

}
