package ua.com.kiloom.simplescope;

import java.awt.image.BufferedImage;

/**
 * Результат измерений
 *
 * @author Vasily Monakhov
 */
class Result {

    /**
     * непосредственно отсчёты
     */
    private final int[] adcData = new int[Const.ADC_DATA_BLOCK_SIZE];

    /**
     * Индекс предела измерения напряжения на момент фиксации данных
     */
    private final int currentVoltageIndex;

    /**
     * Индекс периода развёртки на момент фиксации данных
     */
    private final int currentTimeIndex;

    /**
     * Создаёт результаты
     *
     * @param currentVoltageIndex текущий индекс предела измерения напряжения
     * @param currentTimeIndex текущий индекс времени развёртки
     */
    Result(int currentVoltageIndex, int currentTimeIndex) {
        this.currentTimeIndex = currentTimeIndex;
        this.currentVoltageIndex = currentVoltageIndex;
    }

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
     * Разница времени между вертикальными линейками, сек
     */
    private double deltaT = 1;

    /**
     * Устанавливает разницу между вертикальными линейками
     *
     * @param leftTime отсчёт левой вериткальной линейки
     * @param rightTime отсчёт правой вериткальной линейки
     */
    void setDeltaT(int leftTime, int rightTime) {
        deltaT = adcTimeToRealTime(rightTime - leftTime);
    }

    /**
     * Возвращает разницу между вертикальными линейками
     *
     * @return разница времени между вертикальными линейками, сек
     */
    double getDeltaT() {
        return deltaT;
    }

    /**
     * Разница напряжения между горизонтальными линейками, В
     */
    private double deltaV;

    /**
     * Устанавливает разницу между горизонтальными линейками
     *
     * @param upperValue значение АЦП, соответствующее верхней линейке
     * @param lowerValue значение АЦП, соответствующее нижней линейке
     */
    void setDeltaV(int upperValue, int lowerValue) {
        deltaV = ((upperValue - lowerValue) * Const.VOLTAGES[currentVoltageIndex]) / Const.ADC_MIDDLE;
    }

    /**
     * Возвращает разницу между горизонтальными линейками
     *
     * @return разница напряжения между горизонтальными линейками, В
     */
    double getDeltaV() {
        return deltaV;
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
     * Обрабатывает сырые данные от АЦП ввиде массива байтов
     *
     * @param newBlock сырые данные от АЦП ввиде массива байтов
     * @return true если данные корректные
     */
    boolean processADCData(byte[] newBlock) {
        int j = 0;
        // вычисление напряжений
        vMin = Double.POSITIVE_INFINITY;
        vMax = Double.NEGATIVE_INFINITY;
        double squareVoltage = 0;
        for (int i = 0; i < Const.BYTES_BLOCK_SIZE - 1;) {
            // преобразовать байты данныех в значение АЦП
            int value = newBlock[i++] << 8 | newBlock[i++] & 0x00FF;
            // проверить значение на допустимость
            if (value < 0 || value >= Const.ADC_RANGE) {
                // очевидно, что там какой-то мусор и этот блок стоит забраковать
                return false;
            }
            // запись сырых данных от АЦП для построения графика
            adcData[j] = value;
            // вычислим мгновенное значение напряжения
            double voltage = adcValueToVoltage(value);
            // запишем в массив
            voltages[j] = voltage;
            // найдём минимум и максимум
            if (vMin > voltage) {
                vMin = voltage;
            }
            if (vMax < voltage) {
                vMax = voltage;
            }
            // подсчёт суммы квадратов всех значений
            squareVoltage = squareVoltage + voltage * voltage;
            j++;
        }
        // и среднеквадратического напряжения
        vRms = Math.sqrt(squareVoltage / Const.ADC_DATA_BLOCK_SIZE);
        return true;
    }

    /**
     * Вычисляет напряжение по значению из АЦП в зависимости от текущих
     * параметров выборки
     *
     * @param value значение выборки
     * @return напряжение
     */
    double adcValueToVoltage(int value) {
        return ((value - Const.ADC_MIDDLE) * Const.VOLTAGES[currentVoltageIndex]) / Const.ADC_MIDDLE;
    }

    /**
     * Вычисляет время по номеру отсчёта в массиве данных АЦП в зависимости от
     * текущих параметров выборки
     *
     * @param value номер выборки от 0
     * @return время
     */
    double adcTimeToRealTime(int value) {
        return Const.TIMES[currentTimeIndex] * value / Const.ADC_DATA_BLOCK_SIZE;
    }

    /**
     * готовое изображение
     */
    private BufferedImage image;

    /**
     * Возвращет изображение
     *
     * @return изображение
     */
    BufferedImage getImage() {
        return image;
    }

    /**
     * Задаёт изображение
     *
     * @param image изображение
     */
    void setImage(BufferedImage image) {
        this.image = image;
    }

}
