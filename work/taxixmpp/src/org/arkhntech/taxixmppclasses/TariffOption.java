package org.arkhntech.taxixmppclasses;

public class TariffOption {

	Float abs = (float) .0;
	Float rel = (float) .0;
	long order = 0;

	public TariffOption(long order, Float abs, Float rel) {
		this.abs = abs;
		this.rel = rel;
		this.order = order;
	}
}
