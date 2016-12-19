import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An RIP implementation.
 *
 * @author Daichi Mae
 */
public class RipRouter {
    private byte[] localHostAddress;

    static final byte RIP_VERSION = 2;
    static final int INF = 16;

    static final byte REQUEST = 1;
    static final byte RESPONSE = 2;

    private RoutingTable routingTable;

    private List<RoutingTableEntry> neighbors;

    public static final int UPDATE_PERIOD = 1;
    public static final int EXPIRATION_PERIOD = 180;
    public static final int DELETION_PERIOD = 120;

    private DatagramSocket socket;
    private final int PORT = 520;

    public RipRouter() {
        try {
            localHostAddress = Inet4Address.getLocalHost().getAddress();
        } catch(UnknownHostException e) {
            e.printStackTrace();
        }
        try {
            socket = new DatagramSocket(PORT);
        } catch(SocketException e) {
            e.printStackTrace();
        }
        neighbors = new ArrayList<>();
        routingTable = new RoutingTable();
    }

    /**
     *  Start the router.
     */
    public void start() {
        setTimers();
        new UpdateListener(this).start();
        new RegularUpdate(this);
        System.out.println(routingTable);
        sendRequest();
    }

    /**
     * Create an entry of a neighbor in the table and put the neighbor information
     * in the list for a future reference.
     *
     * @param addressFamilyIdentifier
     * @param routingTag
     * @param destination
     * @param subnetMask
     * @param metric the cost of the network
     */
    public void addNeighbor(short addressFamilyIdentifier, short routingTag,
                         byte[] destination, byte[] subnetMask, int metric) {
        // Let the first neighbor a default route
        RoutingTableEntry entry = new RoutingTableEntry();
        RoutingTableEntry backup = new RoutingTableEntry();
        entry.addressFamilyIdentifier = backup.addressFamilyIdentifier
                = addressFamilyIdentifier;
        entry.routeTag = backup.routeTag = routingTag;
        entry.destination = backup.destination = destination;
        entry.subnetMask = backup.subnetMask = subnetMask;
        entry.nextHop = backup.nextHop = destination;
        entry.metric = backup.metric = metric;
        neighbors.add(backup);
        routingTable.addEntry(entry);
    }

    /**
     * Accessor
     *
     * @return Routing table
     */
    public RoutingTable getRoutingTable() {
        return routingTable;
    }

    /**
     * Accessor
     *
     * @return UDP socket
     */
    public DatagramSocket getSocket() { return socket; }

    /**
     * Accessor
     *
     * @return the list of the neighbors
     */
    private List<RoutingTableEntry> getNeighbors() { return neighbors; }

