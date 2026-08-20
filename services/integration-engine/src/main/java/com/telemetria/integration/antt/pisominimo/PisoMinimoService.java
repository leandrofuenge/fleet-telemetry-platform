package com.telemetria.integration.antt.pisominimo;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.telemetria.integration.sefaz.cte.exception.CteException;

@Service
public class PisoMinimoService {

    private final PisoMinimoClient client;

    public PisoMinimoService(PisoMinimoClient client) {
        this.client = client;
    }

    /**
     * Calcula o valor do piso mínimo de frete enviando todos os parâmetros específicos da ANTT.
     */
    public PisoMinimoResponse calcularPisoMinimo(PisoMinimoRequest request) {
        validarParametrosEntrada(request);
        
        PisoMinimoResponse response = client.calcularPisoMinimo(request);

        if (!response.isSucesso() || response.valorTotal() == null) {
            throw new CteException("Não foi possível calcular o Piso Mínimo do frete: " + response.mensagemErro());
        }

        return response;
    }

    /**
     * Valida se o frete negociado respeita o valor mínimo exigido pela tabela da ANTT.
     *
     * @param request Parâmetros da viagem
     * @param valorFreteOferecido Valor total ofertado ao transportador
     * @return true se o valor oferecido for MAIOR ou IGUAL ao piso mínimo calculado
     */
    public boolean validarFreteEmCompliance(PisoMinimoRequest request, BigDecimal valorFreteOferecido) {
        if (valorFreteOferecido == null || valorFreteOferecido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CteException("O valor do frete a ser validado deve ser superior a zero.");
        }

        PisoMinimoResponse piso = calcularPisoMinimo(request);
        return valorFreteOferecido.compareTo(piso.valorTotal()) >= 0;
    }

    private void validarParametrosEntrada(PisoMinimoRequest r) {
        if (r == null) {
            throw new CteException("Os parâmetros para cálculo do Piso Mínimo não podem ser nulos.");
        }
        if (r.distancia() == null || r.distancia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new CteException("A distância deve ser superior a zero.");
        }
        if (r.eixos() < 2 || r.eixos() > 9) {
            throw new CteException("O número de eixos deve estar entre 2 e 9.");
        }
        if (r.tipoCarga() == null || r.tipoCarga().isBlank()) {
            throw new CteException("O tipo_carga é obrigatório (ex: CARGA_GERAL, GRANEL_SOLIDO).");
        }
    }
}
