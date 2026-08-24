package com.telemetria.api.dto.response;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MotoristaDTO {

    private Long tenantId;
    
    @NotBlank(message = "Nome é obrigatório")
    private String nome;
    
    @NotBlank(message = "CPF é obrigatório")
    @Size(min = 11, max = 14, message = "CPF deve ter entre 11 e 14 caracteres")
    private String cpf;
    
    @NotBlank(message = "CNH é obrigatória")
    private String cnh;
    
    @NotBlank(message = "Categoria da CNH é obrigatória")
    private String categoriaCnh;

    private LocalDate dataVencimentoCnh;
    private LocalDate dataVencimentoAso;
    private Boolean moppValido;
    private String email;
    private String telefone;
    
    // getters e setters
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    
    public String getCnh() { return cnh; }
    public void setCnh(String cnh) { this.cnh = cnh; }
    
    public String getCategoriaCnh() { return categoriaCnh; }
    public void setCategoriaCnh(String categoriaCnh) { this.categoriaCnh = categoriaCnh; }

    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public LocalDate getDataVencimentoCnh() { return dataVencimentoCnh; }
    public void setDataVencimentoCnh(LocalDate dataVencimentoCnh) { this.dataVencimentoCnh = dataVencimentoCnh; }
    public LocalDate getDataVencimentoAso() { return dataVencimentoAso; }
    public void setDataVencimentoAso(LocalDate dataVencimentoAso) { this.dataVencimentoAso = dataVencimentoAso; }
    public Boolean getMoppValido() { return moppValido; }
    public void setMoppValido(Boolean moppValido) { this.moppValido = moppValido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
}
