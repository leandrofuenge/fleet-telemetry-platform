package com.app.telemetria.config;

import com.app.telemetria.service.LocalizacaoValidationService;
import com.app.telemetria.security.JwtService;
import com.app.telemetria.repository.UsuarioRepository;
import com.app.telemetria.repository.MotoristaRepository;
import com.app.telemetria.repository.ViagemRepository;
import com.app.telemetria.entity.Usuario;
import com.app.telemetria.entity.Motorista;
import com.app.telemetria.entity.Perfil;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@Component
public class LocalizacaoValidationFilter extends OncePerRequestFilter {
    
    private final LocalizacaoValidationService localizacaoService;
    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;
    private final MotoristaRepository motoristaRepository;
    private final ViagemRepository viagemRepository;
    
    public LocalizacaoValidationFilter(
            LocalizacaoValidationService localizacaoService,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            MotoristaRepository motoristaRepository,
            ViagemRepository viagemRepository) {
        this.localizacaoService = localizacaoService;
        this.jwtService = jwtService;
        this.usuarioRepository = usuarioRepository;
        this.motoristaRepository = motoristaRepository;
        this.viagemRepository = viagemRepository;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        

        String path = request.getRequestURI();
        if (path.contains("/auth/login") || path.contains("/auth/refresh")) {
            chain.doFilter(request, response);
            return;
        }
        
        // Validar apenas requisições autenticadas
        String token = extractToken(request);
        if (token != null && jwtService.isTokenValid(token)) {
            String login = jwtService.getLogin(token);
            
            // Buscar usuário
            Optional<Usuario> usuarioOpt = usuarioRepository.findByLogin(login);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                
                // Verificar se o usuário é um MOTORISTA
                if (usuario.getPerfil() == Perfil.MOTORISTA) {
                    
                    // Buscar motorista pelo CPF
                    Optional<Motorista> motoristaOpt = buscarMotoristaPorCPF(usuario);
                    
                    if (motoristaOpt.isPresent()) {
                        Motorista motorista = motoristaOpt.get();
                        Long motoristaId = motorista.getId();
                        
                        // Buscar veículo associado ao motorista através de viagem ativa
                        Long veiculoId = null;
                        var viagemAtiva = viagemRepository.findByMotoristaIdAndStatus(motoristaId, "EM_ANDAMENTO");
                        if (viagemAtiva.isPresent()) {
                            veiculoId = viagemAtiva.get().getVeiculo().getId();
                        }
                        
                        String clienteIP = getClientIP(request);
                        
                        // Validar localização em background (não bloquear a requisição)
                        Long finalVeiculoId = veiculoId;
                        new Thread(() -> {
                            localizacaoService.validarLocalizacaoMotorista(
                                motoristaId, finalVeiculoId, clienteIP
                            );
                        }).start();
                    }
                }
            }
        }
        
        chain.doFilter(request, response);
    }
    
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
    
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        // Se estiver rodando localmente, pode pegar 127.0.0.1
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            // Para testes locais, pode usar um IP fixo ou ler de um header customizado
            ip = request.getHeader("X-Real-IP");
            if (ip == null || ip.isEmpty()) {
                ip = "177.87.34.123"; // IP de exemplo do Brasil (São Paulo)
            }
        }
        return ip;
    }
    
    /**
     * Método para buscar motorista associado ao usuário
     */
    private Optional<Motorista> buscarMotoristaPorCPF(Usuario usuario) {
        // Estratégia 1: Buscar motorista pelo CPF 
        if (usuario.getCpf() != null) {
            Optional<Motorista> motorista = motoristaRepository.findByCpf(usuario.getEmail());
            if (motorista.isPresent()) {
                return motorista;
            }
        }

        return Optional.empty();
        
    }
}