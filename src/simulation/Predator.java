package simulation;

import java.util.ArrayList;
import java.util.List;

public class Predator extends Agent {
    private static final int STEP_COST = 1;
    private static final int MEAT_GAIN = 25;
    private static final int REPRODUCE_THRESHOLD = 58;
    private static final int CHILD_ENERGY = 30;
    private static final int MAX_ENERGY = 75;
    private static final double CATCH_PROBABILITY = 0.70;

    private int digestionCooldown = 0;

    public Predator(int x, int y) {
        this(x, y, 28);
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

        Agent prey = (digestionCooldown == 0) ? scanVision(env, Herbivore.class) : null;

        if (prey != null) {
            hunt(env, prey);
        } else if (random.nextDouble() < 0.80) {
            moveRandomly(env);
        }

        if (energy > MAX_ENERGY) {
            energy = MAX_ENERGY;
        }


        if (energy >= REPRODUCE_THRESHOLD) {
            int[] freeCell = env.findEmptyNeighbor(x, y);
            if (freeCell != null) {
                this.energy -= CHILD_ENERGY;
                env.addAgent(new Predator(freeCell[0], freeCell[1], CHILD_ENERGY));
            }
        }
    }

    private void hunt(Environment env, Agent prey) {
        int dx = prey.getX() - x;
        int dy = prey.getY() - y;

        if (Math.abs(dx) + Math.abs(dy) == 1) {
            if (random.nextDouble() < CATCH_PROBABILITY) {
                this.energy = Math.min(MAX_ENERGY, this.energy + MEAT_GAIN);
                this.digestionCooldown = 1;
                env.removeAgent(prey);
                moveTo(env, prey.getX(), prey.getY());
            }
            return;
        }

        int stepX = Integer.compare(prey.getX(), x);
        int stepY = Integer.compare(prey.getY(), y);

        List<int[]> options = new ArrayList<>();
        if (stepX != 0) options.add(new int[]{x + stepX, y});
        if (stepY != 0) options.add(new int[]{x, y + stepY});

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
            if (env.isCellEmpty(pos[0], pos[1])) validMoves.add(pos);
        }

        if (!validMoves.isEmpty()) {
            int[] move = validMoves.get(random.nextInt(validMoves.size()));
            moveTo(env, move[0], move[1]);
        }
    }
}