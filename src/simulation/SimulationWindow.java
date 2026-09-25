package simulation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class SimulationWindow extends JFrame {
    // Начальные параметры поля и популяций
    private int fieldWidth = 60;
    private int fieldHeight = 60;
    private int initPlants = 600;
    private int initHerbivores = 60;
    private int initPredators = 14;

    // Размер клетки под эмодзи
    private int cellSize = 18;
    private int stepCount = 0;
    private boolean isRunning = false;

    // Среда и таймер
    private Environment env;
    private Timer timer;

    // История состояний (буфер 3 000 шагов в битовой упаковке)
    private static final int MAX_HISTORY = 3000;
    private final ArrayDeque<WorldSnapshot> history = new ArrayDeque<>();
    private final ArrayDeque<WorldSnapshot> future = new ArrayDeque<>();

    // Компоненты UI
    private final SimulationPanel canvas;
    private final JButton playPauseBtn;
    private final JButton stepBtn;
    private final JButton stepBackBtn;
    private final JButton jumpToPresentBtn;
    private final JLabel stepCounterLabel;
    private final JLabel plantCountLabel;
    private final JLabel herbivoreCountLabel;
    private final JLabel predatorCountLabel;
    private final JLabel statusLabel;
    private final JLabel inspectorLabel;

    // Спиннеры настроек
    private JSpinner widthSpinner;
    private JSpinner heightSpinner;
    private JSpinner plantsSpinner;
    private JSpinner herbsSpinner;
    private JSpinner predsSpinner;

    public SimulationWindow() {
        super("Экосистема: Панель управления");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 1. Создаем начальный мир
        resetEnvironment(fieldWidth, fieldHeight, initPlants, initHerbivores, initPredators);

        // 2. Инициализируем таймер со стандартной задержкой 80 мс
        timer = new Timer(80, e -> performSimulationSteps(1));

        // 3. Центральное поле со скроллом
        canvas = new SimulationPanel();
        JScrollPane scrollPane = new JScrollPane(canvas);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 4. Панель управления (справа)
        JPanel sidePanel = new JPanel();
        sidePanel.setLayout(new BoxLayout(sidePanel, BoxLayout.Y_AXIS));
        sidePanel.setPreferredSize(new Dimension(360, 760));
        sidePanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        sidePanel.setBackground(new Color(245, 245, 247));

        // --- Блок 1: Счетчики ---
        JPanel statsPanel = createSectionPanel("Статистика экосистемы");
        statsPanel.setLayout(new GridLayout(5, 1, 4, 4));

        stepCounterLabel = new JLabel("⏱ Шаг: 0");
        stepCounterLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        plantCountLabel = createBadgeLabel("🌿 Растения: 0", new Color(39, 174, 96));
        herbivoreCountLabel = createBadgeLabel("🐰 Травоядные: 0", new Color(41, 128, 185));
        predatorCountLabel = createBadgeLabel("🐺 Хищники: 0", new Color(192, 57, 43));

        statusLabel = new JLabel("Статус: На паузе");
        statusLabel.setForeground(Color.DARK_GRAY);

        statsPanel.add(stepCounterLabel);
        statsPanel.add(plantCountLabel);
        statsPanel.add(herbivoreCountLabel);
        statsPanel.add(predatorCountLabel);
        statsPanel.add(statusLabel);
        sidePanel.add(statsPanel);
        sidePanel.add(Box.createVerticalStrut(10));

        // --- Блок 2: Управление ходом ---
        JPanel controlPanel = createSectionPanel("Управление ходом");
        controlPanel.setLayout(new GridLayout(4, 1, 6, 6));

        playPauseBtn = new JButton("▶️ Запуск (Пробел)");
        playPauseBtn.setBackground(new Color(46, 204, 113));
        playPauseBtn.addActionListener(e -> toggleSimulation());

        stepBtn = new JButton("⏭️ Сделать шаг (Вправо)");
        stepBtn.addActionListener(e -> performSimulationSteps(1));

        stepBackBtn = new JButton("⏮️ Шаг назад (Влево)");
        stepBackBtn.setEnabled(false);
        stepBackBtn.addActionListener(e -> stepBack());

        jumpToPresentBtn = new JButton("⏩ В настоящее");
        jumpToPresentBtn.setEnabled(false);
        jumpToPresentBtn.addActionListener(e -> jumpToPresent());

        controlPanel.add(playPauseBtn);
        controlPanel.add(stepBtn);
        controlPanel.add(stepBackBtn);
        controlPanel.add(jumpToPresentBtn);
        sidePanel.add(controlPanel);
        sidePanel.add(Box.createVerticalStrut(10));

        // --- Блок 3: Масштаб клеток (Зум) ---
        JPanel viewPanel = createSectionPanel("Масштаб клеток (Зум)");
        viewPanel.setLayout(new BorderLayout(5, 5));
        JLabel zoomLabel = new JLabel("Размер клетки: " + cellSize + " px");
        JSlider zoomSlider = new JSlider(8, 36, cellSize);
        zoomSlider.addChangeListener(e -> {
            cellSize = zoomSlider.getValue();
            zoomLabel.setText("Размер клетки: " + cellSize + " px");
            canvas.updatePreferredSize();
            canvas.revalidate();
            canvas.repaint();
        });
        viewPanel.add(zoomLabel, BorderLayout.NORTH);
        viewPanel.add(zoomSlider, BorderLayout.CENTER);
        sidePanel.add(viewPanel);
        sidePanel.add(Box.createVerticalStrut(10));

        // --- Блок 4: Настройка параметров ---
        JPanel setupPanel = createSectionPanel("Параметры нового мира");
        setupPanel.setLayout(new GridLayout(6, 2, 6, 6));

        widthSpinner = new JSpinner(new SpinnerNumberModel(fieldWidth, 15, 200, 5));
        heightSpinner = new JSpinner(new SpinnerNumberModel(fieldHeight, 15, 200, 5));
        plantsSpinner = new JSpinner(new SpinnerNumberModel(initPlants, 0, 10000, 50));
        herbsSpinner = new JSpinner(new SpinnerNumberModel(initHerbivores, 0, 5000, 10));
        predsSpinner = new JSpinner(new SpinnerNumberModel(initPredators, 0, 2000, 5));

        setupPanel.add(new JLabel("Ширина сетки:"));
        setupPanel.add(widthSpinner);
        setupPanel.add(new JLabel("Высота сетки:"));
        setupPanel.add(heightSpinner);
        setupPanel.add(new JLabel("Растения:"));
        setupPanel.add(plantsSpinner);
        setupPanel.add(new JLabel("Травоядные:"));
        setupPanel.add(herbsSpinner);
        setupPanel.add(new JLabel("Хищники:"));
        setupPanel.add(predsSpinner);

        JButton applyBtn = new JButton("Перезапустить");
        applyBtn.addActionListener(e -> applyNewSettingsAndRestart());
        setupPanel.add(new JLabel(""));
        setupPanel.add(applyBtn);
        sidePanel.add(setupPanel);

        inspectorLabel = new JLabel("Кликните на клетку для осмотра");
        inspectorLabel.setFont(new Font("SansSerif", Font.ITALIC, 11));
        inspectorLabel.setForeground(Color.GRAY);
        sidePanel.add(Box.createVerticalStrut(10));
        sidePanel.add(inspectorLabel);

        add(sidePanel, BorderLayout.EAST);

        // 5. Горячие клавиши
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (!(e.getSource() instanceof JTextField)) {
                    if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                        toggleSimulation();
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_RIGHT || e.getKeyCode() == KeyEvent.VK_N) {
                        if (!isRunning) performSimulationSteps(1);
                        return true;
                    } else if (e.getKeyCode() == KeyEvent.VK_LEFT || e.getKeyCode() == KeyEvent.VK_B) {
                        if (!isRunning) stepBack();
                        return true;
                    }
                }
            }
            return false;
        });

        updateStatisticsView();
        pack();
        setSize(1180, 820);
        setLocationRelativeTo(null);
    }

    private JPanel createSectionPanel(String title) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(210, 210, 215), 1),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("SansSerif", Font.BOLD, 12),
                        new Color(60, 60, 67)
                ),
                new EmptyBorder(8, 8, 8, 8)
        ));
        return p;
    }

    private JLabel createBadgeLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 13));
        label.setForeground(color);
        return label;
    }

    private void toggleSimulation() {
        isRunning = !isRunning;
        if (isRunning) {
            timer.start();
            playPauseBtn.setText("⏸️ Пауза (Пробел)");
            stepBtn.setEnabled(false);
            stepBackBtn.setEnabled(false);
            jumpToPresentBtn.setEnabled(false);
        } else {
            timer.stop();
            playPauseBtn.setText("▶️ Запуск (Пробел)");
            stepBtn.setEnabled(true);
            updateHistoryButtonsState();
        }
        updateStatusLabel();
    }

    private void performSimulationSteps(int count) {
        for (int i = 0; i < count; i++) {
            if (!future.isEmpty()) {
                pushToHistory(createCurrentSnapshot());
                WorldSnapshot next = future.pop();
                applySnapshot(next);
            } else {
                pushToHistory(createCurrentSnapshot());
                env.update();
                stepCount++;
            }

            if (env.checkExtinction()) {
                if (isRunning) toggleSimulation();
                String reason = getExtinctionReason();
                statusLabel.setText("Статус: Коллапс (" + reason + ")!");
                statusLabel.setForeground(new Color(231, 76, 60));
                break;
            }
        }

        updateStatusLabel();
        updateStatisticsView();
        updateHistoryButtonsState();
        canvas.repaint();
    }

    private void stepBack() {
        if (history.isEmpty()) return;

        future.push(createCurrentSnapshot());
        WorldSnapshot prev = history.pop();
        applySnapshot(prev);

        updateStatusLabel();
        updateHistoryButtonsState();
        updateStatisticsView();
        canvas.repaint();
    }

    private void jumpToPresent() {
        if (future.isEmpty()) return;

        while (!future.isEmpty()) {
            pushToHistory(createCurrentSnapshot());
            WorldSnapshot next = future.pop();
            applySnapshot(next);
        }

        updateStatusLabel();
        updateHistoryButtonsState();
        updateStatisticsView();
        canvas.repaint();
    }

    private void updateStatusLabel() {
        if (env.checkExtinction()) return;

        if (!future.isEmpty()) {
            int maxStep = stepCount + future.size();
            statusLabel.setText("История: " + stepCount + " / " + maxStep);
            statusLabel.setForeground(new Color(142, 68, 173));
        } else {
            if (isRunning) {
                statusLabel.setText("Статус: Запущено");
                statusLabel.setForeground(new Color(39, 174, 96));
            } else {
                statusLabel.setText("Статус: На паузе");
                statusLabel.setForeground(Color.DARK_GRAY);
            }
        }
    }

    private void updateHistoryButtonsState() {
        if (stepBackBtn != null) {
            stepBackBtn.setEnabled(!isRunning && !history.isEmpty());
        }
        if (jumpToPresentBtn != null) {
            jumpToPresentBtn.setEnabled(!isRunning && !future.isEmpty());
            if (!future.isEmpty()) {
                int maxStep = stepCount + future.size();
                jumpToPresentBtn.setText("⏩ В настоящее (" + maxStep + ")");
            } else {
                jumpToPresentBtn.setText("⏩ В настоящее");
            }
        }
        if (stepBtn != null) {
            if (!future.isEmpty()) {
                stepBtn.setText("⏭️ Вперёд (по записи)");
            } else {
                stepBtn.setText("⏭️ Сделать шаг (Вправо)");
            }
        }
    }

    private void pushToHistory(WorldSnapshot snapshot) {
        if (history.size() >= MAX_HISTORY) {
            history.removeLast();
        }
        history.push(snapshot);
    }

    private WorldSnapshot createCurrentSnapshot() {
        int count = 0;
        for (int y = 0; y < env.getHeight(); y++) {
            for (int x = 0; x < env.getWidth(); x++) {
                Agent a = env.getAgent(x, y);
                if (a != null && a.isAlive()) count++;
            }
        }

        int[] packed = new int[count];
        int idx = 0;

        for (int y = 0; y < env.getHeight(); y++) {
            for (int x = 0; x < env.getWidth(); x++) {
                Agent a = env.getAgent(x, y);
                if (a != null && a.isAlive()) {
                    int typeId = (a instanceof Plant) ? 1 : (a instanceof Herbivore) ? 2 : 3;
                    int energy = Math.min(255, Math.max(0, a.getEnergy()));

                    packed[idx++] = ((a.getX() & 0xFF) << 24)
                            | ((a.getY() & 0xFF) << 16)
                            | ((typeId & 0xFF) << 8)
                            | (energy & 0xFF);
                }
            }
        }
        return new WorldSnapshot(stepCount, packed);
    }

    private void applySnapshot(WorldSnapshot snapshot) {
        this.stepCount = snapshot.step;
        this.env = new Environment(fieldWidth, fieldHeight);

        for (int val : snapshot.packedAgents) {
            int x = (val >>> 24) & 0xFF;
            int y = (val >>> 16) & 0xFF;
            int typeId = (val >>> 8) & 0xFF;
            int energy = val & 0xFF;

            Agent a = null;
            if (typeId == 1) a = new Plant(x, y, energy);
            else if (typeId == 2) a = new Herbivore(x, y, energy);
            else if (typeId == 3) a = new Predator(x, y, energy);

            if (a != null) {
                env.addAgent(a);
            }
        }
    }

    private String getExtinctionReason() {
        int p = 0, h = 0, pr = 0;
        for (int y = 0; y < env.getHeight(); y++) {
            for (int x = 0; x < env.getWidth(); x++) {
                Agent a = env.getAgent(x, y);
                if (a != null && a.isAlive()) {
                    if (a instanceof Plant) p++;
                    else if (a instanceof Herbivore) h++;
                    else if (a instanceof Predator) pr++;
                }
            }
        }

        List<String> reasons = new ArrayList<>();
        if (p == 0) reasons.add("растения исчезли");
        if (h == 0) reasons.add("травоядные погибли");
        if (pr == 0) reasons.add("хищники вымерли");

        return reasons.isEmpty() ? "неизвестно" : String.join(", ", reasons);
    }

    private void resetEnvironment(int w, int h, int p, int hb, int pr) {
        this.fieldWidth = w;
        this.fieldHeight = h;
        this.initPlants = p;
        this.initHerbivores = hb;
        this.initPredators = pr;

        this.env = new Environment(fieldWidth, fieldHeight);
        this.env.populate(initPlants, initHerbivores, initPredators);
        this.stepCount = 0;
        this.history.clear();
        this.future.clear();
        updateHistoryButtonsState();
    }

    private void applyNewSettingsAndRestart() {
        if (isRunning) {
            toggleSimulation();
        }

        int w = (int) widthSpinner.getValue();
        int h = (int) heightSpinner.getValue();
        int p = (int) plantsSpinner.getValue();
        int hb = (int) herbsSpinner.getValue();
        int pr = (int) predsSpinner.getValue();

        if (p + hb + pr > w * h) {
            JOptionPane.showMessageDialog(this,
                    "Число организмов (" + (p + hb + pr) + ") превышает вместимость сетки (" + (w * h) + ")!",
                    "Ошибка параметров",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        resetEnvironment(w, h, p, hb, pr);
        canvas.updatePreferredSize();
        canvas.revalidate();
        canvas.repaint();

        statusLabel.setText("Статус: Перезапущено");
        statusLabel.setForeground(Color.DARK_GRAY);
        inspectorLabel.setText("Кликните на клетку для осмотра");
        updateStatisticsView();
    }

    private void updateStatisticsView() {
        int plants = 0, herbs = 0, preds = 0;

        for (int y = 0; y < env.getHeight(); y++) {
            for (int x = 0; x < env.getWidth(); x++) {
                Agent a = env.getAgent(x, y);
                if (a != null && a.isAlive()) {
                    if (a instanceof Plant) plants++;
                    else if (a instanceof Herbivore) herbs++;
                    else if (a instanceof Predator) preds++;
                }
            }
        }

        stepCounterLabel.setText("⏱ Шаг: " + stepCount);
        plantCountLabel.setText("🌿 Растения: " + plants);
        herbivoreCountLabel.setText("🐰 Травоядные: " + herbs);
        predatorCountLabel.setText("🐺 Хищники: " + preds);
    }

    // Панель отрисовки: цветные плитки + центрированные эмодзи
    private class SimulationPanel extends JPanel {
        public SimulationPanel() {
            setBackground(new Color(24, 24, 28));
            updatePreferredSize();

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int cellX = e.getX() / cellSize;
                    int cellY = e.getY() / cellSize;

                    if (env.isInside(cellX, cellY)) {
                        Agent a = env.getAgent(cellX, cellY);
                        if (a == null) {
                            inspectorLabel.setText(String.format("[%d, %d]: Пустая клетка", cellX, cellY));
                        } else {
                            String type = (a instanceof Plant) ? "🌿 Растение" :
                                    (a instanceof Herbivore) ? "🐰 Травоядное" : "🐺 Хищник";
                            inspectorLabel.setText(String.format("[%d, %d] %s (Энергия: %d)",
                                    cellX, cellY, type, a.getEnergy()));
                        }
                    }
                }
            });
        }

        public void updatePreferredSize() {
            setPreferredSize(new Dimension(env.getWidth() * cellSize, env.getHeight() * cellSize));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;

            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int fontSize = Math.max(9, (int) (cellSize * 0.72));
            Font emojiFont = new Font("Apple Color Emoji", Font.PLAIN, fontSize);
            g2d.setFont(emojiFont);
            FontMetrics fm = g2d.getFontMetrics();

            for (int y = 0; y < env.getHeight(); y++) {
                for (int x = 0; x < env.getWidth(); x++) {
                    Agent agent = env.getAgent(x, y);
                    if (agent != null && agent.isAlive()) {
                        String emoji;
                        Color tileColor;

                        if (agent instanceof Plant) {
                            tileColor = new Color(39, 174, 96, 170);
                            emoji = "🌿";
                        } else if (agent instanceof Herbivore) {
                            tileColor = new Color(41, 128, 185, 210);
                            emoji = "🐰";
                        } else {
                            tileColor = new Color(231, 76, 60, 230);
                            emoji = "🐺";
                        }

                        int px = x * cellSize;
                        int py = y * cellSize;
                        int size = Math.max(2, cellSize - 2);

                        g2d.setColor(tileColor);
                        g2d.fillRoundRect(px + 1, py + 1, size, size, Math.max(2, size / 3), Math.max(2, size / 3));

                        if (cellSize >= 11) {
                            int textW = fm.stringWidth(emoji);
                            int textX = px + (cellSize - textW) / 2;
                            int textY = py + (cellSize + fm.getAscent() - fm.getDescent()) / 2;
                            g2d.drawString(emoji, textX, textY);
                        }
                    }
                }
            }
        }
    }

    private static class WorldSnapshot {
        final int step;
        final int[] packedAgents;

        WorldSnapshot(int step, int[] packedAgents) {
            this.step = step;
            this.packedAgents = packedAgents;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SimulationWindow window = new SimulationWindow();
            window.setVisible(true);
        });
    }
}