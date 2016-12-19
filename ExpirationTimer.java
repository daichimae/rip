import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 *  A timer that invalidates the entry after EXPIRATION_PERIOD.
 *
 *  @author Daichi Mae
 */
public class ExpirationTimer extends Timer {
    private RipRouter ripRouter;
    private RoutingTableEntry timeredEntry;

    public ExpirationTimer(RipRouter ripRouter, RoutingTableEntry entry) {
        this.ripRouter = ripRouter;
        this.timeredEntry = entry;
        this.schedule(new ExpireEntry(), RipRouter.EXPIRATION_PERIOD * 1000);
    }

    /**
     * The task of invalidating an entry, setting up a deletion timer and
     * triggering an update.
     */
    private class ExpireEntry extends TimerTask {
        public void run() {
            for(RoutingTableEntry entry : ripRouter.getRoutingTable().getEntries()) {
                if(Arrays.equals(entry.nextHop, timeredEntry.destination)) {
                    entry.metric = RipRouter.INF;
                }
            }
            timeredEntry.timer = new DeletionTimer(ripRouter, timeredEntry);
            ripRouter.sendUpdate(RipRouter.RESPONSE);
        }
    }
}
