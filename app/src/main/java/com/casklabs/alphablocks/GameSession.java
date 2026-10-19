package com.casklabs.alphablocks;

/** Shared navigation and input rules for camera buttons and clickable controls. */
public final class GameSession {
    public static final int HOME = 0, PLAY = 1, PAUSE = 2, OPTIONS = 3, HELP = 4, OVER = 5;
    public static final int LEFT = 1, RIGHT = 2, DOWN = 3, UP = 4, ACCEPT = 5,
            DROP = 6, HOLD = 7, MENU = 8, BACK = 9;
    public interface Listener { void save(int best, int initialLevel, boolean ghost); void exit(); }
    public final Game game;
    private final Listener listener;
    private final RepeatInput repeat = new RepeatInput();
    private int screen = HOME, selected, returnScreen = HOME, best, initialLevel;
    private boolean ghost;

    public GameSession(Game game, int best, int initialLevel, boolean ghost, Listener listener) {
        this.game = game;
        this.best = best;
        this.initialLevel = Math.max(1, Math.min(15, initialLevel));
        this.ghost = ghost;
        this.listener = listener;
    }
    public void press(int action) {
        if (screen == PLAY) {
            switch (action) {
                case LEFT: game.move(-1); repeat.press(RepeatInput.LEFT); break;
                case RIGHT: game.move(1); repeat.press(RepeatInput.RIGHT); break;
                case DOWN: game.softDrop(); repeat.press(RepeatInput.DOWN); break;
                case UP: case ACCEPT: game.rotate(1); break;
                case DROP: game.hardDrop(); break;
                case HOLD: game.hold(); break;
                case MENU: case BACK: suspend(); break;
                default: break;
            }
            checkGame();
            return;
        }
        if (screen == HELP) {
            if (action == ACCEPT || action == DROP || action == BACK || action == MENU) back();
            return;
        }
        if (action == BACK || action == MENU) { back(); return; }
        if (action == UP || action == DOWN) {
            selected = (selected + (action == DOWN ? 1 : rowCount() - 1)) % rowCount();
        } else if (screen == OPTIONS && (action == LEFT || action == RIGHT)) {
            changeOption(action == RIGHT ? 1 : -1);
        } else if (action == ACCEPT || action == DROP) activate(selected);
    }

    public void release(int action) {
        if (action == LEFT) repeat.release(RepeatInput.LEFT);
        if (action == RIGHT) repeat.release(RepeatInput.RIGHT);
        if (action == DOWN) repeat.release(RepeatInput.DOWN);
    }
    public void clearInput() { repeat.clear(); }
    public void tick(long delta) {
        if (screen != PLAY) return;
        repeat.tick(game, delta);
        game.tick(delta);
        checkGame();
    }
    private void checkGame() {
        if (game.getScore() > best) best = game.getScore();
        if (game.getState() == Game.OVER && screen == PLAY) {
            screen = OVER;
            selected = 0;
            repeat.clear();
            save();
        }
    }
    public void suspend() {
        repeat.clear();
        if (screen == PLAY) {
            game.pause();
            screen = PAUSE;
            selected = 0;
        }
        save();
    }
    private void start() {
        game.start(initialLevel);
        screen = PLAY;
        repeat.clear();
    }
    private void resume() {
        game.resume();
        screen = PLAY;
        repeat.clear();
    }
    public void activate(int row) {
        if (row < 0 || row >= rowCount()) return;
        selected = row;
        if (screen == HOME) {
            if (row == 0) start();
            else if (row == 1) open(OPTIONS);
            else if (row == 2) open(HELP);
            else { save(); listener.exit(); }
        } else if (screen == PAUSE) {
            if (row == 0) resume();
            else if (row == 1) open(OPTIONS);
            else if (row == 2) open(HELP);
            else { screen = HOME; selected = 0; }
        } else if (screen == OVER) {
            if (row == 0) start(); else { screen = HOME; selected = 0; }
        } else if (screen == OPTIONS) {
            if (row == 2) back(); else changeOption(1);
        } else if (screen == HELP) back();
    }
    private void open(int target) { returnScreen = screen; screen = target; selected = 0; }
    private void back() {
        if (screen == OPTIONS || screen == HELP) { screen = returnScreen; selected = 0; save(); }
        else if (screen == PAUSE) resume();
        else if (screen == OVER) { screen = HOME; selected = 0; }
        // The home screen has an explicit Exit row to avoid accidental exits.
    }
    private void changeOption(int delta) {
        if (selected == 0) initialLevel = Math.max(1, Math.min(15, initialLevel + delta));
        else if (selected == 1) ghost = !ghost;
        save();
    }
    private void save() { listener.save(best, initialLevel, ghost); }
    public int rowCount() {
        switch (screen) {
            case HOME: case PAUSE: return 4;
            case OPTIONS: return 3;
            case OVER: return 2;
            case HELP: return 1;
            default: return 0;
        }
    }
    public String rowTitle(int row) {
        if (screen == HOME) return new String[]{"Play", "Options", "How to play", "Exit app"}[row];
        if (screen == PAUSE) return new String[]{"Resume", "Options", "How to play", "End game"}[row];
        if (screen == OVER) return row == 0 ? "Play again" : "Main menu";
        if (screen == OPTIONS) return new String[]{"Starting level", "Landing guide", "Done"}[row];
        return "Back";
    }
    public String rowValue(int row) {
        if (screen == OPTIONS && row == 0) return "<  " + initialLevel + "  >";
        if (screen == OPTIONS && row == 1) return ghost ? "On" : "Off";
        return ">";
    }
    public int getScreen() { return screen; }
    public int getSelected() { return selected; }
    public int getBest() { return best; }
    public int getInitialLevel() { return initialLevel; }
    public boolean showGhost() { return ghost; }
}
