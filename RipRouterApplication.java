import java.net.UnknownHostException;
import java.util.Scanner;
import java.net.Inet4Address;

/**
 *  A user interface for the application.
 *
 *  @author Daichi Mae
 */
public class RipRouterApplication {

    public static void main(String[] args)
    {
        RipRouter ripRouter = new RipRouter();

        Scanner sc = new Scanner(System.in);
        System.out.print("Number of neighbors: ");
        int n = sc.nextInt();

        for(int i = 0; i < n; i++) {
            System.out.print("IP address of neighbor " + i + ": ");
            byte[] neighborAddress = null;
            try {
                neighborAddress = Inet4Address.getByName(sc.next()).getAddress();
            } catch(UnknownHostException e) {
                e.printStackTrace();
            }

            System.out.print("Subnet mask: ");
            byte[] subnetMask = null;
            try {
                subnetMask = Inet4Address.getByName(sc.next()).getAddress();
            } catch(UnknownHostException e) {
                e.printStackTrace();
            }

            System.out.print("Cost: ");
            int cost = sc.nextInt();

            ripRouter.addNeighbor(RoutingTable.IPv4, (short)0, neighborAddress,
                    subnetMask, cost);
        }
        sc.close();

        ripRouter.start();
    }
}
