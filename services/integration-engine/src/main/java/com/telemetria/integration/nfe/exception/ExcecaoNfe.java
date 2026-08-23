package com.telemetria.integration.nfe.exception;

/**
 * Exceção a ser lançada na ocorrência de falhas provenientes da Nota Fiscal Eletronica.
 * 
 * @author Samuel Oliveira - samuel@swconsultoria.com.br - www.swconsultoria.com.br
 */
public class ExcecaoNfe extends Exception {

	public ExcecaoNfe(String message) {
		super(message);
	}

	public ExcecaoNfe(String message, Throwable cause) {
		super(message, cause);
	}

	public ExcecaoNfe(Throwable cause) {
		super(cause);
	}
}