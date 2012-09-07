package ru.ufalinux.tasp.dataworks;

import java.util.Vector;

import ru.ufalinux.tasp.jabberworks.Command;

public class FIFO {
	private Vector<Command> data;

	public FIFO() {
		data = new Vector<Command>();
	}

	public void push(Command comm) {
		data.add(comm);
	}

	public Command poll() {
		Command comm;
		if (data.isEmpty())
			comm = null;
		else{
			comm = data.get(0);
			data.remove(0);
		}
		return comm;
	}

	public boolean isEmpty(){
		return data.isEmpty();
	}

	public int size(){
		return data.size();
	}

	public String toString(){
		String out="";
		for(Command comm:data){
			out+="comm "+comm.toString()+". ";
		}
		return out;
	}
	
}
