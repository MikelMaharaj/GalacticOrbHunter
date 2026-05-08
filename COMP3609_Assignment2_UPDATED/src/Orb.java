import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Orb {

    int x, y;
    int w = 40, h = 40;

    boolean collected = false;
    boolean fading = false;

    private float alpha = 1.0f;

    private BufferedImage[] frames = new BufferedImage[2];
    private int frame = 0;
    private int delay = 0;

    public Orb(int x, int y) {
        this.x = x;
        this.y = y;

        try {
            frames[0] = ImageIO.read(new File("assets/orb1.png"));
        } catch (Exception e) {
            frames[0] = createFallbackOrb(false);
        }

        try {
            frames[1] = ImageIO.read(new File("assets/orb2.png"));
        } catch (Exception e) {
            frames[1] = createFallbackOrb(true);
        }
    }

    private BufferedImage createFallbackOrb(boolean alt) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(alt ? new Color(255, 180, 0, 255) : new Color(255, 235, 0, 255));
        g2.fillOval(4, 4, w - 8, h - 8);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillOval(12, 12, 10, 10);

        g2.dispose();
        return img;
    }

    public void animate() {
        delay++;
        if (delay > 15) {
            frame++;
            if (frame >= frames.length) {
                frame = 0;
            }
            delay = 0;
        }

        if (fading) {
            alpha -= 0.05f;
            if (alpha < 0f) {
                alpha = 0f;
            }
        }
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }

    public boolean isFullyGone() {
        return collected && alpha <= 0f;
    }

    public void startFade() {
        collected = true;
        fading = true;
    }

    public void draw(Graphics2D g, int worldX, int worldY) {
        if (collected && alpha <= 0f) {
            return;
        }

        int drawX = x - worldX;
        int drawY = y - worldY;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.drawImage(frames[frame], drawX, drawY, w, h, null);
        g2.dispose();
    }
}