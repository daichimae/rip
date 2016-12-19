import java.net.DatagramPacket;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Math.pow;

/**
 * A routing table for RipRouter.
 *
 * @author Daichi Mae
 */
public class RoutingTable {
    static final short IPv4 = 1;
    private List<RoutingTableEntry> routingTableEntries = new ArrayList<>();

    /**
     * Add an entry to the routing table.
     *
     * @param addressFamilyIdentifier
     * @param routingTag
     * @param destination
     * @param subnetMask
     * @param nextHop
     * @param metric the cost of the network
     */
    public void addEntry(short addressFamilyIdentifier, short routingTag,
                         byte[] destination, byte[] subnetMask, byte[] nextHop,
                         int metric) {
        RoutingTableEntry entry = new RoutingTableEntry();
        entry.addressFamilyIdentifier = addressFamilyIdentifier;
        entry.routeTag = routingTag;
        //entry.destination = destination;
        entry.destination = destination;
        entry.subnetMask = subnetMask;
        entry.nextHop = nextHop;
        entry.metric = metric;
        routingTableEntries.add(entry);
    }

    /**
     *  Add an entry to the routing table.
     *
     * @param entry
     */
    public void addEntry(RoutingTableEntry entry) {
        routingTableEntries.add(entry);
    }

    /**
     * Accessor
     *
     * @return List of the rouging table entries
     */
    public List<RoutingTableEntry> getEntries() {
        return routingTableEntries;
    }

    /**
     * Accessor
     *
     * @param destination the destination part of an entry
     * @return Entry object or null
     */
    public RoutingTableEntry getEntryByDestination(byte[] destination) {
        for(RoutingTableEntry entry : routingTableEntries) {
            if(Arrays.equals(destination, entry.destination))
                return entry;
        }
        return null;
    }

    /**
     * Return true if there's any routeChangeFlag up otherwise return false.
     *
     * @return
     */
    public boolean isUpdated() {
        for (RoutingTableEntry entry : routingTableEntries) {
            if(entry.routeChangeFlag)
                return true;
        }
        return false;
    }

    /**
     * Get all the routeChangeFlags down.
     *
     */
    public void resetRoutingChangeFlags() {
        for (RoutingTableEntry entry : routingTableEntries) {
            entry.routeChangeFlag = false;
        }
    }

    /**
     * Create a byte array that the DatagramPacket class uses to create a UDP
     * packet. Uses split horizon with poisoned reverse.
     *
     * @param command RIP command
     * @param receiver the receiver of the packet
     * @return data to be sent
     */
    public byte[] createPacket(byte command, byte[] receiver) {
        ByteBuffer bb = ByteBuffer.allocate(4 + (routingTableEntries.size() * 20));

        // header
        bb.put(command);
        bb.put(RipRouter.RIP_VERSION);
        bb.putShort((short) 0); // must be zero

        // entries
        for(RoutingTableEntry entry : routingTableEntries) {
            bb.putShort(entry.addressFamilyIdentifier);
            bb.putShort(entry.routeTag);
            bb.put(entry.destination);
            bb.put(entry.subnetMask);
            bb.put(entry.nextHop);
            if(Arrays.equals(receiver, entry.nextHop)
                    && !Arrays.equals(entry.destination, entry.nextHop)) {
                bb.putInt(RipRouter.INF); // poison the entry
            } else {
                bb.putInt(entry.metric);
            }
        }
        return bb.array();
    }

    /**
     * Create a routing table from a packet.
     *
     * @param packet UDP packet
     * @return RoutingTable object
     */
    public static RoutingTable createTable(DatagramPacket packet) {
        RoutingTable routingTable = new RoutingTable();
        int numberOfEntries = (packet.getLength() - 4 ) / 20;
        ByteBuffer bb = ByteBuffer.wrap(packet.getData());

        bb.getInt(); // discard the header


        for(int i = 0; i < numberOfEntries; i++) {
            short addressFamilyIdentifier = bb.getShort();
            short routingTag = bb.getShort();
            byte[] destination = {bb.get(), bb.get(), bb.get(), bb.get()};
            byte[] subnetMask = {bb.get(), bb.get(), bb.get(), bb.get()};
            byte[] nextHop = {bb.get(), bb.get(), bb.get(), bb.get()};
            int metric = bb.getInt();
            routingTable.addEntry(addressFamilyIdentifier, routingTag,
                    destination, subnetMask, nextHop, metric);
        }
        return routingTable;
    }

    /**
     * Calculate the network prefix given an IP address and a subnet mask.
     *
     * @param ipAddress
     * @param subnetMask
     * @return Network prefix
     */
    private byte[] getNetworkPrefix(byte[] ipAddress, byte[] subnetMask) {
        byte[] networkPrefix = new byte[4];
        for(int i = 0; i < networkPrefix.length; i++) {
            networkPrefix[i] = (byte) (ipAddress[i] & subnetMask[i]);
        }
        return networkPrefix;
    }

    /**
     * Calculate the number of 1s in the binary expression of a byte array.
     *
     * @param bytes
     * @return the number of 1s
     */
    private int numberOfOnes(byte[] bytes) {
        int counter = 0;
        for(byte b : bytes) {
            for(int i = 0; i < 8; i++) {
                counter += (b & (int)pow(2, i)) >>> i;
            }
        }
        return counter;
    }

    @Override
    public String toString() {
        String tag = "Destination       | Next Hop           | Cost\n";
        String entries = "";
        for(RoutingTableEntry entry : routingTableEntries) {
            try {
                entries = entries + String.format("%-18s",
                        Inet4Address.getByAddress(getNetworkPrefix
                                (entry.destination, entry.subnetMask)).getHostAddress()
                                + "/" + numberOfOnes(entry.subnetMask))
                                + "  ";
            } catch(UnknownHostException e) {
                e.printStackTrace();
            }

            try {
                String nextHop;
                if(Arrays.equals(entry.destination, entry.nextHop)) {
                    nextHop = "Directly Connected";
                } else {
                    nextHop = Inet4Address.getByAddress(entry.nextHop).getHostAddress();
                }
                entries = entries + String.format("%-19s", nextHop) + "  ";
            } catch(UnknownHostException e) {
                e.printStackTrace();
            }

            entries = entries + entry.metric + "\n";
        }
        return tag + entries;
    }
}
