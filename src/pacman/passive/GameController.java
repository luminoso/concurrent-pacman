package pacman.passive;

import pacman.active.Entity;
import pacman.active.Ghost;
import pacman.active.Pacman;
import pacman.other.AttackTimer;
import pt.ua.concurrent.CObject;
import pt.ua.concurrent.CThread;
import pt.ua.gboard.*;
import pt.ua.gboard.games.Labyrinth;
import pt.ua.gboard.games.LabyrinthGelem;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.System.out;

/**
 * Central game controller.
 * This class is responsible for, and not limited to:
 * - Count Pacman points, pacmanLives, etc
 * - Kill and spawn entities
 * - Enable/Disable attack mode
 * - Synchronization of GBoard
 */
public class GameController extends CObject {

    private final Labyrinth labyrinth;
    private final Point[] portalPoints, portalDst;
    //private final CopyOnWriteArrayList<Entity> entities = new CopyOnWriteArrayList<>();
    private final ArrayList<Entity> entities = new ArrayList<>();
    private final ConcurrentHashMap<Point, Entity> entityTracker = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Point, Character> mapCache = new ConcurrentHashMap<>();
    private final boolean GodMode;
    private final int attackModeDuration;
    private final boolean endless;
    //private final ReentrantLock lock = new ReentrantLock();
    //private final Condition attackEnded = lock.newCondition();
    //private final Condition hasGameEnded = lock.newCondition();
    private int totalNumberOfPoints;
    private AttackTimer timer;
    private int points = 0;
    private boolean attackInProgress = false;
    private int pacmanLives;
    private boolean gameInProgress = true;


    /**
     * Initializes a new game controller.
     */
    public GameController(int numberPacmanLives,
                          int attackModeDuration,
                          boolean endless) {

        pacmanLives = numberPacmanLives;
        this.GodMode = numberPacmanLives == -1;
        this.attackModeDuration = attackModeDuration;
        this.endless = endless;

        LabyrinthGelem.setShowRoadBoundaries();

        char[] allSymbols = {
                // main ghosts
                'C', // inky
                'P', // pinky
                'O', // clyde
                'R', // blinky
                'X', // pacman
                // ghosts representations
                'w', // ghost white
                'b', // ghost black
                'G', // generic ghost
                // game elements
                '.', // point
                '$', // bonus
                // map helpers
                '%', // ghost cage
                '+', // markedPositionSymbol
                'o', // actualPositionSymbol
                '?', // portal
                '!' // portal destination
        };

        File f = new File("./resources/board.txt");
        if (!f.exists()) {
            out.println("Missing resources folder");
            out.println("Looking at:" + f.getAbsolutePath());
            System.exit(1);
        }

        labyrinth = new Labyrinth("./resources/board.txt", allSymbols);

        Gelem[] allGelems = {
                // entities
                new ImageGelem("./resources/inky.png", labyrinth.board, 100),
                new ImageGelem("./resources/pinky.png", labyrinth.board, 100),
                new ImageGelem("./resources/clyde.png", labyrinth.board, 100),
                new ImageGelem("./resources/blinky.png", labyrinth.board, 100),
                new ImageGelem("./resources/pacman_r.png", labyrinth.board, 100),
                new ImageGelem("./resources/attack_w.png", labyrinth.board, 100),
                new ImageGelem("./resources/attack_b.png", labyrinth.board, 100),
                new ImageGelem("./resources/generic.png", labyrinth.board, 100),
                // game elements
                new StringGelem("" + '.', Color.black),
                new CircleGelem(Color.blue, 65),
                // mapPath helpers
                new CircleGelem(Color.GREEN, 35),
                new CircleGelem(Color.GREEN, 45),
                new CircleGelem(Color.BLACK, 35),
                new CircleGelem(Color.LIGHT_GRAY, 5),
                new CircleGelem(Color.LIGHT_GRAY, 5),
                new CircleGelem(Color.ORANGE, 35)};

        // attach all gelems to the map
        for (int i = 0; i < allSymbols.length; i++) {
            labyrinth.attachGelemToRoadSymbol(allSymbols[i], allGelems[i]);
        }

        // query the portal destinations and points
        portalPoints = labyrinth.roadSymbolPositions('?');
        portalDst = labyrinth.roadSymbolPositions('!');

        // check total number of points for pacman to collect
        totalNumberOfPoints = getPositions('.').length;
        out.println("Game maximum number of points: " + totalNumberOfPoints);

        // draw "P" points on the map
        labyrinth.board.draw(new StringGelem("P:", Color.white), 32, 4, 1);
    }

