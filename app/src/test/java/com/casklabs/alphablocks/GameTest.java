package com.casklabs.alphablocks;

import org.junit.Test;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import static org.junit.Assert.*;

public class GameTest {
    private Game game(int first) {
        // A bag is drawn on construction, then a fresh one on start.
        for (int seed = 0; seed < 500; seed++) {
            Game g = new Game(new Random(seed));
            g.start(1);
            if (g.getPiece() == first) return g;
        }
        throw new AssertionError("No seed for requested piece");
    }
    private void clearBoard(Game g) { for (int[] row : g.board) Arrays.fill(row,0); }
    private void tick(Game g,int millis) {
        for (int i = 0; i < millis/50; i++) g.tick(50);
    }
    @Test public void initialBoardAndStartingLevel() {
        Game g = new Game(new Random(1));
        assertEquals(Game.READY,g.getState());
        g.start(5);
        assertEquals(Game.RUNNING,g.getState());
        assertEquals(5,g.getLevel());
        for (int y = 0; y < 20; y++) for (int x = 0; x < 10; x++) assertEquals(0,g.getCell(x,y));
    }
    @Test public void bagsContainEachShapeOnce() {
        Game g = new Game(new Random(14));
        g.start(1);
        for (int bag = 0; bag < 6; bag++) {
            HashSet<Integer> types = new HashSet<Integer>();
            for (int n = 0; n < 7; n++) {
                types.add(g.getPiece());
                g.hardDrop();
                clearBoard(g);
                assertEquals(Game.RUNNING,g.getState());
            }
            assertEquals(7,types.size());
        }
    }
    @Test public void previewMatchesSpawnOrder() {
        Game g = game(Game.T);
        int[] preview = {g.getNext(0),g.getNext(1),g.getNext(2)};
        for (int next : preview) {
            g.hardDrop(); clearBoard(g);
            assertEquals(next,g.getPiece());
        }
    }
    @Test public void heldPieceCanOnlyBeSwappedOncePerTurn() {
        Game g = game(Game.T);
        int first = g.getPiece(), next = g.getNext(0);
        g.hold();
        assertEquals(first,g.getHeld());
        assertEquals(next,g.getPiece());
        assertFalse(g.canHold());
        g.hold(); assertEquals(next,g.getPiece());
        g.hardDrop(); clearBoard(g);
        int swapped = g.getPiece();
        g.rotate(1); g.move(-1); g.softDrop(); g.hold();
        assertEquals(first,g.getPiece());
        assertEquals(swapped,g.getHeld());
        assertEquals(0,g.getRotation());
        assertEquals(3,g.getX());
        assertEquals(-1,g.getY());
    }
    @Test public void horizontalMovementCannotEscapeBoard() {
        Game g = game(Game.I);
        for (int i = 0; i < 30; i++) g.move(-1);
        assertEquals(0,g.getX());
        assertFalse(g.move(-1));
        for (int i = 0; i < 30; i++) g.move(1);
        assertEquals(6,g.getX());
        assertFalse(g.move(1));
    }
    @Test public void piecesCannotMoveThroughStack() {
        Game g = game(Game.O);
        g.board[0][3] = 1;
        assertFalse(g.move(-1));
        assertEquals(3,g.getX());
    }
    @Test public void fourRotationsRestoreEachShape() {
        for (int type = 0; type < 7; type++) {
            Game g = game(type);
            int[][] before = Game.cells(type,0);
            for (int i = 0; i < 4; i++) g.rotate(1);
            assertEquals(0,g.getRotation());
            assertTrue(Arrays.deepEquals(before,Game.cells(type,g.getRotation())));
        }
    }
    @Test public void rotationKicksAwayFromWall() {
        Game g = game(Game.I);
        g.rotate(1);
        while (g.move(-1)) { }
        assertEquals(-2,g.getX());
        assertTrue(g.rotate(-1));
        assertEquals(0,g.getX());
    }
    @Test public void rotationKicksAwayFromFloor() {
        Game g = game(Game.T);
        while (g.softDrop()) { }
        assertTrue(g.rotate(1));
        for (int[] cell : Game.cells(g.getPiece(),g.getRotation())) assertTrue(g.getY()+cell[1] < 20);
    }
    @Test public void rotationCannotOverlapStack() {
        Game g = game(Game.T);
        while (g.softDrop()) { }
        for (int row = 14; row < 20; row++) Arrays.fill(g.board[row],1);
        assertFalse(g.rotate(1));
        assertEquals(0,g.getRotation());
    }
    @Test public void hardDropMatchesGhostAndAwardsPoints() {
        Game g = game(Game.O);
        int landing = g.ghostY(), from = g.getY();
        g.hardDrop();
        assertEquals((landing-from)*2,g.getScore());
        assertEquals(Game.O+1,g.getCell(4,18));
        assertEquals(Game.O+1,g.getCell(5,19));
        assertEquals(1,g.getPieces());
    }
    @Test public void softDropStopsAtFloorAndScoresOnlyMovement() {
        Game g = game(Game.O);
        int distance = g.ghostY()-g.getY();
        while (g.softDrop()) { }
        assertEquals(distance,g.getScore());
        assertFalse(g.softDrop());
        assertEquals(distance,g.getScore());
    }
    @Test public void gravityDoesNotAwardDropPoints() {
        Game g = game(Game.T);
        int before = g.getY();
        tick(g,800);
        assertEquals(before+1,g.getY());
        assertEquals(0,g.getScore());
    }
    @Test public void groundedPieceGetsLockDelay() {
        Game g = game(Game.O);
        while (g.softDrop()) { }
        tick(g,450);
        assertEquals(0,g.getPieces());
        tick(g,50);
        assertEquals(1,g.getPieces());
    }
    @Test public void groundedMovementResetsDelay() {
        Game g = game(Game.O);
        while (g.softDrop()) { }
        tick(g,400);
        g.move(-1);
        tick(g,400);
        assertEquals(0,g.getPieces());
        tick(g,100);
        assertEquals(1,g.getPieces());
    }
    @Test public void lockDelayCannotBeResetForever() {
        Game g = game(Game.O);
        while (g.softDrop()) { }
        for (int i = 0; i < 25 && g.getPieces() == 0; i++) {
            g.move(i%2 == 0 ? -1 : 1);
            tick(g,100);
        }
        assertTrue(g.getPieces() > 0);
    }
    @Test public void pauseFreezesGravityAndEveryInput() {
        Game g = game(Game.T);
        int x = g.getX(), y = g.getY(), piece = g.getPiece();
        g.pause();
        tick(g,5000); g.move(-1); g.rotate(1); g.softDrop(); g.hardDrop(); g.hold();
        assertEquals(x,g.getX()); assertEquals(y,g.getY()); assertEquals(piece,g.getPiece());
        assertEquals(0,g.getPieces()); assertEquals(0,g.getScore());
        g.resume(); tick(g,800); assertEquals(y+1,g.getY());
    }
    @Test public void clearsSingleRowAndMovesRowsAboveDown() {
        Game g = game(Game.I);
        for (int x = 0; x < 10; x++) if (x < 3 || x > 6) g.board[19][x] = 1;
        g.board[18][0] = 2;
        int points = (g.ghostY()-g.getY())*2;
        g.hardDrop();
        assertEquals(1,g.getLines()); assertEquals(1,g.getLastClear());
        assertEquals(points+100,g.getScore());
        assertEquals(2,g.getCell(0,19)); assertEquals(0,g.getCell(0,18));
    }
    @Test public void clearsAdjacentRowsWithoutSkipping() {
        Game g = game(Game.O);
        for (int y = 18; y < 20; y++) for (int x = 0; x < 10; x++)
            if (x != 4 && x != 5) g.board[y][x] = 1;
        int points = (g.ghostY()-g.getY())*2;
        g.hardDrop();
        assertEquals(2,g.getLines()); assertEquals(points+300,g.getScore());
        for (int[] row : g.board) for (int cell : row) assertEquals(0,cell);
    }
    @Test public void fourLineClearScoresEightHundredAtLevelOne() {
        Game g = game(Game.I);
        g.rotate(1);
        for (int y = 16; y < 20; y++) for (int x = 0; x < 10; x++)
            if (x != 5) g.board[y][x] = 1;
        int points = (g.ghostY()-g.getY())*2;
        g.hardDrop();
        assertEquals(4,g.getLastClear()); assertEquals(points+800,g.getScore());
    }
    @Test public void everyTenLinesRaisesLevelAndScoringMultiplier() {
        Game g = game(Game.I);
        for (int n = 0; n < 10; n++) {
            clearBoard(g);
            for (int x = 0; x < 10; x++) g.board[19][x] = 1;
            g.hardDrop();
        }
        assertEquals(10,g.getLines()); assertEquals(2,g.getLevel());
        clearBoard(g);
        Arrays.fill(g.board[19],1);
        int before = g.getScore(), dropPoints = (g.ghostY()-g.getY())*2;
        g.hardDrop(); assertEquals(before+dropPoints+200,g.getScore());
    }
    @Test public void blockedSpawnEndsGame() {
        Game g = game(Game.T);
        // Fill only after moving the active piece below the spawn region.
        for (int i = 0; i < 6; i++) g.softDrop();
        Arrays.fill(g.board[0],1);
        Arrays.fill(g.board[1],1);
        // Leave gaps so the occupied spawn rows are not cleared by lock().
        g.board[0][0] = g.board[1][0] = 0;
        g.hardDrop();
        assertEquals(Game.OVER,g.getState());
        int pieces = g.getPieces(); g.hardDrop(); assertEquals(pieces,g.getPieces());
    }
    @Test public void lockAboveTopEndsGameWithoutIndexError() {
        Game g = game(Game.O);
        Arrays.fill(g.board[1],1);
        g.hardDrop();
        assertEquals(Game.OVER,g.getState());
    }
    @Test public void restartResetsAllProgressAndHeldState() {
        Game g = game(Game.T);
        g.hold(); g.hardDrop(); g.pause(); g.start(3);
        assertEquals(0,g.getScore()); assertEquals(0,g.getLines());
        assertEquals(0,g.getPieces()); assertEquals(-1,g.getHeld());
        assertTrue(g.canHold()); assertEquals(3,g.getLevel());
    }
    @Test public void longStallDoesNotDropAnEntireBoard() {
        Game g = game(Game.T);
        int before = g.getY(); g.tick(60000);
        assertEquals(before,g.getY());
    }
    @Test public void randomActionsKeepBoardAndActivePieceValid() {
        Random random = new Random(42);
        Game g = game(Game.L);
        for (int step = 0; step < 20000; step++) {
            if (g.getState() == Game.OVER) g.start(1);
            switch (random.nextInt(7)) {
                case 0: g.move(-1); break; case 1: g.move(1); break;
                case 2: g.rotate(1); break; case 3: g.rotate(-1); break;
                case 4: g.softDrop(); break; case 5: g.hardDrop(); break;
                default: g.hold(); break;
            }
            g.tick(100);
            for (int[] row : g.board) for (int cell : row) assertTrue(cell >= 0 && cell <= 7);
            if (g.getState() != Game.RUNNING) continue;
            for (int[] cell : Game.cells(g.getPiece(),g.getRotation())) {
                int x = g.getX()+cell[0], y = g.getY()+cell[1];
                assertTrue(x >= 0 && x < 10 && y < 20);
                if (y >= 0) assertEquals(0,g.getCell(x,y));
            }
        }
    }
}
