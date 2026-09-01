package com.telemetria.integration.nfe.soap;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Responsável pela criação de envelopes SOAP 1.2 utilizados
 * na comunicação com os serviços da SEFAZ NF-e.
 *
 * O XML fiscal é inserido dentro do elemento nfeDadosMsg.
 *
 * A declaração XML do documento fiscal é removida porque o XML
 * fiscal passa a fazer parte de outro documento XML (o envelope SOAP).
 */
@Component
public class NfeSoapEnvelopeFactory {

    private static final String SOAP_NAMESPACE =
            "http://www.w3.org/2003/05/soap-envelope";

    private static final String XML_DECLARATION_REGEX =
            "^\\uFEFF?\\s*<\\?xml\\s+version\\s*=\\s*[\"'][^\"']+[\"']"
                    + "(?:\\s+encoding\\s*=\\s*[\"'][^\"']+[\"'])?"
                    + "(?:\\s+standalone\\s*=\\s*[\"'][^\"']+[\"'])?"
                    + "\\s*\\?>";

    private static final Pattern XML_DECLARATION =
            Pattern.compile(
                    XML_DECLARATION_REGEX,
                    Pattern.CASE_INSENSITIVE);

    public String criar(
            NfeSoapService service,
            String xmlFiscal) {

        validarParametros(
                service,
                xmlFiscal);

        String conteudoFiscal =
                removerDeclaracaoXml(xmlFiscal);

        validarConteudoFiscal(
                conteudoFiscal);

        return construirEnvelope(
                service,
                conteudoFiscal);
    }

    /**
     * Remove a declaração XML do documento fiscal.
     *
     * Exemplo:
     *
     * <?xml version="1.0" encoding="UTF-8"?>
     * <NFe>...</NFe>
     *
     * torna-se:
     *
     * <NFe>...</NFe>
     */
    private String removerDeclaracaoXml(
            String xmlFiscal) {

        String conteudo =
                xmlFiscal.strip();

        return XML_DECLARATION
                .matcher(conteudo)
                .replaceFirst("")
                .strip();
    }

    /**
     * Monta o envelope SOAP 1.2.
     */
    private String construirEnvelope(
            NfeSoapService service,
            String conteudoFiscal) {

        String namespace =
                service.namespace();

        StringBuilder envelope =
                new StringBuilder(
                        conteudoFiscal.length() + 300);

        envelope.append(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");

        envelope.append(
                "<soap12:Envelope")
                .append(" xmlns:soap12=\"")
                .append(SOAP_NAMESPACE)
                .append("\">");

        envelope.append(
                "<soap12:Body>");

        if (service.requisicaoEncapsuladaPeloMetodo()) {
            envelope.append('<')
                    .append(service.metodo())
                    .append(" xmlns=\"")
                    .append(namespace)
                    .append("\">");
        }

        envelope.append(
                "<nfeDadosMsg");

        if (!service.requisicaoEncapsuladaPeloMetodo()) {
            envelope.append(" xmlns=\"")
                    .append(namespace)
                    .append('"');
        }

        envelope.append('>');

        envelope.append(
                conteudoFiscal);

        envelope.append(
                "</nfeDadosMsg>");

        if (service.requisicaoEncapsuladaPeloMetodo()) {
            envelope.append("</")
                    .append(service.metodo())
                    .append('>');
        }

        envelope.append(
                "</soap12:Body>");

        envelope.append(
                "</soap12:Envelope>");

        return envelope.toString();
    }

    private void validarParametros(
            NfeSoapService service,
            String xmlFiscal) {

        if (service == null) {

            throw new IllegalArgumentException(
                    "O serviço SOAP NF-e é obrigatório.");
        }

        if (xmlFiscal == null
                || xmlFiscal.isBlank()) {

            throw new IllegalArgumentException(
                    "O XML fiscal NF-e é obrigatório.");
        }
    }

    /**
     * Valida se existe conteúdo XML após a remoção
     * da declaração XML.
     */
    private void validarConteudoFiscal(
            String conteudoFiscal) {

        if (conteudoFiscal == null
                || conteudoFiscal.isBlank()) {

            throw new IllegalArgumentException(
                    "O conteúdo XML fiscal NF-e não pode ser vazio.");
        }

        if (!conteudoFiscal.startsWith("<")
                || !conteudoFiscal.endsWith(">")) {

            throw new IllegalArgumentException(
                    "O XML fiscal NF-e possui formato inválido.");
        }
    }
}
