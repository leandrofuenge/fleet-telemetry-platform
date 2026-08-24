package com.telemetria.integration.nfe;

import java.util.function.UnaryOperator;

import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enums.DocumentoEnum;
import com.telemetria.integration.nfe.dom.enums.ServicosEnum;
import com.telemetria.integration.nfe.exception.ExcecaoNfe;
import com.telemetria.integration.nfe.util.XmlNfeUtil;

import jakarta.xml.bind.JAXBException;

/** Centraliza o fluxo comum de envio e conversão de eventos da NF-e. */
final class EventoNfeSender {

    private static final String XMLDSIG_NAMESPACE =
            " xmlns:ns2=\"http://www.w3.org/2000/09/xmldsig#\"";
    private static final String EVENTO_NAMESPACE =
            "<evento xmlns=\"http://www.portalfiscal.inf.br/nfe\" v";

    private EventoNfeSender() {
    }

    static <T> T enviar(
            ConfiguracoesNfe config,
            Object evento,
            Class<T> tipoRetorno,
            ServicosEnum servico,
            DocumentoEnum documento,
            boolean validar) throws ExcecaoNfe {

        return enviar(config, evento, tipoRetorno, servico, documento, validar, UnaryOperator.identity());
    }

    static <T> T enviar(
            ConfiguracoesNfe config,
            Object evento,
            Class<T> tipoRetorno,
            ServicosEnum servico,
            DocumentoEnum documento,
            boolean validar,
            UnaryOperator<String> normalizadorAdicional) throws ExcecaoNfe {

        try {
            String xml = XmlNfeUtil.objectToXml(evento, config.getEncode());
            xml = normalizarXml(xml);
            xml = normalizadorAdicional.apply(xml);

            String xmlRetorno = Eventos.enviarEvento(
                    config, xml, servico, validar, true, documento);

            if (xmlRetorno == null || xmlRetorno.isBlank()) {
                throw new ExcecaoNfe("A SEFAZ não retornou um XML para o evento " + servico + ".");
            }

            return XmlNfeUtil.xmlToObject(xmlRetorno, tipoRetorno);
        } catch (JAXBException e) {
            throw new ExcecaoNfe(e.getMessage(), e);
        }
    }

    private static String normalizarXml(String xml) throws ExcecaoNfe {
        if (xml == null || xml.isBlank()) {
            throw new ExcecaoNfe("Não foi possível gerar o XML do evento.");
        }

        String xmlNormalizado = xml.replace(XMLDSIG_NAMESPACE, "")
                .replace("<evento v", EVENTO_NAMESPACE);

        if (!xmlNormalizado.contains("<evento")) {
            throw new ExcecaoNfe("XML do evento não contém o elemento <evento>.");
        }

        return xmlNormalizado;
    }
}
