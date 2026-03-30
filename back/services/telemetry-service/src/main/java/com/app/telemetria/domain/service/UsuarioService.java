package com.app.telemetria.domain.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.telemetria.domain.entity.HistoricoSenha;
import com.app.telemetria.domain.entity.SessaoAtiva;
import com.app.telemetria.domain.entity.Usuario;
import com.app.telemetria.domain.enums.Perfil;
import com.app.telemetria.domain.exception.BusinessException;
import com.app.telemetria.domain.exception.ErrorCode;
import com.app.telemetria.infrastructure.persistence.HistoricoSenhaRepository;
import com.app.telemetria.infrastructure.persistence.SessaoAtivaRepository;
import com.app.telemetria.infrastructure.persistence.UsuarioRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class UsuarioService {

	private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

	// Regex para validação de senha
	private static final Pattern SENHA_PATTERN = Pattern
			.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).{8,}$");

	@Autowired
	private UsuarioRepository usuarioRepository;

	@Autowired
	private HistoricoSenhaRepository historicoSenhaRepository;

	@Autowired
	private SessaoAtivaRepository sessaoAtivaRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private HttpServletRequest request;

	// ================ CRUD ================

	@Transactional
	public Usuario criar(Usuario usuario, String senhaPlain) {
		log.info("➕ Criando novo usuário: {}", usuario.getLogin());

		// RN-USR-001: Validar complexidade da senha
		validarComplexidadeSenha(senhaPlain);

		// Verificar duplicidade
		if (usuarioRepository.existsByLogin(usuario.getLogin())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Login já existe");
		}
		if (usuarioRepository.existsByEmail(usuario.getEmail())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Email já existe");
		}
		if (usuarioRepository.existsByCpf(usuario.getCpf())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "CPF já existe");
		}

		// Criptografar senha
		String senhaHash = passwordEncoder.encode(senhaPlain);
		usuario.setSenha(senhaHash);

		// RN-USR-001: Definir expiração da senha (90 dias)
		usuario.atualizarExpiracaoSenha();

		// RN-USR-001: Resetar tentativas de falha
		usuario.resetarTentativasFalha();

		try {
			Usuario salvo = usuarioRepository.save(usuario);

			// RN-USR-001: Registrar no histórico de senhas
			HistoricoSenha historico = new HistoricoSenha(salvo.getId(), senhaHash);
			historicoSenhaRepository.save(historico);

			log.info("✅ Usuário criado com ID: {}", salvo.getId());
			return salvo;

		} catch (DataIntegrityViolationException e) {
			log.error("❌ Erro de integridade ao criar usuário: {}", e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Erro ao criar usuário");
		}
	}

	@Transactional
	public void alterarSenha(Long usuarioId, String senhaAtual, String novaSenha) {
		log.info("🔄 Alterando senha do usuário ID: {}", usuarioId);

		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// Verificar senha atual
		if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR, "Senha atual incorreta");
		}

		// RN-USR-001: Validar complexidade da nova senha
		validarComplexidadeSenha(novaSenha);

		// RN-USR-001: Não repetir últimas 5 senhas
		validarHistoricoSenhas(usuario.getId(), novaSenha);

		// Criptografar nova senha
		String novaSenhaHash = passwordEncoder.encode(novaSenha);
		usuario.setSenha(novaSenhaHash);

		// RN-USR-001: Atualizar expiração (90 dias)
		usuario.atualizarExpiracaoSenha();

		// RN-USR-001: Resetar tentativas de falha
		usuario.resetarTentativasFalha();

		usuarioRepository.save(usuario);

		// RN-USR-001: Registrar no histórico de senhas
		HistoricoSenha historico = new HistoricoSenha(usuario.getId(), novaSenhaHash);
		historicoSenhaRepository.save(historico);

		// RN-USR-002: Invalidar todas as sessões ao trocar senha
		sessaoAtivaRepository.deleteByUsuarioId(usuario.getId());

		log.info("✅ Senha do usuário {} alterada com sucesso", usuario.getLogin());
	}

	// ================ RN-USR-001: Política de Senha ================

	private void validarComplexidadeSenha(String senha) {
		if (senha == null || !SENHA_PATTERN.matcher(senha).matches()) {
			throw new BusinessException(ErrorCode.VALIDATION_ERROR,
					"Senha deve ter no mínimo 8 caracteres, incluindo maiúscula, minúscula, número e caractere especial");
		}
	}

	private void validarHistoricoSenhas(Long usuarioId, String novaSenha) {
		List<String> ultimasSenhas = historicoSenhaRepository.findUltimasSenhasHash(usuarioId);

		for (String senhaHash : ultimasSenhas) {
			if (passwordEncoder.matches(novaSenha, senhaHash)) {
				throw new BusinessException(ErrorCode.VALIDATION_ERROR,
						"Não é permitido repetir uma das últimas 5 senhas utilizadas");
			}
		}
	}

	public void registrarTentativaFalha(String login) {
		usuarioRepository.findByLogin(login).ifPresent(usuario -> {
			usuario.registrarTentativaFalha();
			usuarioRepository.save(usuario);
			log.warn("⚠️ Tentativa de login falha para usuário {}. Tentativas: {}", login,
					usuario.getTentativasFalha());
		});
	}

	public void resetarTentativasFalha(String login) {
		usuarioRepository.findByLogin(login).ifPresent(usuario -> {
			usuario.resetarTentativasFalha();
			usuarioRepository.save(usuario);
		});
	}

	public void verificarBloqueio(String login) {
		Usuario usuario = usuarioRepository.findByLogin(login).orElse(null);
		if (usuario != null && usuario.isBloqueado()) {
			throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
					"Conta bloqueada por múltiplas tentativas de login. Tente novamente em alguns minutos");
		}
	}

	public void verificarSenhaExpirada(Usuario usuario) {
		if (usuario.isSenhaExpirada()) {
			throw new BusinessException(ErrorCode.PASSWORD_EXPIRED,
					"Senha expirada. Por favor, realize a troca de senha");
		}
	}

	// ================ RN-USR-002: Sessões ================

	@Transactional
	public SessaoAtiva criarSessao(Usuario usuario, String tokenJwt) {
		// RN-USR-002: Verificar limite de sessões simultâneas (máximo 3)
		long sessoesAtivas = sessaoAtivaRepository.countByUsuarioId(usuario.getId());

		if (sessoesAtivas >= 3) {
			// Remover a sessão mais antiga
			List<SessaoAtiva> sessoes = sessaoAtivaRepository.findByUsuarioIdOrderByDataCriacaoDesc(usuario.getId());
			if (sessoes.size() >= 3) {
				sessaoAtivaRepository.delete(sessoes.get(sessoes.size() - 1));
				log.info("🗑️ Sessão antiga removida para usuário {}", usuario.getLogin());
			}
		}

		// Obter informações da requisição
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty()) {
			ip = request.getRemoteAddr();
		}
		String userAgent = request.getHeader("User-Agent");

		// Definir expiração do token (ex: 1 dia)
		LocalDateTime dataExpiracao = LocalDateTime.now().plusDays(1);

		SessaoAtiva sessao = new SessaoAtiva(usuario.getId(), tokenJwt, ip, userAgent, dataExpiracao);

		SessaoAtiva salva = sessaoAtivaRepository.save(sessao);
		log.info("✅ Sessão criada para usuário {}. Sessões ativas: {}", usuario.getLogin(), sessoesAtivas + 1);

		return salva;
	}

	@Transactional
	public void removerSessao(String tokenJwt) {
		sessaoAtivaRepository.findByTokenJwt(tokenJwt).ifPresent(sessao -> {
			sessaoAtivaRepository.delete(sessao);
			log.info("🗑️ Sessão removida para token: {}...", tokenJwt.substring(0, Math.min(20, tokenJwt.length())));
		});
	}

	@Transactional
	public void removerTodasSessoes(Long usuarioId) {
		sessaoAtivaRepository.deleteByUsuarioId(usuarioId);
		log.info("🗑️ Todas as sessões removidas para usuário ID: {}", usuarioId);
	}

	@Transactional
	public void limparSessoesExpiradas() {
	    try {
	        int removidas = sessaoAtivaRepository.deleteAllExpiradas();
	        if (removidas > 0) {
	            log.info("🗑️ {} sessões expiradas removidas", removidas);
	        }
	    } catch (Exception e) {
	        log.error("❌ Erro ao remover sessões expiradas: {}", e.getMessage());
	    }
	}

	
	public void verificarLimiteSessoes(Usuario usuario) {
		long sessoesAtivas = sessaoAtivaRepository.countByUsuarioId(usuario.getId());
		if (sessoesAtivas >= 3) {
			log.warn("⚠️ Usuário {} atingiu limite de sessões: {}", usuario.getLogin(), sessoesAtivas);
		}
	}

	@Transactional
	public void atualizarUltimoAcesso(Usuario usuario) {
		usuario.setUltimoAcesso(LocalDateTime.now());
		usuarioRepository.save(usuario);
	}

	@Transactional
	public void atualizarUltimoAcessoDaSessao(String tokenJwt) {
		sessaoAtivaRepository.findByTokenJwt(tokenJwt).ifPresent(sessao -> {
			sessao.setUltimoAcesso(LocalDateTime.now());
			sessaoAtivaRepository.save(sessao);
		});
	}

	// ================ RN-USR-002: MFA ================

	public void ativarMfa(Long usuarioId, String secret) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		usuario.setMfaSecret(secret);
		usuario.setMfaAtivado(true);
		usuarioRepository.save(usuario);

		log.info("🔐 MFA ativado para usuário: {}", usuario.getLogin());
	}

	public void desativarMfa(Long usuarioId) {
		Usuario usuario = usuarioRepository.findById(usuarioId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		usuario.setMfaSecret(null);
		usuario.setMfaAtivado(false);
		usuarioRepository.save(usuario);

		log.info("🔓 MFA desativado para usuário: {}", usuario.getLogin());
	}

	public boolean isMfaObrigatorio(Usuario usuario) {
		// RN-USR-002: MFA obrigatório para ADMIN_TENANT e SUPER_ADMIN
		return usuario.getPerfil() == Perfil.ADMIN || usuario.getPerfil() == Perfil.SUPER_ADMIN;
	}

	public boolean isMfaAtivado(Usuario usuario) {
		return usuario.getMfaAtivado() != null && usuario.getMfaAtivado();
	}
}