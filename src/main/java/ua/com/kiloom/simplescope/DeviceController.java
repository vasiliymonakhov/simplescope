package ua.com.kiloom.simplescope;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
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
    private final SerialPort port;

    private final Runnable onStop;

    /**
     * Создаёт объект для работы с устройством
     *
     * @param portName имя порта
     * @param onStop что вызвать по завершении потока считывания и обработки
     * данных
     */
    DeviceController(String portName, Runnable onStop) {
        port = new SerialPort(portName);
        this.onStop = onStop;
    }

    private final BlockingQueue<byte[]> bytesQueue = new LinkedBlockingQueue<>();

    private final BlockingQueue<ADCResult> adcQueue = new LinkedBlockingQueue<>();

    static int BLOCK_SIZE = 2000;

    private boolean stop;

    /**
     * Открыть устройство
     *
     * @throws SerialPortException
     */
    void open() throws SerialPortException {
        port.openPort();
        port.setParams(SerialPort.BAUDRATE_115200,
                SerialPort.DATABITS_8,
                SerialPort.STOPBITS_1,
                SerialPort.PARITY_NONE);

        port.addEventListener(new SerialPortEventListener() {

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventValue() > BLOCK_SIZE) {
                    try {
                        byte[] data = port.readBytes();
                        bytesQueue.add(data);
                    } catch (SerialPortException ex) {
                        Logger.getLogger(DeviceController.class.getName()).log(Level.SEVERE, "Error reading data from device!", ex);
                    }
                }
            }
        }, SerialPort.MASK_RXCHAR);
        // поток, обрабатывающий данные от АЦП
        Thread th = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        while (!stop) {
                            ADCResult r;
                            if ((r = processAdcData()) != null) {
                                adcQueue.add(r);
                            }
                        }
                        if (port.isOpened()) {
                            port.closePort();
                        }
                        onStop.run();
                        break;
                    } catch (InterruptedException | SerialPortException th) {
                        Logger.getLogger(DeviceController.class.getName()).log(Level.SEVERE, "Error processing data from device!", th);
                    }
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
    void close() throws SerialPortException {
        stop = true;
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
     * Пределы вольт/клетка
     */
    private final static double[] VOLTAGES = {0.01d, 0.02d, 0.05d, 0.1d, 0.2d, 0.5d, 1d, 2d, 5d, 10d, 20d};

    /**
     * Строки с обозначением предела
     */
    private final static String[] VOLTAGE_STRINGS = {
        "10mV", "20mV", "50mV", "100mV", "200mV", "500mV", "1V", "2V", "5V", "10V", "20V"};

    /**
     * Сменить предел напряжения
     *
     * @param voltageIndex индекс предела напряжения
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchVoltage(int voltageIndex) throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xAA);
            port.writeByte((byte) 0xAB);
            port.writeByte((byte) (50 + voltageIndex));
            return true;
        }
        return false;
    }

    /**
     * Время развёртки/клетка, сек
     */
    private final static double[] TIMES = {0.000001d, 0.000002d, 0.000005d, 0, 00001d,
        0.00002d, 0.00005d, 0.0001d, 0.0002d, 0.0005d, 0, 001d, 0, 002d, 0, 005d, 0.01d, 0.02d, 0.05d,
        0.1d, 0.2d, 0.5d};

    /**
     * Строки с времемем развёртки
     */
    private final static String[] TIME_STRINGS = {"1µS", "2µS", "5µS", "10µS", "20µS",
        "50µS", "100µS", "200µS", "500µS", "1mS", "2mS", "5mS", "10mS", "20mS", "50mS", "100mS", "200mS", "500mS"};

    /**
     * Переключение времени развёртки
     *
     * @param timeIndex индекс времени развёртки
     * @return true если успешно
     */
    boolean switchTime(int timeIndex) throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xAC);
            port.writeByte((byte) 0xAD);
            port.writeByte((byte) (20 + timeIndex));
            return true;
        }
        return false;
    }

    /**
     * Переключает вход на постоянное напряжение
     *
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchInputToDc() throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xBE);
            port.writeByte((byte) 0xBF);
            return true;
        }
        return false;
    }

    /**
     * Переключает вход на переменное напряжение
     *
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchInputToAc() throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xBA);
            port.writeByte((byte) 0xBB);
            return true;
        }
        return false;
    }

    /**
     * Переключает вход на землю
     *
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchInputToGnd() throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xBC);
            port.writeByte((byte) 0xBD);
            return true;
        }
        return false;
    }

    /**
     * Переключает в режим осциллоскопа
     *
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchModeToOscilloscope() throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xDF);
            port.writeByte((byte) 0xEA);
            return true;
        }
        return false;
    }

    /**
     * Переключает в режим записи
     *
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchModeToLogger() throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xDC);
            port.writeByte((byte) 0xDE);
            return true;
        }
        return false;
    }

    /**
     * Отключить синхронизацию
     *
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchSyncToNone() throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xCE);
            port.writeByte((byte) 0xCF);
            return true;
        }
        return false;
    }

    /**
     * Включить автосинхронизацию
     *
     * @param edge по фронту (true) или по спаду (false) сигнала
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchSyncToAuto(boolean edge) throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xCA);
            port.writeByte((byte) 0xCB);
            port.writeByte((byte) (edge ? 0x10 : 0x20));
            return true;
        }
        return false;
    }

    /**
     * Включить синхронизацию по уровню
     *
     * @param level уровень 0-200
     * @param edge по фронту (true) или по спаду (false) сигнала
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean switchSyncToLevel(int level, boolean edge) throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xCC);
            port.writeByte((byte) 0xCD);
            port.writeByte((byte) (level & 0xFF));
            port.writeByte((byte) (edge ? 0x10 : 0x20));
            return true;
        }
        return false;
    }

    /**
     * Установить уровень смещения нуля
     *
     * @param level уровень
     * @return true если успешно
     * @throws SerialPortException
     */
    boolean setZeroLevel(int level) throws SerialPortException {
        if (port.isOpened()) {
            port.writeByte((byte) 0xDA);
            port.writeByte((byte) 0xDB);
            port.writeByte((byte) ((byte) level & 0xFF));
            return true;
        }
        return false;
    }

    class ADCResult {
        private final int[] adcData = new int[BLOCK_SIZE / 2];
        private int adcMinValue;
        private int adcMaxValue;

        /**
         * @return the adcData
         */
        int[] getAdcData() {
            return adcData;
        }

        /**
         * @return the adcMinValue
         */
        int getAdcMinValue() {
            return adcMinValue;
        }

        /**
         * @return the adcMaxValue
         */
        int getAdcMaxValue() {
            return adcMaxValue;
        }
    }

    private ADCResult processAdcData() throws InterruptedException {
        int size = bytesQueue.size();
        if (size > 1) {
            // это пропускает очередную порцию данных, на случай если компьютер не успевает обрабатывать данные
            for (int i = 1; i < size; i++) {
                bytesQueue.take();
            }
        }
        byte[] newBlock = bytesQueue.take();
        int j = 0;
        ADCResult r = new ADCResult();
        r.adcMinValue = Integer.MAX_VALUE;
        r.adcMaxValue = Integer.MIN_VALUE;
        for (int i = 0; i < BLOCK_SIZE - 1;) {
            int value = newBlock[i++] << 8 | newBlock[i++] & 0x00FF;
            if (value < 0 || value > 4095) {
                return null;
            }
            r.adcData[j++] = value;
            if (value > r.getAdcMaxValue()) {
                r.adcMaxValue = value;
            }
            if (value < r.getAdcMinValue()) {
                r.adcMinValue = value;
            }
        }
        return r;
    }

}
