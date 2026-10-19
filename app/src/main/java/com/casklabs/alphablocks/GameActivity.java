package com.casklabs.alphablocks;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.KeyEvent;
import android.view.WindowManager;
import com.github.ma1co.openmemories.framework.DisplayManager;
import com.sony.scalar.sysutil.ScalarInput;

/** PMCA display integration adapted from ma1co/PMCADemo (MIT; see notices). */
public final class GameActivity extends Activity implements DisplayManager.Listener,
        GameSession.Listener {
    private GameSession session;
    private GameView gameView;
    private DisplayManager display;
    private SharedPreferences preferences;
    private final SparseIntArray pressed = new SparseIntArray();

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        preferences = getSharedPreferences("alphablocks", MODE_PRIVATE);
        session = new GameSession(new Game(), preferences.getInt("best",0),
                preferences.getInt("level",1), preferences.getBoolean("ghost",true), this);
        gameView = new GameView(this,session);
        setContentView(gameView);
    }
    @Override public void onResume() {
        super.onResume();
        try {
            display = DisplayManager.create(this);
            display.addListener(this);
            display.setColorDepth(DisplayManager.ColorDepth.HIGH);
        } catch (LinkageError e) {
            Log.i("AlphaBlocks","Scalar display unavailable. Using Android display");
        } catch (RuntimeException e) {
            Log.w("AlphaBlocks","Scalar display setup failed",e);
        }
        Intent info = new Intent("com.android.server.DAConnectionManagerService.AppInfoReceive");
        info.putExtra("package_name",getComponentName().getPackageName());
        info.putExtra("class_name",getComponentName().getClassName());
        sendBroadcast(info);
        autoPowerOff(false);
        gameView.startFrames();
    }
    @Override public void onPause() {
        gameView.stopFrames();
        pressed.clear();
        session.suspend();
        autoPowerOff(true);
        if (display != null) {
            try {
                display.setColorDepth(DisplayManager.ColorDepth.LOW);
            } catch (RuntimeException e) { Log.w("AlphaBlocks","Display restore failed",e); }
            finally {
                try { display.release(); }
                catch (RuntimeException e) { Log.w("AlphaBlocks","Display release failed",e); }
                display = null;
            }
        }
        super.onPause();
    }
    private void autoPowerOff(boolean enabled) {
        Intent intent = new Intent("com.android.server.DAConnectionManagerService.apo");
        intent.putExtra("apo_info",enabled ? "APO/NORMAL" : "APO/NO");
        sendBroadcast(intent);
    }
    @Override public void displayChanged(DisplayManager.Display value) {
        if (gameView != null) gameView.postInvalidate();
    }
    @Override public void onWindowFocusChanged(boolean focused) {
        super.onWindowFocusChanged(focused);
        if (!focused && session != null) {
            pressed.clear();
            gameView.cancelTouch();
            session.suspend();
            gameView.invalidate();
        }
    }
    @Override public void save(int best,int level,boolean ghost) {
        // API 10 supports apply(): never block gameplay on a filesystem write.
        preferences.edit().putInt("best",best).putInt("level",level)
                .putBoolean("ghost",ghost).apply();
    }
    @Override public void exit() { finish(); }
    @Override public void onBackPressed() { session.press(GameSession.BACK); gameView.invalidate(); }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int action = mappedAction(event);
        if (action == 0) return super.dispatchKeyEvent(event);
        // Consume focus/half-shutter events without dropping the piece.
        if (action == -1) return true;
        int identity = event.getScanCode() != 0 ? event.getScanCode() : -event.getKeyCode();
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (pressed.indexOfKey(identity) < 0) {
                pressed.put(identity,action);
                session.press(action);
            }
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            int previous = pressed.get(identity,0);
            pressed.delete(identity);
            if (previous != 0) session.release(previous);
        }
        gameView.invalidate();
        return true;
    }
    private int mappedAction(KeyEvent event) {
        switch (event.getScanCode()) {
            case ScalarInput.ISV_KEY_LEFT: return GameSession.LEFT;
            case ScalarInput.ISV_KEY_RIGHT: return GameSession.RIGHT;
            case ScalarInput.ISV_KEY_DOWN: return GameSession.DOWN;
            case ScalarInput.ISV_KEY_UP: return GameSession.UP;
            case ScalarInput.ISV_KEY_ENTER: return GameSession.ACCEPT;
            case ScalarInput.ISV_KEY_S2: return GameSession.DROP;
            case ScalarInput.ISV_KEY_FN: case ScalarInput.ISV_KEY_AEL: return GameSession.HOLD;
            case ScalarInput.ISV_KEY_MENU: case ScalarInput.ISV_KEY_SK1:
            case ScalarInput.ISV_KEY_PLAY: return GameSession.MENU;
            case ScalarInput.ISV_KEY_DELETE: case ScalarInput.ISV_KEY_SK2: return GameSession.BACK;
            case ScalarInput.ISV_KEY_S1_1: case ScalarInput.ISV_KEY_S1_2: return -1;
            default: break;
        }
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_A: return GameSession.LEFT;
            case KeyEvent.KEYCODE_DPAD_RIGHT: case KeyEvent.KEYCODE_D: return GameSession.RIGHT;
            case KeyEvent.KEYCODE_DPAD_DOWN: case KeyEvent.KEYCODE_S: return GameSession.DOWN;
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_W: return GameSession.UP;
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: return GameSession.ACCEPT;
            case KeyEvent.KEYCODE_SPACE: case KeyEvent.KEYCODE_CAMERA: return GameSession.DROP;
            case KeyEvent.KEYCODE_C: return GameSession.HOLD;
            case KeyEvent.KEYCODE_MENU: case KeyEvent.KEYCODE_P: return GameSession.MENU;
            case KeyEvent.KEYCODE_BACK: return GameSession.BACK;
            case KeyEvent.KEYCODE_FOCUS: return -1;
            default: return 0;
        }
    }
}
