package ua.com.kiloom.simplescope;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
import static ua.com.kiloom.simplescope.AppProperties.Keys.*;

/**
 * Класс главного окна
 *
 * @author vasya
 */
public class MainFrame extends javax.swing.JFrame {

    /**
     * Схема шрифтов
     */
    private final FontScheme fontScheme = FontScheme.STANDART;
    /**
     * Цветовая схема скопа
     */
    private ColorScheme colorScheme = ColorScheme.GREEN_MONO_SCHEME;

    /**
     * Настройки
     */
    private final AppProperties prop = new AppProperties();

    /**
     * Создаёт окно
     */
    public MainFrame() {
        initComponents();
        scopeParentPanel.add(scopeRenderPanel);
        harmParentPanel.add(harmRenderPanel);
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                try {
                    drawResults();
                } catch (InterruptedException ex) {
                    Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                searchPorts();
                prop.load();
                setupFrameFromProperties();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                storeFrameToProperties();
                prop.save();
            }
        });
    }

    /**
     * Поиск портов
     */
    private void searchPorts() {
        portsComboBox.removeAllItems();
        String[] portNames = SerialPortList.getPortNames();
        if (portNames.length != 0) {
            for (String portName : portNames) {
                portsComboBox.addItem(portName);
            }
            startButton.setEnabled(true);
        } else {
            startButton.setEnabled(false);
        }
    }

    /**
     * Класс управления устройством
     */
    private final DeviceController deviceController = new DeviceController(new Runnable() {
        @Override
        public void run() {
            startButton.setEnabled(true);
            portsComboBox.setEnabled(true);
            searchPortsButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    });

    /**
     * Класс рисования графиков
     */
    private final ScopeRenderer scopeRenderer = new ScopeRenderer();

    /**
     * Рабочий поток
     */
    private Thread workThread;

    /**
     * Разрешает или запрещает кнопки
     *
     * @param enable разрешить или запретить
     */
    void enableStepButtons(boolean enable) {
        stepButton.setEnabled(enable);
        pngButton.setEnabled(enable);
        txtButton.setEnabled(enable);
        htmlButton.setEnabled(enable);
    }

    /**
     * Текущий результат оцифровки сигнала
     */
    private Result currentResult;

    /**
     * Код, исполняемый в рабочем потоке
     */
    private final Runnable runer = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    updateDeviceSettings();
                    if (continuousMode) {
                        makePicture();
                    } else {
                        if (makeStep) {
                            makeStep = false;
                            makePicture();
                            enableStepButtons(true);
                            continue;
                        }
                        deviceController.getADCResult();
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "OOps", ex);
            }
        }
    };

    /**
     * Сделать картинку. Получить данные, обработать, нарисовать и подстроить.
     *
     * @throws InterruptedException
     */
    void makePicture() throws InterruptedException {
        currentResult = deviceController.getADCResult();
        drawResults();
        autoDcModeAdjust();
        autoLimitModeAdjust();
    }

    /**
     * Отображает результаты
     *
     * @throws InterruptedException
     */
    private void drawResults() throws InterruptedException {
        if (currentResult != null) {
            if (tabbedPane.getSelectedComponent() == scopeParentPanel) {
                Rectangle r = scopeRenderPanel.getBounds();
                if (r.width != 0 && r.height != 0) {
                    scopeRenderer.renderScopeAndUpdateRulers(r.width, r.height, currentResult);
                    scopeRenderPanel.copyImage(currentResult.getScopeImage());
                }
            } else {
                Rectangle r = harmRenderPanel.getBounds();
                if (r.width != 0 && r.height != 0) {
                    scopeRenderer.renderHarmAnalyser(r.width, r.height, currentResult);
                    harmRenderPanel.copyImage(currentResult.getHarmImage());
                }
            }
            drawVoltagesAndTimeFrequency();
        }
    }

    /**
     * Перерисовать картинку
     */
    private void redrawAndMakePicture() {
        try {
            drawResults();
        } catch (InterruptedException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "OOps!", ex);
        }
    }

    /**
     * Запуск рабочего потока
     */
    private void start() {
        if (portsComboBox.getSelectedIndex() != -1) {
            try {
                startButton.setEnabled(false);
                portsComboBox.setEnabled(false);
                searchPortsButton.setEnabled(false);
                stopButton.setEnabled(true);
                if (workThread == null) {
                    workThread = new Thread(runer);
                    workThread.start();
                }
                deviceController.open((String) portsComboBox.getSelectedItem());
                updateDeviceSettings();
            } catch (SerialPortException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка запуска", ex);
            }
        }
    }

    /**
     * Остановка рабочего потока
     */
    private void stop() {
        stopButton.setEnabled(false);
        deviceController.close();
    }

    /**
     * Панель для отображения графика
     */
    private final RenderPanel scopeRenderPanel = new RenderPanel();

    /**
     * Панель для отображения диаграммы гармоник
     */
    private final RenderPanel harmRenderPanel = new RenderPanel();

    /**
     * Отобразить результаты измерений
     */
    void drawVoltagesAndTimeFrequency() {
        vminLabel.setText("Vmin = " + Utils.voltageToString(currentResult.getVMin()));
        vmaxLabel.setText("Vmax = " + Utils.voltageToString(currentResult.getVMax()));
        vppLabel.setText("Vpp = " + Utils.voltageToString(currentResult.getVMax() - currentResult.getVMin()));
        vrmsLabel.setText("Vrms = " + Utils.voltageToString(currentResult.getVRms()));
        deltaVLabel.setText("ΔV = " + Utils.voltageToString(currentResult.getDeltaV()));
        deltaTLabel.setText("ΔT = " + Utils.timeToString(currentResult.getDeltaT()));
        freqLabel.setText("f = " + Utils.frequencyToString(1d / currentResult.getDeltaT()));
        if (tabbedPane.getSelectedComponent() == scopeParentPanel) {
            kHarmLabel.setVisible(false);
        } else {
            kHarmLabel.setText("Kh = " + Utils.valueToPercent(currentResult.getKHarm()));
            kHarmLabel.setVisible(true);
        }
    }

    /**
     * режимы входа
     */
    private static enum InputMode {

        /**
         * Закрытый, переменный ток
         */
        AC,
        /**
         * Заземлён
         */
        GND,
        /**
         * Открытый, постоянный ток
         */
        DC;
    }

    /**
     * Режим входа
     */
    private InputMode inputMode;

    /**
     * Режимы синхронизации
     */
    private static enum SynchMode {

        /**
         * Авто
         */
        AUTO,
        /**
         * Нет
         */
        NONE,
        /**
         * Ручная
         */
        MANUAL
    }

    /**
     * Режим синхронизации
     */
    private SynchMode synchMode;
    /**
     * Синхронизация по фронту
     */
    private boolean triggerMode = true;
    /**
     * Уровень синхронизации
     */
    private int triggerLevel = 100;
    /**
     * Смещение по постоянному току
     */
    private int dcLevel = 125;

    /**
     * Обновить вход на устройстве
     */
    private void updateInputMode() {
        try {
            switch (inputMode) {
                case AC:
                    deviceController.switchInputToAc();
                    break;
                case GND:
                    deviceController.switchInputToGnd();
                    break;
                case DC:
                    deviceController.switchInputToDc();
                    break;
            }
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены входа", ex);
        }
    }

    /**
     * Установить режим входа устройства
     *
     * @param mode режим входа
     */
    private void setInputMode(InputMode mode) {
        inputMode = mode;
    }

    /**
     * Обновить режим синхронизации на устройстве
     */
    private void updateSynchro() {
        try {
            switch (synchMode) {
                case AUTO:
                    deviceController.switchSyncToAuto(triggerMode);
                    break;
                case NONE:
                    deviceController.switchSyncToNone();
                    break;
                case MANUAL:
                    deviceController.switchSyncToLevel(triggerLevel, triggerMode);
                    break;
            }
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены режима синхронизации", ex);
        }
    }

    /**
     * Установить режим синхронизации
     *
     * @param mode режим синхронизации
     */
    private void setSynchMode(SynchMode mode) {
        synchMode = mode;
    }

    /**
     * Установить синхронизацию по фронту или срезу
     *
     * @param mode
     */
    private void setTriggerMode(boolean mode) {
        triggerMode = mode;
    }

    /**
     * Установить уровень синхронизации
     *
     * @param level уровень синхронизации
     */
    private void setTriggerLevel(int level) {
        triggerLevel = level;
    }

    /**
     * Текущий период развёртки
     */
    private int currentPeriod;

    /**
     * Установить период развёртки в устройстве
     */
    private void updatePeriod() {
        try {
            deviceController.switchTime(currentPeriod);
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены времени развёртки", ex);
        }
    }

    /**
     * Установить период развёртки
     *
     * @param periodIndex период развёртки
     */
    private void setPeriod(int periodIndex) {
        currentPeriod = periodIndex;
    }

    /**
     * Текущий предел измерения
     */
    private int currentRange;

    /**
     * Обновить предел измерения на устройстве
     */
    private void updateRange() {
        try {
            deviceController.switchVoltage(currentRange);
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены предела измерений", ex);
        }
    }

    /**
     * Задать предел измерения
     *
     * @param rangeIndex индекс предела измерения
     */
    private void setRange(int rangeIndex) {
        currentRange = rangeIndex;
    }

    /**
     * Обновить смещение входа на устройстве
     */
    private void updateDcOffset() {
        try {
            deviceController.setZeroLevel(dcLevel);
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка изменения смещения", ex);
        }
    }

    /**
     * Установить смещение входа
     *
     * @param offset смещение входа
     */
    private void setDcOffset(int offset) {
        dcLevel = offset;
    }

    /**
     * Обновить настройки устройства
     */
    private void updateDeviceSettings() {
        if (deviceController.isOpen()) {
            updateSynchro();
            updatePeriod();
            updateRange();
            updateDcOffset();
            updateInputMode();
        }
    }

    /**
     * Количество шагов авторегулирования смещения
     */
    private int autoDcSteps;

    /**
     * Среднее отклонение от нуля
     */
    private double autoDcVoltage;

    /**
     * Отрегулировать смещение входа
     */
    private void autoDcModeAdjust() {
        if (autoDcMode) {
            if (currentResult != null) {
                double vmin = currentResult.getVMin();
                double vmax = currentResult.getVMax();
                autoDcVoltage += (vmin + vmax) / 2;
                autoDcSteps++;
                if (autoDcSteps == 10) {
                    autoDcSteps = 0;
                    int sl = dcOffsetSlider.getValue();
                    if (autoDcVoltage > 0) {
                        if (sl > dcOffsetSlider.getMinimum()) {
                            dcOffsetSlider.setValue(sl - 1);
                        }
                    } else {
                        if (sl < dcOffsetSlider.getMaximum()) {
                            dcOffsetSlider.setValue(sl + 1);
                        }
                    }
                    autoDcVoltage = 0;
                }
            }
        }
    }

    /**
     * Количество шагов проверки перегрузки входа
     */
    private int autoLimitSteps;

    /**
     * Счетчик перегрузки входа
     */
    private int overloadCount;

    /**
     * Отрегулировать предел измерения
     */
    private void autoLimitModeAdjust() {
        if (autoRangeMode) {
            if (currentResult != null) {
                if (currentResult.isOverloadSignal()) {
                    overloadCount++;
                }
                autoLimitSteps++;
                if (autoLimitSteps >= Const.OVERLOAD_DETECT_STEPS) {
                    autoLimitSteps = 0;
                    if (overloadCount >= Const.OVERLOAD_COUNT) {
                        upRange();
                    }
                    overloadCount = 0;
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        rangeComboBox = new javax.swing.JComboBox();
        decRangeButton = new javax.swing.JButton();
        incRangeButton = new javax.swing.JButton();
        autoRangeCheckBox = new javax.swing.JCheckBox();
        jPanel5 = new javax.swing.JPanel();
        periodComboBox = new javax.swing.JComboBox();
        decTimeButton = new javax.swing.JButton();
        incTimeButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        inputAcRadioButton = new javax.swing.JRadioButton();
        inputGndRadioButton = new javax.swing.JRadioButton();
        inputDcRadioButton = new javax.swing.JRadioButton();
        jPanel7 = new javax.swing.JPanel();
        synchAutoRadioButton = new javax.swing.JRadioButton();
        synchNoneRadioButton = new javax.swing.JRadioButton();
        synchManualRadioButton = new javax.swing.JRadioButton();
        jPanel8 = new javax.swing.JPanel();
        synchFrontRadioButton = new javax.swing.JRadioButton();
        synchCutRadioButton = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        synchLevelSlider = new javax.swing.JSlider();
        jPanel9 = new javax.swing.JPanel();
        dcOffsetSlider = new javax.swing.JSlider();
        autoDcCheckBox = new javax.swing.JCheckBox();
        jPanel10 = new javax.swing.JPanel();
        startButton = new javax.swing.JButton();
        stopButton = new javax.swing.JButton();
        jPanel13 = new javax.swing.JPanel();
        portsComboBox = new javax.swing.JComboBox();
        searchPortsButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        continuousCheckBox = new javax.swing.JCheckBox();
        stepButton = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
        pngButton = new javax.swing.JButton();
        txtButton = new javax.swing.JButton();
        htmlButton = new javax.swing.JButton();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        jPanel14 = new javax.swing.JPanel();
        autoFreqCheckBox = new javax.swing.JCheckBox();
        autoMeasureCheckBox = new javax.swing.JCheckBox();
        jPanel15 = new javax.swing.JPanel();
        leftOffsetButton = new javax.swing.JButton();
        rightOffsetButton = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();
        vminLabel = new javax.swing.JLabel();
        vmaxLabel = new javax.swing.JLabel();
        vppLabel = new javax.swing.JLabel();
        vrmsLabel = new javax.swing.JLabel();
        deltaVLabel = new javax.swing.JLabel();
        deltaTLabel = new javax.swing.JLabel();
        freqLabel = new javax.swing.JLabel();
        kHarmLabel = new javax.swing.JLabel();
        tabbedPane = new javax.swing.JTabbedPane();
        scopeParentPanel = new javax.swing.JPanel();
        harmParentPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Simplescope v3");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Предел измерения", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        rangeComboBox.setFont(fontScheme.getGuiFont());
        rangeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "50mV", "100mV", "250mV", "500mV", "1V", "2.5V", "5V", "10V", "20V", "50V", "100V" }));
        rangeComboBox.setToolTipText("Выберите предел измерения");
        rangeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rangeComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        jPanel4.add(rangeComboBox, gridBagConstraints);

        decRangeButton.setFont(fontScheme.getGuiFont());
        decRangeButton.setText("-");
        decRangeButton.setToolTipText("Уменьшить предел измерения");
        decRangeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decRangeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel4.add(decRangeButton, gridBagConstraints);

        incRangeButton.setFont(fontScheme.getGuiFont());
        incRangeButton.setText("+");
        incRangeButton.setToolTipText("Увеличить предел измерения");
        incRangeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incRangeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        jPanel4.add(incRangeButton, gridBagConstraints);

        autoRangeCheckBox.setFont(fontScheme.getGuiFont());
        autoRangeCheckBox.setText("Автопереключение");
        autoRangeCheckBox.setToolTipText("Автоматически переключать предел при перегрузке входа");
        autoRangeCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoRangeCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel4.add(autoRangeCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel4, gridBagConstraints);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Период развёртки", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel5.setLayout(new java.awt.GridBagLayout());

        periodComboBox.setFont(fontScheme.getGuiFont());
        periodComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "10µS", "20µS", "50µS", "100µS", "200µS", "500µS", "1mS", "2mS", "5mS", "10mS", "20mS", "50mS", "100mS", "200mS", "500mS", "1s", "2s", "5s" }));
        periodComboBox.setToolTipText("Выберите период развёртки");
        periodComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                periodComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        jPanel5.add(periodComboBox, gridBagConstraints);

        decTimeButton.setFont(fontScheme.getGuiFont());
        decTimeButton.setText("-");
        decTimeButton.setToolTipText("Уменьшить период развёртки");
        decTimeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decTimeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel5.add(decTimeButton, gridBagConstraints);

        incTimeButton.setFont(fontScheme.getGuiFont());
        incTimeButton.setText("+");
        incTimeButton.setToolTipText("Увеличить период развёртки");
        incTimeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incTimeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        jPanel5.add(incTimeButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel5, gridBagConstraints);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Вход", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel6.setLayout(new java.awt.GridBagLayout());

        buttonGroup1.add(inputAcRadioButton);
        inputAcRadioButton.setFont(fontScheme.getGuiFont());
        inputAcRadioButton.setSelected(true);
        inputAcRadioButton.setText("Закр.");
        inputAcRadioButton.setToolTipText("Закрытый вход, переменное напряжение");
        inputAcRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputAcRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel6.add(inputAcRadioButton, gridBagConstraints);

        buttonGroup1.add(inputGndRadioButton);
        inputGndRadioButton.setFont(fontScheme.getGuiFont());
        inputGndRadioButton.setText("Земля");
        inputGndRadioButton.setToolTipText("Заземлить вход");
        inputGndRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputGndRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel6.add(inputGndRadioButton, gridBagConstraints);

        buttonGroup1.add(inputDcRadioButton);
        inputDcRadioButton.setFont(fontScheme.getGuiFont());
        inputDcRadioButton.setText("Откр.");
        inputDcRadioButton.setToolTipText("Открытый вход, любой сигнал");
        inputDcRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputDcRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel6.add(inputDcRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel6, gridBagConstraints);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Синхронизация", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel7.setLayout(new java.awt.GridBagLayout());

        buttonGroup2.add(synchAutoRadioButton);
        synchAutoRadioButton.setFont(fontScheme.getGuiFont());
        synchAutoRadioButton.setSelected(true);
        synchAutoRadioButton.setText("Авто");
        synchAutoRadioButton.setToolTipText("Автоматическая синхронизация");
        synchAutoRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchAutoRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel7.add(synchAutoRadioButton, gridBagConstraints);

        buttonGroup2.add(synchNoneRadioButton);
        synchNoneRadioButton.setFont(fontScheme.getGuiFont());
        synchNoneRadioButton.setText("Нет");
        synchNoneRadioButton.setToolTipText("Без синхронизации");
        synchNoneRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchNoneRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel7.add(synchNoneRadioButton, gridBagConstraints);

        buttonGroup2.add(synchManualRadioButton);
        synchManualRadioButton.setFont(fontScheme.getGuiFont());
        synchManualRadioButton.setText("Ручн.");
        synchManualRadioButton.setToolTipText("Ручная синхронизация");
        synchManualRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchManualRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel7.add(synchManualRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel7, gridBagConstraints);

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Триггер", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel8.setLayout(new java.awt.GridBagLayout());

        buttonGroup3.add(synchFrontRadioButton);
        synchFrontRadioButton.setFont(fontScheme.getGuiFont());
        synchFrontRadioButton.setSelected(true);
        synchFrontRadioButton.setText("Фронт");
        synchFrontRadioButton.setToolTipText("Синхронизировать по нарастанию сигнала");
        synchFrontRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchFrontRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel8.add(synchFrontRadioButton, gridBagConstraints);

        buttonGroup3.add(synchCutRadioButton);
        synchCutRadioButton.setFont(fontScheme.getGuiFont());
        synchCutRadioButton.setText("Спад");
        synchCutRadioButton.setToolTipText("Синхронизхировать по убыванию сигнала");
        synchCutRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchCutRadioButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel8.add(synchCutRadioButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel8, gridBagConstraints);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Уровень синхронизации", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel2.setLayout(new java.awt.GridBagLayout());

        synchLevelSlider.setFont(fontScheme.getGuiFont());
        synchLevelSlider.setMajorTickSpacing(50);
        synchLevelSlider.setMaximum(200);
        synchLevelSlider.setMinorTickSpacing(10);
        synchLevelSlider.setPaintTicks(true);
        synchLevelSlider.setToolTipText("Установить уровень синхронизации");
        synchLevelSlider.setValue(100);
        synchLevelSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                synchLevelSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel2.add(synchLevelSlider, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel2, gridBagConstraints);

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Смещение входа", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel9.setLayout(new java.awt.GridBagLayout());

        dcOffsetSlider.setFont(fontScheme.getGuiFont());
        dcOffsetSlider.setMajorTickSpacing(50);
        dcOffsetSlider.setMaximum(250);
        dcOffsetSlider.setMinorTickSpacing(10);
        dcOffsetSlider.setPaintTicks(true);
        dcOffsetSlider.setToolTipText("Установите смещение входа по постоянному току");
        dcOffsetSlider.setValue(125);
        dcOffsetSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                dcOffsetSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel9.add(dcOffsetSlider, gridBagConstraints);

        autoDcCheckBox.setFont(fontScheme.getGuiFont());
        autoDcCheckBox.setText("Автоотслеживание");
        autoDcCheckBox.setToolTipText("Отслеживать постоянную составляющуу и пытаться компенсировать");
        autoDcCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoDcCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel9.add(autoDcCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel9, gridBagConstraints);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Устройство", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel10.setLayout(new java.awt.GridBagLayout());

        startButton.setFont(fontScheme.getGuiFont());
        startButton.setText("Старт");
        startButton.setToolTipText("Нажмите для запуска работы");
        startButton.setEnabled(false);
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel10.add(startButton, gridBagConstraints);

        stopButton.setFont(fontScheme.getGuiFont());
        stopButton.setText("Стоп");
        stopButton.setToolTipText("Нажмите для остаовки работы");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        jPanel10.add(stopButton, gridBagConstraints);

        jPanel13.setLayout(new java.awt.GridBagLayout());

        portsComboBox.setFont(fontScheme.getGuiFont());
        portsComboBox.setToolTipText("Выберите порт");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel13.add(portsComboBox, gridBagConstraints);

        searchPortsButton.setFont(fontScheme.getGuiFont());
        searchPortsButton.setText("?");
        searchPortsButton.setToolTipText("Нажмите для поиска портов");
        searchPortsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchPortsButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        jPanel13.add(searchPortsButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel10.add(jPanel13, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel10, gridBagConstraints);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Режим работы", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        continuousCheckBox.setFont(fontScheme.getGuiFont());
        continuousCheckBox.setSelected(true);
        continuousCheckBox.setText("Непрерывно");
        continuousCheckBox.setToolTipText("Выберите непрерывный или пошаговый режим");
        continuousCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                continuousCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(continuousCheckBox, gridBagConstraints);

        stepButton.setFont(fontScheme.getGuiFont());
        stepButton.setText("Шаг");
        stepButton.setToolTipText("Нажмите для выполнения следующего шага");
        stepButton.setEnabled(false);
        stepButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel1.add(stepButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel1, gridBagConstraints);

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Сохранить", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel11.setLayout(new java.awt.GridBagLayout());

        pngButton.setFont(fontScheme.getGuiFont());
        pngButton.setText("PNG");
        pngButton.setToolTipText("Сохранить изображение в файл PNG");
        pngButton.setEnabled(false);
        pngButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                pngButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel11.add(pngButton, gridBagConstraints);

        txtButton.setFont(fontScheme.getGuiFont());
        txtButton.setText("TXT");
        txtButton.setToolTipText("Сохранить данные в тектовый файл");
        txtButton.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
        jPanel11.add(txtButton, gridBagConstraints);

        htmlButton.setFont(fontScheme.getGuiFont());
        htmlButton.setText("HTML");
        htmlButton.setToolTipText("Сохранить как веб-страницу");
        htmlButton.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        jPanel11.add(htmlButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel11, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 12;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(filler1, gridBagConstraints);

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Линейки", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel14.setLayout(new java.awt.GridBagLayout());

        autoFreqCheckBox.setFont(fontScheme.getGuiFont());
        autoFreqCheckBox.setText("Авто верт.");
        autoFreqCheckBox.setToolTipText("Автоматически определять период сигнала");
        autoFreqCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoFreqCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel14.add(autoFreqCheckBox, gridBagConstraints);

        autoMeasureCheckBox.setFont(fontScheme.getGuiFont());
        autoMeasureCheckBox.setText("Авто гориз.");
        autoMeasureCheckBox.setToolTipText("Автоматически определять величину сигнала");
        autoMeasureCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                autoMeasureCheckBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel14.add(autoMeasureCheckBox, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 11;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel14, gridBagConstraints);

        jPanel15.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Сдвиг", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel15.setLayout(new java.awt.GridBagLayout());

        leftOffsetButton.setFont(fontScheme.getGuiFont());
        leftOffsetButton.setText("Влево");
        leftOffsetButton.setToolTipText("Сдвинуть график влево");
        leftOffsetButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                leftOffsetButtonMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                leftOffsetButtonMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel15.add(leftOffsetButton, gridBagConstraints);

        rightOffsetButton.setFont(fontScheme.getGuiFont());
        rightOffsetButton.setText("Вправо");
        rightOffsetButton.setToolTipText("Сдвинуть график вправо");
        rightOffsetButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                rightOffsetButtonMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                rightOffsetButtonMouseReleased(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        jPanel15.add(rightOffsetButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel15, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jPanel3, gridBagConstraints);

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Измерения", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, fontScheme.getBorderFont()));
        jPanel12.setLayout(new java.awt.GridLayout(1, 0));

        vminLabel.setFont(fontScheme.getValFont());
        vminLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        vminLabel.setText("Vmin");
        vminLabel.setToolTipText("Величина минимального напряжения");
        jPanel12.add(vminLabel);

        vmaxLabel.setFont(fontScheme.getValFont());
        vmaxLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        vmaxLabel.setText("Vmax");
        vmaxLabel.setToolTipText("Величина максимального напряжения");
        jPanel12.add(vmaxLabel);

        vppLabel.setFont(fontScheme.getValFont());
        vppLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        vppLabel.setText("Vp-p");
        vppLabel.setToolTipText("Разница максимального и минимального напряжений");
        jPanel12.add(vppLabel);

        vrmsLabel.setFont(fontScheme.getValFont());
        vrmsLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        vrmsLabel.setText("Vrms");
        vrmsLabel.setToolTipText("Среднеквадратическое напряжение");
        jPanel12.add(vrmsLabel);

        deltaVLabel.setFont(fontScheme.getValFont());
        deltaVLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        deltaVLabel.setText("ΔV");
        deltaVLabel.setToolTipText("Разница напряжений по горизонтальным линейкам");
        jPanel12.add(deltaVLabel);

        deltaTLabel.setFont(fontScheme.getValFont());
        deltaTLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        deltaTLabel.setText("Δt");
        deltaTLabel.setToolTipText("Величина времени между вертикальными линейками");
        jPanel12.add(deltaTLabel);

        freqLabel.setFont(fontScheme.getValFont());
        freqLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        freqLabel.setText("f");
        freqLabel.setToolTipText("Частота");
        jPanel12.add(freqLabel);

        kHarmLabel.setFont(fontScheme.getValFont());
        kHarmLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        kHarmLabel.setText("Kh");
        kHarmLabel.setToolTipText("Коэффициент гармоник");
        jPanel12.add(kHarmLabel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jPanel12, gridBagConstraints);

        tabbedPane.setFont(fontScheme.getGuiFont());

        scopeParentPanel.setToolTipText("Осциллоскоп");
        scopeParentPanel.setMinimumSize(new java.awt.Dimension(600, 600));
        scopeParentPanel.setPreferredSize(new java.awt.Dimension(600, 600));
        scopeParentPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                scopeParentPanelMouseDragged(evt);
            }
        });
        scopeParentPanel.setLayout(new java.awt.GridLayout(1, 0));
        tabbedPane.addTab("Осциллоскоп", scopeParentPanel);

        harmParentPanel.setToolTipText("Анализ гармоник");
        harmParentPanel.setLayout(new java.awt.GridLayout(1, 0));
        tabbedPane.addTab("Анализ гармоник", harmParentPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(tabbedPane, gridBagConstraints);

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void inputAcRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputAcRadioButtonActionPerformed
        setInputMode(InputMode.AC);
        autoDcCheckBox.setEnabled(true);
    }//GEN-LAST:event_inputAcRadioButtonActionPerformed

    private void inputGndRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputGndRadioButtonActionPerformed
        setInputMode(InputMode.GND);
        autoDcCheckBox.setEnabled(true);
    }//GEN-LAST:event_inputGndRadioButtonActionPerformed

    private void inputDcRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputDcRadioButtonActionPerformed
        setInputMode(InputMode.DC);
        autoDcCheckBox.setSelected(false);
        autoDcCheckBox.setEnabled(false);
        autoDcMode = false;
    }//GEN-LAST:event_inputDcRadioButtonActionPerformed

    private void synchAutoRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchAutoRadioButtonActionPerformed
        setSynchMode(SynchMode.AUTO);
    }//GEN-LAST:event_synchAutoRadioButtonActionPerformed

    private void synchManualRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchManualRadioButtonActionPerformed
        setSynchMode(SynchMode.MANUAL);
    }//GEN-LAST:event_synchManualRadioButtonActionPerformed

    private void synchNoneRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchNoneRadioButtonActionPerformed
        setSynchMode(SynchMode.NONE);
    }//GEN-LAST:event_synchNoneRadioButtonActionPerformed

    private void periodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_periodComboBoxActionPerformed
        setPeriod(periodComboBox.getSelectedIndex());
    }//GEN-LAST:event_periodComboBoxActionPerformed

    private void rangeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rangeComboBoxActionPerformed
        setRange(rangeComboBox.getSelectedIndex());
    }//GEN-LAST:event_rangeComboBoxActionPerformed

    private void dcOffsetSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_dcOffsetSliderStateChanged
        setDcOffset(dcOffsetSlider.getValue());
    }//GEN-LAST:event_dcOffsetSliderStateChanged

    private void synchFrontRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchFrontRadioButtonActionPerformed
        setTriggerMode(true);
    }//GEN-LAST:event_synchFrontRadioButtonActionPerformed

    private void synchCutRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchCutRadioButtonActionPerformed
        setTriggerMode(false);
    }//GEN-LAST:event_synchCutRadioButtonActionPerformed

    private void synchLevelSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_synchLevelSliderStateChanged
        setTriggerLevel(synchLevelSlider.getValue());
    }//GEN-LAST:event_synchLevelSliderStateChanged

    /**
     * Уменьшить предел измерения
     */
    private void downRange() {
        int idx = rangeComboBox.getSelectedIndex();
        if (idx > 0) {
            rangeComboBox.setSelectedIndex(idx - 1);
        }
    }

    private void decRangeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decRangeButtonActionPerformed
        downRange();
    }//GEN-LAST:event_decRangeButtonActionPerformed

    /**
     * Увеличить предел измерения
     */
    private void upRange() {
        int idx = rangeComboBox.getSelectedIndex();
        if (idx < 10) {
            rangeComboBox.setSelectedIndex(idx + 1);
        }
    }

    private void incRangeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incRangeButtonActionPerformed
        upRange();
    }//GEN-LAST:event_incRangeButtonActionPerformed

    private void decTimeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decTimeButtonActionPerformed
        int idx = periodComboBox.getSelectedIndex();
        if (idx > 0) {
            periodComboBox.setSelectedIndex(idx - 1);
        }
    }//GEN-LAST:event_decTimeButtonActionPerformed

    private void incTimeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incTimeButtonActionPerformed
        int idx = periodComboBox.getSelectedIndex();
        if (idx < 17) {
            periodComboBox.setSelectedIndex(idx + 1);
        }
    }//GEN-LAST:event_incTimeButtonActionPerformed

    private void searchPortsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_searchPortsButtonActionPerformed
        searchPorts();
    }//GEN-LAST:event_searchPortsButtonActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        start();
    }//GEN-LAST:event_startButtonActionPerformed

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        stop();
    }//GEN-LAST:event_stopButtonActionPerformed

    /**
     * Непрерывный режим
     */
    private volatile boolean continuousMode = true;

    /**
     * Флажок шага
     */
    private volatile boolean makeStep;

    private void continuousCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_continuousCheckBoxActionPerformed
        continuousMode = continuousCheckBox.isSelected();
        enableStepButtons(!continuousMode);
    }//GEN-LAST:event_continuousCheckBoxActionPerformed

    private void stepButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepButtonActionPerformed
        makeStep = true;
        enableStepButtons(false);
    }//GEN-LAST:event_stepButtonActionPerformed

    /**
     * Режим автоматической подстройки смещения входа
     */
    private volatile boolean autoDcMode;

    private void autoDcCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoDcCheckBoxActionPerformed
        autoDcMode = autoDcCheckBox.isSelected();
    }//GEN-LAST:event_autoDcCheckBoxActionPerformed

    private void pngButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pngButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pngButtonActionPerformed

    private void scopeParentPanelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_scopeParentPanelMouseDragged
        int res = scopeRenderer.addMouseClick(evt.getX(), evt.getY());
        if (res == 1) {
            autoFreqCheckBox.setSelected(false);
            deviceController.setAutoFreq(false);
        } else if (res == 2) {
            autoMeasureCheckBox.setSelected(false);
            deviceController.setAutoMeasure(false);
        }
        if (!continuousMode) {
            redrawAndMakePicture();
        }
    }//GEN-LAST:event_scopeParentPanelMouseDragged

    private void autoFreqCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoFreqCheckBoxActionPerformed
        deviceController.setAutoFreq(autoFreqCheckBox.isSelected());
    }//GEN-LAST:event_autoFreqCheckBoxActionPerformed

    /**
     * Таймер для сдвига влево
     */
    private final Timer leftTimer = new Timer(25, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            deviceController.addTimeOffset(1);
        }
    });

    private void leftOffsetButtonMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_leftOffsetButtonMousePressed
        leftTimer.start();
    }//GEN-LAST:event_leftOffsetButtonMousePressed

    private void leftOffsetButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_leftOffsetButtonMouseReleased
        leftTimer.stop();
    }//GEN-LAST:event_leftOffsetButtonMouseReleased

    /**
     * Таймер для сдвига вправо
     */
    private final Timer rightTimer = new Timer(25, new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            deviceController.addTimeOffset(-1);
        }
    });

    private void rightOffsetButtonMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rightOffsetButtonMousePressed
        rightTimer.start();
    }//GEN-LAST:event_rightOffsetButtonMousePressed

    private void rightOffsetButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_rightOffsetButtonMouseReleased
        rightTimer.stop();
    }//GEN-LAST:event_rightOffsetButtonMouseReleased

    /**
     * Режим автоматического изменения предела измерения
     */
    private boolean autoRangeMode;

    private void autoRangeCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoRangeCheckBoxActionPerformed
        autoRangeMode = autoRangeCheckBox.isSelected();
    }//GEN-LAST:event_autoRangeCheckBoxActionPerformed

    private void autoMeasureCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoMeasureCheckBoxActionPerformed
        deviceController.setAutoMeasure(autoMeasureCheckBox.isSelected());
    }//GEN-LAST:event_autoMeasureCheckBoxActionPerformed

    public static void main(String args[]) {

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox autoDcCheckBox;
    private javax.swing.JCheckBox autoFreqCheckBox;
    private javax.swing.JCheckBox autoMeasureCheckBox;
    private javax.swing.JCheckBox autoRangeCheckBox;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JCheckBox continuousCheckBox;
    private javax.swing.JSlider dcOffsetSlider;
    private javax.swing.JButton decRangeButton;
    private javax.swing.JButton decTimeButton;
    private javax.swing.JLabel deltaTLabel;
    private javax.swing.JLabel deltaVLabel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.JLabel freqLabel;
    private javax.swing.JPanel harmParentPanel;
    private javax.swing.JButton htmlButton;
    private javax.swing.JButton incRangeButton;
    private javax.swing.JButton incTimeButton;
    private javax.swing.JRadioButton inputAcRadioButton;
    private javax.swing.JRadioButton inputDcRadioButton;
    private javax.swing.JRadioButton inputGndRadioButton;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JLabel kHarmLabel;
    private javax.swing.JButton leftOffsetButton;
    private javax.swing.JComboBox periodComboBox;
    private javax.swing.JButton pngButton;
    private javax.swing.JComboBox portsComboBox;
    private javax.swing.JComboBox rangeComboBox;
    private javax.swing.JButton rightOffsetButton;
    private javax.swing.JPanel scopeParentPanel;
    private javax.swing.JButton searchPortsButton;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stepButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JRadioButton synchAutoRadioButton;
    private javax.swing.JRadioButton synchCutRadioButton;
    private javax.swing.JRadioButton synchFrontRadioButton;
    private javax.swing.JSlider synchLevelSlider;
    private javax.swing.JRadioButton synchManualRadioButton;
    private javax.swing.JRadioButton synchNoneRadioButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JButton txtButton;
    private javax.swing.JLabel vmaxLabel;
    private javax.swing.JLabel vminLabel;
    private javax.swing.JLabel vppLabel;
    private javax.swing.JLabel vrmsLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Панель для рисования графика или диаграммы
     */
    private class RenderPanel extends JPanel {

        /**
         * Изображение, которое нужно нарисовать на панели
         */
        private BufferedImage image;

        /**
         * Скопировать на панель изображение
         *
         * @param bi изображение, которое должно рисоваться на этой панели
         * @throws InterruptedException
         */
        void copyImage(BufferedImage bi) throws InterruptedException {
            if (bi != null) {
                if (image == null || image.getWidth() != bi.getWidth() || image.getHeight() != bi.getHeight()) {
                    // новое изображение отличается от старого
                    image = new BufferedImage(bi.getWidth(), bi.getHeight(), bi.getType());
                }
                Graphics g = image.getGraphics();
                g.drawImage(bi, 0, 0, null);
                repaint();
                // вернуть использованное изображение для повторной работы
                scopeRenderer.returnUsedImage(bi);
            }
        }

        /**
         * Перерисовывает панель по требованию системы
         *
         * @param g графический контекст, на котором производится рисования
         */
        @Override
        public void paint(Graphics g) {
            int w = this.getWidth();
            int h = this.getHeight();
            if (image != null) {
                int x = (w - image.getWidth()) / 2;
                int y = (h - image.getHeight()) / 2;
                g.drawImage(image, x, y, null);
                return;
            }
            // если изображение не задано, то просто залить цветом фона
            g.setColor(colorScheme.getBackgroundColor());
            g.fillRect(0, 0, w, h);
        }

    }

    /**
     * Задать параметры элементов управления окна из настроек
     */
    void setupFrameFromProperties() {
        portsComboBox.setSelectedItem(prop.getString(PORT_NAME, ""));
        autoRangeCheckBox.setSelected(prop.getBoolean(AUTO_RANGE_MODE, false));
        autoRangeMode = autoRangeCheckBox.isSelected();
        rangeComboBox.setSelectedItem(prop.getString(RANGE, "10V"));
        periodComboBox.setSelectedItem(prop.getString(PERIOD, "100mS"));
        switch (prop.getInteger(INPUT, 0)) {
            case 0:
                inputAcRadioButton.setSelected(true);
                setInputMode(InputMode.AC);
                autoDcCheckBox.setEnabled(true);
                break;
            case 2:
                inputDcRadioButton.setSelected(true);
                setInputMode(InputMode.DC);
                autoDcCheckBox.setEnabled(false);
                break;
            default:
                inputGndRadioButton.setSelected(true);
                setInputMode(InputMode.GND);
                autoDcCheckBox.setEnabled(true);
        }
        autoDcCheckBox.setSelected(prop.getBoolean(AUTO_DC, false));
        autoDcMode = autoDcCheckBox.isSelected();
        dcOffsetSlider.setValue(prop.getInteger(DC_OFFSET, 125));
        setDcOffset(dcOffsetSlider.getValue());
        switch (prop.getInteger(SYNCH, 0)) {
            case 0:
                synchAutoRadioButton.setSelected(true);
                setSynchMode(SynchMode.AUTO);
                break;
            case 2:
                synchManualRadioButton.setSelected(true);
                setSynchMode(SynchMode.MANUAL);
                break;
            default:
                synchNoneRadioButton.setSelected(true);
                setSynchMode(SynchMode.NONE);
        }
        switch (prop.getInteger(SYNCH_FRONT, 0)) {
            case 0:
                synchFrontRadioButton.setSelected(true);
                setTriggerMode(true);
                break;
            default:
                synchCutRadioButton.setSelected(true);
                setTriggerMode(false);
        }
        synchLevelSlider.setValue(prop.getInteger(SYNCH_LEVEL, 100));
        setTriggerLevel(synchLevelSlider.getValue());
        autoFreqCheckBox.setSelected(prop.getBoolean(AUTO_FREQ, false));
        deviceController.setAutoFreq(autoFreqCheckBox.isSelected());
        autoMeasureCheckBox.setSelected(prop.getBoolean(AUTO_MEASURE, false));
        deviceController.setAutoMeasure(autoMeasureCheckBox.isSelected());
        Rectangle r = new Rectangle();
        r.x = prop.getInteger(X, 0);
        r.y = prop.getInteger(Y, 0);
        r.width = prop.getInteger(W, 640);
        r.height = prop.getInteger(H, 480);
        this.setBounds(r);
        if (prop.getBoolean(MAXIMIZED, true)) {
            setExtendedState(MAXIMIZED_BOTH);
        }
        colorScheme = prop.getColorScheme();
        scopeRenderer.setColorScheme(colorScheme);
    }

    /**
     * Сохранить параметры элементов управления окна в настройки
     */
    void storeFrameToProperties() {
        prop.setString(PORT_NAME, (String) portsComboBox.getSelectedItem());
        prop.setBoolean(AUTO_RANGE_MODE, autoRangeCheckBox.isSelected());
        prop.setString(RANGE, (String) rangeComboBox.getSelectedItem());
        prop.setString(PERIOD, (String) periodComboBox.getSelectedItem());
        if (inputAcRadioButton.isSelected()) {
            prop.setInteger(INPUT, 0);
        } else if (inputDcRadioButton.isSelected()) {
            prop.setInteger(INPUT, 2);
        } else {
            prop.setInteger(INPUT, 1);
        }
        prop.setBoolean(AUTO_DC, autoDcCheckBox.isSelected());
        prop.setInteger(DC_OFFSET, dcOffsetSlider.getValue());
        if (synchAutoRadioButton.isSelected()) {
            prop.setInteger(SYNCH, 0);
        } else if (synchManualRadioButton.isSelected()) {
            prop.setInteger(SYNCH, 2);
        } else {
            prop.setInteger(SYNCH, 1);
        }
        if (synchFrontRadioButton.isSelected()) {
            prop.setInteger(SYNCH_FRONT, 0);
        } else {
            prop.setInteger(SYNCH_FRONT, 1);
        }
        prop.setInteger(SYNCH_LEVEL, synchLevelSlider.getValue());
        prop.setBoolean(AUTO_FREQ, autoFreqCheckBox.isSelected());
        prop.setBoolean(AUTO_MEASURE, autoMeasureCheckBox.isSelected());
        Rectangle r = this.getBounds();
        prop.setInteger(X, r.x);
        prop.setInteger(Y, r.y);
        prop.setInteger(W, r.width);
        prop.setInteger(H, r.height);
        prop.setBoolean(MAXIMIZED, getExtendedState() == MAXIMIZED_BOTH);
        prop.setColorScheme(colorScheme);
    }

}
