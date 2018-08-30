package ca.onepair.authid.common.exceptions;

public class AuthIDDriverException extends Exception {

	public static final String NO_WALLET_EXCEPTION = "Wallet was not set";

	public AuthIDDriverException(String message) {
		super(message);
	}

}
