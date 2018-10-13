package edu.wisc.cs.sdn.vnet.sw;

import java.util.*;
import java.util.concurrent.*;
import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.*;

public class SwitchTable
{
    // ip address -> interface (router table)
    ConcurrentHashMap<String, SwitchData> table;

    public SwitchTable() {
	table = new ConcurrentHashMap<>();
    }   

    public void addMACAddress(String mac, Iface iface) {
	table.put(mac, new SwitchData(iface));
    }

    public boolean checkMACExists(String mac) {
	return table.containsKey(mac);
    }

    public Iface getMACIface(String mac) {
	if (checkMACExists(mac)) {
	    return table.get(mac).getIface();
	}
	return null;
    }

    public String toString() {
	StringBuilder sb = new StringBuilder();
	for (String mac: table.keySet()) {
	    sb.append(mac + "->" + table.get(mac).toString() + ", ");
	}
	return sb.toString();
    }

    public void timeout() {
	new Timer().schedule(
	    new TimerTask() {
		public void run () {
		    //System.out.println("Inside run");
		    for (String mac : table.keySet()) {
			SwitchData sd = table.get(mac);
			//System.out.println("time diff: " + (System.currentTimeMillis() - sd.getTime()));
			if (System.currentTimeMillis() - sd.getTime() >= 15000) {
			    table.remove(mac);
			    //System.out.println("removing mac");
			}
		    }
		}
	    }, 15000);
    }
}

class SwitchData {
    private Iface iface;
    private long time;

    SwitchData(Iface iface) {
	this.iface = iface;
	this.time = System.currentTimeMillis();
    }   

    public long getTime() {
	return this.time;
    }   

    public Iface getIface() {
	return this.iface;
    }

    public String toString() {
	return iface.getName() + "time: " + getTime(); 
    }
}
