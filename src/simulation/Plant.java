package simulation;

public class Plant extends Agent {
    private static final int ENERGY_GAIN = 2;
    private static final int REPRODUCE_THRESHOLD = 6;
    private static final int CHILD_ENERGY = 2;
    private static final int MAX_ENERGY = 10;

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
            // Не дает зарастать полю сплошной непроходимой стеной
            if (countNeighborPlants(env) < 4) {
                int[] freeCell = env.findEmptyNeighbor(x, y);
                if (freeCell != null) {
                    this.energy -= CHILD_ENERGY;
                    env.addAgent(new Plant(freeCell[0], freeCell[1], CHILD_ENERGY));
                }
            } else {
                energy = REPRODUCE_THRESHOLD - 2;
            }
        }

        if (energy > MAX_ENERGY) {
            energy = MAX_ENERGY;
        }
    }

    private int countNeighborPlants(Environment env) {
        int count = 0;
        int minX = Math.max(0, x - 1);
        int maxX = Math.min(env.getWidth() - 1, x + 1);
        int minY = Math.max(0, y - 1);
        int maxY = Math.min(env.getHeight() - 1, y + 1);

        for (int ny = minY; ny <= maxY; ny++) {
            for (int nx = minX; nx <= maxX; nx++) {
                if (nx == x && ny == y) continue;
                Agent a = env.getAgent(nx, ny);
                if (a instanceof Plant && a.isAlive()) count++;
            }
        }
        return count;
    }
}