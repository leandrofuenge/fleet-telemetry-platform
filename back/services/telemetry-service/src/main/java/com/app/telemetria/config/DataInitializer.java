package com.app.telemetria.config;

import com.app.telemetria.entity.Usuario;
import com.app.telemetria.enums.Perfil;
import com.app.telemetria.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // Verifica se já existem usuários
        if (usuarioRepository.count() == 0) {
            // Criar usuário admin com CPF
            Usuario admin = new Usuario();
            admin.setLogin("admin");
            admin.setSenha(passwordEncoder.encode("admin123"));
            admin.setNome("Administrador Sistema");
            admin.setEmail("admin@telemetria.com");
            admin.setCpf("11122233344"); // <-- CPF ADICIONADO
            admin.setAtivo(true);
            admin.setPerfil(Perfil.ADMIN);
            usuarioRepository.save(admin);

            // Criar operador com CPF
            Usuario operador = new Usuario();
            operador.setLogin("operador");
            operador.setSenha(passwordEncoder.encode("operador123"));
            operador.setNome("Operador Padrão");
            operador.setEmail("operador@telemetria.com");
            operador.setCpf("22233344455"); // <-- CPF ADICIONADO
            operador.setAtivo(true);
            operador.setPerfil(Perfil.OPERADOR);
            usuarioRepository.save(operador);

            System.out.println("✅ Usuários iniciais criados com sucesso!");
        }
    }
}