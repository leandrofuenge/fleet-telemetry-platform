package com.telemetria.domain.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Geofence;
import com.telemetria.domain.entity.MensagemViagem;
import com.telemetria.domain.entity.Multa;
import com.telemetria.domain.entity.OcorrenciaOperacional;
import com.telemetria.domain.entity.Sinistro;
import com.telemetria.domain.entity.Telemetria;
import com.telemetria.domain.exception.BusinessException;
import com.telemetria.domain.exception.ErrorCode;
import com.telemetria.infrastructure.persistence.GeofenceRepository;
import com.telemetria.infrastructure.persistence.MensagemViagemRepository;
import com.telemetria.infrastructure.persistence.MultaRepository;
import com.telemetria.infrastructure.persistence.OcorrenciaOperacionalRepository;
import com.telemetria.infrastructure.persistence.SinistroRepository;
import com.telemetria.infrastructure.persistence.TelemetriaRepository;

@Service
public class OperacaoService {
    private final MensagemViagemRepository mensagens;
    private final OcorrenciaOperacionalRepository ocorrencias;
    private final SinistroRepository sinistros;
    private final MultaRepository multas;
    private final GeofenceRepository geofences;
    private final TelemetriaRepository telemetrias;

    public OperacaoService(
            MensagemViagemRepository mensagens,
            OcorrenciaOperacionalRepository ocorrencias,
            SinistroRepository sinistros,
            MultaRepository multas,
            GeofenceRepository geofences,
            TelemetriaRepository telemetrias) {
        this.mensagens = mensagens;
        this.ocorrencias = ocorrencias;
        this.sinistros = sinistros;
        this.multas = multas;
        this.geofences = geofences;
        this.telemetrias = telemetrias;
    }

    @Transactional
    public MensagemViagem enviar(MensagemViagem mensagem, boolean veiculoParado) {
        if ("MOTORISTA".equals(mensagem.getTipoRemetente())
                && "TEXTO".equals(mensagem.getTipoConteudo())
                && !veiculoParado) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    "Motorista só pode digitar mensagens com veículo parado");
        }
        return mensagens.save(mensagem);
    }

    public List<MensagemViagem> historico(Long viagemId) {
        return mensagens.findByViagemIdOrderByCriadoEmAsc(viagemId);
    }

    @Transactional
    public OcorrenciaOperacional registrar(OcorrenciaOperacional ocorrencia) {
        if (ocorrencia.getTenantId() == null
                || ocorrencia.getTipo() == null
                || ocorrencia.getTitulo() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dados da ocorrência inválidos");
        }

        OcorrenciaOperacional salva = ocorrencias.save(ocorrencia);
        if (ocorrencia.getLatitude() != null
                && ocorrencia.getLongitude() != null
                && ocorrencia.getRaioImpactoM() != null) {
            Geofence geofence = Geofence.builder()
                    .tenantId(ocorrencia.getTenantId())
                    .nome("Ocorrência: " + ocorrencia.getTitulo())
                    .tipo(Geofence.TipoGeofence.CIRCULO)
                    .latitudeCentro(ocorrencia.getLatitude())
                    .longitudeCentro(ocorrencia.getLongitude())
                    .raio(ocorrencia.getRaioImpactoM() / 1000)
                    .tipoAlerta(Geofence.TipoAlertaGeofence.ENTRADA)
                    .build();
            geofences.save(geofence);
        }
        return salva;
    }

    @Transactional
    public Sinistro abrirPorTelemetria(Telemetria telemetria) {
        Sinistro sinistro = new Sinistro();
        sinistro.setTenantId(telemetria.getTenantId());
        sinistro.setVeiculoId(telemetria.getVeiculoId());
        sinistro.setViagemId(telemetria.getViagemId());
        sinistro.setMotoristaId(telemetria.getMotoristaId());
        sinistro.setTipo(Boolean.TRUE.equals(telemetria.getColisaoDetectada()) ? "COLISAO" : "OUTRO");
        sinistro.setDataHoraOcorrencia(telemetria.getDataHora());
        sinistro.setLatitude(telemetria.getLatitude());
        sinistro.setLongitude(telemetria.getLongitude());
        sinistro.setVelocidadeNoMomento(telemetria.getVelocidade());
        sinistro.setTelemetriaSnapshot(Map.of("telemetriaId", telemetria.getId(), "preservarDados", true));
        telemetria.setPreservarDados(true);
        telemetrias.save(telemetria);
        return sinistros.save(sinistro);
    }

    @Transactional
    public Multa registrarMulta(Multa multa) {
        if (multa.getTenantId() == null || multa.getVeiculoId() == null || multa.getAutoInfracao() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Dados da multa inválidos");
        }
        return multas.save(multa);
    }
}
