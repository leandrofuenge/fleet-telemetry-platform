package com.telemetria.application.startup;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.telemetria.domain.entity.Usuario;
import com.telemetria.domain.enums.Perfil;
import com.telemetria.infrastructure.persistence.UsuarioRepository;

@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("🚀 Inicializando DataInitializer...");
        
        try {
            criarUsuarioAdminSeNaoExistir();
            
        } catch (Exception e) {
            log.error("❌ Erro ao inicializar dados: {}", e.getMessage(), e);
        }
    }
    
    private void criarUsuarioAdminSeNaoExistir() {
        Optional<Usuario> adminExistente = usuarioRepository.findByLogin("admin");
        
        if (adminExistente.isEmpty()) {
            log.info("🔧 Usuário 'admin' não encontrado. Criando...");
            
            Usuario admin = new Usuario();
            admin.setLogin("admin");
            
            String senhaCodificada = passwordEncoder.encode("admin123");
            admin.setSenha(senhaCodificada);
            admin.setNome("Administrador Sistema");
            admin.setEmail("admin@telemetria.com");
            admin.setCpf("11122233344");
            admin.setAtivo(true);
            admin.setPerfil(Perfil.ADMIN);
            
            log.debug("🔑 Senha original: admin123");
            log.debug("🔑 Senha codificada: {}", senhaCodificada);
            
            usuarioRepository.save(admin);
            log.info("✅ Usuário ADMIN criado com sucesso!");
            
            boolean validacao = passwordEncoder.matches("admin123", senhaCodificada);
            log.debug("✅ Teste de validação da senha: {}", validacao ? "OK" : "FALHOU");
            
        } else {
            log.info("✅ Usuário 'admin' já existe no banco. ID: {}", adminExistente.get().getId());
        }
    }
}