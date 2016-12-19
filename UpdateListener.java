import java.io.IOException;
import java.net.DatagramPacket;
import java.util.Arrays;

/**
 * Receive updates.
 *
 * @author Daichi Mae
 */
public class UpdateListener extends Thread {
    private RipRouter ripRouter;

    public UpdateListener(RipRouter ripRouter) {
        this.ripRouter = ripRouter;
    }

    /**
     * Receive an RIP packet and read it.
     */
    public void run() {
        while(true) {
            try {
                byte[] buf = new byte[512];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                ripRouter.getSocket().receive(packet);

                byte[] sender = packet.getAddress().getAddress();
                int senderPort = packet.getPort();
                byte[] header = Arrays.copyOfRange(buf, 0, 4);

                ripRouter.readUpdate(header, RoutingTable.createTable(packet),
                        sender, senderPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } // end while
    } // end run
}
