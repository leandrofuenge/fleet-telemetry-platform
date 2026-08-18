package com.telemetria.integration.senatran.serpro;

import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.telemetria.integration.sefaz.cte.CteException;

@Service
public class SerproConsultaService {

    private final SerproConsultaClient client;

    public SerproConsultaService(SerproConsultaClient client) {
        this.client = client;
    }

    public SerproVeiculoResponse consultarVeiculo(String placa, String renavam) {
        String placaSanitizada = sanitizarPlaca(placa);
        String renavamSanitizado = apenasNumeros(renavam);
        validarParametrosConsulta(placaSanitizada, renavamSanitizado);

        SerproVeiculoResponse response = client.consultarVeiculo(
                new SerproVeiculoRequest(placaSanitizada, renavamSanitizado)
        );
        if (response == null || !response.isSucesso()) {
            String mensagem = response != null
                    ? response.mensagemErro()
                    : "Resposta nula recebida do cliente SERPRO/RADAR.";
            throw new CteException("Falha na consulta do veículo no SERPRO/RADAR: " + mensagem);
        }
        return response;
    }

    public boolean isVeiculoAptoParaViagem(String placa, String renavam) {
        SerproVeiculoResponse response = consultarVeiculo(placa, renavam);
        if (response.data() == null || response.data().isEmpty()) {
            return false;
        }

        return response.data().stream()
                .flatMap(veiculo -> veiculo.infracoes() != null
                        ? veiculo.infracoes().stream()
                        : Stream.empty())
                .noneMatch(infracao -> "AUTUADO".equalsIgnoreCase(infracao.situacao())
                        || "PENDENTE".equalsIgnoreCase(infracao.situacao()));
    }

    private void validarParametrosConsulta(String placa, String renavam) {
        if (placa == null || !placa.matches("[A-Z0-9]{7}")) {
            throw new CteException("Placa do veículo inválida. Deve conter 7 caracteres alfanuméricos.");
        }
        if (renavam == null || !renavam.matches("\\d{9,11}")) {
            throw new CteException("RENAVAM inválido. Deve possuir entre 9 e 11 dígitos numéricos.");
        }
    }

    private String sanitizarPlaca(String input) {
        return Objects.nonNull(input) ? input.toUpperCase().replaceAll("[^A-Z0-9]", "") : null;
    }

    private String apenasNumeros(String input) {
        return Objects.nonNull(input) ? input.replaceAll("\\D", "") : null;
    }
}
