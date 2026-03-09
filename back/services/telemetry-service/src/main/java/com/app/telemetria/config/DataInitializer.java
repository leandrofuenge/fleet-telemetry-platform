package com.app.telemetria.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;

import com.app.telemetria.entity.Usuario;
import com.app.telemetria.repository.UsuarioRepository;
import com.app.telemetria.entity.Perfil;

@Configuration
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {

        if (usuarioRepository.findByLogin("admin").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setLogin("admin");

            admin.setSenha(passwordEncoder.encode("123456"));
            admin.setNome("Administrador");
            admin.setPerfil(Perfil.ADMIN); 
            admin.setEmail("admin@telemetria.com"); 
            usuarioRepository.save(admin);

            System.out.println("Usuário ADMIN criado com login: admin / senha: 123456");
        } else {
        	System.out.println("Perfil ja criado");       }
    }
}
