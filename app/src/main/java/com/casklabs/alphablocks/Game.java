package com.casklabs.alphablocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

/** Android-independent game rules. All mutations happen on the UI thread. */
public final class Game {
    public static final int WIDTH = 10, HEIGHT = 20;
    public static final int READY = 0, RUNNING = 1, PAUSED = 2, OVER = 3;
    public static final int I = 0, O = 1, T = 2, S = 3, Z = 4, J = 5, L = 6;
    private static final int[][][] SPAWN = {
        {{0,1},{1,1},{2,1},{3,1}}, {{1,0},{2,0},{1,1},{2,1}},
        {{1,0},{0,1},{1,1},{2,1}}, {{1,0},{2,0},{0,1},{1,1}},
        {{0,0},{1,0},{1,1},{2,1}}, {{0,0},{0,1},{1,1},{2,1}},
        {{2,0},{0,1},{1,1},{2,1}}
    };
    // Small wall/floor kicks; I can move two cells away from an obstruction.
    private static final int[][] KICKS = {{0,0},{-1,0},{1,0},{-2,0},{2,0},
            {0,-1},{-1,-1},{1,-1},{0,-2}};
    final int[][] board = new int[HEIGHT][WIDTH]; // zero empty, piece type + 1 occupied
    private final Random random;
    private final LinkedList<Integer> queue = new LinkedList<Integer>();
    private int state = READY, piece, rotation, x, y, held = -1;
    private int score, lines, level = 1, startingLevel = 1;
    private int lastClear, pieces, lockResets;
    private long gravityTime, lockTime;
    private boolean holdUsed;

    public Game() { this(new Random()); }
    public Game(Random random) {
        this.random = random;
        refill();
    }

    public void start(int initialLevel) {
        for (int row = 0; row < HEIGHT; row++)
            for (int col = 0; col < WIDTH; col++) board[row][col] = 0;
        queue.clear();
        score = lines = pieces = lastClear = 0;
        level = startingLevel = Math.max(1, Math.min(15, initialLevel));
        held = -1;
        state = RUNNING;
        spawnNext();
    }

    private void refill() {
        if (queue.size() >= 7) return;
        ArrayList<Integer> bag = new ArrayList<Integer>();
        for (int i = 0; i < 7; i++) bag.add(i);
        Collections.shuffle(bag, random);
        queue.addAll(bag);
    }

    private void spawnNext() {
        refill();
        spawn(queue.removeFirst());
        holdUsed = false;
        refill();
    }

    private void spawn(int type) {
        piece = type;
        rotation = 0;
        x = 3;
        y = -1;
        gravityTime = lockTime = 0;
        lockResets = 0;
        if (!fits(piece, rotation, x, y)) state = OVER;
    }

    public static int[][] cells(int type, int turn) {
        int[][] result = new int[4][2];
        for (int i = 0; i < 4; i++) {
            int cx = SPAWN[type][i][0], cy = SPAWN[type][i][1];
            if (type != O) {
                int size = type == I ? 4 : 3;
                for (int t = 0; t < (turn & 3); t++) {
                    int previous = cx;
                    cx = size - 1 - cy;
                    cy = previous;
                }
            }
            result[i][0] = cx;
            result[i][1] = cy;
        }
        return result;
    }

    private boolean fits(int type, int turn, int px, int py) {
        int[][] shape = cells(type, turn);
        for (int i = 0; i < 4; i++) {
            int cx = px + shape[i][0], cy = py + shape[i][1];
            if (cx < 0 || cx >= WIDTH || cy >= HEIGHT || cy < -4) return false;
            if (cy >= 0 && board[cy][cx] != 0) return false;
        }
        return true;
    }

    public boolean move(int direction) {
        if (state != RUNNING || (direction != -1 && direction != 1)) return false;
        if (!fits(piece, rotation, x + direction, y)) return false;
        resetLock();
        x += direction;
        return true;
    }

    public boolean rotate(int direction) {
        if (state != RUNNING || piece == O) return false;
        int next = (rotation + (direction < 0 ? 3 : 1)) & 3;
        for (int i = 0; i < KICKS.length; i++) {
            int dx = KICKS[i][0], dy = KICKS[i][1];
            if (fits(piece, next, x + dx, y + dy)) {
                resetLock();
                rotation = next;
                x += dx;
                y += dy;
                return true;
            }
        }
        return false;
    }

    private void resetLock() {
        if (isGrounded() && lockResets < 15) {
            lockTime = 0;
            lockResets++;
        }
    }

    public boolean softDrop() {
        if (state != RUNNING || !fits(piece, rotation, x, y + 1)) return false;
        y++;
        score++;
        gravityTime = 0;
        return true;
    }

    public void hardDrop() {
        if (state != RUNNING) return;
        int landing = ghostY();
        score += (landing - y) * 2;
        y = landing;
        lock();
    }

    public void hold() {
        if (state != RUNNING || holdUsed) return;
        int previous = held;
        held = piece;
        if (previous < 0) spawnNext(); else spawn(previous);
        holdUsed = true;
    }

    public void tick(long elapsedMillis) {
        if (state != RUNNING || elapsedMillis <= 0) return;
        // No large catch-up jumps after stalls or a display switch.
        long delta = Math.min(elapsedMillis, 100);
        boolean grounded = isGrounded();
        gravityTime += delta;
        long interval = Math.max(65, (long) (800 * Math.pow(0.8, level - 1)));
        while (gravityTime >= interval) {
            gravityTime -= interval;
            if (fits(piece, rotation, x, y + 1)) y++;
            else { gravityTime = 0; break; }
        }
        if (isGrounded()) {
            if (grounded) lockTime += delta;
            if (lockTime >= 500) lock();
        } else if (lockResets < 15) lockTime = 0;
    }

    private void lock() {
        int[][] shape = cells(piece, rotation);
        for (int i = 0; i < 4; i++) {
            if (y + shape[i][1] < 0) { state = OVER; return; }
        }
        for (int i = 0; i < 4; i++) board[y + shape[i][1]][x + shape[i][0]] = piece + 1;
        pieces++;
        lastClear = clearLines();
        int[] rewards = {0,100,300,500,800};
        score += rewards[lastClear] * level;
        lines += lastClear;
        level = startingLevel + lines / 10;
        spawnNext();
    }

    private int clearLines() {
        int count = 0;
        for (int row = HEIGHT - 1; row >= 0; row--) {
            boolean full = true;
            for (int col = 0; col < WIDTH; col++) if (board[row][col] == 0) full = false;
            if (!full) continue;
            for (int above = row; above > 0; above--)
                System.arraycopy(board[above - 1], 0, board[above], 0, WIDTH);
            for (int col = 0; col < WIDTH; col++) board[0][col] = 0;
            count++;
            row++; // Inspect the row that just moved down too.
        }
        return count;
    }

    public void pause() { if (state == RUNNING) state = PAUSED; }
    public void resume() { if (state == PAUSED) state = RUNNING; }
    public boolean isGrounded() { return !fits(piece, rotation, x, y + 1); }
    public int ghostY() {
        int landing = y;
        while (fits(piece, rotation, x, landing + 1)) landing++;
        return landing;
    }
    public int getCell(int col, int row) { return board[row][col]; }
    public int getState() { return state; }
    public int getPiece() { return piece; }
    public int getRotation() { return rotation; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getHeld() { return held; }
    public boolean canHold() { return !holdUsed; }
    public int getNext(int index) { return queue.get(index); }
    public int getScore() { return score; }
    public int getLines() { return lines; }
    public int getLevel() { return level; }
    public int getLastClear() { return lastClear; }
    public int getPieces() { return pieces; }
}
