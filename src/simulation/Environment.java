package simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Environment {
    private final int width;
    private final int height;
    private final Agent[][] grid;
    private final List<Agent> agents = new ArrayList<>();
    private final Random random = new Random();

    public Environment(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Agent[height][width];
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    // Проверка попадания в жесткие границы поля
    public boolean isInside(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public boolean isCellEmpty(int x, int y) {
        return isInside(x, y) && grid[y][x] == null;
    }

    // Метод 1: получение агента
    public Agent getAgent(int x, int y) {
        if (!isInside(x, y)) return null;
        return grid[y][x];
    }

    // Метод 2: размещение / удаление агента
    public void setAgent(int x, int y, Agent agent) {
        if (isInside(x, y)) {
            grid[y][x] = agent;
            if (agent != null) {
                agent.setX(x);
                agent.setY(y);
            }
        }
    }

    // Метод 3: получение списка соседних клеток строго внутри границ
    public List<int[]> getNeighbors(int x, int y, int radius) {
        List<int[]> neighbors = new ArrayList<>();
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) continue;
                int nx = x + dx;
                int ny = y + dy;
                if (isInside(nx, ny)) {
                    neighbors.add(new int[]{nx, ny});
                }
            }
        }
        return neighbors;
    }

    public void addAgent(Agent agent) {
        agents.add(agent);
        setAgent(agent.getX(), agent.getY(), agent);
    }

    public void removeAgent(Agent agent) {
        agent.setAlive(false);
        if (isInside(agent.getX(), agent.getY()) && grid[agent.getY()][agent.getX()] == agent) {
            grid[agent.getY()][agent.getX()] = null;
        }
    }

    public void populate(int plants, int herbivores, int predators) {
        spawnBatch(plants, () -> new Plant(0, 0));
        spawnBatch(herbivores, () -> new Herbivore(0, 0));
        spawnBatch(predators, () -> new Predator(0, 0));
    }

    private interface AgentSupplier { Agent create(); }

    private void spawnBatch(int count, AgentSupplier supplier) {
        int placed = 0;
        while (placed < count) {
            int rx = random.nextInt(width);
            int ry = random.nextInt(height);
            if (grid[ry][rx] == null) {
                Agent a = supplier.create();
                a.setX(rx);
                a.setY(ry);
                addAgent(a);
                placed++;
            }
        }
    }

    public void update() {
        List<Agent> currentAgents = new ArrayList<>(agents);
        // Случайный порядок ходов исключает позиционный детерминизм
        Collections.shuffle(currentAgents, random);

        for (Agent a : currentAgents) {
            if (a.isAlive()) {
                a.step(this);
            }
        }
        agents.removeIf(a -> !a.isAlive());

        // Почвенный банк
        for (int i = 0; i < 5; i++) {
            int rx = random.nextInt(width);
            int ry = random.nextInt(height);
            if (isCellEmpty(rx, ry)) {
                addAgent(new Plant(rx, ry, 4));
            }
        }
    }

    // Для растений: строго пустая соседняя клетка
    public int[] findEmptyNeighbor(int x, int y) {
        List<int[]> emptyCells = new ArrayList<>();
        for (int[] pos : getNeighbors(x, y, 1)) {
            if (isCellEmpty(pos[0], pos[1])) {
                emptyCells.add(pos);
            }
        }
        if (!emptyCells.isEmpty()) {
            return emptyCells.get(random.nextInt(emptyCells.size()));
        }
        return null;
    }

    // Для животных: свободная клетка либо занятая травой
    public int[] findSpawnCellForAnimal(int x, int y) {
        List<int[]> cells = new ArrayList<>();
        for (int[] pos : getNeighbors(x, y, 1)) {
            Agent a = getAgent(pos[0], pos[1]);
            if (a == null || a instanceof Plant) {
                cells.add(pos);
            }
        }
        if (!cells.isEmpty()) {
            int[] chosen = cells.get(random.nextInt(cells.size()));
            Agent occupant = getAgent(chosen[0], chosen[1]);
            if (occupant instanceof Plant) {
                removeAgent(occupant);
            }
            return chosen;
        }
        return null;
    }

    public boolean checkExtinction() {
        int p = 0, h = 0, pr = 0;
        for (Agent a : agents) {
            if (a.isAlive()) {
                if (a instanceof Plant) p++;
                else if (a instanceof Herbivore) h++;
                else if (a instanceof Predator) pr++;
            }
        }

        if (p == 0 || h == 0 || pr == 0) {
            System.out.println("\n=========================================");
            System.out.println("⚠️  ЭКОЛОГИЧЕСКИЙ КОЛЛАПС: ВЫМИРАНИЕ ВИДА!");
            if (p == 0)  System.out.println("-> Рм астения полностью исчезли.");
            if (h == 0)  System.out.println("-> Травоядные полностью погибли.");
            if (pr == 0) System.out.println("-> Хищники вымерли.");
            System.out.printf("Финальный баланс: Растения: %d | Травоядные: %d | Хищники: %d%n", p, h, pr);
            System.out.println("=========================================");
            return true;
        }
        return false;
    }

    public void printPopulationStats() {
        int p = 0, h = 0, pr = 0;
        for (Agent a : agents) {
            if (a.isAlive()) {
                if (a instanceof Plant) p++;
                else if (a instanceof Herbivore) h++;
                else if (a instanceof Predator) pr++;
            }
        }
        System.out.printf("Растения: %4d | Травоядные: %4d | Хищники: %4d%n", p, h, pr);
    }

    public void display() {
        int plantCount = 0, herbCount = 0, predCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (grid[y][x] == null) {
                    System.out.print(" . ");
                } else {
                    char s = grid[y][x].getSymbol();
                    System.out.print("[" + s + "]");
                    if (s == 'P') plantCount++;
                    else if (s == 'T') herbCount++;
                    else if (s == 'X') predCount++;
                }
            }
            System.out.println();
        }
        System.out.printf("Популяция: Растения: %d | Травоядные: %d | Хищники: %d%n",
                plantCount, herbCount, predCount);
    }
}