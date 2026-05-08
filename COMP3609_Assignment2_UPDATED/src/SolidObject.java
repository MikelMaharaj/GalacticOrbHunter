import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

public class SolidObject {

    int x, y;
    int w = 64, h = 64;

    private static BufferedImage rock1;
    private static BufferedImage rock2;

    private BufferedImage img;

    public SolidObject(int x, int y) {
        this.x = x;
        this.y = y;

        // Load images only once
        if (rock1 == null || rock2 == null) {
            try {
                rock1 = ImageIO.read(new File("assets/rock.png"));
                rock2 = ImageIO.read(new File("assets/rock2.png"));
            } catch (Exception e) {
                rock1 = createFallbackRock();
                rock2 = createFallbackRock();
            }
        }

        // Randomly assign ONE image to this rock
        Random rand = new Random();
        if (rand.nextBoolean()) {
            img = rock1;
        } else {
            img = rock2;
        }
    }

    private BufferedImage createFallbackRock() {
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(120, 120, 120, 255));
        Polygon p = new Polygon();
        p.addPoint(10, 52);
        p.addPoint(20, 14);
        p.addPoint(40, 8);
        p.addPoint(56, 24);
        p.addPoint(52, 48);
        p.addPoint(28, 58);
        g2.fillPolygon(p);

        g2.setColor(new Color(160, 160, 160, 180));
        g2.fillOval(22, 18, 14, 10);

        g2.dispose();
        return image;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }

    public void draw(Graphics2D g, int worldX, int worldY) {
        g.drawImage(img, x - worldX, y - worldY, w, h, null);
    }
}