package com.telemetria.api.dto.request;

public class AuthRequest {

    private String login;
    private String senha;
    private String mfaCodigo;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }

    public String getMfaCodigo() { return mfaCodigo; }
    public void setMfaCodigo(String mfaCodigo) { this.mfaCodigo = mfaCodigo; }
}
