package org.arkhntech.taxixmppclasses;

import java.util.Vector;

public class DCard{
	int num=0;
	int discountid=0;
	Vector<TariffOption> options=new Vector<TariffOption>();
	
	public DCard(int num,int discountid,Vector<TariffOption>options){
		this.num=num;
		this.discountid=discountid;
		this.options=options;
	}
}
