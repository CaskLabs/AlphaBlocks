package com.casklabs.alphablocks;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/** Canvas only: compatible with API 10, no camera or network permission needed. */
public final class GameView extends View implements GameScene.Draw {
    private final GameSession session;
    private final GameScene scene;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private Canvas canvas;
    private boolean frames;
    private long previousTime;
    private int touchAction = -1;
    private float scale = 1, offsetX, offsetY;
    private final Runnable frame = new Runnable() {
        public void run() {
            if (!frames) return;
            long now = SystemClock.uptimeMillis();
            boolean wasPlaying = session.getScreen() == GameSession.PLAY;
            session.tick(now-previousTime);
            previousTime = now;
            if (wasPlaying) invalidate();
            postDelayed(this,33);
        }
    };
    public GameView(Context context,GameSession session) {
        super(context);
        this.session = session;
        scene = new GameScene(session);
        setFocusable(true);
        setContentDescription("AlphaBlocks game. Use direction keys, center to rotate, shutter to drop.");
    }
    public void startFrames() {
        if (frames) return;
        frames = true;
        previousTime = SystemClock.uptimeMillis();
        invalidate();
        post(frame);
    }
    public void stopFrames() { frames = false; removeCallbacks(frame); cancelTouch(); }
    public void cancelTouch() { touchAction = -1; session.clearInput(); }
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        scale = GameScene.scale(w,h);
        offsetX = (w-GameScene.WIDTH*scale)/2;
        offsetY = (h-GameScene.HEIGHT*scale)/2;
    }
    @Override protected void onDraw(Canvas target) {
        super.onDraw(target);
        target.drawColor(GameScene.BG);
        if (scale <= 0) return;
        target.save();
        target.translate(offsetX,offsetY);
        target.scale(scale,scale);
        canvas = target;
        scene.draw(this);
        canvas = null;
        target.restore();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (scale <= 0) return true;
        float x = (event.getX()-offsetX)/scale, y = (event.getY()-offsetY)/scale;
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                cancelTouch();
                touchAction = scene.hit(x,y);
                // Continuous controls start immediately. Menu rows activate on release.
                if (touchAction == GameSession.LEFT || touchAction == GameSession.RIGHT
                        || touchAction == GameSession.DOWN) scene.click(touchAction);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (scene.hit(x,y) != touchAction) cancelTouch();
                return true;
            case MotionEvent.ACTION_UP:
                int action = touchAction;
                cancelTouch();
                if (action >= 0 && scene.hit(x,y) == action && action != GameSession.LEFT
                        && action != GameSession.RIGHT && action != GameSession.DOWN) {
                    scene.click(action);
                    performClick();
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL: cancelTouch(); return true;
            default: return true;
        }
    }
    @Override public boolean performClick() { super.performClick(); return true; }
    @Override protected void onDetachedFromWindow() { stopFrames(); super.onDetachedFromWindow(); }
    public void rect(float x,float y,float w,float h,float radius,int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL);
        bounds.set(x,y,x+w,y+h);
        canvas.drawRoundRect(bounds,radius,radius,paint);
    }
    public void outline(float x,float y,float w,float h,float radius,int color) {
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1.5f);
        bounds.set(x,y,x+w,y+h);
        canvas.drawRoundRect(bounds,radius,radius,paint);
    }
    public void text(String value,float x,float baseline,float size,int color,boolean bold) {
        paint.setColor(color); paint.setStyle(Paint.Style.FILL); paint.setTextSize(size);
        paint.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        canvas.drawText(value,x,baseline,paint);
    }
}
