package ua.com.kiloom.simplescope;

/**
 * Результат измерений АЦП
 *
 * @author Vasily Monakhov
 */
class ADCResult {

    /**
     * непосредственно отсчёты
     */
    private final int[] adcData = new int[Const.ADC_DATA_BLOCK_SIZE];

    /**
     * Индекс предела измерения напряжения на момент фиксации данных
     */
    private int currentVoltageIndex;

    /**
     * Индекс периода развёртки на момент фиксации данных
     */
    private int currentTimeIndex;

    /**
     * Максимальное нампряжение
     */
    private double vMax;

    /**
     * Минимальное напряжение
     */
    private double vMin;

    /**
     * Среднеквадратическое напряжение
     */
    private double vRms;

    /**
     * Отсчёты ввиде напряжения
     */
    private final double[] voltages = new double[Const.ADC_DATA_BLOCK_SIZE];

    /**
     * Возвращает сырые данные АЦП для рисования графика
     *
     * @return сырые данные АЦП для рисования графика
     */
    int[] getAdcData() {
        return adcData;
    }

    double[] getVoltages() {
        return voltages;
    }

    /**
     * Возвращает максимальное напряжение
     *
     * @return максимальное напряжение
     */
    double getVMax() {
        return vMax;
    }

    /**
     * Возвращает минимальное напряжение
     *
     * @return минимальное напряжение
     */
    double getVMin() {
        return vMin;
    }

    /**
     * Возвращает среднеквадратическое напряжение
     *
     * @return среднеквадратическое напряжение
     */
    double getVRms() {
        return vRms;
    }

    /**
     * Возвращает величину времени на клетку
     *
     * @return величина времени на клетку
     */
    int getTimePerCell() {
        return Const.TIME_PER_CELL[currentTimeIndex];
    }

    /**
     * Возращает единицу времени на клетку
     *
     * @return единица времени на клетку
     */
    String getTimeString() {
        return Const.TIME_STRINGS[currentTimeIndex];
    }

    /**
     * Возвращает величину напряжения на клетку
     *
     * @return величина напряжения на клетку
     */
    int getVoltagePerCell() {
        return Const.VOLTAGE_PER_CELL[currentVoltageIndex];
    }

    /**
     * Возвращает единцу напряжения на клетку
     *
     * @return единца напряжения на клетку
     */
    String getVoltageString() {
        return Const.VOLTAGE_STRINGS[currentVoltageIndex];
    }

    /**
     * Устанавливает текущий индекс предела измерения напряжения
     * @param currentVoltageIndex текущий индекс предела измерения напряжения
     */
    void setCurrentVoltageIndex(int currentVoltageIndex) {
        this.currentVoltageIndex = currentVoltageIndex;
    }

    /**
     * Устанавливает текущий индекс времени развёртки
     * @param currentTimeIndex текущий индекс времени развёртки
     */
    void setCurrentTimeIndex(int currentTimeIndex) {
        this.currentTimeIndex = currentTimeIndex;
    }

    /**
     * Уснатавливает значение минимального напряжения
     * @param v значение минимального напряжения
     */
    void setVMin(double v) {
        vMin = v;
    }

    /**
     * Устанавливает значение максимального напряжения
     * @param v значение максимального напряжения
     */
    void setVMax(double v) {
        vMax = v;
    }

    /**
     * Устанавливает значение среднеквадратического напряжения
     * @param v значение среднеквадратического напряжения
     */

    void setVRms(double v) {
        vRms = v;
    }

}
