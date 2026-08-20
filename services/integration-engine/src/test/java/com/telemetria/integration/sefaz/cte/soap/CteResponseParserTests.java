package com.telemetria.integration.sefaz.cte.soap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.telemetria.integration.sefaz.cte.retorno.CteResultadoCategoria;
import com.telemetria.integration.sefaz.cte.retorno.CteSoapFaultException;

class CteResponseParserTests {

    private final CteResponseParser parser = new CteResponseParser();

    @Test
    void deveInterpretarAutorizacaoPeloInfProt() {
        var result = parser.parseAutorizacao(envelope("""
                <retCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <tpAmb>2</tpAmb><cUF>51</cUF><verAplic>1.0</verAplic><cStat>104</cStat><xMotivo>Lote processado</xMotivo>
                  <protCTe><infProt><tpAmb>2</tpAmb><verAplic>1.0</verAplic><chCTe>CHAVE</chCTe>
                    <dhRecbto>2026-08-19T10:00:00-04:00</dhRecbto><nProt>151260000000001</nProt>
                    <digVal>DIGEST</digVal><cStat>100</cStat><xMotivo>Autorizado o uso do CT-e</xMotivo>
                  </infProt></protCTe>
                </retCTe>
                """));

        assertEquals(100, result.codigo());
        assertEquals("151260000000001", result.protocolo());
        assertTrue(result.autorizado());
    }

    @Test
    void deveInterpretarRejeicaoDeAutorizacao() {
        var result = parser.parseAutorizacao(envelope("""
                <retCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <cStat>225</cStat><xMotivo>Rejeição: Falha no Schema XML</xMotivo>
                </retCTe>
                """));

        assertEquals(225, result.codigo());
        assertFalse(result.autorizado());
        assertEquals(CteResultadoCategoria.REJEICAO, result.categoria());
    }

    @Test
    void deveInterpretarConsultaCancelada() {
        var result = parser.parseConsulta(envelope("""
                <retConsSitCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <tpAmb>2</tpAmb><verAplic>1.0</verAplic><cStat>101</cStat>
                  <xMotivo>Cancelamento de CT-e homologado</xMotivo><cUF>51</cUF><chCTe>CHAVE</chCTe>
                  <protCTe><infProt><nProt>151260000000001</nProt><dhRecbto>2026-08-19T10:00:00-04:00</dhRecbto></infProt></protCTe>
                </retConsSitCTe>
                """));

        assertTrue(result.cancelado());
        assertEquals(CteResultadoCategoria.CANCELADO, result.categoria());
    }

    @Test
    void deveInterpretarRejeicaoDeConsulta() {
        var result = parser.parseConsulta(envelope("""
                <retConsSitCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <cStat>217</cStat><xMotivo>CT-e não consta na base de dados da SEFAZ</xMotivo>
                </retConsSitCTe>
                """));

        assertEquals(217, result.codigo());
        assertEquals(CteResultadoCategoria.REJEICAO, result.categoria());
    }

    @Test
    void deveSepararCodigoDoLoteEDoEvento() {
        var result = parser.parseEvento(envelope("""
                <retEventoCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <idLote>1</idLote><tpAmb>2</tpAmb><verAplic>1.0</verAplic>
                  <cOrgao>51</cOrgao><cStat>128</cStat><xMotivo>Lote de evento processado</xMotivo>
                  <retEventoCTe><infEvento><tpAmb>2</tpAmb><verAplic>1.0</verAplic><cOrgao>51</cOrgao>
                    <cStat>135</cStat><xMotivo>Evento registrado e vinculado a CT-e</xMotivo>
                    <chCTe>CHAVE</chCTe><tpEvento>110111</tpEvento><nSeqEvento>1</nSeqEvento>
                    <dhRegEvento>2026-08-19T10:00:00-04:00</dhRegEvento><nProt>151260000000002</nProt>
                  </infEvento></retEventoCTe>
                </retEventoCTe>
                """));

        assertEquals(128, result.codigoLote());
        assertEquals(135, result.codigoEvento());
        assertTrue(result.registrado());
    }

    @Test
    void deveInterpretarRejeicaoDeEvento() {
        var result = parser.parseEvento(envelope("""
                <retEventoCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <cStat>128</cStat><xMotivo>Lote de evento processado</xMotivo>
                  <retEventoCTe><infEvento><cStat>573</cStat>
                    <xMotivo>Rejeição: Duplicidade de evento</xMotivo>
                  </infEvento></retEventoCTe>
                </retEventoCTe>
                """));

        assertEquals(573, result.codigoEvento());
        assertFalse(result.registrado());
        assertEquals(CteResultadoCategoria.REJEICAO, result.categoria());
    }

    @Test
    void deveInterpretarIndisponibilidadeDoStatus() {
        var result = parser.parseStatus(envelope("""
                <retConsStatServCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <tpAmb>2</tpAmb><verAplic>1.0</verAplic><cStat>108</cStat>
                  <xMotivo>Serviço paralisado momentaneamente</xMotivo><cUF>51</cUF>
                  <dhRecbto>2026-08-19T10:00:00-04:00</dhRecbto><tMed>1</tMed>
                </retConsStatServCTe>
                """));

        assertFalse(result.disponivel());
        assertEquals(CteResultadoCategoria.INDISPONIVEL, result.categoria());
    }

    @Test
    void deveInterpretarStatusOperacional() {
        var result = parser.parseStatus(envelope("""
                <retConsStatServCTe xmlns="http://www.portalfiscal.inf.br/cte" versao="4.00">
                  <tpAmb>2</tpAmb><verAplic>1.0</verAplic><cStat>107</cStat>
                  <xMotivo>Serviço em Operação</xMotivo><cUF>51</cUF><tMed>1</tMed>
                </retConsStatServCTe>
                """));

        assertTrue(result.disponivel());
        assertEquals(CteResultadoCategoria.SUCESSO, result.categoria());
    }

    @Test
    void devePropagarSoapFaultComCodigoEMotivo() {
        CteSoapFaultException exception = assertThrows(CteSoapFaultException.class,
                () -> parser.parseStatus("""
                        <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"><soap:Body>
                          <soap:Fault><soap:Code><soap:Value>soap:Sender</soap:Value></soap:Code>
                          <soap:Reason><soap:Text xml:lang="pt-BR">Ação SOAP inválida</soap:Text></soap:Reason></soap:Fault>
                        </soap:Body></soap:Envelope>
                        """));
        assertEquals("soap:Sender", exception.getFaultCode());
    }

    private String envelope(String body) {
        return "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Body>"
                + body + "</soap:Body></soap:Envelope>";
    }
}
