package com.casklabs.alphablocks;

/** Controlled repeat independent of Android/Scalar key-repeat behavior. */
public final class RepeatInput {
    public static final int LEFT = 1, RIGHT = 2, DOWN = 3;
    private int horizontal;
    private boolean down;
    private long horizontalTime, downTime;

    public void press(int action) {
        if (action == DOWN) { down = true; downTime = 0; }
        else { horizontal = action; horizontalTime = -140; }
    }
    public void release(int action) {
        if (action == DOWN) down = false;
        else if (horizontal == action) horizontal = 0;
    }
    public void clear() { horizontal = 0; down = false; }
    public void tick(Game game, long elapsed) {
        if (game.getState() != Game.RUNNING) { clear(); return; }
        long delta = Math.max(0, Math.min(elapsed, 100));
        if (horizontal != 0) {
            horizontalTime += delta;
            while (horizontalTime >= 55) {
                horizontalTime -= 55;
                game.move(horizontal == LEFT ? -1 : 1);
            }
        }
        if (down) {
            downTime += delta;
            while (downTime >= 40) { downTime -= 40; game.softDrop(); }
        }
    }
}
