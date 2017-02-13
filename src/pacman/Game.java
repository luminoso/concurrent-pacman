package pacman;

import pacman.active.Entity;
import pacman.active.Ghost;
import pacman.active.Pacman;
import pacman.passive.GameController;
import pt.ua.concurrent.CThread;

import java.awt.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.System.out;

public class Game {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        if (args.length != 1) {
            printHelp();
            System.exit(1);
        }

        int lives = 3;
        int attackDuration = 5000;
        int slowdownFactor = 3;
        int blinkSpeed = 500;
        int extraGhots = 0;
        int extraPacmans = 0;
        boolean endless = false;
        ArrayList<Entity> entities = new ArrayList<>();

        switch (args[0].charAt(0)) {
            case '1': {
                // normal mode
                // no changes to default
                break;
            }
            case '2': {
                // aggressive mode. 100 lives, 16 ghosts
                lives = 100;
                extraGhots = 12;
                attackDuration = 15000;
                break;
            }
            case '3': {
                // endless
                endless = true;
                break;
            }
            case '4': {
                // crazy mode
                lives = -1;
                extraPacmans = 31;
                extraGhots = 28;
                endless = true;
                break;
            }
            case '5': {
                lives = -1;
                endless = true;
                extraPacmans = 127;

                GameController gc = new GameController(lives, attackDuration, endless);
                entities.add(new Pacman("pacman", gc, 'X', gc.getPositions('X')[0], 125));
                entities.add(new Ghost("blinky", gc, 'R', gc.getPositions('R')[0], 100, slowdownFactor, blinkSpeed));
                for (int i = 0; i < extraPacmans; i++) {
                    Pacman pc = new Pacman("Pacman nr: " + i,
                            gc,
                            'X',
                            gc.getPositions('X')[0],
                            ThreadLocalRandom.current().nextInt(80, 90 + 1));

                    entities.add(pc);
                }

                entities.forEach(gc::attachExtraEntity);
                entities.forEach(CThread::start);

                gc.waitingForGameToEnd();

                break;
            }
            default: {
                printHelp();
                System.exit(1);
            }
        }

        GameController gc = new GameController(lives, attackDuration, endless);


        entities.add(new Pacman("pacman", gc, 'X', gc.getPositions('X')[0], 125));

        // enemies
        entities.add(new Ghost("inky", gc, 'C', gc.getPositions('C')[0], 125, slowdownFactor, blinkSpeed));
        entities.add(new Ghost("pinky", gc, 'P', gc.getPositions('P')[0], 175, slowdownFactor, blinkSpeed));
        entities.add(new Ghost("clyde", gc, 'O', gc.getPositions('O')[0], 250, slowdownFactor, blinkSpeed));
        entities.add(new Ghost("blinky", gc, 'R', gc.getPositions('R')[0], 100, slowdownFactor, blinkSpeed));

        for (int i = 0; i < extraGhots; i++) {
            Point[] freeSlots = gc.getPositions('.');
            Point pos = freeSlots[ThreadLocalRandom.current().nextInt(0, freeSlots.length)];
            Ghost ghost = new Ghost("Generic Ghost " + i,
                    gc,
                    'G', // generic ghost
                    pos,
                    ThreadLocalRandom.current().nextInt(125, 500 + 1),
                    slowdownFactor,
                    blinkSpeed);

            entities.add(ghost);
        }

        for (int i = 0; i < extraPacmans; i++) {
            Pacman pc = new Pacman("Pacman nr: " + i,
                    gc,
                    'X',
                    gc.getPositions('X')[0],
                    ThreadLocalRandom.current().nextInt(80, 90 + 1));

            entities.add(pc);
        }

        entities.forEach(gc::attachExtraEntity);
        entities.forEach(CThread::start);

        boolean won = gc.waitingForGameToEnd();
        out.println("Game Ended! " + "Pacman as " + (won ? "won!" : "lost"));


    }

    private static void printHelp() {
        out.println("PCOO Pacman Simulation 2016/2017");
        out.println("Guilherme Cardoso <gjc@ua.pt>");
        out.println("");
        out.println("Usage: java -ea -jar Pacman.jar <preset>");
        out.println("");
        out.println("Available presets:");
        out.println("");
        out.println("1:  Normal pacman game. 3 lives, 4 ghosts.");
        out.println("    Game whens when all points collected or no more lives");
        out.println("");
        out.println("2:  Aggressive mode: 100 lives, 16 ghosts");
        out.println("    Attack duration lasts 15 seconds");
        out.println("");
        out.println("3:  Endless mode: infinite lives");
        out.println("    Game doesn't end when all points are collected.");
        out.println("");
        out.println("4:  Crazy mode: infinite lives, 32 pacmans, 32 ghosts");
        out.println("    Same as 3, but with more entities");
        out.println("");
        out.println("5:  Developer mode. 128 pacmans killing one Ghost. Tests interrupts and concurrency.");
    }

}
