package com.app.telemetria.domain.service;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.app.telemetria.domain.entity.Carga;
import com.app.telemetria.domain.entity.Motorista;
import com.app.telemetria.domain.entity.Veiculo;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.infrastructure.persistence.MotoristaRepository;

@Service
public class MotoristaService {

    private static final Logger log = LoggerFactory.getLogger(MotoristaService.class);

    @Autowired
    private MotoristaRepository motoristaRepository;
    
    @Autowired
    private AlertaService alertaService;

    // ================ MÉTODOS CRUD ================

    public Motorista salvar(Motorista motorista) {
        boolean isNovo = motorista.getId() == null;
        
        if (isNovo) {
            log.info("➕ Salvando novo motorista - Nome: {}, CPF: {}, CNH: {}", 
                    motorista.getNome(), motorista.getCpf(), motorista.getCnh());
        } else {
            log.info("🔄 Atualizando motorista ID: {} - Nome: {}, CPF: {}, CNH: {}", 
                    motorista.getId(), motorista.getNome(), motorista.getCpf(), motorista.getCnh());
        }
        
        // RN-MOT-001: Validar dígitos do CPF
        if (!validarCpf(motorista.getCpf())) {
            log.error("❌ CPF inválido: {}", motorista.getCpf());
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "CPF inválido. Verifique os dígitos verificadores.");
        }
        
        log.debug("📝 Dados do motorista: {}", motorista);
        
        try {
            Motorista salvo = motoristaRepository.save(motorista);
            
            if (isNovo) {
                log.info("✅ Motorista salvo com ID: {}", salvo.getId());
            } else {
                log.info("✅ Motorista ID: {} atualizado com sucesso", salvo.getId());
            }
            log.debug("📝 Motorista salvo completo: {}", salvo);
            
            return salvo;
            
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage();
            log.error("❌ Violação de integridade ao salvar motorista: {}", message);
            
            if (message != null) {
                String lowerMessage = message.toLowerCase();
                log.debug("🔍 Mensagem de erro: {}", lowerMessage);

                if (lowerMessage.contains("cpf")) {
                    log.error("❌ CPF duplicado: {}", motorista.getCpf());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com o CPF: " + motorista.getCpf()
                    );
                }

                if (lowerMessage.contains("cnh")) {
                    log.error("❌ CNH duplicada: {}", motorista.getCnh());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com a CNH: " + motorista.getCnh()
                    );
                }
                