    /**
     * Receives a report from an entity where it wants to move.
     * This movement triggers collisions with other entities, killing, spawning, points calculation etc
     *
     * @param pos to move the entity to
     * @return new position, if teleported by the portal
     */
    public synchronized Point reportPosition(Point pos) {
        assert pos != null;
        assert isRoad(pos);

        Entity entity = (Entity) CThread.currentThread();
        assert entity.isAlive();

        // portal
        if (pos.equals(portalPoints[0]) || pos.equals(portalPoints[1])) {

            // remove presence from structures
            entityTracker.remove(entity.lastPos);
            labyrinth.putRoadSymbol(entity.lastPos.y, entity.lastPos.x, ' ');

            pos = (pos.equals(portalPoints[1])) ? portalDst[0] : portalDst[1]; // transport entity to portal destination

            entityTracker.put(pos, entity); // re-add presence
            entity.lastPos = (pos.equals(portalDst[1])) ? portalPoints[1] : portalPoints[0]; // update last position to portal
        }

        char mapSymbol = RealRoadSymbol(pos);

        // clean up our presence on map. check if cell is occupied from some entity
        Entity entity1 = entityTracker.get(pos);
        entityTracker.remove(pos);
        entityTracker.remove(entity.lastPos);

        // cell is occupied. did pacman and a ghost crossed each other?
        if (entity1 != null) {
            if (entity.isGhost() && !entity1.isGhost()
                    || !entity.isGhost() && entity1.isGhost()) {
                out.println("I am:" + entity.getName() + " killing " + entity1.getName() + " " + pos + " on cache:" + entity1.getName());

                if (!entity.isGhost()) {
                    if (entity1.underAttack() || GodMode) {
                        spawnNewEntity(entity1); // kill entity
                    } else {
                        labyrinth.putRoadSymbol(entity.lastPos.y, entity.lastPos.x, ' ');
                        spawnNewEntity(entity); // pacman dies
                    }

                } else {
                    if (entity.underAttack() || GodMode) {
                        spawnNewEntity(entity); // we, ghost, die
                    } else {
                        labyrinth.putRoadSymbol(entity1.lastPos.y, entity1.lastPos.x, ' ');
                        spawnNewEntity(entity1); // we kill pacman
                    }
                }

                //labyrinth.putRoadSymbol(pos.y, pos.x, mapCache.computeIfAbsent(pos, t -> ' '));
                labyrinth.putRoadSymbol(pos.y, pos.x, mapCache.computeIfAbsent(pos, t -> symbolIsEntity(mapSymbol) ? mapSymbol : ' ')); // After killing, restore map

                if (entity.isInterrupted()) return pos;

            }
        }

        // pacman login
        if (!entity.isGhost()) {
            if (RealRoadSymbol(pos) == '.') { // point counting logic
                points++;

                if ((points % 30) == 0) out.println("Points " + points + "/" + totalNumberOfPoints);

                drawPoints();

                if (points == totalNumberOfPoints && !endless)
                    endGame();

            } else if (RealRoadSymbol(pos) == '$') { // bonus logic
                attackInProgress = true;
                entities.stream().filter(Entity::isGhost).forEach(Entity::attackMode);
                startAttackTimer();
            }

            mapCache.put(pos, ' '); // collectible was collected. mapCache is just road now
        } else {
            // cache the current bonus/point under target position
            mapCache.computeIfAbsent(pos, t -> symbolIsEntity(mapSymbol) ? ' ' : mapSymbol);
        }

        paintPosition(pos); // representation on map
        entityTracker.put(pos, entity);

        return pos;
    }

    /**
     * Draw points in the GBoard
     */
    private synchronized void drawPoints() {
        assert points <= totalNumberOfPoints;
        assert points >= 0;
        assert labyrinth.numberOfLines >= 32; // TODO remove hardcoded point positions

        String sPoints = new StringBuilder(String.valueOf(points)).reverse().toString();
        labyrinth.board.draw(new FilledGelem(Color.blue, 100), 32, 5, 1);
        labyrinth.board.draw(new FilledGelem(Color.blue, 100), 32, 6, 1);
        labyrinth.board.draw(new FilledGelem(Color.blue, 100), 32, 7, 1);

        for (int i = 0; i < sPoints.length(); i++) {
            labyrinth.board.draw(new StringGelem(String.valueOf(sPoints.charAt(i)), Color.white), 32, 7 - i, 1);
        }

    }

    /**
     * Spawns a new cloned entity
     *
     * @param entity to clone
     */
    private synchronized void spawnNewEntity(Entity entity) {
        assert entity != null;
        assert entity.isAlive();
        assert !entity.isInterrupted();
        assert entities.contains(entity);

        entities.remove(entity);
        entity.interrupt();

        if (entity.isGhost()) {
            out.println("Spawning new " + entity.getName());
            Ghost ghost = (Ghost) entity;
            ghost = new Ghost(entity.getName(), this, entity.initialSymbol, entity.initPos, entity.initialSpeed, ghost.attackModeSlowdownFactor, ghost.blinkSpeed);

            entities.add(ghost);
            ghost.start();
            out.println(entity.getName() + " spawned at " + entity.initPos);
        } else {
            if(!endless) pacmanLives--;
            if (pacmanLives > 0) {
                out.println("Pacman has " + pacmanLives + " pacmanLives left.");
                Pacman pm = new Pacman(entity.getName(), this, entity.initialSymbol, entity.initPos, entity.getInitialSpeed());
                entities.add(pm);
                pm.start();
            } else {
                endGame();
            }
        }
    }

