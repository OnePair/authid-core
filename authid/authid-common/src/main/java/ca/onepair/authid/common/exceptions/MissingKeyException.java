package ca.onepair.authid.common.exceptions;

public class MissingKeyException extends Exception {
	private static final String EXCEPTION_MESSAGE = "Key does not exist in this wallet";

	public MissingKeyException(String message) {
		super(message);
	}

	public MissingKeyException() {
		this(EXCEPTION_MESSAGE);
	}

}
