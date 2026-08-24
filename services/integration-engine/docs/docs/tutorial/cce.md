# Carta de Correção Eletrônica

Função para Gerar a Carta De Correção Eletrônica.

### Envio
```java title="CartaCorrecaoTeste.java"
import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.Evento;
import com.telemetria.integration.nfe.schema.envcce.TEnvEvento;
import com.telemetria.integration.nfe.schema.envcce.TRetEnvEvento;
import com.telemetria.integration.nfe.util.CartaCorrecaoUtil;
import com.telemetria.integration.nfe.util.RetornoUtil;

import java.time.LocalDateTime;

/**
 *
 */
public class CartaCorrecaoTeste {

    public static void main(String[] args) {

        try {
            // Inicia As Configurações (1)
            ConfiguracoesNfe config = Config.iniciaConfiguracoes();

            //Agora o evento pode aceitar uma lista de cancelaemntos para envio em Lote.
            //Para isso Foi criado o Objeto Cancela
            Evento cce = new Evento();
            //Informe a chave da Nota a ser feita a CArta de Correção
            cce.setChave("XXX");
            //Informe o CNPJ do emitente
            cce.setCnpj("XXX");
            //Informe o Texto da Carta de Correção
            cce.setMotivo("Teste de Carta de Correção");
            //Informe a data da Carta de Correção
            cce.setDataEvento(LocalDateTime.now());
            //Informe a sequencia do Evento
            cce.setSequencia(1);

            // Monta o Evento
            TEnvEvento envEvento = CartaCorrecaoUtil.montaCCe(cce,config);

            //Envia a CCe
            TRetEnvEvento retorno = Nfe.cce(config, envEvento, true);

            //Valida o Retorno do Carta de Correção
            RetornoUtil.validaCartaCorrecao(retorno);

            //Resultado
            System.out.println();
            retorno.getRetEvento().forEach( resultado -> {
                System.out.println("# Chave: " + resultado.getInfEvento().getChNFe());
                System.out.println("# Status: " + resultado.getInfEvento().getCStat() + " - " + resultado.getInfEvento().getXMotivo());
                System.out.println("# Protocolo: " + resultado.getInfEvento().getNProt());
            });

            //Cria ProcEvento da CCe
            String proc = CartaCorrecaoUtil.criaProcEventoCCe(config, envEvento, retorno);
            System.out.println();
            System.out.println("# ProcEvento : " + proc);

        } catch (Exception e) {
            System.err.println();
            System.err.println(e.getMessage());
        }

    }

}
```

1.  Acesse o menu [Configurações (Certificado)](./configuracoes.md)
