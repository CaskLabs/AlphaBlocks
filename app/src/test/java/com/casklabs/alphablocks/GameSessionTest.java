package com.casklabs.alphablocks;

import org.junit.Test;
import java.util.Random;
import static org.junit.Assert.*;

public class GameSessionTest {
    private static class Listener implements GameSession.Listener {
        int best, level, saves, exits;
        boolean ghost;
        public void save(int best,int level,boolean ghost) {
            this.best=best; this.level=level; this.ghost=ghost; saves++;
        }
        public void exit() { exits++; }
    }
    private final Listener listener = new Listener();
    private final GameSession session = new GameSession(new Game(new Random(7)),0,1,true,listener);
    private void start() { session.press(GameSession.ACCEPT); }
    @Test public void menuWrapsAndStartsWithShutter() {
        session.press(GameSession.UP); assertEquals(3,session.getSelected());
        session.press(GameSession.DOWN); assertEquals(0,session.getSelected());
        session.press(GameSession.DROP); assertEquals(GameSession.PLAY,session.getScreen());
        assertEquals(0,session.game.getPieces());
    }
    @Test public void onlyExplicitExitRowFinishesApp() {
        session.press(GameSession.BACK); assertEquals(0,listener.exits);
        session.activate(3); assertEquals(1,listener.exits);
    }
    @Test public void lifecyclePauseDoesNotAutoResumeOrKeepHeldMovement() {
        start(); session.press(GameSession.LEFT); session.suspend();
        int x = session.game.getX(), y = session.game.getY();
        session.tick(1000);
        assertEquals(x,session.game.getX()); assertEquals(y,session.game.getY());
        assertEquals(GameSession.PAUSE,session.getScreen());
        session.activate(0); session.tick(100);
        assertEquals(x,session.game.getX());
    }
    @Test public void repeatHasInitialDelayAndStopsOnRelease() {
        start(); int x = session.game.getX();
        session.press(GameSession.LEFT); assertEquals(x-1,session.game.getX());
        session.tick(100); assertEquals(x-1,session.game.getX());
        session.tick(95); assertEquals(x-2,session.game.getX());
        session.release(GameSession.LEFT); session.tick(100);
        assertEquals(x-2,session.game.getX());
    }
    @Test public void releasingOlderDirectionDoesNotCancelNewerDirection() {
        start(); session.press(GameSession.LEFT); session.press(GameSession.RIGHT);
        int x = session.game.getX(); session.release(GameSession.LEFT);
        session.tick(100); session.tick(100);
        assertEquals(x+1,session.game.getX());
    }
    @Test public void downRepeatsAndStopsAfterFocusLoss() {
        start(); session.press(GameSession.DOWN); int y = session.game.getY();
        session.tick(80); assertEquals(y+2,session.game.getY());
        session.clearInput(); session.tick(80); assertEquals(y+2,session.game.getY());
    }
    @Test public void optionsPreservePausedGameAndPersistSettings() {
        start(); session.press(GameSession.DROP); int pieces = session.game.getPieces();
        session.press(GameSession.MENU); session.activate(1);
        session.press(GameSession.RIGHT); assertEquals(2,listener.level);
        session.press(GameSession.DOWN); session.press(GameSession.LEFT);
        assertFalse(listener.ghost);
        session.press(GameSession.BACK);
        assertEquals(GameSession.PAUSE,session.getScreen());
        assertEquals(pieces,session.game.getPieces()); assertEquals(1,session.game.getLevel());
        session.activate(0); assertEquals(GameSession.PLAY,session.getScreen());
    }
    @Test public void clickAndCameraActivationShareTheSameMenus() {
        session.activate(1); assertEquals(GameSession.OPTIONS,session.getScreen());
        session.activate(0); assertEquals(2,session.getInitialLevel());
        session.activate(1); assertFalse(session.showGhost());
        session.activate(2); assertEquals(GameSession.HOME,session.getScreen());
        session.activate(0); assertEquals(2,session.game.getLevel());
    }
    @Test public void helpReturnsToTheMenuItCameFrom() {
        start(); session.suspend(); session.activate(2);
        assertEquals(GameSession.HELP,session.getScreen());
        session.press(GameSession.ACCEPT);
        assertEquals(GameSession.PAUSE,session.getScreen());
    }
    @Test public void bestScoreIsSavedOnPause() {
        start(); session.press(GameSession.DROP); session.suspend();
        assertTrue(listener.best > 0);
        assertEquals(session.game.getScore(),listener.best);
    }
    @Test public void gameOverPresentsReplayAndPersistsScore() {
        start();
        for (int i = 0; i < 100 && session.getScreen() == GameSession.PLAY; i++)
            session.press(GameSession.DROP);
        assertEquals(GameSession.OVER,session.getScreen()); assertTrue(listener.best > 0);
        session.activate(0);
        assertEquals(GameSession.PLAY,session.getScreen()); assertEquals(0,session.game.getScore());
    }
    @Test public void startingLevelIsBounded() {
        session.activate(1);
        for (int i = 0; i < 30; i++) session.press(GameSession.RIGHT);
        assertEquals(15,session.getInitialLevel());
        for (int i = 0; i < 30; i++) session.press(GameSession.LEFT);
        assertEquals(1,session.getInitialLevel());
    }
    private static final GameScene.Draw NO_DRAW = new GameScene.Draw() {
        public void rect(float x,float y,float w,float h,float r,int c) { }
        public void outline(float x,float y,float w,float h,float r,int c) { }
        public void text(String v,float x,float y,float s,int c,boolean b) { }
    };
    @Test public void clickableMenuAndHoldPanelWorkWithoutBottomControls() {
        GameScene scene = new GameScene(session); scene.draw(NO_DRAW);
        scene.click(scene.hit(200,170)); assertEquals(GameSession.PLAY,session.getScreen());
        scene.draw(NO_DRAW);
        for (int i = 0; i < 6; i++) assertEquals(-1,scene.hit(60+i*102,450));
        assertEquals(GameSession.HOLD,scene.hit(500,300));
        int piece = session.game.getPiece(); scene.click(scene.hit(500,300));
        assertEquals(piece,session.game.getHeld());
        assertEquals(GameSession.PLAY,session.getScreen());
    }
    @Test public void pausedOverlayDoesNotExposeHoldOrBoardControls() {
        start(); session.suspend();
        GameScene scene = new GameScene(session); scene.draw(NO_DRAW);
        assertEquals(-1,scene.hit(600,280)); assertEquals(-1,scene.hit(40,450));
    }
    @Test public void layoutScalesUniformlyAtCameraAndWideDisplaySizes() {
        assertEquals(1,GameScene.scale(640,480),0.001);
        assertEquals(1,GameScene.scale(800,480),0.001);
        assertEquals(0.5,GameScene.scale(320,240),0.001);
        assertEquals(2,GameScene.scale(1280,960),0.001);
    }
}
