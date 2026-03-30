package com.telemetria.infrastructure.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.telemetria.domain.entity.Usuario;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret; 

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${jwt.refresh.expiration}")
    private long refreshExpirationMs;

    // ==============================
    // Converte a string secreta em uma chave segura de 256 bits
    // ==============================
    private SecretKey getSigningKey() {
        log.trace("🔑 Obtendo chave de assinatura JWT");
        byte[] keyBytes = secret.getBytes();
        log.trace("📏 Tamanho da chave: {} bytes", keyBytes.length);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ==============================
    // Gera token de acesso
    // ==============================
    public String generateAccessToken(Usuario usuario) {
        log.debug("🔐 Gerando access token para usuário: {}", usuario.getLogin());
        
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + jwtExpirationMs);
        
        log.debug("📅 Issued at: {}, Expiration: {}, Expiration ms: {}", 
                 issuedAt, expiration, jwtExpirationMs);

        String token = Jwts.builder()
                .setSubject(usuario.getLogin())
                .claim("userId", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim("perfil", usuario.getPerfil())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("✅ Access token gerado: {}...", token.substring(0, Math.min(20, token.length())));
        return token;
    }

    // ==============================
    // Gera refresh token
    // ==============================
    public String generateRefreshToken(Usuario usuario) {
        log.debug("🔄 Gerando refresh token para usuário: {}", usuario.getLogin());
        
        Date issuedAt = new Date();
        Date expiration = new Date(System.currentTimeMillis() + refreshExpirationMs);
        
        log.debug("📅 Issued at: {}, Expiration: {}, Expiration ms: {}", 
                 issuedAt, expiration, refreshExpirationMs);

        String token = Jwts.builder()
                .setSubject(usuario.getLogin())
                .claim("userId", usuario.getId())
                .setIssuedAt(issuedAt)
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();

        log.debug("✅ Refresh token gerado: {}...", token.substring(0, Math.min(20, token.length())));
        return token;
    }

    // ==============================
    // Valida token
    // ==============================
    public boolean isTokenValid(String token) {
        log.debug("🔍 Validando token: {}...", token.substring(0, Math.min(20, token.length())));
        
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
            
            Date expiration = claims.getExpiration();
            Date now = new Date();
            
            log.debug("📅 Token expira em: {}, Agora: {}, Válido: {}", 
                     expiration, now, expiration.after(now));
            
            if (expiration.before(now)) {
                log.warn("⚠️ Token expirado em: {}", expiration);
                return false;
            }
            
            log.debug("✅ Token válido para usuário: {}", claims.getSubject());
            return true;
            
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ Token expirado: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("⚠️ Token mal formatado: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("⚠️ Assinatura inválida: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("⚠️ Token não suportado: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("⚠️ Argumento inválido: {}", e.getMessage());
            return false;
        } catch (JwtException e) {
            log.warn("⚠️ Erro JWT: {}", e.getMessage());
            return false;
        }
    }

    // ==============================
    // Extrai login do token
    // ==============================
    public String getLogin(String token) {
        log.debug("🔍 Extraindo login do token: {}...", token.substring(0, Math.min(20, token.length())));
        
        try {
            String login = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            
            log.debug("✅ Login extraído: {}", login);
            return login;
            
        } catch (ExpiredJwtException e) {
            log.warn("⚠️ Token expirado, retornando subject do token expirado: {}", e.getClaims().getSubject());
            return e.getClaims().getSubject();
        } catch (JwtException e) {
            log.error("❌ Erro ao extrair login do token: {}", e.getMessage());
            return null;
        }
    }

    // ==============================
    // Extrai claims do token (método auxiliar)
    // ==============================
    private Claims extractAllClaims(String token) {
        log.trace("🔍 Extraindo claims do token");
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("📝 Retornando claims de token expirado");
            return e.getClaims();
        }
    }

    // ==============================
    // Verifica se token está expirado
    // ==============================
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            boolean expired = expiration.before(new Date());
            log.debug("📅 Token expira em: {}, Expirado: {}", expiration, expired);
            return expired;
        } catch (Exception e) {
            log.error("❌ Erro ao verificar expiração: {}", e.getMessage());
            return true;
        }
    }

    // ==============================
    // Obtém tempo restante do token em minutos
    // ==============================
    public long getTokenRemainingTime(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            Date now = new Date();
            long remainingMs = expiration.getTime() - now.getTime();
            long remainingMinutes = remainingMs / (60 * 1000);
            
            log.debug("⏳ Tempo restante do token: {} minutos", remainingMinutes);
            return remainingMinutes;
        } catch (Exception e) {
            log.error("❌ Erro ao calcular tempo restante: {}", e.getMessage());
            return 0;
        }
    }
}