package edu.wisc.cs.sdn.vnet.rt;

import edu.wisc.cs.sdn.vnet.Device;
import edu.wisc.cs.sdn.vnet.DumpFile;
import edu.wisc.cs.sdn.vnet.Iface;
import java.util.*;
import net.floodlightcontroller.packet.*;

/**
 * @author Aaron Gember-Jacobson and Anubhavnidhi Abhashkumar
 */
public class Router extends Device
{	
	/** Routing table for the router */
	private RouteTable routeTable;
	
	/** ARP cache for the router */
	private ArpCache arpCache;
	
	/**
	 * Creates a router for a specific host.
	 * @param host hostname for the router
	 */
	public Router(String host, DumpFile logfile)
	{
		super(host,logfile);
		this.routeTable = new RouteTable();
		this.arpCache = new ArpCache();
	}
	
	/**
	 * @return routing table for the router
	 */
	public RouteTable getRouteTable()
	{ return this.routeTable; }
	
	/**
	 * Load a new routing table from a file.
	 * @param routeTableFile the name of the file containing the routing table
	 */
	public void loadRouteTable(String routeTableFile)
	{
		if (!routeTable.load(routeTableFile, this))
		{
			System.err.println("Error setting up routing table from file "
					+ routeTableFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static route table");
		System.out.println("-------------------------------------------------");
		System.out.print(this.routeTable.toString());
		System.out.println("-------------------------------------------------");
	}
	
	/**
	 * Load a new ARP cache from a file.
	 * @param arpCacheFile the name of the file containing the ARP cache
	 */
	public void loadArpCache(String arpCacheFile)
	{
		if (!arpCache.load(arpCacheFile))
		{
			System.err.println("Error setting up ARP cache from file "
					+ arpCacheFile);
			System.exit(1);
		}
		
		System.out.println("Loaded static ARP cache");
		System.out.println("----------------------------------");
		System.out.print(this.arpCache.toString());
		System.out.println("----------------------------------");
	}

	/**
	 * Handle an Ethernet packet received on a specific interface.
	 * @param etherPacket the Ethernet packet that was received
	 * @param inIface the interface on which the packet was received
	 */
	public void handlePacket(Ethernet etherPacket, Iface inIface)
	{
	    System.out.println("Inside handlePacket()");

	    System.out.println("*** -> Received packet: " +
            etherPacket.toString().replace("\n", "\n\t"));

	    /********************************************************************/
	    /* TODO: Handle packets*/

	    if (etherPacket.getEtherType() == Ethernet.TYPE_IPv4) {
		// Get the payload
		IPv4 ipv4 = (IPv4) etherPacket.getPayload();

		// Getting the old checksum
	        short oldChecksum = ipv4.getChecksum(); 	
		System.out.println("oldChecksum: " + oldChecksum);
	
		// Calculating the checksum again
		ipv4.resetChecksum();
		ipv4.serialize();
		
		if (ipv4.getChecksum() == oldChecksum) {
		    byte ttl = ipv4.getTtl();
		    ttl = (byte)(ttl - (byte)1);
		    if (ttl > 0) {
			ipv4.setTtl(ttl);
			
			// Now the checksum will match at the other router
			ipv4.resetChecksum();
			ipv4.serialize();
			
			// Check if the ip matches
			int destIP = ipv4.getDestinationAddress();

			// Checks if there is a interface with the packet destination
			// IP
			Map<String, Iface> interfaces = this.getInterfaces();
			System.out.println("interfaces: " + Arrays.asList(interfaces));
		
			boolean shouldPacketBeForwarded = true;
			for (String name : interfaces.keySet()) {   
			    Iface destIface = interfaces.get(name);
			    if (!destIface.getName().equals(inIface.getName())) {
				if (destIface.getIpAddress() == destIP) {
				    shouldPacketBeForwarded = false;
				}
			    }
			}

			if (shouldPacketBeForwarded) {
			    RouteEntry entry = routeTable.lookup(destIP); 
			    if (entry != null) {
				// gateway is not zero then the destination is outside the network
				// need to use gateway
				if (entry.getGatewayAddress() != 0) {
				    destIP = entry.getGatewayAddress();
				}
				
				//Call lookup (ArpCache Class)
				System.out.println("Dest ip: " + destIP + ", " +  IPv4.fromIPv4Address(destIP) + ", " + Integer.toBinaryString(destIP));
				ArpEntry arpEntry = arpCache.lookup(destIP);				

				//This returns an address that will be the destMac
				//Update the source Mac in the ethernet frame using the Outgoing Inface Mac
				MACAddress sourceMac = entry.getInterface().getMacAddress();
				String newSourceMac = sourceMac.toString(); 
				
				String newDestMac = arpEntry.getMac().toString();
				System.out.println("newSourceMac: " + newSourceMac);
				System.out.println("newDestMac: " + newDestMac);

				etherPacket.setDestinationMACAddress(newDestMac);
				etherPacket.setSourceMACAddress(newSourceMac);
				//After all this, call sendPacket(Device Class) 
				
				// Avoid routing to the same interface
				if (entry.getInterface() != inIface) {
				    this.sendPacket(etherPacket, entry.getInterface());
				}
			    }
			}
		    }
		}
	    }
	    /********************************************************************/
	}
}
