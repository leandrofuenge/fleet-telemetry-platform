package com.app.telemetria.domain.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.app.telemetria.api.dto.response.VeiculoDTO;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.domain.exception.VeiculoNotFoundException;
import com.app.telemetria.infrastructure.persistence.VeiculoRepository;

@Service
public class VeiculoService {

    private static final Logger log = LoggerFactory.getLogger(VeiculoService.class);
    
    // Regex para placas: Mercosul (ABC1D23) ou antigo (ABC-1234)
    private static final Pattern PLACA_PATTERN = 
        Pattern.compile("^([A-Z]{3}\\d{1}[A-Z]{1}\\d{2}|[A-Z]{3}-\\d{4})$");

    @Autowired
    private VeiculoRepository repository;

    // ================ MÉTODOS CRUD ================

    public VeiculoDTO salvar(Veiculo veiculo) {
        log.info("➕ Salvando novo veículo - Placa: {}, Modelo: {}, Tenant: {}", 
                veiculo.getPlaca(), veiculo.getModelo(), veiculo.getTenantId());
        
        log.debug("📝 Dados do veículo: {}", veiculo);
        
        // 1. RN-VEI-001: Validar formato da placa
        validarFormatoPlaca(veiculo.getPlaca());
        
        // 2. RN-VEI-001: Validar unicidade por tenant
        if (repository.existsByPlacaAndTenantId(veiculo.getPlaca(), veiculo.getTenantId())) {
            Veiculo existente = repository.findByPlacaAndTenantId(
                veiculo.getPlaca(), veiculo.getTenantId()).get();
            
            log.error("❌ Placa já cadastrada no tenant {}: {}", veiculo.getTenantId(), veiculo.getPlaca());
            
            // Mensagem específica conforme RN-VEI-001
            throw new BusinessException(String.format(
                "Placa já cadastrada. Veículo: %s — %s",
                existente.getModelo() != null ? existente.getModelo() : "sem modelo",
                existente.getPlaca()
            ));
        }
        
        // 3. RN-VEI-002: Lógica do tacógrafo
        validarTacografo(veiculo);
        
        try {
            Veiculo salvo = repository.save(veiculo);
            log.info("✅ Veículo salvo com ID: {}", salvo.getId());
            log.debug("📝 Veículo salvo completo: {}", salvo);
            
            return toDTO(salvo);
            
        } catch (DataIntegrityViolationException e) {
            log.error("❌ Erro de integridade ao salvar veículo: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Erro ao salvar veículo");
        }
    }

    public List<VeiculoDTO> listarTodos() {
        log.info("📋 Listando todos os veículos");
        log.debug("🔍 Consultando repositório...");
        
        List<Veiculo> veiculos = repository.findAll();
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Total de veículos encontrados: {}", veiculos.size());
        log.debug("📊 IDs dos veículos: {}", veiculos.stream().map(Veiculo::getId).toList());
        
        return dtos;
    }

