package edu.wisc.cs.sdn.vnet.sw;

import net.floodlightcontroller.packet.Ethernet;
import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import net.floodlightcontroller.packet.*;
import org.openflow.util.HexString;
import java.util.Map;
import java.util.*;

/**
 * @author Aaron Gember-Jacobson
 */
public class Switch extends Device
{	
    SwitchTable switchTable;
    //Thread t;

    /**
     * Creates a router for a specific host.
     * @param host hostname for the router
     */
    public Switch(String host, DumpFile logfile)
    {
	super(host,logfile);
	switchTable = new SwitchTable();

    }

    /**
     * Handle an Ethernet packet received on a specific interface.
     * @param etherPacket the Ethernet packet that was received
     * @param inIface the interface on which the packet was received
     */
    public void handlePacket(Ethernet etherPacket, Iface inIface)
    {
	System.out.println("*** -> Received packet: " +
		etherPacket.toString().replace("\n", "\n\t"));

	/********************************************************************/
	/* TODO: Handle packets                                             */
	String sourceMAC = etherPacket.getSourceMAC().toString();
	String destMAC = etherPacket.getDestinationMAC().toString();
	//System.out.println("Source MAC: " + sourceMAC + ", destMAC: " + destMAC);

	// Put source MAC
	switchTable.addMACAddress(sourceMAC, inIface);
	//System.out.println("switchTable: " + Arrays.asList(switchTable));
	
	switchTable.timeout();

	Iface destIface = switchTable.getMACIface(destMAC);
	//System.out.println("DestIface: " + destIface);
	if (destIface == null) {

	    // broadcast
	    Map<String, Iface> interfaces = this.getInterfaces();
	    System.out.println(Arrays.asList(interfaces));

	    for (String name : interfaces.keySet()) {	
		destIface = interfaces.get(name);
		if (!destIface.getName().equals(inIface.getName())) {
		    if (this.sendPacket(etherPacket, destIface)) {
			//System.out.println("Packet sent");
		    }
		}
	    }
	} else {
	    this.sendPacket(etherPacket, destIface);		    
	}


	//System.out.println("Done with handlePacket()");
	/********************************************************************/
    }
}
