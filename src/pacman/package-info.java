/**
 * Pacman game simulation that includes concurrency.
 * Ghosts and Pacman are active entities that interact with GameControlled shared class, fully synchronized.
 * Game controller includes synchronization points where is possible wait for a game to end or where ghosts wait for attack mode to end.
 */
package pacman;