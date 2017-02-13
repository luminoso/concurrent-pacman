package pacman.active;

import pacman.passive.GameController;
import pt.ua.concurrent.ThreadInterruptedException;

import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.out;

/**
 * Implements a Pacman. Simplest active entity
 */
public class Pacman extends Entity {

    private final char endSymbol = '$';

    public Pacman(String name, GameController gc, char symbol, Point pos, int speed) {
        super(name, symbol, gc, pos, speed);
        //maze.board.draw(new CircleGelem(Color.BLACK, 100), pos.y, pos.x, 1);

    }

    @Override
    public void run() {
        //System.out.println(super.getName() + " started");
        try {
            gc.reportPosition(initPos);

            while (!searchPath(1, initPos)) {
                //out.println("no solutions"); // note that pacman levels up before
                searchPath(1, initPos);
                pathLog = new ConcurrentHashMap<>();
            }
        } catch (ThreadInterruptedException ex) {
            out.println(super.getName() + " interrupted at position: " + lastPos);
        }
    }

    boolean searchPath(int distance, Point pos) {

        if (gc.validPosition(pos) && gc.isRoad(pos))
            super.searchPath(distance, pos);

        return false;
    }

    /**
     * No special modes for pacman when attack mode.
     */
    @Override
    public void attackMode() {
        throw new UnsupportedOperationException("Pacman has no attack mode implemented.");
    }


}
