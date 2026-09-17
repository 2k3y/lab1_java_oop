package simulation;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Симуляция: Искусственная жизнь (Жёсткие границы) ===");
        System.out.println("1 - Ручной пошаговый режим (ENTER)");
        System.out.println("2 - Тест на заданное число шагов (например, 1000)");
        System.out.println("3 - Бесконечный марафон (до вымирания или остановки)");
        System.out.println("4 - Визуальная анимация (100 мс/шаг)");
        System.out.print("Ваш выбор (1/2/3/4): ");

        String choice = scanner.nextLine().trim();

        Environment env = new Environment(60, 60);
        env.populate(700, 90, 8);

        switch (choice) {
            case "1":
                runManualMode(env, scanner);
                break;
            case "2":
                System.out.print("Введите количество шагов для теста: ");
                int steps = 1000;
                try {
                    steps = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Некорректное число, установлено значение по умолчанию: 1000");
                }
                runFixedStressTest(env, steps);
                break;
            case "3":
                runInfiniteMarathon(env);
                break;
            case "4":
                runAutoAnimation(env, 100);
                break;
            default:
                System.out.println("Неверный ввод, запущен бесконечный марафон.");
                runInfiniteMarathon(env);
        }

        scanner.close();
    }

    // Режим бесконечного прогона
    private static void runInfiniteMarathon(Environment env) {
        System.out.println("\n>>> Запущен БЕСКОНЕЧНЫЙ МАРАФОН.");
        System.out.println(">>> Чтобы корректно остановить симуляцию, нажмите [ENTER] в консоли...\n");

        java.util.concurrent.atomic.AtomicBoolean isRunning = new java.util.concurrent.atomic.AtomicBoolean(true);

        // Фоновый поток ожидает нажатия ENTER
        Thread stopListener = new Thread(() -> {
            try {
                System.in.read();
                isRunning.set(false);
            } catch (Exception ignored) {}
        });
        stopListener.setDaemon(true);
        stopListener.start();

        long startTime = System.currentTimeMillis();
        long step = 1;

        while (isRunning.get()) {
            env.update();

            // Промежуточная статистика каждые 500 ходов
            if (step % 500 == 0) {
                long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                System.out.printf("Шаг %7d | Время: %4d с | ", step, elapsedSeconds);
                env.printPopulationStats();
            }

            // Проверка экологического коллапса
            if (step % 50 == 0 && env.checkExtinction()) {
                long totalTime = System.currentTimeMillis() - startTime;
                System.out.printf("%nЭкосистема пала на шаге %d после %d мс работы.%n", step, totalTime);
                return;
            }

            step++;
        }

        long totalTime = (System.currentTimeMillis() - startTime) / 1000;
        System.out.println("\n=========================================");
        System.out.println(" ОСТАНОВКА ПОЛЬЗОВАТЕЛЕМ:");
        System.out.printf("Симуляция успешно остановлена на шаге %d (%d сек)%n", step, totalTime);
        System.out.print("Финальный баланс: ");
        env.printPopulationStats();
        System.out.println("=========================================");
    }

    private static void runFixedStressTest(Environment env, int totalSteps) {
        System.out.println("\nЗапуск теста на " + totalSteps + " шагов...");
        long startTime = System.currentTimeMillis();
        boolean survived = true;

        for (int step = 1; step <= totalSteps; step++) {
            env.update();

            if (step % 200 == 0 || step == totalSteps) {
                System.out.printf("Прогресс: шаг %6d / %d | ", step, totalSteps);
                env.printPopulationStats();
            }

            if (env.checkExtinction()) {
                System.out.println("Тест прерван на шаге " + step + " из-за вымирания.");
                survived = false;
                break;
            }
        }

        long timeTaken = System.currentTimeMillis() - startTime;
        if (survived) {
            System.out.println("\nУСПЕХ: Экосистема стабильно прожила все " + totalSteps + " шагов!");
            System.out.println("Время выполнения: " + timeTaken + " мс.");
        }
    }

    private static void runManualMode(Environment env, Scanner scanner) {
        int step = 0;
        while (true) {
            System.out.println("\n--- Шаг: " + step + " ---");
            env.display();

            if (step % 50 == 0 && env.checkExtinction()) {
                System.out.println("Симуляция остановлена на шаге " + step + ".");
                break;
            }

            System.out.print("[ENTER - шаг, q - выход]: ");
            String input = scanner.nextLine();
            if (input.trim().equalsIgnoreCase("q")) break;

            env.update();
            step++;
        }
    }

    private static void runAutoAnimation(Environment env, int delayMs) {
        int step = 0;
        // Работает до тех пор, пока система жива
        while (true) {
            System.out.println("\n--- Шаг: " + step + " ---");
            env.display();

            if (env.checkExtinction()) {
                System.out.println("Симуляция прервана на шаге " + step + ".");
                break;
            }

            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            env.update();
            step++;
        }
    }
}