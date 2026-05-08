import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Random;

public class Boss {

    int x, y;
    int w = 240, h = 120;

    private int health = 800;
    private int maxHealth = 800;
    private int stage = 1;

    private int direction = 1;
    private int speed = 3;

    private int shootCooldown = 45;
    private Random rand = new Random();

    public Boss(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void update(ArrayList<Projectile> attacks, int arenaWidth) {
        updateStage();
        x += direction * speed;

        if (x < 20) {
            x = 20;
            direction = 1;
        }

        if (x + w > arenaWidth - 20) {
            x = arenaWidth - w - 20;
            direction = -1;
        }

        if (stage == 1) speed = 3;
        else if (stage == 2) speed = 5;
        else speed = 7;

        if (shootCooldown > 0) {
            shootCooldown--;
        } else {
            firePattern(attacks);
            if (stage == 1) {
                shootCooldown = 50;
            } else if (stage == 2) {
                shootCooldown = 35;
            } else {
                shootCooldown = 20;
            }
        }
    }

    private void firePattern(ArrayList<Projectile> attacks) {

        int centerX = x + w / 2;
        int bottomY = y + h;

        SoundPlayer.play("assets/bossShoot.wav");

        if (stage == 1) {
            // SIMPLE
            attacks.add(new Projectile(centerX - 6, bottomY, 0, 8, 12, 24, 10, false, Color.RED));
        }

        else if (stage == 2) {
            // WIDE SPREAD
            attacks.add(new Projectile(centerX - 20, bottomY, -3, 7, 12, 24, 12, false, Color.ORANGE));
            attacks.add(new Projectile(centerX - 6, bottomY, 0, 8, 12, 24, 12, false, Color.RED));
            attacks.add(new Projectile(centerX + 8, bottomY, 3, 7, 12, 24, 12, false, Color.ORANGE));
        }

        else if (stage == 3) {
            // CHAOTIC BURST
            for (int i = -2; i <= 2; i++) {
                attacks.add(new Projectile(
                        centerX + (i * 10),
                        bottomY,
                        i * 2,
                        6 + Math.abs(i),
                        12,
                        24,
                        12,
                        false,
                        Color.PINK
                ));
            }

            // EXTRA straight laser
            attacks.add(new Projectile(centerX - 4, bottomY, 0, 12, 8, 30, 15, false, Color.RED));
        }
    }

    private void updateStage() {
        double hpPercent = (double) health / maxHealth;

        if (hpPercent <= 0.33) {
            stage = 3;
        } else if (hpPercent <= 0.66) {
            stage = 2;
        } else {
            stage = 1;
        }
    }

    public void takeDamage(int amount) {
        health -= amount;
        if (health < 0) {
            health = 0;
        }
    }

    public boolean isDefeated() {
        return health <= 0;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, w, h);
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void draw(Graphics2D g, int worldX, int worldY) {
        Graphics2D g2 = (Graphics2D) g.create();

        int drawX = x - worldX;
        int drawY = y - worldY;

        if (stage == 1) g2.setColor(new Color(45, 20, 80));
        else if (stage == 2) g2.setColor(new Color(120, 40, 80));
        else g2.setColor(new Color(180, 30, 30));
        g2.fillRoundRect(drawX, drawY, w, h, 40, 40);

        g2.setColor(new Color(140, 60, 220));
        g2.fillRoundRect(drawX + 20, drawY + 18, w - 40, h - 36, 25, 25);

        g2.setColor(new Color(80, 30, 140));
        Polygon leftWing = new Polygon();
        leftWing.addPoint(drawX + 10, drawY + 28);
        leftWing.addPoint(drawX - 34, drawY + 46);
        leftWing.addPoint(drawX + 10, drawY + 64);
        g2.fillPolygon(leftWing);

        Polygon rightWing = new Polygon();
        rightWing.addPoint(drawX + w - 10, drawY + 28);
        rightWing.addPoint(drawX + w + 34, drawY + 46);
        rightWing.addPoint(drawX + w - 10, drawY + 64);
        g2.fillPolygon(rightWing);

        g2.setColor(new Color(255, 70, 70));
        g2.fillOval(drawX + w / 2 - 22, drawY + h / 2 - 22, 44, 44);

        g2.setColor(new Color(255, 255, 255, 180));
        g2.fillOval(drawX + w / 2 - 8, drawY + h / 2 - 8, 16, 16);

        g2.setColor(new Color(255, 255, 255, 120));
        g2.fillRect(drawX + 30, drawY + h - 18, w - 60, 8);

        g2.dispose();
    }
}