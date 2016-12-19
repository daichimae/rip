import java.util.Timer;
import java.util.TimerTask;

/**
 *  A timer that deletes the entry after DELETION_PERIOD.
 *
 *  @author Daichi Mae
 */
public class DeletionTimer extends Timer {
    private RipRouter ripRouter;
    private RoutingTableEntry entry;

    public DeletionTimer(RipRouter ripRouter, RoutingTableEntry entry) {
        this.ripRouter = ripRouter;
        this.entry = entry;
        this.schedule(new DeleteEntry(), RipRouter.DELETION_PERIOD * 1000);
    }

    /**
     * The task of removing an entry and triggering an update.
     */
    private class DeleteEntry extends TimerTask {
        public void run() {
            ripRouter.getRoutingTable().getEntries().remove(entry);
            ripRouter.sendUpdate(RipRouter.RESPONSE);
        }
    }
}
