# Manifestação

Função para Manifestar a NF-e Recebida na Sefaz.

### Enviar evento de Manifestação
```java title="ManifestacaoTeste.java"
import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.dom.enuns.ManifestacaoEnum;
import com.telemetria.integration.nfe.schema.envConfRecebto.TEnvEvento;
import com.telemetria.integration.nfe.schema.envConfRecebto.TRetEnvEvento;
import com.telemetria.integration.nfe.util.ManifestacaoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;

import java.time.LocalDateTime;

/**
 */
public class ManifestacaoTeste {
    public static void main(String[] args) {
        try {
            // Inicia As Configurações (1)
            ConfiguracoesNfe config = Config.iniciaConfiguracoes();

            //Agora o evento pode aceitar uma lista de Manifestções para envio em Lote.
            //Para isso Foi criado o Objeto Manifestada
            Evento manifesta = new Evento();
            //Informe a chave da Nota a ser Manifestada
            manifesta.setChave("ZZZ");
            //Informe o CNPJ do emitente
            manifesta.setCnpj("XXX");
            //Caso o Tipo de manifestação seja OPERAÇÂO Não REALIZADA, Informe o Motivo do Manifestacao
            manifesta.setMotivo("Teste Operação Não Realizada");
            //Informe a data do Manifestacao
            manifesta.setDataEvento(LocalDateTime.now());
            //Informe o Tipo da Manifestação
            manifesta.setTipoManifestacao(ManifestacaoEnum.CIENCIA_DA_OPERACAO);

            //Monta o Evento de Manifestação
            TEnvEvento enviEvento = ManifestacaoUtil.montaManifestacao(manifesta, config);

            //Envia o Evento de Manifestação
            TRetEnvEvento retorno = Nfe.manifestacao(config, enviEvento, true);

            //Valida o Retorno do Cancelamento
            RetornoUtil.validaManifestacao(retorno);

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach( resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

            //Cria ProcEvento de Manifestacao
            String proc = ManifestacaoUtil.criaProcEventoManifestacao(config, enviEvento, retorno.getRetEvento().get(0));
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
