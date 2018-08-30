package ca.onepair.authid.common.exceptions;

public class InvalidCertException extends Exception {
	private static final String INVALID_CERT_EXCEPTION = "Certificate is not valid";

	public InvalidCertException(String message) {
		super(message);
	}

	public InvalidCertException() {
		this(INVALID_CERT_EXCEPTION);
	}

}
