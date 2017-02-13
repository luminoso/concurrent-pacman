package pacman.active;

import pacman.passive.GameController;
import pt.ua.concurrent.CThread;
import pt.ua.concurrent.ThreadInterruptedException;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.System.err;
import static java.lang.System.out;

/**
 * Ghost class. Special cases include trespassing ghost cages and continually running.
 */
public class Ghost extends Entity {

    public final int attackModeSlowdownFactor;
    public final int blinkSpeed;
    private final boolean alive = true;
    private ScheduledExecutorService es;

    public Ghost(String name, GameController gc, char symbol, Point pos, int speed, int slowdownFactor, int blinkSpeed) {
        super(name, symbol, gc, pos, speed);
        attackModeSlowdownFactor = slowdownFactor;
        this.blinkSpeed = blinkSpeed;
    }


    /**
     * Ghost class runs around until killed
     */
    @Override
    public void run() {
        //out.println(super.getName() + " started");

        try {
            //noinspection InfiniteLoopStatement
            while (alive) {
                searchPath(1, initPos);
                pathLog = new ConcurrentHashMap<>();
            }
        } catch (ThreadInterruptedException ex) {
            out.println(super.getName() + " interrupted at position " + lastPos);
        }
    }

    @Override
    boolean searchPath(int distance, Point pos) {
        assert pos != null;
        assert distance > 0;

        if (gc.validPosition(pos) && gc.isRoad(pos))
            super.searchPath(distance, pos);

        return false;
    }

    /**
     * Allow to pass ghost cage gate
     *
     * @param pos to check
     * @return true if valid path to wander
     */
    @Override
    boolean freePosition(Point pos) {
        assert pos != null;
        assert gc.isRoad(pos);

        return super.freePosition(pos)
                || roadSymbol(pos) == '%'; // allow passing though ghost cage gate
    }

    /**
     * Schedule threads at a fixed rate that toggle black and white ghost representation
     */
    @Override
    public void attackMode() {

        if (es == null || es.isTerminated()) {

            symbol = 'b'; // ghost black
            underAttack = true;

            es = java.util.concurrent.Executors.newScheduledThreadPool(1);

            es.scheduleAtFixedRate(() -> {
                //out.println("Toggling " + super.getName() + " symbol");
                if (symbol == 'w')
                    symbol = 'b';
                else
                    symbol = 'w';

            }, 0, blinkSpeed, TimeUnit.MILLISECONDS);

            speed = speed * attackModeSlowdownFactor;

            // wait for attack mode to end.
            new CThread(() -> {
                gc.hasAttackModeEnded();
                disableAttackMode();
            }).start();
        }

    }

    private void disableAttackMode() {
        assert !es.isTerminated();

        try {
            out.println("Ending " + super.getName() + " attack mode");
            speed = speed / attackModeSlowdownFactor;
            underAttack = false;
            es.shutdown();
            es.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // was blinking stuck? this should never happen
            err.println("Forcing stop of a blinking Executor");
            System.exit(1);
        } finally {
            // deal with the fault and force shutdown of blinking service
            es.shutdownNow();
        }

        symbol = markedStartSymbol;
    }

}
