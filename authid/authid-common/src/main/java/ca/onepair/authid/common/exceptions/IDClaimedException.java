package ca.onepair.authid.common.exceptions;

public class IDClaimedException extends Exception {
	private static final String MESSAGE = "The ID Has already been claimed";

	public IDClaimedException() {
		super(MESSAGE);
	}

}
