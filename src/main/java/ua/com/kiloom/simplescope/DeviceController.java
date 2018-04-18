package ua.com.kiloom.simplescope;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

/**
 * Класс для работы с устройством
 *
 * @author coolbassnik
 * @author Vasiliy Monakhov
 */
public class DeviceController {

    /**
     * Последовательный порт для работы с устройством
     */
    private SerialPort port;

    /**
     * Что вызвать при завершении потока считывания от устройства
     */
    private final Runnable onStop;

    /**
     * Создаёт объект для работы с устройством
     *
     * @param onStop что вызвать по завершении потока считывания и обработки
     * данных
     */
    DeviceController(Runnable onStop) {
        this.onStop = onStop;
    }

    /**
     * Очередь для байтов от АЦП
     */
    private final BlockingQueue<byte[]> bytesQueue = new LinkedBlockingQueue<>();

    /**
     * Очередь обработанных данных от АЦП
     */
    private final BlockingQueue<ADCResult> adcQueue = new LinkedBlockingQueue<>();

    /**
     * Размер блока данных в байтах в одном периоде
     */
    private static final int BYTES_BLOCK_SIZE = 2000;

    /**
     * Размер выборки для чтения из АЦП
     */
    private static final int ADC_DATA_BLOCK_SIZE = BYTES_BLOCK_SIZE / 2;

    /**
     * Флажок для остановки получения данных от устройства
     */
    private boolean stop;

