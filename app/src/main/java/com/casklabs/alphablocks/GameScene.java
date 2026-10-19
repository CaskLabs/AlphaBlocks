package com.casklabs.alphablocks;

import java.util.ArrayList;

/** One 640 x 480 scene, uniformly scaled and letterboxed by each rendering host. */
public final class GameScene {
    public static final int WIDTH = 640, HEIGHT = 480;
    public static final int BG = 0xff0b1220, PANEL = 0xff131f32, INK = 0xfff0f6ff;
    public static final int MUTED = 0xff9aacbf, ACCENT = 0xff58ead2;
    public static final int[] COLORS = {0xff53d8ef,0xffffd466,0xffb59aff,0xff6edaaa,
            0xfff17e91,0xff779fff,0xffffac70};
    public interface Draw {
        void rect(float x, float y, float w, float h, float radius, int color);
        void outline(float x, float y, float w, float h, float radius, int color);
        void text(String value, float x, float baseline, float size, int color, boolean bold);
    }
    private static final float BX = 224, BY = 40, CELL = 19;
    private static final int ROW = 100;
    private final ArrayList<Target> targets = new ArrayList<Target>();
    private final GameSession session;
    public GameScene(GameSession session) { this.session = session; }

    public void draw(Draw d) {
        targets.clear();
        d.rect(0,0,WIDTH,HEIGHT,0,BG);
        logo(d,22,14,5);
        d.text("ALPHABLOCKS",51,27,17,INK,true);
        d.text("CASKLABS / 1.0",502,26,11,MUTED,false);
        drawBoard(d);
        int screen = session.getScreen();
        if (screen == GameSession.PLAY) drawControls(d);
        else {
            d.rect(0,36,640,444,0,0xde0b1220);
            if (screen == GameSession.HELP) help(d);
            else menu(d,screen);
        }
    }