    /**
     * End the game, interrupts all entities and signals who's waiting for the end of the game.
     */
    private synchronized void endGame() {
        assert gameInProgress;

        entities.forEach(Thread::interrupt);
        gameInProgress = false;
        broadcast();
    }

    /**
     * Waiting point for the end of the made
     *
     * @return true if pacman won, false otherwise
     */
    public synchronized boolean waitingForGameToEnd() {
        assert gameInProgress;

        while (gameInProgress)
            await();

        out.println("Final points:" + points);
        return points == totalNumberOfPoints;
    }

    /**
     * Paints an entity in the map
     *
     * @param pos to paint
     */
    private synchronized void paintPosition(Point pos) {
        assert pos != null;
        assert isRoad(pos);

        Entity entity = (Entity) CThread.currentThread();

        labyrinth.putRoadSymbol(entity.lastPos.y, entity.lastPos.x, mapCache.computeIfAbsent(entity.lastPos, t -> RealRoadSymbol(pos)));
        mapCache.computeIfAbsent(pos, t -> RealRoadSymbol(pos));
        labyrinth.putRoadSymbol(pos.y, pos.x, entity.symbol);

        if (!entity.lastPos.equals(pos))
            entity.lastPos = pos;
    }


    /**
     * Starts a timer counting the duration of an attack.
     * An attack is when pacman eats a bonus and ghosts can be killed by pacman.
     */
    private synchronized void startAttackTimer() {
        assert attackInProgress;

        if (timer == null || !timer.isAlive()) {
            timer = new AttackTimer(this, attackModeDuration);
            timer.start();
        } else {
            timer.addMore(attackModeDuration);
        }
    }

    /**
     * Synchronized GBoard's roadSymbol() method
     *
     * @param pos to get the road symbol
     * @return char road symbol
     */
    private synchronized char RealRoadSymbol(Point pos) {
        assert pos != null;
        assert isRoad(pos);

        return labyrinth.roadSymbol(pos.y, pos.x);
    }

    public synchronized char CachedRoadSymbol(Point pos) {
        assert pos != null;
        assert isRoad(pos);

        return mapCache.computeIfAbsent(pos, t -> RealRoadSymbol(pos));
    }

    /**
     * Synchronized GBoard's isRoad() method
     *
     * @param pos to check if is road
     * @return true if it is a road position, otherwise it returns false
     */
    public synchronized boolean isRoad(Point pos) {
        assert pos != null;

        return labyrinth.isRoad(pos.y, pos.x);
    }

    /**
     * Synchronized GBoard's validPosition() method
     *
     * @param pos to check if valid
     * @return true if position is valid, otherwise it returns false
     */
    public synchronized boolean validPosition(Point pos) {
        assert pos != null;

        return labyrinth.validPosition(pos.y, pos.x);
    }

    /**
     * Synchronized GBoard roadSymbolPositions() method
     *
     * @param symbol to road
     * @return array of positions
     */
    public synchronized Point[] getPositions(char symbol) {

        return labyrinth.roadSymbolPositions(symbol);
    }

    /**
     * Disables attack mode and broadcasts signal to everyone waiting for its end
     */
    public synchronized void disableAttackMode() {
        assert attackInProgress;

        attackInProgress = false;
        broadcast();

    }

    /**
     * Waiting point for the end of an attack
     */
    public synchronized void hasAttackModeEnded() {
        assert attackInProgress;

        while (attackInProgress)
            await();

    }

    /**
     * Attaches extra entities to the game
     *
     * @param entity to attach
     */
    public synchronized void attachExtraEntity(Entity entity) {

        labyrinth.putRoadSymbol(entity.initPos.y, entity.initPos.x, entity.symbol);
        mapCache.put(entity.initPos, ' ');
        entities.add(entity);
        totalNumberOfPoints = getPositions('.').length;
        out.println("Updated maximum number of points to " + totalNumberOfPoints);
    }

    /**
     * Checks if the symbol is an active entity
     *
     * @param symbol to check
     * @return true if active entity
     */
    public synchronized boolean symbolIsEntity(char symbol) {
        return symbol == 'C' // inky
                || symbol == 'P' // pinky
                || symbol == 'O' // clyde
                || symbol == 'R' // blinky
                || symbol == 'X' // pacman
                || symbol == 'w' // ghost white
                || symbol == 'b' // ghost black
                || symbol == 'G'; // generic ghost
    }

}