    /**
     * Открыть устройство
     *
     * @param portName имя порта
     * @throws SerialPortException
     */
    void open(String portName) throws SerialPortException {
        port = new SerialPort(portName);
        port.openPort();
        port.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);
        port.addEventListener(new SerialPortEventListener() {
            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventValue() > BYTES_BLOCK_SIZE) {
                    try {
                        byte[] data = port.readBytes();
                        bytesQueue.add(data);
                    } catch (SerialPortException ex) {
                        Logger.getLogger(DeviceController.class.getName()).log(Level.SEVERE, "Ошибка чтения данных из устройства!", ex);
                    }
                }
            }
        }, SerialPort.MASK_RXCHAR);
        stop = false;
        // поток, обрабатывающий данные от АЦП
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!stop) {
                        // бесконечный цикл получения данных
                        ADCResult r;
                        if ((r = processAdcData()) != null) {
                            adcQueue.add(r);
                        }
                    }
                } catch (InterruptedException th) {
                    Logger.getLogger(DeviceController.class.getName()).log(Level.SEVERE, "Ошибка обработки данных от устройства!", th);
                } finally {
                    // закрыть за собой порт
                    if (port.isOpened()) {
                        try {
                            port.closePort();
                        } catch (SerialPortException ex) {
                            Logger.getLogger(DeviceController.class.getName()).log(Level.SEVERE, "Ошибка закрытия порта", ex);
                        }
                    }
                    // очистить очереди с данными
                    bytesQueue.clear();
                    adcQueue.clear();
                    // отрапортавать о завершении работы
                    onStop.run();
                }
            }
        });
        th.start();
    }

    /**
     * Отключить устройство. Работа потока обработки данных от устройсва
     * прекращается
     *
     * @throws SerialPortException
     */
    void close() {
        stop = true;
    }

    /**
     * Можно ли работать с устройством
     * @return 
     */
    boolean isOpen() {
        return !stop && port != null && port.isOpened();
    }

    /**
     * Возвращает очередную порцию данных из очереди результатов
     *
     * @return результат очередного считывания
     * @throws InterruptedException
     */
    ADCResult getADCResult() throws InterruptedException {
        int size = adcQueue.size();
        if (size > 1) {
            // это пропускает очередную порцию данных, на случай если компьютер не успевает обрабатывать данные
            for (int i = 1; i < size; i++) {
                adcQueue.take();
                System.out.println("Пропуск обработанных данных");
            }
        }
        return adcQueue.take();
    }

    /**
     * Очищает очередь результатов считывания
     */
    void clearADCQueue() {
        adcQueue.clear();
    }

    /**
     * Блокировка для переключений времени развёртки и пределов напряжений
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * текущий предел измерений напряжения
     */
    private int currentVoltageIndex;

    /**
     * Пределы вольт
     */
    private final static double[] VOLTAGES = {0.05d, 0.1d, 0.25d, 0.5d, 1d, 2.5d, 5d, 10d, 25d, 50d, 100d};

    /**
     * Напряжение на клетку
     */
    private final static int[] VOLTAGE_PER_CELL = {
        10, 20, 50, 100, 200, 500, 1, 2, 5, 10, 20};

    /**
     * Строки с обозначением величины единиц измерения предела
     */
    private final static String[] VOLTAGE_STRINGS = {
        "mV", "mV", "mV", "mV", "mV", "mV", "V", "V", "V", "V", "V"};

    /**
     * Сменить предел напряжения
     *
     * @param voltageIndex индекс предела напряжения
     * @throws SerialPortException
     */
    void switchVoltage(int voltageIndex) throws SerialPortException {
        try {
            lock.lock();
            port.writeByte((byte) 0xAA);
            port.writeByte((byte) 0xAB);
            port.writeByte((byte) (50 + voltageIndex));
            currentVoltageIndex = voltageIndex;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Текущее время развёртки
     */
    private int currentTimeIndex;

    /**
     * Время развёртки, сек
     */
    private final static double[] TIMES = {0.00001d, 0.00002d, 0.00005d, 0.0001d,
        0.0002d, 0.0005d, 0.001d, 0.002d, 0.005d, 0.01d, 0.02d, 0.05d, 0.1d, 0.2d, 0.5d,
        1d, 2d, 5d};

    /**
     * Время на клетку
     */
    private final static int[] TIME_PER_CELL = {1, 2, 5, 10, 20, 50, 100, 200, 500, 1, 2, 5, 10, 20, 50, 100, 200, 500};

    /**
     * Строки с времемем развёртки
     */
    private final static String[] TIME_STRINGS = {"µS", "µS", "µS", "µS", "µS",
        "µS", "µS", "µS", "µS", "mS", "mS", "mS", "mS", "mS", "mS", "mS", "mS", "mS"};

    /**
     * Переключение времени развёртки
     *
     * @param timeIndex индекс времени развёртки
     */
    void switchTime(int timeIndex) throws SerialPortException {
        try {
            lock.lock();
            port.writeByte((byte) 0xAC);
            port.writeByte((byte) 0xAD);
            port.writeByte((byte) (20 + timeIndex));
            currentTimeIndex = timeIndex;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Переключает вход на постоянное напряжение
     *
     * @throws SerialPortException
     */
    void switchInputToDc() throws SerialPortException {
        port.writeByte((byte) 0xBE);
        port.writeByte((byte) 0xBF);
    }

    /**
     * Переключает вход на переменное напряжение
     *
     * @throws SerialPortException
     */
    void switchInputToAc() throws SerialPortException {
        port.writeByte((byte) 0xBA);
        port.writeByte((byte) 0xBB);
    }

    /**
     * Переключает вход на землю
     *
     * @throws SerialPortException
     */
    void switchInputToGnd() throws SerialPortException {
        port.writeByte((byte) 0xBC);
        port.writeByte((byte) 0xBD);
    }

    /**
     * Переключает в режим осциллоскопа
     *
     * @throws SerialPortException
     */
    void switchModeToOscilloscope() throws SerialPortException {
        port.writeByte((byte) 0xDF);
        port.writeByte((byte) 0xEA);
    }

    /**
     * Переключает в режим записи
     *
     * @return true если успешно
     * @throws SerialPortException
     */
    void switchModeToLogger() throws SerialPortException {
        port.writeByte((byte) 0xDC);
        port.writeByte((byte) 0xDE);
    }

    /**
     * Отключить синхронизацию
     *
     * @throws SerialPortException
     */
    void switchSyncToNone() throws SerialPortException {
        port.writeByte((byte) 0xCE);
        port.writeByte((byte) 0xCF);
    }

    /**
     * Включить автосинхронизацию
     *
     * @param edge по фронту (true) или по спаду (false) сигнала
     * @throws SerialPortException
     */
    void switchSyncToAuto(boolean edge) throws SerialPortException {
        port.writeByte((byte) 0xCA);
        port.writeByte((byte) 0xCB);
        port.writeByte((byte) (edge ? 0x10 : 0x20));
    }

    /**
     * Включить синхронизацию по уровню
     *
     * @param level уровень 0-200
     * @param edge по фронту (true) или по спаду (false) сигнала
     * @throws SerialPortException
     */
    void switchSyncToLevel(int level, boolean edge) throws SerialPortException {
        port.writeByte((byte) 0xCC);
        port.writeByte((byte) 0xCD);
        port.writeByte((byte) (level & 0xFF));
        port.writeByte((byte) (edge ? 0x10 : 0x20));
    }

    /**
     * Установить уровень смещения нуля
     *
     * @param level уровень
     * @throws SerialPortException
     */
    void setZeroLevel(int level) throws SerialPortException {
        port.writeByte((byte) 0xDA);
        port.writeByte((byte) 0xDB);
        port.writeByte((byte) ((byte) level & 0xFF));
    }

    /**
     * Результат измерений АЦП
     */
    class ADCResult {

        /**
         * непосредственно отсчёты
         */
        private final int[] adcData = new int[ADC_DATA_BLOCK_SIZE];

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
        private final double[] voltages = new double[ADC_DATA_BLOCK_SIZE];

        /**
         * Возвращает сырые данные АЦП для рисования графика
         * @return the adcData
         */
        int[] getAdcData() {
            return adcData;
        }

        /**
         * Возвращает максимальное напряжение
         * @return the vMax
         */
        double getVMax() {
            return vMax;
        }

        /**
         * Возвращает минимальное напряжение
         * @return the vMin
         */
        double getVMin() {
            return vMin;
        }

        /**
         * Возвращает среднеквадратическое напряжение
         * @return the vRms
         */
        double getVRms() {
            return vRms;
        }

        /**
         * Возвращает величину времени на клетку
         * @return
         */
        int getTimePerCell() {
            return TIME_PER_CELL[currentTimeIndex];
        }

        /**
         * Возращает единицу времени на клетку
         * @return
         */
        String getTimeString() {
            return TIME_STRINGS[currentTimeIndex];
        }

        /**
         * Возвращает величину напряжения на клетку
         * @return
         */
        int getVoltagePerCell() {
            return VOLTAGE_PER_CELL[currentVoltageIndex];
        }

        /**
         * Возвращает единцу напряжения на клетку
         * @return
         */
        String getVoltageString() {
            return VOLTAGE_STRINGS[currentVoltageIndex];
        }

    }

    /**
     * Обрабатывает данные от ЦАП
     * @return объект с результатами обработки
     * @throws InterruptedException
     */
    private ADCResult processAdcData() throws InterruptedException {
        int size = bytesQueue.size();
        if (size > 1) {
            // это пропускает очередную порцию данных, на случай если компьютер не успевает обрабатывать данные
            for (int i = 1; i < size; i++) {
                bytesQueue.take();
            }
        }
        byte[] newBlock = bytesQueue.take();
        try {
            lock.lock();
            int j = 0;
            ADCResult r = new ADCResult();
            double minVoltage = Double.POSITIVE_INFINITY;
            double maxVoltage = Double.NEGATIVE_INFINITY;
            double squareVoltage = 0;
            for (int i = 0; i < BYTES_BLOCK_SIZE - 1;) {
                // преобразовать байты данныех в значение АЦП
                int value = newBlock[i++] << 8 | newBlock[i++] & 0x00FF;
                // проверить значение на допустимость
                if (value < 0 || value > 4095) {
                    // очевидно, что там какой-то мусор и этот блок стоит забраковать
                    return null;
                }
                // запись сырых данных от АЦП для построения графика
                r.adcData[j] = value;
                // вычислим мгновенное значение напряжения
                double voltage = ((value - 2048) * VOLTAGES[currentVoltageIndex]) / 2048;
                // запишем в массив
                r.voltages[j] = voltage;
                // найдём минимум и максимум
                if (minVoltage > voltage) {
                    minVoltage = voltage;
                }
                if (maxVoltage < voltage) {
                    maxVoltage = voltage;
                }
                // подсчёт суммы квадратов всех значений
                squareVoltage = squareVoltage + voltage * voltage;
                j++;
            }
            // запись параметров выборки
            r.currentTimeIndex = currentTimeIndex;
            r.currentVoltageIndex = currentVoltageIndex;
            r.vMin = minVoltage;
            r.vMax = maxVoltage;
            // и среднеквадратического напряжения
            r.vRms = Math.sqrt(squareVoltage / ADC_DATA_BLOCK_SIZE);
            return r;
        } finally {
            lock.unlock();
        }
    }

}
