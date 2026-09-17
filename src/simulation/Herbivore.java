package simulation;

import java.util.ArrayList;
import java.util.List;

public class Herbivore extends Agent {
    private static final int STEP_COST = 1;
    private static final int FOOD_GAIN = 8;
    private static final int REPRODUCE_THRESHOLD = 48;
    private static final int CHILD_ENERGY = 26;
    private static final int MAX_ENERGY = 55;

    public Herbivore(int x, int y) {
        this(x, y, 22);
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

        // РАЗМНОЖЕНИЕ: только наличие энергии и свободной соседней клетки
        if (energy >= REPRODUCE_THRESHOLD) {
            int[] freeCell = env.findEmptyNeighbor(x, y);
            if (freeCell != null) {
                this.energy -= CHILD_ENERGY;
                env.addAgent(new Herbivore(freeCell[0], freeCell[1], CHILD_ENERGY));
            }
        }
    }

    private void fleeFrom(Environment env, Agent threat) {
        int maxDistSq = -1;
        List<int[]> bestPositions = new ArrayList<>();

        for (int[] pos : env.getOrthogonalNeighbors(x, y)) {
            Agent occ = env.getAgent(pos[0], pos[1]);
            if (occ == null || occ instanceof Plant) {
                int distSq = (pos[0] - threat.getX()) * (pos[0] - threat.getX())
                        + (pos[1] - threat.getY()) * (pos[1] - threat.getY());

                if (distSq > maxDistSq) {
                    maxDistSq = distSq;
                    bestPositions.clear();
                    bestPositions.add(pos);
                } else if (distSq == maxDistSq) {
                    bestPositions.add(pos);
                }
            }
        }

        if (!bestPositions.isEmpty()) {
            int[] chosen = bestPositions.get(random.nextInt(bestPositions.size()));
            Agent target = env.getAgent(chosen[0], chosen[1]);
            if (target instanceof Plant) {
                eatPlant(env, target, chosen[0], chosen[1]);
            } else {
                moveTo(env, chosen[0], chosen[1]);
            }
        }
    }

    private void moveTowards(Environment env, int targetX, int targetY) {
        int dx = targetX - x;
        int dy = targetY - y;

        if (Math.abs(dx) + Math.abs(dy) == 1) {
            Agent target = env.getAgent(targetX, targetY);
            if (target instanceof Plant) {
                eatPlant(env, target, targetX, targetY);
                return;
            }
        }

        int stepX = Integer.compare(targetX, x);
        int stepY = Integer.compare(targetY, y);

        List<int[]> options = new ArrayList<>();
        if (stepX != 0) options.add(new int[]{x + stepX, y});
        if (stepY != 0) options.add(new int[]{x, y + stepY});

        for (int[] opt : options) {
            Agent a = env.getAgent(opt[0], opt[1]);
            if (a instanceof Plant) {
                eatPlant(env, a, opt[0], opt[1]);
                return;
            }
        }

        List<int[]> emptyOptions = new ArrayList<>();
        for (int[] opt : options) {
            if (env.isCellEmpty(opt[0], opt[1])) emptyOptions.add(opt);
        }

        if (!emptyOptions.isEmpty()) {
            int[] chosen = emptyOptions.get(random.nextInt(emptyOptions.size()));
            moveTo(env, chosen[0], chosen[1]);
        } else {
            moveRandomly(env);
        }
    }

    private void moveRandomly(Environment env) {
        List<int[]> validMoves = new ArrayList<>();
        for (int[] pos : env.getOrthogonalNeighbors(x, y)) {
            Agent a = env.getAgent(pos[0], pos[1]);
            if (a == null || a instanceof Plant) {
                validMoves.add(pos);
            }
        }

        if (!validMoves.isEmpty()) {
            int[] move = validMoves.get(random.nextInt(validMoves.size()));
            Agent target = env.getAgent(move[0], move[1]);
            if (target instanceof Plant) {
                eatPlant(env, target, move[0], move[1]);
            } else {
                moveTo(env, move[0], move[1]);
            }
        }
    }

    private void eatPlant(Environment env, Agent plant, int nx, int ny) {
        this.energy = Math.min(MAX_ENERGY, this.energy + FOOD_GAIN);
        env.removeAgent(plant);
        moveTo(env, nx, ny);
    }
}