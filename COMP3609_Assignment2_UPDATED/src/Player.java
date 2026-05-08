import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class Player {

    int x, y;
    int w = 64, h = 64;

    double rotation = 0;

    private BufferedImage[] frames = new BufferedImage[2];
    private int frame = 0;
    private int delay = 0;

    Rectangle[] hitboxes = new Rectangle[4];

    public Player(int x, int y) {
        this.x = x;
        this.y = y;

        try {
            frames[0] = ImageIO.read(new File("assets/player1.png"));
        } catch (Exception e) {
            frames[0] = createFallbackFrame(false);
        }

        try {
            frames[1] = ImageIO.read(new File("assets/player2.png"));
        } catch (Exception e) {
            frames[1] = createFallbackFrame(true);
        }

        updateHitboxes();
    }

    private BufferedImage createFallbackFrame(boolean alt) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(alt ? new Color(0, 160, 220, 255) : new Color(0, 200, 255, 255));

        Path2D ship = new Path2D.Double();
        ship.moveTo(w / 2.0, 4);
        ship.lineTo(w - 8, h / 2.0);
        ship.lineTo(w / 2.0, h - 6);
        ship.lineTo(8, h / 2.0);
        ship.closePath();
        g2.fill(ship);

        g2.setColor(Color.WHITE);
        g2.fillOval(w / 2 - 6, 18, 12, 16);

        g2.dispose();
        return img;
    }

    public void animate() {
        delay++;
        if (delay > 10) {
            frame++;
            if (frame >= frames.length) {
                frame = 0;
            }
            delay = 0;
        }
    }

    public void updateHitboxes() {
        hitboxes[0] = new Rectangle(x + 16, y + 4, 32, 20);
        hitboxes[1] = new Rectangle(x + 8, y + 20, 16, 28);
        hitboxes[2] = new Rectangle(x + 40, y + 20, 16, 28);
        hitboxes[3] = new Rectangle(x + 16, y + 40, 32, 20);
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }

    public Rectangle[] getCollisionBoxesAt(int px, int py) {
        Rectangle[] boxes = new Rectangle[4];

        boxes[0] = new Rectangle(px + 16, py + 4, 32, 20);
        boxes[1] = new Rectangle(px + 8, py + 20, 16, 28);
        boxes[2] = new Rectangle(px + 40, py + 20, 16, 28);
        boxes[3] = new Rectangle(px + 16, py + 40, 32, 20);

        return boxes;
    }

    public Rectangle[] getCollisionBoxes() {
        return getCollisionBoxesAt(x, y);
    }

    public void draw(Graphics2D g, int worldX, int worldY) {
        int drawX = x - worldX;
        int drawY = y - worldY;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.rotate(rotation, drawX + w / 2.0, drawY + h / 2.0);
        g2.drawImage(frames[frame], drawX, drawY, w, h, null);
        g2.dispose();
    }
}