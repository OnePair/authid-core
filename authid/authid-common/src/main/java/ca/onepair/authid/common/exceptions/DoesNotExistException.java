package ca.onepair.authid.common.exceptions;

public class DoesNotExistException extends Exception {
	private static final String MESSAGE = "The AuthID does not exist!";

	public DoesNotExistException() {
		super(MESSAGE);
	}

}
