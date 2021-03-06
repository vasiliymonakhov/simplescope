package ua.com.kiloom.simplescope;

/**
 * Константы
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
     * Пределы вольт
     */
    final static double[] VOLTAGES = {0.05d, 0.1d, 0.25d, 0.5d, 1d, 2.5d, 5d, 10d, 25d, 50d, 100d};

    /**
     * Размер выборки для чтения из АЦП
     */
    static final int ADC_DATA_BLOCK_SIZE = 500;

    /**
     * Размер блока данных в байтах в одном периоде
     */
    static final int BYTES_BLOCK_SIZE = ADC_DATA_BLOCK_SIZE * 2;

    /**
     * Диапазон значений ЦАП от 0 до исключительно
     */
    static final int ADC_RANGE = 4096;

    /**
     * Максимальное значение от АЦП
     */
    static final int ADC_MAX = ADC_RANGE - 1;

    /**
     * Среднее значение от АЦП
     */
    static final int ADC_MIDDLE = ADC_RANGE / 2;

    /**
     * Размер блока для автоматического измерения сигнала
     */
    static final int AUTO_MEASURE_BLOCK = ADC_RANGE / 256;

    /**
     * количество вычисленных гармоник
     */
    static final int HARMONICS_COUNT = 20;

    /**
     * Максимальное расстояние от точки события мыши до ближайшей линейки, при
     * котором линейка прилипает к указателю мыши
     */
    static final int MOUSE_MAX_DISTANCE_TO_RULER = 100;

    /**
     * Количество шагов, которые нужно сделать для определения перегрузки входа или
     * слабого сигнала
     */
    static final int AUTORANGE_DETECT_STEPS = 10;

    /**
     * Количество перегрузок входа или слишком низких показаний, которое должно
     * произойти для того, чтобы переключить вход на следующий предел
     */
    static final int AUTORANGE_COUNT = 5;

    /**
     * Тайм-аут последовательного порта, мсек
     */
    static final int PORT_TIMEOUT = 1000;

    /**
     * желаемый отступ для рисования графика по веритикали
     */
    final static int V_GAP = 64;

    /**
     * желаемый отступ для рисования графика по горизонтали
     */
    final static int H_GAP = 96;

}