    /**
     * Return true if hostAddress is a neighbor of this router otherwise return
     * false.
     *
     * @param hostAddress The IP address of a host
     * @return boolean valuse
     */
    private boolean isNeighbor(byte[] hostAddress) {
        for(RoutingTableEntry neighbor : neighbors) {
            if(Arrays.equals(neighbor.destination, hostAddress)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Send an update to all the neighbors.
     *
     * @param command RIP command
     */
    public void sendUpdate(byte command) {
        for(RoutingTableEntry neighbor : getNeighbors()) {
                sendUpdateTo(command, neighbor.destination, PORT);
        }
    }

    /**
     * Send an update to receiver.
     *
     * @param command RIP command
     * @param receiver the recipient of this update
     * @param port the port to send the update from
     */
    private void sendUpdateTo(byte command, byte[] receiver, int port) {
        byte[] buf = routingTable.createPacket(command, receiver);
        try {
            InetAddress address = Inet4Address.getByAddress(receiver);
             DatagramPacket packet = new DatagramPacket(buf, buf.length,
                     address, port);
            socket.send(packet);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the routing table of this router given an update.
     *
     * @param update Routing table from other RIP router
     * @param sender the sender of the update
     */
    private void updateTable(RoutingTable update, byte[] sender) {
        // if a neighbor has come back up, create an entry
        RoutingTableEntry senderEntry = routingTable.getEntryByDestination(sender);
        if(senderEntry == null) {
            for(RoutingTableEntry neighbor : neighbors) {
                if(Arrays.equals(sender, neighbor.destination)) {
                    routingTable.addEntry(neighbor.addressFamilyIdentifier,
                            neighbor.routeTag, neighbor.destination,
                            neighbor.subnetMask, neighbor.nextHop, neighbor.metric);
                    neighbor.routeChangeFlag = true;
                    resetTimer(neighbor.destination);
                }
            }
        } else {
            for(RoutingTableEntry neighbor : neighbors) {
                if (Arrays.equals(sender, neighbor.destination)) {
                    senderEntry.metric = neighbor.metric;
                }
            }
        }

        for( RoutingTableEntry entry : update.getEntries()) {
            RoutingTableEntry thisEntry
                    = routingTable.getEntryByDestination(entry.destination);
            if(Arrays.equals(localHostAddress, entry.destination)) {
                // if the destination is this router, skip updating this entry
                continue;
            }
            int costToNextHop = routingTable.getEntryByDestination(sender).metric;
            if(thisEntry == null) {
                // if the destination is not in the table and reachable,create
                // a new entry
                if(entry.metric < INF) {
                    entry.metric = entry.metric + costToNextHop;
                    entry.nextHop = sender;
                    entry.routeChangeFlag = true;
                    routingTable.addEntry(entry);
                }
            } else {
                // the destination is already in the table
                if(Arrays.equals(thisEntry.nextHop, sender)) {
                    // if the next hop to the destination is equal to the sender
                    // of the update, always update the cost
                    /*if(thisEntry.metric != entry.metric + costToNextHop
                            && entry.metric <= INF)
                        thisEntry.routeChangeFlag = true;*/
                    thisEntry.routeChangeFlag = true;
                    thisEntry.metric = entry.metric + costToNextHop;
                } else {
                    // otherwise update only if the cost is better
                    if(thisEntry.metric > entry.metric + costToNextHop) {
                        thisEntry.nextHop = sender;
                        thisEntry.metric = entry.metric + costToNextHop;
                        thisEntry.routeChangeFlag = true;
                    }
                }
            }
            if(thisEntry != null && thisEntry.metric > INF)
                thisEntry.metric = INF;
        } // end for
    } // end updateTable

    /**
     * Send a request to all the neighbors.
     */
    private void sendRequest() { sendUpdate(REQUEST); }

    /**
     * Analyze an update and take appropriate action.
     *
     * @param header the header of an RIP packet
     * @param update the body of an RIP packet
     * @param sender the sender of the packet
     * @param senderPort the port from which the sender sent the packet
     */
    public void readUpdate(byte[] header, RoutingTable update, byte[] sender,
                           int senderPort) {
        // if the update is not from any of the neighbors, drop the update
        if(!isNeighbor(sender)) {
            System.out.println("Received a packet from a non-neighbor.");
            try {
                System.out.println("From " + Inet4Address.getByAddress(sender));
            } catch(UnknownHostException e) {
                e.printStackTrace();
            }
            return;
        }

        resetTimer(sender);

        updateTable(update, sender);

        byte command = header[0];
        if(command == REQUEST) {
            sendUpdateTo(RESPONSE, sender, senderPort);
        }

        // triggered update
        if(command == REQUEST || command == RESPONSE) {
            if(routingTable.isUpdated()) {
                sendUpdate(RESPONSE);
            }
        } else {
            System.err.println("Invalid command: " + command);
        }

        configureDeletionClock();

        routingTable.resetRoutingChangeFlags();
    }

    /**
     * Set an expiration timer to each neighbor entry.
     */
    private void setTimers() {
        for(RoutingTableEntry neighbor : neighbors) {
                resetTimer(neighbor.destination);
        }
    }

    /**
     * Reset the expiration timer of an entry.
     *
     * @param hostAddress The IP address of a host whose entry's expiration timer
     *                    will be reset.
     */
    private void resetTimer(byte[] hostAddress) {
        for(RoutingTableEntry entry : routingTable.getEntries()) {
            if(isNeighbor(entry.destination)
                    && Arrays.equals(entry.destination, hostAddress)) {
                if(entry.timer != null)
                    entry.timer.cancel();
                entry.timer = new ExpirationTimer(this, entry);
            }
        }
    }

    /**
     * Set deletion timers to entries whose metrics became INF and remove the
     * deletion timers from entries that became valid.
     */
    private void configureDeletionClock() {
        for(RoutingTableEntry entry : routingTable.getEntries()) {
            if(entry.routeChangeFlag && entry.metric == INF) {
                entry.timer = new DeletionTimer(this, entry);
            } else if(entry.routeChangeFlag && entry.timer instanceof DeletionTimer
                    && entry.metric < INF) {
                entry.timer.cancel();
            }
        }
    }
}