    private void drawBoard(Draw d) {
        Game g = session.game;
        panel(d,18,48,184,132);
        d.text("SCORE",32,72,12,MUTED,true);
        d.text(String.valueOf(g.getScore()),32,112,32,INK,true);
        d.text("BEST  " + session.getBest(),32,156,14,ACCENT,true);
        panel(d,18,192,184,98);
        d.text("LEVEL",32,218,12,MUTED,true);
        d.text(String.valueOf(g.getLevel()),32,252,25,INK,true);
        d.text("LINES",112,218,12,MUTED,true);
        d.text(String.valueOf(g.getLines()),112,252,25,INK,true);
        d.text("10 lines = next level",24,382,12,MUTED,false);
        d.text("MENU / PLAY to pause",24,405,12,MUTED,false);
        d.rect(BX-3,BY-3,196,386,5,0xff26374f);
        d.rect(BX,BY,190,380,0,0xff080f1b);
        for (int row = 0; row < Game.HEIGHT; row++) {
            for (int col = 0; col < Game.WIDTH; col++) {
                float x = BX + col*CELL, y = BY + row*CELL;
                d.rect(x+8.5f,y+8.5f,1.5f,1.5f,0,0xff1a2a3e);
                int value = g.getCell(col,row);
                if (value != 0) block(d,x,y,CELL,COLORS[value-1],false);
            }
        }
        if (g.getState() != Game.READY && g.getState() != Game.OVER) {
            if (session.showGhost()) active(d,g,g.ghostY(),true);
            active(d,g,g.getY(),false);
        }
        panel(d,436,48,186,198);
        d.text("UP NEXT",450,72,12,MUTED,true);
        for (int i = 0; i < 3; i++) preview(d,g.getNext(i),476,87+i*51,15,false);
        panel(d,436,260,186,122);
        d.text("HOLD / FN",450,285,12,MUTED,true);
        if (g.getHeld() >= 0) preview(d,g.getHeld(),476,303,17,!g.canHold());
        else d.text("Empty",485,328,16,MUTED,false);
        d.text(g.canHold() ? "Once per piece" : "Used this turn",450,365,12,MUTED,false);
        if (session.getScreen() == GameSession.PLAY) {
            targets.add(new Target(436,260,186,122,GameSession.HOLD));
            d.text("SHUTTER = DROP",450,406,13,ACCENT,true);
        }
    }
    private void active(Draw d, Game g, int py, boolean ghost) {
        int[][] cells = Game.cells(g.getPiece(),g.getRotation());
        for (int i = 0; i < 4; i++) {
            int row = py + cells[i][1];
            if (row >= 0 && row < 20)
                block(d,BX+(g.getX()+cells[i][0])*CELL,BY+row*CELL,CELL,COLORS[g.getPiece()],ghost);
        }
    }
    private void block(Draw d,float x,float y,float size,int color,boolean ghost) {
        if (ghost) {
            d.outline(x+2,y+2,size-4,size-4,2,0xff4c7087);
        } else {
            d.rect(x+1,y+1,size-2,size-2,3,color);
            d.rect(x+3,y+3,size-6,2,1,0x70ffffff);
            d.rect(x+3,y+size-5,size-6,2,1,0x22000000);
        }
    }
    private void preview(Draw d,int type,float x,float y,float size,boolean muted) {
        int[][] cells = Game.cells(type,0);
        for (int i = 0; i < 4; i++) block(d,x+cells[i][0]*size,y+cells[i][1]*size,
                size,muted ? 0xff586479 : COLORS[type],false);
    }
    private void panel(Draw d,float x,float y,float w,float h) { d.rect(x,y,w,h,9,PANEL); }
    private void drawControls(Draw d) {
        String[] labels = {"LEFT","RIGHT","ROTATE","DOWN","DROP","PAUSE"};
        int[] actions = {GameSession.LEFT,GameSession.RIGHT,GameSession.ACCEPT,
                GameSession.DOWN,GameSession.DROP,GameSession.MENU};
        for (int i = 0; i < labels.length; i++) {
            float x = 18+i*102;
            button(d,x,436,94,34,labels[i],actions[i],i == 4);
        }
    }
    private void button(Draw d,float x,float y,float w,float h,String text,int action,boolean primary) {
        d.rect(x,y,w,h,7,primary ? ACCENT : PANEL);
        d.outline(x,y,w,h,7,primary ? ACCENT : 0xff344963);
        // Center using the renderer's monospaced-ish button label approximation.
        d.text(text,x+(w-text.length()*7)/2,y+h/2+5,12,primary ? BG : INK,true);
        targets.add(new Target(x,y,w,h,action));
    }
    private void menu(Draw d,int screen) {
        boolean home = screen == GameSession.HOME;
        boolean options = screen == GameSession.OPTIONS;
        boolean over = screen == GameSession.OVER;
        String title = home ? "One more block." : options ? "Your game. Your pace."
                : over ? "Room for another?" : "Take a breather.";
        String subtitle = home ? "A falling-block puzzle for your camera."
                : options ? "Changes to starting level apply to the next game."
                : over ? "SCORE " + session.game.getScore() + "    BEST " + session.getBest()
                : "Your game is paused.";
        float top = 72;
        d.text(title,112,top+26,30,INK,true);
        d.text(subtitle,112,top+53,13,MUTED,false);
        int count = session.rowCount();
        for (int i = 0; i < count; i++) {
            float y = top+76+i*56;
            boolean focused = i == session.getSelected();
            d.rect(112,y,416,46,8,focused ? 0xff164d50 : PANEL);
            d.outline(112,y,416,46,8,focused ? ACCENT : 0xff2b3d54);
            d.text(session.rowTitle(i),130,y+29,18,INK,true);
            d.text(session.rowValue(i),options && i < 2 ? 450 : 500,y+29,16,ACCENT,true);
            targets.add(new Target(112,y,416,46,ROW+i));
        }
        d.text("UP / DOWN  Select     CENTER / SHUTTER  Open",112,452,12,MUTED,false);
        if (options) d.text("LEFT / RIGHT to adjust. Tap a row to change it.",112,415,12,MUTED,false);
    }
    private void help(Draw d) {
        d.text("Small camera. Big game.",86,85,29,INK,true);
        d.text("Complete rows. Keep the stack below the top.",86,114,14,MUTED,false);
        String[] keys = {"LEFT / RIGHT", "UP / CENTER", "DOWN", "SHUTTER", "FN", "PLAY / MENU"};
        String[] values = {"Move (hold to repeat)", "Rotate clockwise", "Soft drop (hold)",
                "Drop and lock", "Hold / swap piece", "Pause / resume"};
        for (int i = 0; i < keys.length; i++) {
            float y = 155+i*34;
            d.text(keys[i],86,y,13,ACCENT,true);
            d.text(values[i],287,y,15,INK,false);
        }
        d.text("1 / 2 / 3 / 4 rows: 100 / 300 / 500 / 800 x level",86,376,13,MUTED,false);
        button(d,86,402,468,44,"BACK",ROW,true);
    }
    private void logo(Draw d,float x,float y,float s) {
        int[][] cells = {{0,0},{1,0},{1,1},{2,1},{1,2},{2,2}};
        for (int i = 0; i < cells.length; i++)
            d.rect(x+cells[i][0]*(s+1),y+cells[i][1]*(s+1),s,s,1,
                    i < 3 ? ACCENT : COLORS[2]);
    }
    public int hit(float x,float y) {
        // Menu overlays must completely block the gameplay targets underneath.
        for (int i = targets.size()-1; i >= 0; i--) {
            Target t = targets.get(i);
            if (session.getScreen() != GameSession.PLAY && t.action < ROW) continue;
            if (x >= t.x && x <= t.x+t.w && y >= t.y && y <= t.y+t.h) return t.action;
        }
        return -1;
    }
    public void click(int action) {
        if (action >= ROW) session.activate(action-ROW);
        else session.press(action);
    }
    public static float scale(int width,int height) {
        return Math.min(width/(float) WIDTH,height/(float) HEIGHT);
    }
    private static final class Target {
        final float x,y,w,h;
        final int action;
        Target(float x,float y,float w,float h,int action) {
            this.x=x; this.y=y; this.w=w; this.h=h; this.action=action;
        }
    }
}
