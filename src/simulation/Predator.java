package simulation;

import java.util.ArrayList;
import java.util.List;

public class Predator extends Agent {
    private static final int STEP_COST = 1;
    private static final int MEAT_GAIN = 26;
    private static final int REPRODUCE_THRESHOLD = 78; // порог разможения
    private static final int CHILD_ENERGY = 35;
    private static final int MAX_ENERGY = 90;
    private static final double CATCH_PROBABILITY = 0.30; // шанс успешного захвата

    private int digestionCooldown = 0;

    public Predator(int x, int y) {
        this(x, y, 35);
    }

    public Predator(int x, int y, int energy) {
        super(x, y, energy);
    }

    @Override
    public char getSymbol() {
        return 'X';
    }

    @Override
    public void step(Environment env) {
        energy -= STEP_COST;
        if (energy <= 0) {
            env.removeAgent(this);
            return;
        }

        if (digestionCooldown > 0) {
            digestionCooldown--;
        }

        Agent prey = null;
        if (digestionCooldown == 0) {
            prey = scanVision(env, Herbivore.class);
        }

        if (prey != null) {
            hunt(env, prey);
        } else if (random.nextDouble() < 0.80) {
            moveRandomly(env);
        }

        if (energy > MAX_ENERGY) {
            energy = MAX_ENERGY;
        }

        // Деление: только в одиночестве (нет волков в радиусе 3 клеток)
        if (energy >= REPRODUCE_THRESHOLD && countNearby(env, Predator.class, 3) == 0) {
            int[] freeCell = env.findSpawnCellForAnimal(x, y);
            if (freeCell != null) {
                this.energy -= CHILD_ENERGY;
                env.addAgent(new Predator(freeCell[0], freeCell[1], CHILD_ENERGY));
            }
        }
    }

    private void hunt(Environment env, Agent prey) {
        int dx = Integer.compare(prey.getX(), x);
        int dy = Integer.compare(prey.getY(), y);
        int nx = x + dx;
        int ny = y + dy;

        Agent target = env.getAgent(nx, ny);
        if (target instanceof Herbivore) {
            if (random.nextDouble() < CATCH_PROBABILITY) { // Обязательно!
                this.energy = Math.min(MAX_ENERGY, this.energy + MEAT_GAIN);
                this.digestionCooldown = 1;
                env.removeAgent(target);
                moveTo(env, nx, ny);
            }
        } else if (env.isCellEmpty(nx, ny)) {
            moveTo(env, nx, ny);
        } else if (target instanceof Plant) {
            env.removeAgent(target);
            moveTo(env, nx, ny);
        } else {
            moveRandomly(env);
        }
    }

    private void moveRandomly(Environment env) {
        List<int[]> validMoves = new ArrayList<>();
        for (int[] pos : env.getNeighbors(x, y, 1)) {
            Agent a = env.getAgent(pos[0], pos[1]);
            if (a == null || a instanceof Plant) {
                validMoves.add(pos);
            }
        }
        if (!validMoves.isEmpty()) {
            int[] move = validMoves.get(random.nextInt(validMoves.size()));
            Agent target = env.getAgent(move[0], move[1]);
            if (target instanceof Plant) {
                env.removeAgent(target);
            }
            moveTo(env, move[0], move[1]);
        }
    }

    private int countNearby(Environment env, Class<? extends Agent> type, int radius) {
        int count = 0;
        int minX = Math.max(0, x - radius);
        int maxX = Math.min(env.getWidth() - 1, x + radius);
        int minY = Math.max(0, y - radius);
        int maxY = Math.min(env.getHeight() - 1, y + radius);

        for (int ny = minY; ny <= maxY; ny++) {
            for (int nx = minX; nx <= maxX; nx++) {
                if (nx == x && ny == y) continue;
                Agent a = env.getAgent(nx, ny);
                if (a != null && type.isInstance(a) && a.isAlive()) {
                    count++;
                }
            }
        }
        return count;
    }
}