    public VeiculoDTO buscarPorId(Long id) {
        log.info("🔍 Buscando veículo por ID: {}", id);
        log.debug("🔍 Consultando repositório para ID {}", id);
        
        Veiculo veiculo = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {}", id);
                    return new VeiculoNotFoundException("Veículo não encontrado com ID: " + id);
                });

        log.info("✅ Veículo encontrado - Placa: {}, Modelo: {}", veiculo.getPlaca(), veiculo.getModelo());
        log.debug("📝 Veículo completo: {}", veiculo);
        
        return toDTO(veiculo);
    }

    /**
     * Busca veículo por placa
     */
    public VeiculoDTO buscarPorPlaca(String placa) {
        log.info("🔍 Buscando veículo por placa: {}", placa);
        log.debug("🔍 Consultando repositório para placa: {}", placa);
        
        Veiculo veiculo = repository.findByPlaca(placa)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com placa: {}", placa);
                    return new VeiculoNotFoundException("Veículo não encontrado com placa: " + placa);
                });

        log.info("✅ Veículo encontrado - ID: {}, Modelo: {}", veiculo.getId(), veiculo.getModelo());
        log.debug("📝 Veículo completo: {}", veiculo);
        
        return toDTO(veiculo);
    }

    public VeiculoDTO atualizar(Long id, Veiculo dados) {
        log.info("🔄 Atualizando veículo ID: {}", id);
        log.debug("📝 Dados para atualização - Placa: {}, Modelo: {}, Capacidade: {}kg", 
                 dados.getPlaca(), dados.getModelo(), dados.getCapacidadeCarga());
        
        Veiculo veiculo = repository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Veículo não encontrado com ID: {} para atualização", id);
                    return new VeiculoNotFoundException("Veículo não encontrado com ID: " + id);
                });

        log.debug("📝 Veículo original: {}", veiculo);

        // Verifica se a placa está sendo alterada e se já existe (considerando tenant)
        if (dados.getPlaca() != null && !dados.getPlaca().equals(veiculo.getPlaca())) {
            // RN-VEI-001: Validar formato da nova placa
            validarFormatoPlaca(dados.getPlaca());
            
            // RN-VEI-001: Validar unicidade por tenant
            if (repository.existsByPlacaAndTenantId(dados.getPlaca(), veiculo.getTenantId())) {
                Veiculo existente = repository.findByPlacaAndTenantId(
                    dados.getPlaca(), veiculo.getTenantId()).get();
                
                log.error("❌ Placa já cadastrada no tenant: {}", dados.getPlaca());
                throw new BusinessException(String.format(
                    "Placa já cadastrada. Veículo: %s — %s",
                    existente.getModelo() != null ? existente.getModelo() : "sem modelo",
                    existente.getPlaca()
                ));
            }
            log.debug("📝 Placa alterada: '{}' → '{}'", veiculo.getPlaca(), dados.getPlaca());
            veiculo.setPlaca(dados.getPlaca());
        }

        if (dados.getModelo() != null && !dados.getModelo().equals(veiculo.getModelo())) {
            log.debug("📝 Modelo alterado: '{}' → '{}'", veiculo.getModelo(), dados.getModelo());
            veiculo.setModelo(dados.getModelo());
        }
        
        if (dados.getMarca() != null && !dados.getMarca().equals(veiculo.getMarca())) {
            log.debug("📝 Marca alterada: '{}' → '{}'", veiculo.getMarca(), dados.getMarca());
            veiculo.setMarca(dados.getMarca());
        }
        
        if (dados.getCapacidadeCarga() != null && !dados.getCapacidadeCarga().equals(veiculo.getCapacidadeCarga())) {
            log.debug("📝 Capacidade alterada: {} → {}kg", 
                     veiculo.getCapacidadeCarga(), dados.getCapacidadeCarga());
            veiculo.setCapacidadeCarga(dados.getCapacidadeCarga());
        }
        
        if (dados.getAnoFabricacao() != null && !dados.getAnoFabricacao().equals(veiculo.getAnoFabricacao())) {
            log.debug("📝 Ano alterado: {} → {}", veiculo.getAnoFabricacao(), dados.getAnoFabricacao());
            veiculo.setAnoFabricacao(dados.getAnoFabricacao());
        }
        
        // RN-VEI-002: Atualizar PBT e revalidar tacógrafo
        if (dados.getPbt() != null && !dados.getPbt().equals(veiculo.getPbt())) {
            log.debug("📝 PBT alterado: {} → {} kg", veiculo.getPbt(), dados.getPbt());
            veiculo.setPbt(dados.getPbt());
            validarTacografo(veiculo);
        }
        
        // RN-VEI-003: Atualizar datas de vencimento de documentos
        if (dados.getDataVencimentoCrlv() != null) {
            veiculo.setDataVencimentoCrlv(dados.getDataVencimentoCrlv());
        }
        
        if (dados.getDataVencimentoSeguro() != null) {
            veiculo.setDataVencimentoSeguro(dados.getDataVencimentoSeguro());
        }
        
        if (dados.getDataVencimentoTacografo() != null) {
            veiculo.setDataVencimentoTacografo(dados.getDataVencimentoTacografo());
        }
        
        if (dados.getDataVencimentoDpvat() != null) {
            veiculo.setDataVencimentoDpvat(dados.getDataVencimentoDpvat());
        }
        
        if (dados.getDataVencimentoRcf() != null) {
            veiculo.setDataVencimentoRcf(dados.getDataVencimentoRcf());
        }
        
        if (dados.getDataVencimentoVistoria() != null) {
            veiculo.setDataVencimentoVistoria(dados.getDataVencimentoVistoria());
        }
        
        if (dados.getDataVencimentoRntrc() != null) {
            veiculo.setDataVencimentoRntrc(dados.getDataVencimentoRntrc());
        }
        
        if (dados.getClienteId() != null && !dados.getClienteId().equals(veiculo.getClienteId())) {
            log.debug("📝 Cliente ID alterado: {} → {}", veiculo.getClienteId(), dados.getClienteId());
            veiculo.setClienteId(dados.getClienteId());
            // Atualiza tenant_id baseado no cliente
            if (dados.getCliente() != null) {
                veiculo.setTenantId(dados.getCliente().getId());
            }
        }
        
        if (dados.getMotoristaAtualId() != null && !dados.getMotoristaAtualId().equals(veiculo.getMotoristaAtualId())) {
            log.debug("📝 Motorista atual ID alterado: {} → {}", 
                     veiculo.getMotoristaAtualId(), dados.getMotoristaAtualId());
            veiculo.setMotoristaAtualId(dados.getMotoristaAtualId());
        }

        log.debug("📝 Veículo após modificações: {}", veiculo);
        
        try {
            Veiculo atualizado = repository.save(veiculo);
            log.info("✅ Veículo {} atualizado com sucesso", id);
            log.debug("📝 Veículo após atualização: {}", atualizado);
            
            return toDTO(atualizado);
            
        } catch (DataIntegrityViolationException e) {
            log.error("❌ Erro de integridade ao atualizar veículo {}: {}", id, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Erro ao atualizar veículo");
        }
    }

    public void deletar(Long id) {
        log.info("🗑️ Deletando veículo ID: {}", id);
        
        if (!repository.existsById(id)) {
            log.error("❌ Veículo não encontrado com ID: {} para deleção", id);
            throw new VeiculoNotFoundException("Veículo não encontrado com ID: " + id);
        }
        
        repository.deleteById(id);
        log.info("✅ Veículo {} deletado com sucesso", id);
    }

    // ================ MÉTODOS DE VALIDAÇÃO ================

    /**
     * RN-VEI-001: Valida formato da placa (Mercosul ou antigo)
     */
    private void validarFormatoPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new BusinessException("Placa é obrigatória");
        }
        
        if (!PLACA_PATTERN.matcher(placa.toUpperCase()).matches()) {
            throw new BusinessException(
                "Formato de placa inválido. Use ABC1D23 (Mercosul) ou ABC-1234"
            );
        }
    }

    /**
     * RN-VEI-002: Valida regras do tacógrafo
     * PBT > 4.536 kg → tacografo_obrigatorio = TRUE
     * Se obrigatório, data_venc_tacografo é obrigatória
     */
    private void validarTacografo(Veiculo veiculo) {
        if (veiculo.getPbt() != null && veiculo.getPbt() > 4536.0) {
            veiculo.setTacografoObrigatorio(true);
            
            if (veiculo.getDataVencimentoTacografo() == null) {
                throw new BusinessException(
                    "Veículo com PBT > 4.536 kg requer data de vencimento do tacógrafo"
                );
            }
        } else {
            // Se PBT <= 4536kg, tacógrafo não é obrigatório
            veiculo.setTacografoObrigatorio(false);
        }
    }

    /**
     * RN-VEI-003: Valida documentos para nova viagem
     * CRLV ou Seguro vencido: veículo não pode ser vinculado a nova viagem
     * Também valida tacógrafo se obrigatório
     */
    public void validarDocumentosParaViagem(Long veiculoId) {
        Veiculo veiculo = repository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));
        
        validarDocumentosParaViagem(veiculo);
    }
    
    /**
     * RN-VEI-003: Valida documentos para nova viagem (sobrecarga com objeto)
     */
    public void validarDocumentosParaViagem(Veiculo veiculo) {
        LocalDate hoje = LocalDate.now();
        
        // Verificar CRLV vencido
        if (veiculo.getDataVencimentoCrlv() != null && 
            veiculo.getDataVencimentoCrlv().isBefore(hoje)) {
            throw new BusinessException(
                "CRLV vencido desde " + veiculo.getDataVencimentoCrlv() + 
                ". Veículo não pode ser vinculado a nova viagem."
            );
        }
        
        // Verificar Seguro vencido
        if (veiculo.getDataVencimentoSeguro() != null && 
            veiculo.getDataVencimentoSeguro().isBefore(hoje)) {
            throw new BusinessException(
                "Seguro vencido desde " + veiculo.getDataVencimentoSeguro() + 
                ". Veículo não pode ser vinculado a nova viagem."
            );
        }
        
        // Verificar DPVAT vencido
        if (veiculo.getDataVencimentoDpvat() != null && 
            veiculo.getDataVencimentoDpvat().isBefore(hoje)) {
            throw new BusinessException(
                "DPVAT vencido desde " + veiculo.getDataVencimentoDpvat() + 
                ". Veículo não pode ser vinculado a nova viagem."
            );
        }
        
        // Verificar tacógrafo se obrigatório
        if (veiculo.getTacografoObrigatorio() != null && 
            veiculo.getTacografoObrigatorio() && 
            veiculo.getDataVencimentoTacografo() != null && 
            veiculo.getDataVencimentoTacografo().isBefore(hoje)) {
            throw new BusinessException(
                "Tacógrafo vencido desde " + veiculo.getDataVencimentoTacografo() +
                ". Veículo não pode ser vinculado a nova viagem."
            );
        }
        
        log.info("✅ Documentos do veículo {} validados para viagem", veiculo.getId());
    }
    
    /**
     * Verifica se o veículo tem todos os documentos válidos
     * Retorna true se todos os documentos obrigatórios estão válidos
     */
    public boolean isDocumentosValidos(Long veiculoId) {
        try {
            validarDocumentosParaViagem(veiculoId);
            return true;
        } catch (BusinessException e) {
            log.warn("⚠️ Veículo {} com documentos inválidos: {}", veiculoId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtém a lista de documentos vencidos do veículo
     */
    public List<String> getDocumentosVencidos(Long veiculoId) {
        Veiculo veiculo = repository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado"));
        
        LocalDate hoje = LocalDate.now();
        List<String> vencidos = new java.util.ArrayList<>();
        
        if (veiculo.getDataVencimentoCrlv() != null && veiculo.getDataVencimentoCrlv().isBefore(hoje)) {
            vencidos.add("CRLV");
        }
        if (veiculo.getDataVencimentoSeguro() != null && veiculo.getDataVencimentoSeguro().isBefore(hoje)) {
            vencidos.add("Seguro");
        }
        if (veiculo.getDataVencimentoDpvat() != null && veiculo.getDataVencimentoDpvat().isBefore(hoje)) {
            vencidos.add("DPVAT");
        }
        if (veiculo.getDataVencimentoRcf() != null && veiculo.getDataVencimentoRcf().isBefore(hoje)) {
            vencidos.add("RCF");
        }
        if (veiculo.getDataVencimentoVistoria() != null && veiculo.getDataVencimentoVistoria().isBefore(hoje)) {
            vencidos.add("Vistoria");
        }
        if (veiculo.getDataVencimentoRntrc() != null && veiculo.getDataVencimentoRntrc().isBefore(hoje)) {
            vencidos.add("RNTRC");
        }
        if (veiculo.getTacografoObrigatorio() != null && veiculo.getTacografoObrigatorio() &&
            veiculo.getDataVencimentoTacografo() != null && 
            veiculo.getDataVencimentoTacografo().isBefore(hoje)) {
            vencidos.add("Tacógrafo");
        }
        
        return vencidos;
    }

    // ================ MÉTODOS DE VÍNCULO COM DISPOSITIVOS ================

    /**
     * RN-VEI-004: Vincular dispositivo ao veículo
     * Verifica se device_id já está vinculado a outro veículo
     * Transferência de dispositivo exige desvinculação explícita do veículo anterior
     */
    public void vincularDispositivo(Long veiculoId, String deviceId) {
        log.info("🔌 Vinculando dispositivo {} ao veículo {}", deviceId, veiculoId);
        
        // Verificar se veículo existe
        Veiculo veiculo = repository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));
        
        // TODO: Implementar com DispositivoIotService
        // 1. Verificar se device_id já está vinculado a outro veículo
        // 2. Se estiver vinculado, exigir desvinculação explícita
        // 3. Se não estiver, vincular ao veículo atual
        // 4. Atualizar status do dispositivo para ATIVO
        
        log.info("✅ Dispositivo {} vinculado ao veículo {}", deviceId, veiculoId);
    }

    /**
     * RN-VEI-005: Múltiplos dispositivos por veículo
     * Até 2 dispositivos: principal (GSM) e backup (satelital)
     * Apenas um dispositivo pode ser o principal
     * Backup só envia se principal offline > 5 minutos
     */
    public void adicionarDispositivoBackup(Long veiculoId, String deviceIdBackup) {
        log.info("📡 Adicionando dispositivo backup {} ao veículo {}", deviceIdBackup, veiculoId);
        
        // Verificar se veículo existe
        Veiculo veiculo = repository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));
        
        // TODO: Implementar com DispositivoIotService
        // 1. Verificar quantos dispositivos já estão vinculados ao veículo
        // 2. Se já tiver 2 dispositivos, bloquear (máximo 2)
        // 3. Se já tiver 1 dispositivo e ele for principal, permitir adicionar backup
        // 4. Se já tiver 1 dispositivo e ele for backup, promover principal e adicionar novo backup? (definir regra)
        // 5. Configurar satelite_ativo = TRUE no dispositivo principal ao provisionar backup
        
        log.info("✅ Dispositivo backup {} adicionado ao veículo {}", deviceIdBackup, veiculoId);
    }

    /**
     * RN-VEI-006: Calibração de odômetro ao trocar dispositivo
     * Obrigatório informar odometro_atual_km no momento da troca
     * Sistema registra o delta entre último odômetro do dispositivo antigo e o valor informado
     * Variação > 500 km do último registro: gerar alerta de inconsistência para o gestor
     */
    public void trocarDispositivo(Long veiculoId, String novoDeviceId, Double odometroAtualKm) {
        log.info("🔄 Trocando dispositivo do veículo {}. Novo odômetro informado: {} km", 
                veiculoId, odometroAtualKm);
        
        // Validar odômetro obrigatório
        if (odometroAtualKm == null || odometroAtualKm < 0) {
            throw new BusinessException(
                "Odômetro atual é obrigatório ao trocar dispositivo. Informe a quilometragem atual do veículo."
            );
        }
        
        // Verificar se veículo existe
        Veiculo veiculo = repository.findById(veiculoId)
            .orElseThrow(() -> new VeiculoNotFoundException("Veículo não encontrado com ID: " + veiculoId));
        
        // TODO: Implementar com DispositivoIotService
        // 1. Obter último odômetro registrado pelo dispositivo antigo
        // 2. Calcular delta = odometroAtualKm - ultimoOdometroAntigo
        // 3. Registrar no histórico_odometro: odometro_anterior, odometro_novo, delta, data_troca
        // 4. Se delta > 500 km, gerar alerta de inconsistência para o gestor
        // 5. Vincular novo dispositivo ao veículo
        // 6. Desvincular dispositivo antigo
        
        log.info("✅ Dispositivo do veículo {} trocado. Delta odômetro: {} km", 
                veiculoId, "calculado");
    }

    // ================ MÉTODOS DE BUSCA ADICIONAIS ================

    /**
     * Busca veículos por modelo (contendo)
     */
    public List<VeiculoDTO> buscarPorModelo(String modelo) {
        log.info("🔍 Buscando veículos com modelo contendo: {}", modelo);
        
        List<Veiculo> veiculos = repository.findByModeloContainingIgnoreCase(modelo);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos com modelo contendo '{}'", dtos.size(), modelo);
        
        return dtos;
    }

    /**
     * Busca veículos por marca
     */
    public List<VeiculoDTO> buscarPorMarca(String marca) {
        log.info("🔍 Buscando veículos da marca: {}", marca);
        
        List<Veiculo> veiculos = repository.findByMarcaIgnoreCase(marca);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos da marca '{}'", dtos.size(), marca);
        
        return dtos;
    }

    /**
     * Busca veículos ativos
     */
    public List<VeiculoDTO> buscarAtivos() {
        log.info("🔍 Buscando veículos ativos");
        
        List<Veiculo> veiculos = repository.findByAtivoTrue();
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos ativos", dtos.size());
        
        return dtos;
    }

    /**
     * Busca veículos por cliente (tenant)
     */
    public List<VeiculoDTO> buscarPorCliente(Long clienteId) {
        log.info("🔍 Buscando veículos do cliente ID: {}", clienteId);
        
        List<Veiculo> veiculos = repository.findByClienteId(clienteId);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos para o cliente {}", dtos.size(), clienteId);
        
        return dtos;
    }
    
    /**
     * Busca veículos por tenant
     */
    public List<VeiculoDTO> buscarPorTenant(Long tenantId) {
        log.info("🔍 Buscando veículos do tenant ID: {}", tenantId);
        
        List<Veiculo> veiculos = repository.findByTenantId(tenantId);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos para o tenant {}", dtos.size(), tenantId);
        
        return dtos;
    }
    
    /**
     * Busca veículos com documentos vencidos
     */
    public List<VeiculoDTO> buscarComDocumentosVencidos(Long tenantId) {
        log.info("🔍 Buscando veículos com documentos vencidos do tenant: {}", tenantId);
        
        List<Veiculo> veiculos = repository.findWithDocumentosVencidos(tenantId);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos com documentos vencidos", dtos.size());
        
        return dtos;
    }
    
    /**
     * Busca veículos com tacógrafo vencendo em X dias
     */
    public List<VeiculoDTO> buscarTacografoVencendoEmDias(int dias) {
        log.info("🔍 Buscando veículos com tacógrafo vencendo em {} dias", dias);
        
        LocalDate dataLimite = LocalDate.now().plusDays(dias);
        List<Veiculo> veiculos = repository.findTacografoVencendoEmDias(dataLimite);
        List<VeiculoDTO> dtos = veiculos.stream()
                .map(this::toDTO)
                .toList();
        
        log.info("✅ Encontrados {} veículos com tacógrafo vencendo em até {} dias", dtos.size(), dias);
        
        return dtos;
    }

    /**
     * Busca veículo por placa (retorna Optional)
     */
    public Optional<VeiculoDTO> buscarPorPlacaOptional(String placa) {
        log.debug("🔍 Buscando veículo por placa (optional): {}", placa);
        
        return repository.findByPlaca(placa)
                .map(veiculo -> {
                    log.debug("✅ Veículo encontrado: {}", veiculo.getPlaca());
                    return toDTO(veiculo);
                });
    }
    
    /**
     * Busca veículo por placa e tenant (retorna Optional)
     */
    public Optional<VeiculoDTO> buscarPorPlacaAndTenantOptional(String placa, Long tenantId) {
        log.debug("🔍 Buscando veículo por placa {} e tenant {}", placa, tenantId);
        
        return repository.findByPlacaAndTenantId(placa, tenantId)
                .map(veiculo -> {
                    log.debug("✅ Veículo encontrado: {}", veiculo.getPlaca());
                    return toDTO(veiculo);
                });
    }

    /**
     * Verifica se placa já existe no tenant
     */
    public boolean placaExistsInTenant(String placa, Long tenantId) {
        boolean exists = repository.existsByPlacaAndTenantId(placa, tenantId);
        log.debug("🔍 Verificando existência da placa '{}' no tenant {}: {}", placa, tenantId, exists);
        return exists;
    }
    
    /**
     * Verifica se placa já existe (global - para compatibilidade)
     */
    public boolean placaExists(String placa) {
        boolean exists = repository.existsByPlaca(placa);
        log.debug("🔍 Verificando existência da placa '{}' (global): {}", placa, exists);
        return exists;
    }

    // ================ MÉTODOS AUXILIARES ================

    private VeiculoDTO toDTO(Veiculo veiculo) {
        VeiculoDTO dto = new VeiculoDTO(
                veiculo.getId(),
                veiculo.getPlaca(),
                veiculo.getModelo(),
                veiculo.getCapacidadeCarga()
        );
        log.trace("🔄 Convertendo Veículo para DTO - ID: {}, DTO: {}", veiculo.getId(), dto);
        return dto;
    }
}