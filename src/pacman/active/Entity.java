package pacman.active;

import pacman.passive.GameController;
import pt.ua.concurrent.CThread;

import java.awt.*;
import java.util.Collections;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity is an abstract class that can represent an active entity in the map
 * Shared algorithms already implemented
 */
public abstract class Entity extends CThread {

    public final Point initPos;
    public final int initialSpeed;
    public final char initialSymbol;
    final GameController gc;
    final char markedStartSymbol;
    private final char startSymbol;
    private final char markedPositionSymbol = '+';
    private final char actualPositionSymbol = 'o';
    public volatile Point lastPos;
    public char symbol;
    protected boolean underAttack = false;
    ConcurrentHashMap<Point, Character> pathLog;
    int speed;


    /**
     * Initializes a new Entity
     *
     * @param name   of the entity
     * @param symbol respective symbol that represents the entity
     * @param gc     game controller
     * @param pos    initial position
     * @param speed  of the entity
     */
    Entity(String name, char symbol, GameController gc, Point pos, int speed) {
        super(name);
        assert gc != null;
        assert pos != null;
        assert speed > 0;

        this.symbol = symbol;
        initialSymbol = symbol;
        this.gc = gc;
        lastPos = pos;
        this.initPos = pos;
        this.speed = speed;
        initialSpeed = speed;

        this.startSymbol = symbol;
        this.markedStartSymbol = symbol;

        pathLog = new ConcurrentHashMap<>();
    }

    @Override
    public abstract void run();

    /**
     * Randomly walks around the map
     *
     * @param distance to jump from the current location
     * @param pos      position to travel to
     * @return true if a objective was found
     */
    boolean searchPath(int distance, Point pos) {
        assert distance > 0;
        assert pos != null;
        assert super.isAlive();

        boolean result = false;

        if (freePosition(pos) && !isInterrupted()) {
            CThread.pause(speed);

            markPosition(pos);

            Point lp = (Point) pos.clone();
            pos = gc.reportPosition(pos);
            assert gc.isRoad(pos);

            if (!lp.equals(pos)) {
                markPosition(lastPos);
                markPosition(pos);
            }

            Stack<Point> shuffleDirections = shuffleDirections();

            while (!shuffleDirections.isEmpty()) {
                if (searchPath(distance + 1, getDirection(pos, shuffleDirections.pop()))) {
                    result = true;
                }
            }
            CThread.pause(speed);

            unmarkPosition(pos);
            pos = gc.reportPosition(pos);
            assert gc.isRoad(pos);
            unmarkPosition(pos);
        }

        return result;
    }

    /**
     * Shuffles the direction of the movement. Making it more random
     *
     * @return directions shuffled
     */
    private Stack<Point> shuffleDirections() {
        Stack<Point> directions = new Stack<>();
        directions.push(new Point(-1, 0));
        directions.push(new Point(0, +1));
        directions.push(new Point(0, -1));
        directions.push(new Point(+1, 0));
        Collections.shuffle(directions);
        return directions;
    }

    /**
     * Computes the next Point position according to the movement direction
     *
     * @param a actual position
     * @param b direction of the movement
     * @return position of the desired movement
     */
    private Point getDirection(Point a, Point b) {
        return new Point(a.x + b.x, a.y + b.y);
    }

    /**
     * Checks if the position is a valid, free position to move
     *
     * @param pos to check
     * @return if valid
     */
    boolean freePosition(Point pos) {
        assert pos != null;
        assert gc.isRoad(pos);

        char rs = roadSymbol(pos);

        return rs == ' ' // road
                || rs == '$' // bonus
                || rs == '.' // point
                || rs == '?' // portal
                || rs == '!' // portal destination
                || gc.symbolIsEntity(rs);
    }

    /**
     * Marks the travelled path with an symbol
     *
     * @param pos position to mark
     */
    private void markPosition(Point pos) {
        assert pos != null;
        assert gc.isRoad(pos);

        if (isStartPosition(pos)) {
            putRoadSymbol(pos, markedStartSymbol);
        } else {
            putRoadSymbol(pos, actualPositionSymbol);
        }

    }

    /**
     * Unmarks an travelled path
     *
     * @param pos position to unmark
     */
    private void unmarkPosition(Point pos) {
        assert pos != null;
        assert gc.isRoad(pos);

        if (!isStartPosition(pos)) {
            putRoadSymbol(pos, markedPositionSymbol);
        }
    }

    /**
     * Checks if the position is the start position
     *
     * @param pos position to check
     * @return true if is the start
     */
    private boolean isStartPosition(Point pos) {
        assert pos != null;
        assert gc.isRoad(pos);

        return roadSymbol(pos) == startSymbol
                || roadSymbol(pos) == markedStartSymbol;
    }

    /**
     * Checks the symbol on the road
     * This method caches the map from the map
     *
     * @param pos position to check
     * @return symbol of the road
     */
    char roadSymbol(Point pos) {
        assert pos != null;
        assert gc.isRoad(pos);

        return pathLog.computeIfAbsent(pos, (t) -> gc.CachedRoadSymbol(pos));
    }

    /**
     * Puts an symbol in the road in the local cached map
     *
     * @param pos    position to mark
     * @param symbol symbol to use in the mark
     */
    private void putRoadSymbol(Point pos, char symbol) {
        assert pos != null;
        assert gc.isRoad(pos);

        pathLog.put(pos, symbol);
    }

    /**
     * When pacman is attacking what behaviour to have
     */
    public abstract void attackMode();

    /**
     * Returns the entity speed
     *
     * @return speed of the entity
     */
    public int getInitialSpeed() {
        return initialSpeed;
    }

    /**
     * Checks if entity is a Ghost
     *
     * @return true if ghost.
     */
    public boolean isGhost() {
        return this instanceof Ghost;
    }

    /**
     * Checks if entity is under attack
     *
     * @return true if under attack
     */
    public boolean underAttack() {
        return underAttack;
    }

}
