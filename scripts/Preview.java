import com.casklabs.alphablocks.Game;
import com.casklabs.alphablocks.GameScene;
import com.casklabs.alphablocks.GameSession;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/** Desktop render of the actual scene used by the APK; no emulated hardware. */
public final class Preview implements GameScene.Draw {
    private final BufferedImage image = new BufferedImage(1280,960,BufferedImage.TYPE_INT_RGB);
    private final Graphics2D g = image.createGraphics();
    private Preview() {
        g.scale(2,2);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    public void rect(float x,float y,float w,float h,float r,int c) {
        g.setColor(new Color(c,true)); g.fill(new RoundRectangle2D.Float(x,y,w,h,r*2,r*2));
    }
    public void outline(float x,float y,float w,float h,float r,int c) {
        g.setColor(new Color(c,true)); g.setStroke(new BasicStroke(1.5f));
        g.draw(new RoundRectangle2D.Float(x,y,w,h,r*2,r*2));
    }
    public void text(String v,float x,float y,float s,int c,boolean b) {
        g.setColor(new Color(c,true)); g.setFont(new Font("SansSerif",b ? Font.BOLD : Font.PLAIN,1).deriveFont(s));
        g.drawString(v,x,y);
    }
    private static void render(GameSession session,String name) throws Exception {
        Preview p = new Preview(); new GameScene(session).draw(p);
        ImageIO.write(p.image,"png",new File("docs/"+name+".png")); p.g.dispose();
    }
    public static void main(String[] args) throws Exception {
        GameSession s = new GameSession(new Game(new Random(42)),12840,1,true,
            new GameSession.Listener() {
                public void save(int b,int l,boolean gh) { }
                public void exit() { }
            });
        render(s,"menu");
        s.activate(0);
        int[] columns = {-3,0,3,-2,2,0};
        for (int target : columns) {
            int action = target < 0 ? GameSession.LEFT : GameSession.RIGHT;
            for (int i = 0; i < Math.abs(target); i++) { s.press(action); s.release(action); }
            s.press(GameSession.DROP);
        }
        s.press(GameSession.HOLD);
        for (int i = 0; i < 5; i++) { s.press(GameSession.DOWN); s.release(GameSession.DOWN); }
        render(s,"gameplay");
        s.suspend(); render(s,"pause");
        s.activate(1); render(s,"options");
        s.press(GameSession.BACK); s.activate(2); render(s,"controls");
    }
}