                if (lowerMessage.contains("email")) {
                    log.error("❌ Email duplicado");
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com este email"
                    );
                }
            }

            log.error("❌ Erro interno de integridade: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao salvar motorista: {}", e.getMessage(), e);
            throw e;
        }
    }

    public List<Motorista> listar() {
        log.info("📋 Listando todos os motoristas");
        log.debug("🔍 Consultando repositório...");
        
        List<Motorista> motoristas = motoristaRepository.findAll();
        
        log.info("✅ Total de motoristas encontrados: {}", motoristas.size());
        log.debug("📊 IDs dos motoristas: {}", motoristas.stream().map(Motorista::getId).toList());
        
        return motoristas;
    }

    public Motorista buscarPorId(Long id) {
        log.info("🔍 Buscando motorista por ID: {}", id);
        log.debug("🔍 Consultando repositório para ID: {}", id);
        
        Motorista motorista = motoristaRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Motorista não encontrado com id: {}", id);
                    return new BusinessException(
                            ErrorCode.MOTORISTA_NOT_FOUND,
                            "Motorista não encontrado com id: " + id
                    );
                });

        log.info("✅ Motorista encontrado - Nome: {}, CPF: {}, CNH: {}", 
                motorista.getNome(), motorista.getCpf(), motorista.getCnh());
        log.debug("📝 Motorista completo: {}", motorista);
        
        return motorista;
    }

    public Motorista buscarPorCpf(String cpf) {
        log.info("🔍 Buscando motorista por CPF: {}", cpf);
        log.debug("🔍 Consultando repositório para CPF: {}", cpf);
        
        Motorista motorista = motoristaRepository.findByCpf(cpf)
                .orElseThrow(() -> {
                    log.error("❌ Motorista não encontrado com CPF: {}", cpf);
                    return new BusinessException(
                            ErrorCode.MOTORISTA_NOT_FOUND,
                            "Motorista não encontrado com CPF: " + cpf
                    );
                });

        log.info("✅ Motorista encontrado - ID: {}, Nome: {}", motorista.getId(), motorista.getNome());
        log.debug("📝 Motorista completo: {}", motorista);
        
        return motorista;
    }

    public Motorista atualizar(Long id, Motorista dados) {
        log.info("🔄 Atualizando motorista ID: {}", id);
        log.debug("📝 Dados para atualização: Nome='{}', CPF='{}', CNH='{}', Categoria='{}'", 
                 dados.getNome(), dados.getCpf(), dados.getCnh(), dados.getCategoriaCnh());

        Motorista motorista = buscarPorId(id);
        log.debug("📝 Motorista original: {}", motorista);

        // RN-MOT-001: Validar dígitos do CPF se foi alterado
        if (!motorista.getCpf().equals(dados.getCpf())) {
            if (!validarCpf(dados.getCpf())) {
                log.error("❌ CPF inválido: {}", dados.getCpf());
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "CPF inválido. Verifique os dígitos verificadores.");
            }
        }

        // Log das alterações
        if (!motorista.getNome().equals(dados.getNome())) {
            log.debug("📝 Nome alterado: '{}' → '{}'", motorista.getNome(), dados.getNome());
        }
        if (!motorista.getCpf().equals(dados.getCpf())) {
            log.debug("📝 CPF alterado: '{}' → '{}'", motorista.getCpf(), dados.getCpf());
        }
        if (!motorista.getCnh().equals(dados.getCnh())) {
            log.debug("📝 CNH alterada: '{}' → '{}'", motorista.getCnh(), dados.getCnh());
        }
        if (!motorista.getCategoriaCnh().equals(dados.getCategoriaCnh())) {
            log.debug("📝 Categoria CNH alterada: '{}' → '{}'", 
                     motorista.getCategoriaCnh(), dados.getCategoriaCnh());
        }

        motorista.setNome(dados.getNome());
        motorista.setCpf(dados.getCpf());
        motorista.setCnh(dados.getCnh());
        motorista.setCategoriaCnh(dados.getCategoriaCnh());

        try {
            log.debug("🔄 Salvando motorista atualizado...");
            Motorista atualizado = motoristaRepository.save(motorista);
            log.info("✅ Motorista ID: {} atualizado com sucesso", id);
            log.debug("📝 Motorista após atualização: {}", atualizado);
            
            return atualizado;
            
        } catch (DataIntegrityViolationException e) {
            String message = e.getMostSpecificCause().getMessage();
            log.error("❌ Violação de integridade ao atualizar motorista {}: {}", id, message);
            
            if (message != null) {
                String lowerMessage = message.toLowerCase();

                if (lowerMessage.contains("cpf")) {
                    log.error("❌ CPF duplicado: {}", dados.getCpf());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com o CPF: " + dados.getCpf()
                    );
                }

                if (lowerMessage.contains("cnh")) {
                    log.error("❌ CNH duplicada: {}", dados.getCnh());
                    throw new BusinessException(
                            ErrorCode.MOTORISTA_DUPLICATE,
                            "Já existe um motorista com a CNH: " + dados.getCnh()
                    );
                }
            }

            log.error("❌ Erro interno de integridade: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            
        } catch (Exception e) {
            log.error("❌ Erro inesperado ao atualizar motorista {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    public void deletar(Long id) {
        log.info("🗑️ Deletando motorista ID: {}", id);
        
        Motorista motorista = buscarPorId(id);
        log.debug("📝 Motorista a ser deletado: {}", motorista);
        
        motoristaRepository.delete(motorista);
        log.info("✅ Motorista ID: {} deletado com sucesso", id);
    }
    
    // ================ RN-MOT-002: Validação de CNH ================

    /**
     * RN-MOT-002: Valida se a CNH do motorista está válida
     * CNH vencida bloqueia novas viagens
     */
    public void validarCnhParaViagem(Motorista motorista) {
        if (motorista.getDataVencimentoCnh() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "CNH do motorista " + motorista.getNome() + " não tem data de vencimento cadastrada");
        }
        
        LocalDate hoje = LocalDate.now();
        if (motorista.getDataVencimentoCnh().isBefore(hoje)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("CNH do motorista %s está vencida desde %s. " +
                              "Renove a CNH para iniciar uma nova viagem.",
                              motorista.getNome(), motorista.getDataVencimentoCnh()));
        }
    }

    /**
     * RN-MOT-002: Valida se a categoria da CNH é compatível com o veículo
     */
    public void validarCategoriaCnhParaVeiculo(Motorista motorista, Veiculo veiculo) {
        String categoria = motorista.getCategoriaCnh();
        // Categorias para veículos pesados: C, D, E
        if (!categoria.contains("C") && !categoria.contains("D") && !categoria.contains("E")) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("Motorista %s possui categoria %s, mas veículo %s requer categoria C, D ou E",
                              motorista.getNome(), categoria, veiculo.getPlaca()));
        }
    }

    // ================ RN-MOT-003: Validação de ASO e MOPP ================

    /**
     * RN-MOT-003: Valida se o ASO do motorista está válido
     */
    public void validarAsoParaViagem(Motorista motorista) {
        if (motorista.getDataVencimentoAso() == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                "ASO do motorista " + motorista.getNome() + " não tem data de vencimento cadastrada");
        }
        
        LocalDate hoje = LocalDate.now();
        if (motorista.getDataVencimentoAso().isBefore(hoje)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("ASO do motorista %s está vencido desde %s. " +
                              "Realize um novo ASO para iniciar uma nova viagem.",
                              motorista.getNome(), motorista.getDataVencimentoAso()));
        }
    }

    /**
     * RN-MOT-003: Valida se o MOPP é válido para carga perigosa
     */
    public void validarMoppingParaCargaPerigosa(Motorista motorista, Carga carga) {
        if (carga.getTipo() != null && "PERIGOSA".equalsIgnoreCase(carga.getTipo())) {
            if (motorista.getMoppValido() == null || !motorista.getMoppValido()) {
                throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    String.format("Carga perigosa requer MOPP válido. Motorista %s não possui MOPP.",
                                  motorista.getNome()));
            }
        }
    }

    // ================ RN-MOT-004: Validação de Score ================

    /**
     * RN-MOT-004: Valida se o score do motorista permite nova viagem
     * Score < 400: bloqueio de novas viagens
     * Score < 600: alerta para gestor
     */
    public void validarScoreParaViagem(Motorista motorista) {
        if (motorista.getScore() == null) {
            motorista.setScore(1000);
            motoristaRepository.save(motorista);
        }
        
        if (motorista.getScore() < 400) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                String.format("Score do motorista %s é %d (abaixo de 400). " +
                              "Motorista bloqueado para novas viagens.",
                              motorista.getNome(), motorista.getScore()));
        }
        
        if (motorista.getScore() < 600) {
            // Criar alerta para gestor
            alertaService.criarAlertaScoreBaixo(motorista);
            log.warn("⚠️ Motorista {} com score baixo: {}", motorista.getNome(), motorista.getScore());
        }
    }
    
    // ================ RN-MOT-001: Validação de CPF ================

    /**
     * RN-MOT-001: Valida o dígito verificador do CPF
     */
    public static boolean validarCpf(String cpf) {
        // Remove caracteres não numéricos
        cpf = cpf.replaceAll("\\D", "");
        
        // Verifica se tem 11 dígitos
        if (cpf.length() != 11) {
            return false;
        }
        
        // Verifica se todos os dígitos são iguais (CPF inválido)
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }
        
        // Calcula primeiro dígito verificador
        int soma = 0;
        for (int i = 0; i < 9; i++) {
            soma += (cpf.charAt(i) - '0') * (10 - i);
        }
        int primeiroDigito = 11 - (soma % 11);
        if (primeiroDigito >= 10) primeiroDigito = 0;
        
        // Verifica primeiro dígito
        if (primeiroDigito != (cpf.charAt(9) - '0')) {
            return false;
        }
        
        // Calcula segundo dígito verificador
        soma = 0;
        for (int i = 0; i < 10; i++) {
            soma += (cpf.charAt(i) - '0') * (11 - i);
        }
        int segundoDigito = 11 - (soma % 11);
        if (segundoDigito >= 10) segundoDigito = 0;
        
        // Verifica segundo dígito
        return segundoDigito == (cpf.charAt(10) - '0');
    }
}