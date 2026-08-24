package com.telemetria.integration.nfe.exception;

/**
 * Exceção a ser lançada na ocorrência de falhas provenientes da validação da Nota Fiscal Eletronica.
 * 
 */
public class ExcecaoValidacaoNfe extends ExcecaoNfe {

	public ExcecaoValidacaoNfe(Throwable e) {
		super(e);
	}

	public ExcecaoValidacaoNfe(String message) {
		super(message);
	}

	public ExcecaoValidacaoNfe(String message, Throwable cause) {
		super(message, cause);
	}
}
