package com.wilee8.coyotereader2.otto;


import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

public final class BusProvider {
	private static final Bus BUS = new Bus(ThreadEnforcer.MAIN);

	public static Bus getInstance() {
		return BUS;
	}

	private BusProvider() {
		// No instances
	}
}
