import javax.imageio.ImageIO;
import javax.swing.JPanel;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends JPanel implements Runnable, KeyListener {

    private enum GameState {
        LEVEL1,
        LEVEL1_COMPLETE,
        LEVEL2,
        PLAYER_DIED,
        VICTORY
    }

    private final int panelWidth = 1200;
    private final int panelHeight = 800;

    private Thread gameThread;
    private boolean isRunning = false;

    private BufferedImage backBuffer;
    private BufferedImage background;

    private int backgroundWidth = 2400;
    private int backgroundHeight = 1800;

    private double cameraX = 0;
    private double cameraY = 0;
    private int worldX = 0;
    private int worldY = 0;

    private Player player;
    private Boss boss;

    private ArrayList<Orb> orbs = new ArrayList<>();
    private ArrayList<SolidObject> solids = new ArrayList<>();
    private ArrayList<Projectile> playerBullets = new ArrayList<>();
    private ArrayList<Projectile> bossProjectiles = new ArrayList<>();

    private Random rand = new Random();

    private boolean up = false;
    private boolean down = false;
    private boolean left = false;
    private boolean right = false;
    private boolean spaceHeld = false;

    private int facingDirection = 0;

    private GameState state = GameState.LEVEL1;
    private int currentLevel = 1;

    private String startMessage = "";
    private int startMessageTimer = 0;

    private int score = 0;
    private int playerHealth = 100;

    private int collected = 0;
    private int orbsToCollect = 15;

    private int playerShootCooldown = 0;

    private boolean redTint = false;
    private long tintStart = 0L;
    private int rockHitCooldown = 0;

    private int fps = 0;
    private int frames = 0;
    private long lastFPS = System.currentTimeMillis();

    private int transitionTimer = 0;
    private int deathTimer = 0;


    private String effectText = "None";

    public GamePanel() {
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        setFocusable(true);
        setDoubleBuffered(false);
        addKeyListener(this);

        loadBackground();

        backBuffer = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_ARGB);

        player = new Player(200, 200);
        loadLevel1(true);
    }

    private void loadBackground() {
        try {
            background = ImageIO.read(new File("assets/background.png"));
            backgroundWidth = background.getWidth();
            backgroundHeight = background.getHeight();
        } catch (Exception e) {
            backgroundWidth = 2400;
            backgroundHeight = 1800;

            background = new BufferedImage(backgroundWidth, backgroundHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = background.createGraphics();
            g2.setColor(new Color(10, 10, 30));
            g2.fillRect(0, 0, backgroundWidth, backgroundHeight);

            for (int i = 0; i < 800; i++) {
                int x = rand.nextInt(backgroundWidth);
                int y = rand.nextInt(backgroundHeight);
                int size = rand.nextInt(2) + 1;
                g2.setColor(Color.WHITE);
                g2.fillOval(x, y, size, size);
            }

            g2.dispose();
        }
    }

    public void startGame() {
        if (gameThread == null) {
            isRunning = true;
            gameThread = new Thread(this);
            gameThread.start();
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            gameUpdate();
            gameRender();
            updateFPS();

            try {
                Thread.sleep(16);
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private void updateFPS() {
        frames++;
        long now = System.currentTimeMillis();

        if (now - lastFPS >= 1000) {
            fps = frames;
            frames = 0;
            lastFPS = now;
        }
    }

    private void loadLevel1(boolean resetScore) {

        startMessage = "Collect 15 orbs to power the spaceship's weapon systems";
        startMessageTimer = 240;

        state = GameState.LEVEL1;
        currentLevel = 1;

        if (resetScore) {
            score = 0;
        }

        playerHealth = 100;
        collected = 0;
        orbsToCollect = 15;

        playerShootCooldown = 0;
        spaceHeld = false;

        redTint = false;
        tintStart = 0L;
        rockHitCooldown = 0;
        effectText = "None";

        transitionTimer = 0;
        deathTimer = 0;

        boss = null;
        orbs.clear();
        solids.clear();
        playerBullets.clear();
        bossProjectiles.clear();

        player.x = 200;
        player.y = 200;
        player.rotation = 0;

        createLevel1World();
    }

    private void loadLevel2() {
        state = GameState.LEVEL2;
        currentLevel = 2;

        playerHealth = 100;
        collected = 0;
        orbsToCollect = 0;

        playerShootCooldown = 0;
        spaceHeld = false;

        redTint = false;
        tintStart = 0L;
        rockHitCooldown = 0;
        effectText = "None";

        transitionTimer = 0;
        deathTimer = 0;

        orbs.clear();
        solids.clear();
        playerBullets.clear();
        bossProjectiles.clear();

        boss = new Boss(panelWidth / 2 - 120, 55);

        player.x = panelWidth / 2 - player.w / 2;
        player.y = panelHeight - player.h - 60;
        player.rotation = 0;

        cameraX = 0;
        cameraY = 0;
        worldX = 0;
        worldY = 0;

        createBossArena();
        ensureSafePlayerSpawn();
    }

    private void ensureSafePlayerSpawn() {
        boolean safe = false;

        while (!safe) {
            safe = true;

            Rectangle playerRect = new Rectangle(player.x, player.y, player.w, player.h);

            for (SolidObject s : solids) {
                if (playerRect.intersects(s.getBounds())) {
                    // Move player slightly and re-check
                    player.y += 10;
                    safe = false;
                    break;
                }
            }
        }
    }

    private void createLevel1World() {
        spawnOrbs(15);
        spawnRocks(40, false);
    }

    private void createBossArena() {
        spawnRocks(14, true);
    }

    private void spawnOrbs(int count) {
        int attempts = 0;

        while (orbs.size() < count && attempts < 5000) {
            int x = rand.nextInt(Math.max(1, backgroundWidth - 40));
            int y = rand.nextInt(Math.max(1, backgroundHeight - 40));
            Rectangle orbRect = new Rectangle(x, y, 40, 40);

            if (isNearPlayerStart(orbRect) || overlapsAnySolid(orbRect) || overlapsAnyOrb(orbRect)) {
                attempts++;
                continue;
            }

            orbs.add(new Orb(x, y));
            attempts++;
        }
    }

    private void spawnRocks(int count, boolean bossArena) {
        int attempts = 0;

        while (solids.size() < count && attempts < 8000) {
            int x;
            int y;

            if (bossArena) {
                x = rand.nextInt(Math.max(1, panelWidth - 64));
                y = 180 + rand.nextInt(Math.max(1, panelHeight - 260));
            } else {
                x = rand.nextInt(Math.max(1, backgroundWidth - 64));
                y = rand.nextInt(Math.max(1, backgroundHeight - 64));
            }

            Rectangle rockRect = new Rectangle(x, y, 64, 64);

            if (isNearPlayerStart(rockRect) || overlapsAnySolid(rockRect) || overlapsAnyOrb(rockRect)) {
                attempts++;
                continue;
            }

            solids.add(new SolidObject(x, y));
            attempts++;
        }
    }

    private boolean isNearPlayerStart(Rectangle rect) {
        Rectangle start = new Rectangle(200, 200, player.w, player.h);
        return rect.intersects(start);
    }

    private boolean overlapsAnySolid(Rectangle rect) {
        for (SolidObject s : solids) {
            if (rect.intersects(s.getBounds())) {
                return true;
            }
        }
        return false;
    }

    private boolean overlapsAnyOrb(Rectangle rect) {
        for (Orb o : orbs) {
            if (rect.intersects(o.getBounds())) {
                return true;
            }
        }
        return false;
    }

    private void playSound(String path) {
        SoundPlayer.play(path);
    }

    private void applyDamage(int amount) {
        if (state == GameState.PLAYER_DIED || state == GameState.VICTORY) {
            return;
        }

        playerHealth -= amount;
        if (playerHealth < 0) {
            playerHealth = 0;
        }

        redTint = true;
        tintStart = System.currentTimeMillis();
        effectText = "Tint";

        playSound("assets/hit.wav");

        if (playerHealth <= 0) {
            triggerDeath();
        }
    }

    private void triggerDeath() {
        state = GameState.PLAYER_DIED;
        deathTimer = 120;
        bossProjectiles.clear();
        playerBullets.clear();
        effectText = "Grayscale";
    }

    private void triggerVictory() {
        state = GameState.VICTORY;
        playSound("assets/bossDefeat.wav");
        score += 1000;
        bossProjectiles.clear();
        playerBullets.clear();
        effectText = "None";
    }

    private void triggerLevel1Complete() {
        state = GameState.LEVEL1_COMPLETE;
        transitionTimer = 90;
        playSound("assets/levelup.wav");
        effectText = "Fade";
    }

    private boolean collidesWithRocksAt(int testX, int testY) {
        Rectangle[] boxes = player.getCollisionBoxesAt(testX, testY);

        for (Rectangle box : boxes) {
            for (SolidObject s : solids) {
                if (box.intersects(s.getBounds())) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean projectileHitsPlayer(Projectile projectile) {
        Rectangle projectileRect = projectile.getBounds();

        for (Rectangle box : player.getCollisionBoxes()) {
            if (box.intersects(projectileRect)) {
                return true;
            }
        }

        return false;
    }

    private void movePlayer(boolean bossArenaMode) {
        int speed = 5;

        int dx = 0;
        int dy = 0;

        if (up) {
            dy -= speed;
        }
        if (down) {
            dy += speed;
        }
        if (left) {
            dx -= speed;
        }
        if (right) {
            dx += speed;
        }

        int minX = 0;
        int minY = 0;
        int maxX = backgroundWidth - player.w;
        int maxY = backgroundHeight - player.h;

        if (bossArenaMode) {
            maxX = panelWidth - player.w;
            maxY = panelHeight - player.h;
            minY = 140;
        }

        boolean hitRock = false;

        if (dx != 0) {
            int candidateX = clamp(player.x + dx, minX, maxX);

            if (!collidesWithRocksAt(candidateX, player.y)) {
                player.x = candidateX;
            } else {
                hitRock = true;
            }
        }

        if (dy != 0) {
            int candidateY = clamp(player.y + dy, minY, maxY);

            if (!collidesWithRocksAt(player.x, candidateY)) {
                player.y = candidateY;
            } else {
                hitRock = true;
            }
        }

        if (hitRock) {
            if (rockHitCooldown <= 0) {
                applyDamage(5);
                rockHitCooldown = 25;
            } else {
                redTint = true;
                tintStart = System.currentTimeMillis();
                effectText = "Tint";
            }
        }

        player.updateHitboxes();
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private void updateLevel1() {
        if (rockHitCooldown > 0) {
            rockHitCooldown--;
        }

        movePlayer(false);

        if (up || down || left || right) {
            score++;
        } else {
            score++;
        }

        player.animate();

        boolean orbFadeActive = false;

        for (Orb orb : orbs) {
            if (!orb.collected && player.getBounds().intersects(orb.getBounds())) {
                orb.startFade();
                collected++;
                score += 100;
                playSound("assets/orb.wav");
            }

            orb.animate();

            if (!orb.isFullyGone() && orb.fading) {
                orbFadeActive = true;
            }
        }

        if (collected >= orbsToCollect) {
            triggerLevel1Complete();
        }

        if (redTint) {
            effectText = "Tint";
        } else if (orbFadeActive) {
            effectText = "Fade";
        } else {
            effectText = "None";
        }

        cameraX += ((player.x - panelWidth / 2.0) - cameraX) * 0.10;
        cameraY += ((player.y - panelHeight / 2.0) - cameraY) * 0.10;

        cameraX = Math.max(0, Math.min(cameraX, backgroundWidth - panelWidth));
        cameraY = Math.max(0, Math.min(cameraY, backgroundHeight - panelHeight));

        worldX = (int) Math.round(cameraX);
        worldY = (int) Math.round(cameraY);
    }

    private void updateBossLevel() {
        if (rockHitCooldown > 0) {
            rockHitCooldown--;
        }

        // Fixed arena camera so the boss stays at the top of the screen
        worldX = 0;
        worldY = 0;
        cameraX = 0;
        cameraY = 0;

        movePlayer(true);

        if (spaceHeld && playerShootCooldown <= 0) {
            shootPlayerBullet();
            playerShootCooldown = 8;
        }

        if (playerShootCooldown > 0) {
            playerShootCooldown--;
        }

        player.animate();

        if (boss != null) {
            boss.update(bossProjectiles, panelWidth);
        }

        updatePlayerBullets();
        updateBossProjectiles();

        if (boss != null && player.getBounds().intersects(boss.getBounds())) {
            applyDamage(20);
        }

        if (boss != null && boss.isDefeated()) {
            triggerVictory();
        }

        effectText = redTint ? "Tint" : "None";
    }

    private void shootPlayerBullet() {
        int bulletX = player.x + player.w / 2 - 4;
        int bulletY = player.y - 16;

        playerBullets.add(new Projectile(
                bulletX,
                bulletY,
                0,
                -10,
                10,
                18,
                10,
                true,
                new Color(0, 230, 255, 230)
        ));

        playSound("assets/playerShoot.wav");
    }

    private void updatePlayerBullets() {
        Iterator<Projectile> iterator = playerBullets.iterator();

        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update();

            if (projectile.isOffScreen(panelWidth, panelHeight) || projectileHitsSolid(projectile)) {
                iterator.remove();
                continue;
            }

            if (boss != null && projectile.getBounds().intersects(boss.getBounds())) {
                boss.takeDamage(projectile.getDamage());
                score += 50;
                playSound("assets/bossHit.wav");
                iterator.remove();
            }
        }
    }

    private void updateBossProjectiles() {
        Iterator<Projectile> iterator = bossProjectiles.iterator();

        while (iterator.hasNext()) {
            Projectile projectile = iterator.next();
            projectile.update();

            if (projectile.isOffScreen(panelWidth, panelHeight) || projectileHitsSolid(projectile)) {
                iterator.remove();
                continue;
            }

            if (projectileHitsPlayer(projectile)) {
                applyDamage(projectile.getDamage());
                iterator.remove();
            }
        }
    }

    private boolean projectileHitsSolid(Projectile projectile) {
        Rectangle projectileRect = projectile.getBounds();

        for (SolidObject solid : solids) {
            if (projectileRect.intersects(solid.getBounds())) {
                return true;
            }
        }

        return false;
    }

    private void gameUpdate() {
        if (startMessageTimer > 0) {
            startMessageTimer--;
        }
        if (redTint && System.currentTimeMillis() - tintStart > 2000) {
            redTint = false;
        }

        switch (state) {
            case LEVEL1:
                updateLevel1();
                break;

            case LEVEL1_COMPLETE:
                if (transitionTimer > 0) {
                    transitionTimer--;
                } else {
                    loadLevel2();
                }
                break;

            case LEVEL2:
                updateBossLevel();
                break;

            case PLAYER_DIED:
                if (deathTimer > 0) {
                    deathTimer--;
                } else {
                    loadLevel1(true);
                }
                break;

            case VICTORY:
                // Wait for R to restart
                break;
        }
    }

    private void drawHealthBar(Graphics2D g2, int x, int y, int width, int height, int current, int max, Color fill, String label) {
        g2.setColor(Color.BLACK);
        g2.fillRect(x - 1, y - 1, width + 2, height + 2);

        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(x, y, width, height);

        int fillWidth = (int) (width * (current / (double) max));
        if (fillWidth < 0) fillWidth = 0;
        if (fillWidth > width) fillWidth = width;

        g2.setColor(fill);
        g2.fillRect(x, y, fillWidth, height);

        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, width, height);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString(label, x, y - 4);
    }

    private void drawHUD(Graphics2D g2) {
        g2.setFont(new Font("Arial", Font.PLAIN, 18));
        g2.setColor(Color.WHITE);

        g2.drawString("Level: " + currentLevel, 10, 24);
        g2.drawString("FPS: " + fps, 10, 46);
        g2.drawString("Score: " + score, 10, 68);
        g2.drawString("Player X: " + player.x, 10, 90);
        g2.drawString("Player Y: " + player.y, 10, 112);
        g2.drawString("Effect: " + effectText, 10, 134);

        drawHealthBar(g2, 10, 160, 240, 18, playerHealth, 100, new Color(60, 220, 60), "Player Health");

        if (state == GameState.LEVEL1 || state == GameState.LEVEL1_COMPLETE) {
            g2.drawString("Orbs: " + collected + " / " + orbsToCollect, 10, 200);
        } else if (state == GameState.LEVEL2 || state == GameState.VICTORY) {
            if (boss != null) {
                drawHealthBar(g2, panelWidth / 2 - 180, 18, 360, 20, boss.getHealth(), boss.getMaxHealth(), new Color(220, 60, 60), "Boss Health");
            }
            g2.drawString("Fire with SPACE", 10, 200);
        }
    }

    private void gameRender() {
        Graphics2D g2 = backBuffer.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, panelWidth, panelHeight);

        if (background != null) {
            g2.drawImage(background, 0, 0, panelWidth, panelHeight,
                    worldX, worldY, worldX + panelWidth, worldY + panelHeight, null);
        }

        for (SolidObject solid : new ArrayList<>(solids) ) {
            solid.draw(g2, worldX, worldY);
        }

        for (Orb orb : new ArrayList<>(orbs)) {
            orb.draw(g2, worldX, worldY);
        }

        for (Projectile projectile : new ArrayList<>(playerBullets)) {
            projectile.draw(g2, worldX, worldY);
        }

        for (Projectile projectile :new ArrayList<>(bossProjectiles) ) {
            projectile.draw(g2, worldX, worldY);
        }

        if (boss != null && (state == GameState.LEVEL2 || state == GameState.VICTORY)) {
            boss.draw(g2, worldX, worldY);
        }

        player.draw(g2, worldX, worldY);

        drawHUD(g2);

        if (state == GameState.LEVEL1_COMPLETE) {
            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(panelWidth / 2 - 220, panelHeight / 2 - 70, 440, 140, 25, 25);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 34));
            g2.drawString("SHIP FULLY POWERED", panelWidth / 2 - 195, panelHeight / 2 - 10);

            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            g2.drawString("Entering Boss Battle...", panelWidth / 2 - 110, panelHeight / 2 + 28);
        }

        if (startMessageTimer > 0 && state == GameState.LEVEL1) {
            int alpha = Math.min(180, startMessageTimer * 2);

            // Background box
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRoundRect(panelWidth / 2 - 320, panelHeight / 2 - 40, 640, 80, 20, 20);

            // Text
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 22));

            int textWidth = g2.getFontMetrics().stringWidth(startMessage);
            g2.drawString(startMessage, (panelWidth - textWidth) / 2, panelHeight / 2 + 8);
        }

        if (redTint) {
            g2.setColor(new Color(255, 0, 0, 100));
            g2.fillRect(0, 0, panelWidth, panelHeight);
        }

        if (state == GameState.PLAYER_DIED) {
            applyGrayscale(backBuffer);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 42));
            String text = "YOU WERE DESTROYED";
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (panelWidth - textWidth) / 2, panelHeight / 2);

            g2.setFont(new Font("Arial", Font.PLAIN, 22));
            String sub = "Restarting Level 1...";
            int subWidth = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, (panelWidth - subWidth) / 2, panelHeight / 2 + 40);
        }

        if (state == GameState.VICTORY) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRoundRect(panelWidth / 2 - 220, panelHeight / 2 - 70, 440, 140, 25, 25);

            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 42));
            String text = "BOSS DEFEATED!";
            int textWidth = g2.getFontMetrics().stringWidth(text);
            g2.drawString(text, (panelWidth - textWidth) / 2, panelHeight / 2);

            g2.setFont(new Font("Arial", Font.PLAIN, 22));
            String sub = "Press R to Restart";
            int subWidth = g2.getFontMetrics().stringWidth(sub);
            g2.drawString(sub, (panelWidth - subWidth) / 2, panelHeight / 2 + 40);
        }

        g2.dispose();

        Graphics g = getGraphics();
        if (g != null) {
            g.drawImage(backBuffer, 0, 0, null);
            g.dispose();
        }
    }

    private void applyGrayscale(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixel = image.getRGB(x, y);

                int a = (pixel >> 24) & 255;
                int r = (pixel >> 16) & 255;
                int g = (pixel >> 8) & 255;
                int b = pixel & 255;

                int gray = (r + g + b) / 3;
                int newPixel = (a << 24) | (gray << 16) | (gray << 8) | gray;

                image.setRGB(x, y, newPixel);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backBuffer, 0, 0, null);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();

        if (k == KeyEvent.VK_W || k == KeyEvent.VK_UP) {
            up = true;
            facingDirection = 0;
            player.rotation = 0;
        }
        if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) {
            right = true;
            facingDirection = 1;
            player.rotation = Math.PI / 2.0;
        }
        if (k == KeyEvent.VK_S || k == KeyEvent.VK_DOWN) {
            down = true;
            facingDirection = 2;
            player.rotation = Math.PI;
        }
        if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT) {
            left = true;
            facingDirection = 3;
            player.rotation = -Math.PI / 2.0;
        }

        if (k == KeyEvent.VK_SPACE) {
            spaceHeld = true;
        }

        if (k == KeyEvent.VK_R) {
            loadLevel1(true);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();

        if (k == KeyEvent.VK_W || k == KeyEvent.VK_UP) {
            up = false;
        }
        if (k == KeyEvent.VK_D || k == KeyEvent.VK_RIGHT) {
            right = false;
        }
        if (k == KeyEvent.VK_S || k == KeyEvent.VK_DOWN) {
            down = false;
        }
        if (k == KeyEvent.VK_A || k == KeyEvent.VK_LEFT) {
            left = false;
        }

        if (k == KeyEvent.VK_SPACE) {
            spaceHeld = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}