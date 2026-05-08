import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Projectile {

    int x, y;
    int w, h;
    int dx, dy;
    int damage;
    boolean friendly;
    Color color;

    public Projectile(int x, int y, int dx, int dy, int w, int h, int damage, boolean friendly, Color color) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.w = w;
        this.h = h;
        this.damage = damage;
        this.friendly = friendly;
        this.color = color;
    }

    public void update() {
        x += dx;
        y += dy;
    }

    public boolean isOffScreen(int panelWidth, int panelHeight) {
        return x + w < 0 || y + h < 0 || x > panelWidth || y > panelHeight;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }

    public int getDamage() {
        return damage;
    }

    public boolean isFriendly() {
        return friendly;
    }

    public void draw(Graphics2D g, int worldX, int worldY) {
        Graphics2D g2 = (Graphics2D) g.create();

        int drawX = x - worldX;
        int drawY = y - worldY;

        g2.setColor(color);

        if (friendly) {
            g2.fillRoundRect(drawX, drawY, w, h, 8, 8);
        } else {
            g2.fillOval(drawX, drawY, w, h);
        }

        g2.dispose();
    }
}
