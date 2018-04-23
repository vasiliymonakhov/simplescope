package ua.com.kiloom.simplescope;

import java.awt.image.BufferedImage;
import java.util.Arrays;

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
     * положение левой линейки в массиве данных от АЦП
     */
    private int leftRulerPos = -1;

    /**
     * Положение правой линейки в массиве данных от АЦП
     */
    private int rightRulerPos = -1;

    /**
     * Обрабатывает сырые данные от АЦП ввиде массива байтов
     *
     * @param newBlock сырые данные от АЦП ввиде массива байтов
     * @param autoFreq требуется автоматически определить частоту сигнала
     * @return true если данные корректные
     */
    boolean processADCData(byte[] newBlock, boolean autoFreq) {
        int j = 0;
        // вычисление напряжений
        vMin = Double.POSITIVE_INFINITY;
        vMax = Double.NEGATIVE_INFINITY;
        double squareVoltage = 0;
        // определить размер полученного блока

        int steps = Const.BYTES_BLOCK_SIZE - 1;
        boolean needAppend = false;
        if (newBlock.length < Const.BYTES_BLOCK_SIZE) {
            steps = newBlock.length - 1;
            needAppend = true;
        }
        for (int i = 0; i < steps;) {
            // преобразовать байты данныех в значение АЦП
            int value = newBlock[i++] << 8 | newBlock[i++] & 0x00FF;
            // проверить значение на допустимость
            if (value < 0 || value >= Const.ADC_RANGE) {
                // очевидно, что там какой-то мусор и этот блок стоит забраковать
                return false;
            }

            // Отладка - меандр
            // value = ((j /100) % 2 == 0) ? Const.ADC_MIDDLE + 1000 : Const.ADC_MIDDLE - 1000;
            // положительный меандр
            // value = ((j /100) % 2 == 0) ? Const.ADC_MIDDLE + 1000 : Const.ADC_MIDDLE;
            // Отладка - синус
            //value = Const.ADC_MIDDLE + (int)Math.round(1000 * Math.sin(j * Math.PI * 3 /Const.ADC_DATA_BLOCK_SIZE));
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
        vRms = Math.sqrt(squareVoltage / j);
        // если размер блока меньше, то добить остаток последними значениями
        if (needAppend) {
            for (int i = steps / 2; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
                adcData[i] = adcData[i - 1];
                voltages[i] = voltages[i - 1];
            }
        }
        processAutoFreq(autoFreq);
        return true;
    }

    /**
     * Определить частоту сигнала
     *
     * @param autoFreq определять или нет
     */
    private void processAutoFreq(boolean autoFreq) {
        if (autoFreq) {
            // попытаемся найти по очень крутому фронту
            int r1 = searchCoolSignalFront(1);
            if (r1 >= 2) {
                // что-то нашли
                // продолжить поиск
                int r2 = searchCoolSignalFront(r1 + 2);
                if (r2 >= r1) {
                    // нашли вторую точку
                    leftRulerPos = r1;
                    rightRulerPos = r2;
                    return;
                }
            }
            // не нашли, выполнить поиск переходов через 0 по фронту
            // начать со второй точки
            r1 = searchSignalFront(1);
            if (r1 >= 2) {
                // что-то нашли
                // продолжить поиск
                int r2 = searchSignalFront(r1 + 2);
                if (r2 >= r1) {
                    // нашли вторую точку
                    leftRulerPos = r1;
                    rightRulerPos = r2;
                    return;
                }
            }
            // не нашли, попытаемся найти по срезу
            r1 = searchSignalCutoff(1);
            if (r1 >= 2) {
                // что-то нашли
                // продолжить поиск
                int r2 = searchSignalCutoff(r1 + 2);
                if (r2 >= r1) {
                    // нашли вторую точку
                    leftRulerPos = r1;
                    rightRulerPos = r2;
                }
            }
        } else {
            // положение линеек будет задано вручную
            leftRulerPos = -1;
            rightRulerPos = -1;
        }
    }

    /**
     * Найти в массиве напряжений точку, в которой напряжение резко возрастает
     *
     * @param from с какой точки начать поиск
     * @return номер найденной точки или -1 если подходящей точки не нашлось
     */
    private int searchCoolSignalFront(int from) {
        double trig = Const.VOLTAGES[currentVoltageIndex] / 10;
        for (int i = from; i < Const.ADC_DATA_BLOCK_SIZE - 1; i++) {
            // взять 2 идущих подряд значений напряжения
            double v1 = voltages[i - 1];
            double v2 = voltages[i];
            if (v1 < v2 && (v2 - v1 >= trig)) {
                // нашли резкий скачек напряжения на одну клетку скопа
                return i;
            }
        }
        return -1;
    }

    /**
     * Найти в массиве напряжений точку, в которой напряжение возрастает и
     * пересекает нуль
     *
     * @param from с какой точки начать поиск
     * @return номер найденной точки или -1 если подходящей точки не нашлось
     */
    private int searchSignalFront(int from) {
        for (int i = from; i < Const.ADC_DATA_BLOCK_SIZE - 1; i++) {
            // взять 3 идущих подряд значений напряжения
            double v1 = voltages[i - 1];
            double v2 = voltages[i];
            double v3 = voltages[i + 1];
            if (v1 < 0 && v3 > 0 && v1 < v2 && v2 < v3) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Найти в массиве напряжений точку, в которой напряжение спадает и
     * пересекает нуль
     *
     * @param from с какой точки начать поиск
     * @return номер найденной точки или -1 если подходящей точки не нашлось
     */
    private int searchSignalCutoff(int from) {
        for (int i = from; i < Const.ADC_DATA_BLOCK_SIZE - 1; i++) {
            // взять 3 идущих подряд значений напряжения
            double v1 = voltages[i - 1];
            double v2 = voltages[i];
            double v3 = voltages[i + 1];
            if (v1 > 0 && v3 < 0 && v1 > v2 && v2 > v3) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Доля каждой гармоники
     */
    private final double[] harmonics = new double[Const.HARMONICS_COUNT];

    /**
     * Коэффициент гармоник
     */
    private double kHarm;

    /**
     * Вычислить долю гармоник для периодического колебания, находящегося между
     * двумя отметками графика
     *
     * @param fromT от какой отметки начать
     * @param toT до ккой отметки
     */
    void processHarmonicsData(int fromT, int toT) {
        if (fromT >= Const.ADC_DATA_BLOCK_SIZE || toT >= Const.ADC_DATA_BLOCK_SIZE) {
            kHarm = 0;
            Arrays.fill(harmonics, 0);
            return;
        }
        // определить количество значений ддля вычисления
        int count = toT - fromT + 1;
        // шаг изменения фазы синусоиды
        double dfi = 2 * Math.PI / count;
        // перебор заданного количества гармоник
        for (int i = 0; i < Const.HARMONICS_COUNT; i++) {
            // это фаза
            double fi = dfi / 2;
            // приращение фазы на каждом следующем отсчёте в зависимости от порядкового номера гармоники
            double sdfi = (i + 1) * dfi;
            // тут накапливаются значения
            double value = 0;
            // перебираем все значения в нашем диапазоне
            for (int j = fromT; j <= toT; j++) {
                // интегрируем
                value += voltages[j] * Math.sin(fi);
                // фаза на следующем отсчёте
                fi += sdfi;
            }
            // в данный момент есть накопленное значение гармоники
            harmonics[i] = Math.abs(value);
        }

        // вычислить коэффициент гармоник
        double total = 0;
        for (int i = 1; i < Const.HARMONICS_COUNT; i++) {
            total += getHarmonics()[i] * getHarmonics()[i];
        }
        kHarm = Math.sqrt(total) / getHarmonics()[0];

        // теперь нужно просуммировать значения всех гармоник
        double sum = 0;
        for (int i = 0; i < Const.HARMONICS_COUNT; i++) {
            sum += getHarmonics()[i];
        }
        // здесь вычисляется доля каждой гармоники
        for (int i = 0; i < Const.HARMONICS_COUNT; i++) {
            harmonics[i] /= sum;
        }
    }

    /**
     * Возвращает коэффиуиент гармоник
     *
     * @return коэффиуиент гармоник
     */
    double getKHarm() {
        return kHarm;
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
     * готовое изображение осциллограммы
     */
    private BufferedImage scopeImage;

    /**
     * Возвращет изображение осциллограммы
     *
     * @return изображение осциллограммы
     */
    BufferedImage getScopeImage() {
        return scopeImage;
    }

    /**
     * Задаёт изображение осциллограммы
     *
     * @param scopeImage изображение осциллограммы
     */
    void setScopeImage(BufferedImage scopeImage) {
        this.scopeImage = scopeImage;
    }

    /**
     * готовое изображение анализа гармоник
     */
    private BufferedImage harmImage;

    /**
     * Возвращет изображение анализа гармоник
     *
     * @return изображение анализа гармоник
     */
    BufferedImage getHarmImage() {
        return harmImage;
    }

    /**
     * Задаёт изображение анализа гармоник
     *
     * @param harmImage изображение осциллограммы
     */
    void setHarmImage(BufferedImage harmImage) {
        this.harmImage = harmImage;
    }

    /**
     * Возвращает массив значений гармоник
     *
     * @return массив значений гармоник
     */
    double[] getHarmonics() {
        return harmonics;
    }

    /**
     * Возвращает положение левой линейки
     *
     * @return положение левой линейки
     */
    int getLeftRulerPos() {
        return leftRulerPos;
    }

    /**
     * Возвращает положение правой линейки
     *
     * @return положение правой линейки
     */
    int getRightRulerPos() {
        return rightRulerPos;
    }

}
