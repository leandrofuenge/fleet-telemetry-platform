# Cancelamento

Função para Cancelar a NFe/NFCe.

### Cancelar NFe/NFCe.
```java title="CancelarTeste.java"
import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.schema.envEventoCancNFe.TEnvEvento;
import com.telemetria.integration.nfe.schema.envEventoCancNFe.TRetEnvEvento;
import com.telemetria.integration.nfe.util.CancelamentoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;

import java.time.LocalDateTime;

/**
 * @author Samuel Oliveira
 */
public class CancelarTeste {

    public static void main(String[] args) {
        try {
            // Inicia As Configurações (1)
            ConfiguracoesNfe config = Config.iniciaConfiguracoes();

            //Agora o evento pode aceitar uma lista de cancelaemntos para envio em Lote.
            //Para isso Foi criado o Objeto Cancela
            Evento cancela = new Evento();
            //Informe a chave da Nota a ser Cancelada
            cancela.setChave("XXX");
            //Informe o protocolo da Nota a ser Cancelada
            cancela.setProtocolo("XXX");
            //Informe o CNPJ do emitente
            cancela.setCnpj("XXX");
            //Informe o Motivo do Cancelamento
            cancela.setMotivo("Teste de Cancelamento");
            //Informe a data do Cancelamento
            cancela.setDataEvento(LocalDateTime.now());

            //Monta o Evento de Cancelamento
            TEnvEvento enviEvento = CancelamentoUtil.montaCancelamento(cancela, config);

            //Envia o Evento de Cancelamento
            TRetEnvEvento retorno = Nfe.cancelarNfe(config, enviEvento, true, DocumentoEnum.NFE);

            //Valida o Retorno do Cancelamento
            RetornoUtil.validaCancelamento(retorno);

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach( resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

            //Cria ProcEvento de Cacnelamento
            String proc = CancelamentoUtil.criaProcEventoCancelamento(config, enviEvento, retorno.getRetEvento().get(0));
            System.out.println();
            System.out.println("# ProcEvento : " + proc);

        } catch (Exception e) {
            System.err.println();
            System.err.println("# Erro: "+e.getMessage());
        }
    }
}
```

1.  Acesse o menu [Configurações (Certificado)](./configuracoes.md)


### Cancelamento Substituição (NFCe)
```java title="CancelarTeste.java"
import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.schema.envEventoCancSubst.TEnvEvento;
import com.telemetria.integration.nfe.schema.envEventoCancSubst.TRetEnvEvento;
import com.telemetria.integration.nfe.util.CancelamentoSubstituicaoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;

import java.time.LocalDateTime;

/**
 * @author Samuel Oliveira
 */
public class CancelarSubstituicaoTeste {

    public static void main(String[] args) {
        try {
            // Inicia As Configurações (1)
            ConfiguracoesNfe config = Config.iniciaConfiguracoes();

            //Agora o evento pode aceitar uma lista de cancelaemntos para envio em Lote.
            //Para isso Foi criado o Objeto Cancela
            Evento cancela = new Evento();
            //Informe a chave da Nota a ser Cancelada
            cancela.setChave("XXX");
            //Informe a chave da Nota a Substituta
            cancela.setChaveSusbstituta("XXX");
            //Informe o protocolo da Nota a ser Cancelada
            cancela.setProtocolo("XXX");
            //Informe o CNPJ do emitente
            cancela.setCnpj("XXX");
            //Informe o Motivo do Cancelamento
            cancela.setMotivo("Teste de Cancelamento");
            //Informe a data do Cancelamento
            cancela.setDataEvento(LocalDateTime.now());

            //Monta o Evento de Cancelamento
            TEnvEvento enviEvento = CancelamentoSubstituicaoUtil.montaCancelamento(cancela, config);

            //Envia o Evento de Cancelamento
            TRetEnvEvento retorno = Nfe.cancelarSubstituicaoNfe(config, enviEvento, true);

            //Valida o Retorno do Cancelamento
            RetornoUtil.validaCancelamentoSubstituicao(retorno);

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach( resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

            //Cria ProcEvento de Cacnelamento
            String proc = CancelamentoSubstituicaoUtil.criaProcEventoCancelamento(config, enviEvento, retorno.getRetEvento().get(0));
            System.out.println();
            System.out.println("# ProcEvento : " + proc);

        } catch (Exception e) {
            System.err.println();
            System.err.println("# Erro: "+e.getMessage());
        }
    }
}
```

1.  Acesse o menu [Configurações (Certificado)](./configuracoes.md)
