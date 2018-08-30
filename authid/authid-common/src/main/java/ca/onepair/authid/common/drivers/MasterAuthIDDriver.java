package ca.onepair.authid.common.drivers;

import java.util.HashMap;
import java.util.Map;

import ca.onepair.authid.common.exceptions.NoDriverException;

public class MasterAuthIDDriver {

	private Map<String, AuthIDDriver> authIDDrivers;

	public MasterAuthIDDriver(Map<String, AuthIDDriver> authIDDrivers) {
		this.authIDDrivers = authIDDrivers;
	}

	public MasterAuthIDDriver() {
		this(new HashMap<String, AuthIDDriver>());
	}

	public void addDriver(AuthIDDriver authIDDriver, String protocolIdentifier) {
		this.authIDDrivers.put(protocolIdentifier.toUpperCase(), authIDDriver);
	}

	public AuthIDDriver getAuthIDDriver(String protocol) throws NoDriverException {
		protocol = protocol.toUpperCase();
		if (!this.authIDDrivers.containsKey(protocol))
			throw new NoDriverException();

		return this.authIDDrivers.get(protocol);
	}

}
