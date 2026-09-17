package simulation;

import java.util.Random;

public abstract class Agent {
    protected int x;
    protected int y;
    protected int energy;
    protected boolean alive = true;
    protected static final Random random = new Random();

    public Agent(int x, int y, int energy) {
        this.x = x;
        this.y = y;
        this.energy = energy;
    }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getEnergy() { return energy; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public abstract char getSymbol();
    public abstract void step(Environment env);

    protected void moveTo(Environment env, int newX, int newY) {
        env.setAgent(this.x, this.y, null);
        this.x = newX;
        this.y = newY;
        env.setAgent(newX, newY, this);
    }

    // Зрение: безопасное сканирование зоны 5х5 через getNeighbors среды
    protected Agent scanVision(Environment env, Class<? extends Agent> targetType) {
        Agent closest = null;
        int minDistance = Integer.MAX_VALUE;

        int minX = Math.max(0, x - 2);
        int maxX = Math.min(env.getWidth() - 1, x + 2);
        int minY = Math.max(0, y - 2);
        int maxY = Math.min(env.getHeight() - 1, y + 2);

        for (int ny = minY; ny <= maxY; ny++) {
            for (int nx = minX; nx <= maxX; nx++) {
                if (nx == x && ny == y) continue;
                Agent neighbor = env.getAgent(nx, ny);
                if (neighbor != null && targetType.isInstance(neighbor) && neighbor.isAlive()) {
                    int dist = Math.abs(nx - x) + Math.abs(ny - y);
                    if (dist < minDistance) {
                        minDistance = dist;
                        closest = neighbor;
                    }
                }
            }
        }
        return closest;
    }
}