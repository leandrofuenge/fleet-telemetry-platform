# Consulta Situação

Função para consultar a Situação da NF-e na Sefaz.

```java title="ConsultaXmlTeste.java"
import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.DocumentoEnum;
import com.telemetria.integration.nfe.schema_4.retConsSitNFe.TRetConsSitNFe;

/**
 * @author Samuel Oliveira
 *
 */
public class ConsultaXmlTeste {
    public static void main(String[] args) {
        try {
            // Inicia As Configurações (1)
            ConfiguracoesNfe config = Config.iniciaConfiguracoes();
            //Informe a chave a ser Consultada
            String chave = "52190310732644000128550010000125531000125532";

            //Efetua a consulta
            TRetConsSitNFe retorno = Nfe.consultaXml(config, chave, DocumentoEnum.NFE);

            //Resultado
            System.out.println();
            System.out.println("# Status: " + retorno.getCStat() + " - " + retorno.getXMotivo());
        } catch (Exception e) {
            System.err.println();
            System.err.println(e.getMessage());
        }
    }
}
```

1.  Acesse o menu [Configurações (Certificado)](./configuracoes.md)