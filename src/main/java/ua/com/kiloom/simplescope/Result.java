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
        if (leftTime < rightTime) {
            leftRulerPos = leftTime;
            rightRulerPos = rightTime;
        } else {
            leftRulerPos = rightTime;
            rightRulerPos = leftTime;
        }
        deltaT = adcTimeToRealTime(rightRulerPos - leftRulerPos);
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
        if (upperValue > lowerValue) {
            lowerRulerPos = lowerValue;
            upperRulerPos = upperValue;
        } else {
            lowerRulerPos = upperValue;
            upperRulerPos = lowerValue;
        }
        deltaV = ((upperRulerPos - lowerRulerPos) * Const.VOLTAGES[currentVoltageIndex]) / Const.ADC_MIDDLE;
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
     * Возвращает величину времени экрана
     *
     * @return величина времени на клетку
     */
    double getTime() {
        return Const.TIMES[currentTimeIndex];
    }

    /**
     * Возвращает величину напряжения экрана
     *
     * @return величина напряжения на клетку
     */
    double getVoltage() {
        return Const.VOLTAGES[currentVoltageIndex];
    }

    /**
     * положение левой линейки в массиве данных от АЦП
     */
    private int leftRulerPos;

    /**
     * Положение правой линейки в массиве данных от АЦП
     */
    private int rightRulerPos;

    /**
     * Положение верхней линейки
     */
    private int upperRulerPos;

    /**
     * Положение нижней линейки
     */
    private int lowerRulerPos;

    /**
     * Автоматически вычислять частоту
     */
    private boolean autoFreq;

    /**
     * автоматически обмерять сигнал
     */
    private boolean autoMeasure;

    /**
     * Признак перегрузки входа АЦП
     */
    private boolean overloadSignal;

    /**
     * Обрабатывает сырые данные от АЦП ввиде массива байтов
     *
     * @param newBlock сырые данные от АЦП ввиде массива байтов
     * @param autoFreq требуется автоматически определить частоту сигнала
     * @param autoMeasure требуется автоматически обмерять сигнал
     * @return true если данные корректные
     */
    boolean processADCData(byte[] newBlock, boolean autoFreq, boolean autoMeasure) {
        this.autoFreq = autoFreq;
        this.autoMeasure = autoMeasure;
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
        overloadSignal = false;
        for (int i = 0; i < steps;) {
            // преобразовать байты данныех в значение АЦП
            int value = newBlock[i++] << 8 | newBlock[i++] & 0x00FF;
            // проверить значение на допустимость
            if (value < 0 || value >= Const.ADC_RANGE) {
                // очевидно, что там какой-то мусор и этот блок стоит забраковать
                return false;
            }
            // проверить перегрузку входа, если есть абсолютный 0 или максимально возможное значение,
            // то скорее всего стоит изменить предел измерения вниз
            if (value <= 0 || value >= Const.ADC_MAX) {
                overloadSignal = true;
            }

            // Отладка - меандр
            // value = ((j /100) % 2 == 0) ? Const.ADC_MIDDLE + 1000 : Const.ADC_MIDDLE - 1000;
            // положительный меандр
            // value = ((j /100) % 2 == 0) ? Const.ADC_MIDDLE + 1000 : Const.ADC_MIDDLE;
            // Отладка - синус
            // value = Const.ADC_MIDDLE + (int) Math.round(400 * Math.sin(i * Math.PI * 4 / Const.ADC_DATA_BLOCK_SIZE)
            //         + 300 * Math.sin(i * Math.PI * 8 / Const.ADC_DATA_BLOCK_SIZE)
            //         + 200 * Math.sin(i * Math.PI * 12 / Const.ADC_DATA_BLOCK_SIZE)
            //        + 100 * Math.sin(i * Math.PI * 16 / Const.ADC_DATA_BLOCK_SIZE));
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
        if (processAutoFreq()) {
            setDeltaT(leftRulerPos, rightRulerPos);
        }
        processAutoMeasure();
        return true;
    }

    /**
     * Массив для накопления количества попаданий сигнала в блок
     */
    private final int[] measures = new int[Const.ADC_RANGE / Const.AUTO_MEASURE_BLOCK];

    /**
     * Изерить линейками сигнал
     */
    private void processAutoMeasure() {
        if (autoMeasure) {
            Arrays.fill(measures, 0);
            // подсчитать попадания сигнала в каждый блок
            // и среднее значение
            int middle = 0;
            for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
                int block = adcData[i] / Const.AUTO_MEASURE_BLOCK;
                measures[block]++;
                middle += adcData[i];
            }
            middle /= Const.ADC_DATA_BLOCK_SIZE;
            // найдём в какой блок попадает среднее значение
            int middlepos = middle / Const.AUTO_MEASURE_BLOCK;

            // ищем максимум ниже положения среднего значения
            int m1 = Integer.MIN_VALUE;
            int p1 = middle;
            for (int i = middlepos - 1; i >= 0; i--) {
                if (measures[i] > m1) {
                    m1 = measures[i];
                    p1 = i;
                }
            }

            // ищем максимум выше положения среднего значения
            int m2 = Integer.MIN_VALUE;
            int p2 = middle;
            for (int i = middlepos + 1; i < measures.length; i++) {
                if (measures[i] > m2) {
                    m2 = measures[i];
                    p2 = i;
                }
            }

            // вычислим новое положение линеек
            int vp1 = p1 * Const.AUTO_MEASURE_BLOCK + Const.AUTO_MEASURE_BLOCK / 2;
            int vp2 = p2 * Const.AUTO_MEASURE_BLOCK + Const.AUTO_MEASURE_BLOCK / 2;
            setDeltaV(vp1, vp2);
        }
    }

    /**
     * Определить частоту сигнала
     *
     * @param autoFreq определять или нет
     * @return true если частота определена автоматически
     */
    private boolean processAutoFreq() {
        if (autoFreq) {
            // попытаемся найти по очень крутому фронту
            int r1 = searchCoolSignalFront(1);
            if (r1 >= 2) {
                // что-то нашли
                // продолжить поиск
                int r2 = searchCoolSignalFront(r1 + 2);
                if (r2 >= r1) {
                    // нашли вторую точку
                    setDeltaT(r1, r2);
                    return true;
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
                    setDeltaT(r1, r2);
                    return true;
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
                    setDeltaT(r1, r2);
                    return true;
                }
            }
        } else {
            // положение линеек будет задано вручную
            leftRulerPos = -1;
            rightRulerPos = -1;
        }
        return false;
    }

    /**
     * Найти в массиве напряжений точку, в которой напряжение резко возрастает.
     * Разница напряжений между соседними точками должна составить 90% от vRms
     *
     * @param from с какой точки начать поиск
     * @return номер найденной точки или -1 если подходящей точки не нашлось
     */
    private int searchCoolSignalFront(int from) {
        double trig = vRms * 0.9d;
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
    private final double[] harmonics = new double[AppProperties.getHarmonicsCount()];

    /**
     * Коэффициент гармоник
     */
    private double kHarm;

    /**
     * Вычислить долю гармоник для периодического колебания, находящегося между
     * двумя отметками графика
     *
     * @param fromT от какой отметки начать
     * @param toT до какой отметки
     */
    void processHarmonicsData(int fromT, int toT) {
        if (fromT >= Const.ADC_DATA_BLOCK_SIZE || toT >= Const.ADC_DATA_BLOCK_SIZE) {
            kHarm = 0;
            Arrays.fill(harmonics, 0);
            return;
        }

        int harmonicsCount = AppProperties.getHarmonicsCount();

        // определить количество значений для вычисления
        int count = toT - fromT;
        // шаг изменения фазы синусоиды
        double dfi = 2 * Math.PI / count;
        // перебор заданного количества гармоник
        for (int i = 0; i < harmonicsCount; i++) {
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
        for (int i = 1; i < harmonicsCount; i++) {
            total += harmonics[i] * harmonics[i];
        }
        kHarm = Math.sqrt(total) / harmonics[0];

        // теперь нужно просуммировать значения всех гармоник
        double sum = 0;
        for (int i = 0; i < harmonicsCount; i++) {
            sum += harmonics[i];
        }
        boolean db = AppProperties.isHarmonicsInDb();
        // здесь вычисляется доля каждой гармоники
        for (int i = 0; i < harmonicsCount; i++) {
            if (db) {
                harmonics[i] = 20 * Math.log10(harmonics[i] / sum);
            } else {
                harmonics[i] /= sum;
            }
        }
    }

    /**
     * Возвращает коэффициент гармоник
     *
     * @return коэффициент гармоник
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

    /**
     * Был ли включен режим автоматического определения частоты
     *
     * @return the autoFreq
     */
    boolean isAutoFreq() {
        return autoFreq;
    }

    /**
     * Была ли перегрузка входа
     *
     * @return the overloadSignal
     */
    boolean isOverloadSignal() {
        return overloadSignal;
    }

    /**
     * Был ли включен режим автоматического обмера сигнала
     *
     * @return the autoMeasure
     */
    boolean isAutoMeasure() {
        return autoMeasure;
    }

    /**
     * Возвращает положение верхней линейки
     *
     * @return положение верхней линейки
     */
    int getUpperRulerPos() {
        return upperRulerPos;
    }

    /**
     * Возвращает положение нижней линейки
     *
     * @return положение нижней линейки
     */
    int getLowerRulerPos() {
        return lowerRulerPos;
    }

}
