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

        for (int[] pos : env.getNeighbors(x, y, 2)) {
            Agent neighbor = env.getAgent(pos[0], pos[1]);
            if (neighbor != null && targetType.isInstance(neighbor) && neighbor.isAlive()) {
                int dist = Math.abs(pos[0] - x) + Math.abs(pos[1] - y);
                if (dist < minDistance) {
                    minDistance = dist;
                    closest = neighbor;
                }
            }
        }
        return closest;
    }
}