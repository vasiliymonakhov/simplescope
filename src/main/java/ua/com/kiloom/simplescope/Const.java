package ua.com.kiloom.simplescope;

/**
 *
 * @author Vasily Monakhov
 */
class Const {

    /**
     * Время развёртки, сек
     */
    final static double[] TIMES = {0.00001d, 0.00002d, 0.00005d, 0.0001d,
        0.0002d, 0.0005d, 0.001d, 0.002d, 0.005d, 0.01d, 0.02d, 0.05d, 0.1d, 0.2d, 0.5d,
        1d, 2d, 5d};

    /**
     * Время на клетку
     */
    final static int[] TIME_PER_CELL = {1, 2, 5, 10, 20, 50, 100, 200, 500, 1, 2, 5, 10, 20, 50, 100, 200, 500};

    /**
     * Строки с времемем развёртки
     */
    final static String[] TIME_STRINGS = {"µS", "µS", "µS", "µS", "µS",
        "µS", "µS", "µS", "µS", "mS", "mS", "mS", "mS", "mS", "mS", "mS", "mS", "mS"};

    /**
     * Пределы вольт
     */
    final static double[] VOLTAGES = {0.05d, 0.1d, 0.25d, 0.5d, 1d, 2.5d, 5d, 10d, 25d, 50d, 100d};

    /**
     * Напряжение на клетку
     */
    final static int[] VOLTAGE_PER_CELL = {
        10, 20, 50, 100, 200, 500, 1, 2, 5, 10, 20};

    /**
     * Строки с обозначением величины единиц измерения предела
     */
    final static String[] VOLTAGE_STRINGS = {
        "mV", "mV", "mV", "mV", "mV", "mV", "V", "V", "V", "V", "V"};

    /**
     * Размер выборки для чтения из АЦП
     */
    static final int ADC_DATA_BLOCK_SIZE = 500;

    /**
     * Размер блока данных в байтах в одном периоде
     */
    static final int BYTES_BLOCK_SIZE = ADC_DATA_BLOCK_SIZE * 2;

}
