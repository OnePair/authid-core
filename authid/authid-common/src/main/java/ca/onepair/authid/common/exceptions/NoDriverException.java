package ca.onepair.authid.common.exceptions;

public class NoDriverException extends Exception {

	private static final String EXCEPTION_MESSAGE = "No driver found";

	public NoDriverException(String message) {
		super(message);
	}

	public NoDriverException() {
		this(EXCEPTION_MESSAGE);
	}

}
