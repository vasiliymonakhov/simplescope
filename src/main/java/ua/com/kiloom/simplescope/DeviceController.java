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
 * Класс для работы с устройством. Осуществляет выбор режимов работы а также
 * циклическое получение данных от АЦП, вычисление напряжений, обработку и
 * накопление данных в очереди.
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
     * Что вызвать при завершении потока считывания от устройства. Когда поток
     * остановится, вызывается этот код и приложение может понять что устройство
     * прекратило работу
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
     * Очередь для байтов от АЦП. Сюда обработчик прерывания от
     * последовательного порта помещает массивы байтов.
     */
    private final BlockingQueue<byte[]> bytesQueue = new LinkedBlockingQueue<>();

    /**
     * Очередь обработанных данных от АЦП. Сюда помещаются вычисленные значения.
     */
    private final BlockingQueue<ADCResult> adcQueue = new LinkedBlockingQueue<>();

    /**
     * Флажок-сигнал для остановки получения данных от устройства. Чтобы
     * остановить поток обработки данных от АЦП нужно взвести этот флажок и
     * дождаться вызова onStop.run();
     *
     * @see onStop
     */
    private boolean stop;

    /**
     * Открыть устройство. Устанавливает связь с портом и запускает поток
     * обработки данных от АЦП.
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
                if (event.getEventValue() >= Const.BYTES_BLOCK_SIZE) {
                    try {
                        byte[] data = port.readBytes(Const.BYTES_BLOCK_SIZE);
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
     * Можно ли работать с устройством. Порт должен быть открыт и не должен быть
     * послан сигнал остановить поток обарботки данных.
     *
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
     * Блокировка для переключений времени развёртки и пределов напряжений.
     * Важно не давать изменять пределы измерений и время развёртки пока
     * происходит вычисление напряжений, иначе данные будут неправильно
     * интерпретированы.
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * текущий предел измерений напряжения
     */
    private int currentVoltageIndex;

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
     * Обрабатывает данные от ЦАП
     *
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
            for (int i = 0; i < Const.BYTES_BLOCK_SIZE - 1;) {
                // преобразовать байты данныех в значение АЦП
                int value = newBlock[i++] << 8 | newBlock[i++] & 0x00FF;
                // проверить значение на допустимость
                if (value < 0 || value > 4095) {
                    // очевидно, что там какой-то мусор и этот блок стоит забраковать
                    return null;
                }
                // запись сырых данных от АЦП для построения графика
                r.getAdcData()[j] = value;
                // вычислим мгновенное значение напряжения
                double voltage = ((value - 2048) * Const.VOLTAGES[currentVoltageIndex]) / 2048;
                // запишем в массив
                r.getVoltages()[j] = voltage;
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
            r.setCurrentTimeIndex(currentTimeIndex);
            r.setCurrentVoltageIndex(currentVoltageIndex);
            r.setVMin(minVoltage);
            r.setVMax(maxVoltage);
            // и среднеквадратического напряжения
            r.setVRms(Math.sqrt(squareVoltage / Const.ADC_DATA_BLOCK_SIZE));
            return r;
        } finally {
            lock.unlock();
        }
    }

}
