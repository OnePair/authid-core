package ca.onepair.authid.common.exceptions;

public class RevokedException extends Exception {
	private static String MESSAGE = "This has been revoked";

	public RevokedException() {
		super(MESSAGE);
	}
}
