# Concurrent Pacman

Concurrent Pacman is a game simulation developed at [Aveiro University](https://www.ua.pt) in the course [42593-Object-oriented Concurrent Programming](http://www.ua.pt/ensino/uc/6726) for academic purposes in order to demonstrate concurrency in a object oriented language. Other goals of the project is to use Design by Contract (DbC) in Java.

This project has the following dependencies (included):
* [GBoard](http://sweet.ua.pt/mos/pt.ua.gboard/index.xhtml): a DbC Graphical Console Board
* [DbC Concurrency Library](http://sweet.ua.pt/mos/pt.ua.concurrent/index.xhtml): a DbC java concurrent library replacement that replaces checked exceptions to unchecked


## How to run
This simulation has 5 operating modes in order to test race conditions. The source root must be same as the folder *resources* that contains game elements. Help is available when no switch is specified.

    $ ls
    pacman  resources
    $ java -ea pacman.Game
    
A zip containing a jar and the resources is also available at the [releases](https://github.com/luminoso/concurrent-pacman/releases) section. Unzip and run as follow:

    java -ea -jar Pacman.jar

### Mode 1

Normal game in which pacman has 3 lives for pacman, 4 ghosts. Game ends when pacman has no more lives left or when all points (238) are collected. Bonus duration is 5 seconds.

    java -ea pacman.Game 1
    
![Mode 1](https://github.com/luminoso/concurrent-pacman/raw/master/doc/mode1.gif)

### Mode 2

Aggressive mode. Pacman has 100 lives with 16 ghosts. Bonus duration increases to 15 seconds.

    java -ea pacman.Game 2
    
![Mode 2](https://github.com/luminoso/concurrent-pacman/raw/master/doc/mode2.gif)
    
### Mode 3

Endless mode: infinite lives that doesn't end when all points are collected.
    
    java -ea pacman.Game 3
    
### Mode 4

Crazy mode. inifinite live, 32 pacmans and 32 ghosts running. Also infinite pacman lives so the game is endless.

    java -ea pacman.Game 4
    
![Mode 4](https://github.com/luminoso/concurrent-pacman/raw/master/doc/mode4.gif)

### Mode 5

This mode is intended to stress-test thread termination and race conditions. 128 pacmans (threads) killing one ghost

    java -ea pacman.Game 5
    
![Mode 5](https://github.com/luminoso/concurrent-pacman/raw/master/doc/mode5.gif)
    
## Bugs

For some reason Java may not be running with opengl acceleration and graphical frame rate can drop. In order to force acceleration run the simulation with *-Dsun.java2d.opengl=True* flag, for example:

    java -ea -Dsun.java2d.opengl=True pacman.Game 5
    
## License

GNU Lesser General Public License v3.0
