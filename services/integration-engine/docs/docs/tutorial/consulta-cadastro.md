# Consulta Cadastro

Função para Consultar o Cadastro do Contribuinte na Sefaz.
### Consulta Cadastro 
```java title="ConsultaCadastroTeste.java"
import com.telemetria.integration.nfe.Nfe;
import com.telemetria.integration.nfe.dom.ConfiguracoesNfe;
import com.telemetria.integration.nfe.dom.enuns.EstadosEnum;
import com.telemetria.integration.nfe.dom.enuns.PessoaEnum;
import com.telemetria.integration.nfe.schema.retConsCad.TRetConsCad;
import com.telemetria.integration.nfe.util.RetornoUtil;

/**
 * @author Samuel Oliveira
 *
 */
public class ConsultaCadastroTeste {
    public static void main(String[] args) {
        try {
            // Inicia As Configurações (1)
            ConfiguracoesNfe config = Config.iniciaConfiguracoes();

            //Envia a Consulta
            TRetConsCad retorno = Nfe.consultaCadastro(config, PessoaEnum.JURIDICA, "XXX", EstadosEnum.GO);

            //Valida o Retorno da Consulta Cadastro
            RetornoUtil.validaConsultaCadastro(retorno);

            //Resultado
            System.out.println();
            System.out.println("# Status: " + retorno.getInfCons().getCStat() + " - " + retorno.getInfCons().getXMotivo());
            System.out.println();
            retorno.getInfCons().getInfCad().forEach( cadastro -> {
                System.out.println("# Razão Social: " + cadastro.getXNome());
                System.out.println("# Cnpj: " + cadastro.getCNPJ());
                System.out.println("# Ie: " + cadastro.getIE());
            });

        } catch (Exception e) {
            System.err.println();
            System.err.println(e.getMessage());
        }
    }
}
```

1.  Acesse o menu [Configurações (Certificado)](./configuracoes.md)