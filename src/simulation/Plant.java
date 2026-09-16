package simulation;

public class Plant extends Agent {
    private static final int ENERGY_GAIN = 1;
    private static final int REPRODUCE_THRESHOLD = 11;
    private static final int CHILD_ENERGY = 4;
    private static final int MAX_ENERGY = 16;

    public Plant(int x, int y) {
        this(x, y, 4);
    }

    public Plant(int x, int y, int energy) {
        super(x, y, energy);
    }

    @Override
    public char getSymbol() {
        return 'P';
    }

    @Override
    public void step(Environment env) {
        energy += ENERGY_GAIN;

        if (energy >= REPRODUCE_THRESHOLD) {
            // Ограничение скученности: если рядом уже 4+ растений, побег не дается
            if (countNeighborPlants(env) < 4) {
                int[] freeCell = env.findEmptyNeighbor(x, y);
                if (freeCell != null) {
                    this.energy -= CHILD_ENERGY;
                    env.addAgent(new Plant(freeCell[0], freeCell[1], CHILD_ENERGY));
                } else {
                    energy = REPRODUCE_THRESHOLD - 2;
                }
            } else {
                energy = REPRODUCE_THRESHOLD - 3;
            }
        }

        if (energy > MAX_ENERGY) {
            energy = MAX_ENERGY;
        }
    }

    private int countNeighborPlants(Environment env) {
        int count = 0;
        for (int[] pos : env.getNeighbors(x, y, 1)) {
            Agent a = env.getAgent(pos[0], pos[1]);
            if (a instanceof Plant && a.isAlive()) {
                count++;
            }
        }
        return count;
    }
}