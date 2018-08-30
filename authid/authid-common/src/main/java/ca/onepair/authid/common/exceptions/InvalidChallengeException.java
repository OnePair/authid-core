package ca.onepair.authid.common.exceptions;

public class InvalidChallengeException extends Exception {
	private static final String INVALID_CHALLENGE_MESSAGE = "Challenge is invalid";

	public InvalidChallengeException(String message) {
		super(message);
	}

	public InvalidChallengeException() {
		this(INVALID_CHALLENGE_MESSAGE);
	}

}
