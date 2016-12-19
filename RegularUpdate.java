import java.util.Timer;
import java.util.TimerTask;

/**
 * Schedule a regular update.
 *
 * @author Daichi Mae
 */
public class RegularUpdate {
    private RipRouter ripRouter;

    public RegularUpdate(RipRouter ripRouter) {
        this.ripRouter = ripRouter;
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new SendUpdate(), RipRouter.UPDATE_PERIOD * 1000,
                RipRouter.UPDATE_PERIOD * 1000);
    }

    /**
     *  The task of sending the routing table to the neighbours and displaying
     *  the routing table.
     */
    private class SendUpdate extends TimerTask {
        public void run() {
            ripRouter.sendUpdate(RipRouter.RESPONSE);
            System.out.println(ripRouter.getRoutingTable());
        }
    }
}
