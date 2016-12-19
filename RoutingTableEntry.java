import java.util.Timer;

/**
 * An entry for the RoutingTable class.
 *
 * @author Daichi Mae
 */
public class RoutingTableEntry {
    public short addressFamilyIdentifier;
    public short routeTag;
    public byte[] destination;
    public byte[] subnetMask;
    public byte[] nextHop;
    public int metric;
    public boolean routeChangeFlag = false;
    public Timer timer = null;
}
