package simulation;

import java.util.ArrayList;
import java.util.List;

public class Herbivore extends Agent {
    private static final int STEP_COST = 1;
    private static final int FOOD_GAIN = 20;
    private static final int REPRODUCE_THRESHOLD = 34;
    private static final int CHILD_ENERGY = 17;
    private static final int MAX_ENERGY = 55;

    public Herbivore(int x, int y) {
        this(x, y, 25);
    }

    public Herbivore(int x, int y, int energy) {
        super(x, y, energy);
    }

    @Override
    public char getSymbol() {
        return 'T';
    }

    @Override
    public void step(Environment env) {
        energy -= STEP_COST;
        if (energy <= 0) {
            env.removeAgent(this);
            return;
        }

        Agent threat = scanVision(env, Predator.class);
        if (threat != null) {
            fleeFrom(env, threat);
        } else {
            Agent food = (energy < MAX_ENERGY) ? scanVision(env, Plant.class) : null;
            if (food != null) {
                moveTowards(env, food.getX(), food.getY());
            } else if (random.nextDouble() < 0.85) {
                moveRandomly(env);
            }
        }

        if (energy > MAX_ENERGY) {
            energy = MAX_ENERGY;
        }

        // Размножение при отсутствии скученности
        if (energy >= REPRODUCE_THRESHOLD && countNearby(env, Herbivore.class, 2) < 4) {
            int[] freeCell = env.findSpawnCellForAnimal(x, y);
            if (freeCell != null) {
                this.energy -= CHILD_ENERGY;
                env.addAgent(new Herbivore(freeCell[0], freeCell[1], CHILD_ENERGY));
            }
        }
    }

    private void moveTowards(Environment env, int targetX, int targetY) {
        int dx = Integer.compare(targetX, x);
        int dy = Integer.compare(targetY, y);
        int nx = x + dx;
        int ny = y + dy;

        Agent target = env.getAgent(nx, ny);
        if (target instanceof Plant) {
            this.energy = Math.min(MAX_ENERGY, this.energy + FOOD_GAIN);
            env.removeAgent(target);
            moveTo(env, nx, ny);
        } else if (env.isCellEmpty(nx, ny)) {
            moveTo(env, nx, ny);
        } else {
            moveRandomly(env);
        }
    }

    // Скольжение вдоль стен: максимизация расстояния до угрозы среди доступных клеток
    private void fleeFrom(Environment env, Agent threat) {
        int bestX = -1, bestY = -1;
        int maxDistSq = -1;

        for (int[] pos : env.getNeighbors(x, y, 1)) {
            Agent occ = env.getAgent(pos[0], pos[1]);
            if (occ == null || occ instanceof Plant) {
                int distSq = (pos[0] - threat.getX()) * (pos[0] - threat.getX())
                        + (pos[1] - threat.getY()) * (pos[1] - threat.getY());
                if (distSq > maxDistSq) {
                    maxDistSq = distSq;
                    bestX = pos[0];
                    bestY = pos[1];
                }
            }
        }

        if (bestX != -1) {
            Agent target = env.getAgent(bestX, bestY);
            if (target instanceof Plant) {
                this.energy = Math.min(MAX_ENERGY, this.energy + FOOD_GAIN);
                env.removeAgent(target);
            }
            moveTo(env, bestX, bestY);
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
                this.energy = Math.min(MAX_ENERGY, this.energy + FOOD_GAIN);
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