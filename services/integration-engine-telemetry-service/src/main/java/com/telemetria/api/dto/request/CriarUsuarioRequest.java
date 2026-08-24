package com.telemetria.api.dto.request;

import com.telemetria.domain.enums.Perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CriarUsuarioRequest {
    private Long tenantId;
    @NotBlank private String login;
    @NotBlank private String senha;
    @NotBlank private String nome;
    private String email;
    @NotBlank private String cpf;
    @NotNull private Perfil perfil;

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public Perfil getPerfil() { return perfil; }
    public void setPerfil(Perfil perfil) { this.perfil = perfil; }
}
