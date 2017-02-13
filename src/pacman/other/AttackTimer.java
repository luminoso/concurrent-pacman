package pacman.other;

import pacman.passive.GameController;
import pt.ua.concurrent.CThread;
import pt.ua.concurrent.Metronome;

import static java.lang.System.out;

/**
 * Simple timer that uses an Metronome implementation to count duration of an attack time.
 */
public class AttackTimer extends CThread {

    private final static Metronome metronome = new Metronome(1);
    private GameController gc;
    private int pause;

    /**
     * Initializes a new attack timer
     *
     * @param gc game controller reference (for calling the end of the attack mode)
     */
    public AttackTimer(GameController gc, int attackModeDuration) {
        assert gc != null;

        this.gc = gc;
        this.pause = attackModeDuration;
    }

    @Override
    public void run() {
        out.println("AttackingTimer started");

        for (int i = 0; i < pause; i++) {
            metronome.sync();
        }

        // once ended, disable attack mode
        gc.disableAttackMode();
    }

    /**
     * Adds more time to the timer
     */
    public void addMore(int pause) {

        out.println("Added more " + pause + " seconds to timer");
        this.pause += pause;
    }
}