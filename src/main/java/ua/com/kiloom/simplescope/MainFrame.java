/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ua.com.kiloom.simplescope;

import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import jssc.SerialPortException;
import jssc.SerialPortList;

/**
 *
 * @author vasya
 */
public class MainFrame extends javax.swing.JFrame {

    /**
     * Creates new form MainFrame
     */
    public MainFrame() {
        initComponents();
        scopeParentPanel.add(sp);
        searchPorts();
    }

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

    private final DeviceController dc = new DeviceController(new Runnable() {
        @Override
        public void run() {
            startButton.setEnabled(true);
            portsComboBox.setEnabled(true);
            searchPortsButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    });

    private final ScopeRenderer sr = new ScopeRenderer();

    private Thread workThread;

    void enableStepButtons(boolean enable) {
        stepButton.setEnabled(enable);
        pngButton.setEnabled(enable);
        txtButton.setEnabled(enable);
        htmlButton.setEnabled(enable);
    }

    private BufferedImage currentImage;

    private ADCResult currentADCResult;

    private final Runnable runer = new Runnable() {
        @Override
        public void run() {
            try {
                while (true) {
                    Rectangle r = sp.getBounds();
                    if (continuousMode) {
                        currentADCResult = dc.getADCResult();
                        autoDcModeAdjust();
                        sp.image = sr.render(r.width, r.height, currentADCResult);
                        sp.repaint();
                    } else {
                        if (makeStep) {
                            makeStep = false;
                            currentADCResult = dc.getADCResult();
                            currentImage = sr.render(r.width, r.height, currentADCResult);
                            sp.image = currentImage;
                            sp.repaint();
                            enableStepButtons(true);
                            continue;
                        }
                        dc.getADCResult();
                    }
                    updateDeviceSettings();
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "OOps", ex);
            }
        }
    };

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
                dc.open((String) portsComboBox.getSelectedItem());
            } catch (SerialPortException ex) {
                Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка запуска", ex);
            }
        }
    }

    private void stop() {
        stopButton.setEnabled(false);
        dc.close();
    }

    private final ScopePanel sp = new ScopePanel();

    private int inputMode;
    private int synchMode;
    private boolean triggerMode;
    private int triggerLevel = 100;
    private int dcLevel = 125;

    private void updateInputMode() {
        try {
            switch (inputMode) {
                case 0:
                    dc.switchInputToAc();
                    break;
                case 1:
                    dc.switchInputToGnd();
                    break;
                case 2:
                    dc.switchInputToDc();
                    break;
            }
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены входа", ex);
        }
    }

    private void setInputMode(int mode) {
        inputMode = mode;
    }

    private void updateSynchro() {
        try {
            switch (synchMode) {
                case 0:
                    dc.switchSyncToAuto(triggerMode);
                    break;
                case 1:
                    dc.switchSyncToNone();
                    break;
                case 2:
                    dc.switchSyncToLevel(triggerLevel, triggerMode);
                    break;
            }
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены режима синхронизации", ex);
        }
    }

    private void setSynchMode(int mode) {
        synchMode = mode;
    }

    private void setTriggerMode(boolean mode) {
        triggerMode = mode;
    }

    private void setTriggerLevel(int level) {
        triggerLevel = level;
    }

    private int currentTime;

    private void updateTime() {
        try {
            dc.switchTime(currentTime);
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены времени развёртки", ex);
        }
    }

    private void setTime(int time) {
        currentTime = time;
    }

    private int currentVoltage;

    private void updateVoltage() {
        try {
            dc.switchVoltage(currentVoltage);
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка смены предела измерений", ex);
        }
    }

    private void setVoltage(int volt) {
        currentVoltage = volt;
    }

    void updateDcOffset() {
        try {
            dc.setZeroLevel(dcLevel);
        } catch (SerialPortException ex) {
            Logger.getLogger(MainFrame.class.getName()).log(Level.SEVERE, "Ошибка изменения смещения", ex);
        }
    }

    private void setDcOffset(int offset) {
        dcLevel = offset;
    }

    private void updateDeviceSettings() {
        if (dc.isOpen()) {
            updateTime();
            updateVoltage();
            updateSynchro();
            updateInputMode();
            updateDcOffset();
        }
    }

    private int autoDcSteps;

    private double autoDcVoltage;

    private void autoDcModeAdjust() {
        if (autoDcMode) {
            if (currentADCResult != null) {
                double vmin = currentADCResult.getVMin();
                double vmax = currentADCResult.getVMax();
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

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        scopeParentPanel = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        rangeComboBox = new javax.swing.JComboBox();
        decRangeButton = new javax.swing.JButton();
        incRangeButton = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        timeComboBox = new javax.swing.JComboBox();
        decTimeButton = new javax.swing.JButton();
        incTimeButton = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        inputAcRadioButton = new javax.swing.JRadioButton();
        inputGndRadioButton = new javax.swing.JRadioButton();
        inputDcRadioButton = new javax.swing.JRadioButton();
        jPanel7 = new javax.swing.JPanel();
        syncAutoRadioButton = new javax.swing.JRadioButton();
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
        portsComboBox = new javax.swing.JComboBox();
        searchPortsButton = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        continuousCheckBox = new javax.swing.JCheckBox();
        stepButton = new javax.swing.JButton();
        jPanel12 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        pngButton = new javax.swing.JButton();
        txtButton = new javax.swing.JButton();
        htmlButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Simplescope v3");
        getContentPane().setLayout(new java.awt.GridBagLayout());

        scopeParentPanel.setLayout(new java.awt.GridLayout(1, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(scopeParentPanel, gridBagConstraints);

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel3.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Предел измерения"));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        rangeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "50mV", "100mV", "250mV", "500mV", "1V", "2.5V", "5V", "10V", "20V", "50V", "100V" }));
        rangeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rangeComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel4.add(rangeComboBox, gridBagConstraints);

        decRangeButton.setText("-");
        decRangeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decRangeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel4.add(decRangeButton, gridBagConstraints);

        incRangeButton.setText("+");
        incRangeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incRangeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel4.add(incRangeButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel4, gridBagConstraints);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Период развёртки"));
        jPanel5.setLayout(new java.awt.GridBagLayout());

        timeComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "10µS", "20µS", "50µS", "100µS", "200µS", "500µS", "1mS", "2mS", "5mS", "10mS", "20mS", "50mS", "100mS", "200mS", "500mS", "1s", "2s", "5s" }));
        timeComboBox.setToolTipText("");
        timeComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                timeComboBoxActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel5.add(timeComboBox, gridBagConstraints);

        decTimeButton.setText("-");
        decTimeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                decTimeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel5.add(decTimeButton, gridBagConstraints);

        incTimeButton.setText("+");
        incTimeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                incTimeButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel5.add(incTimeButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel5, gridBagConstraints);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Вход"));
        jPanel6.setLayout(new java.awt.GridLayout(1, 0));

        buttonGroup1.add(inputAcRadioButton);
        inputAcRadioButton.setSelected(true);
        inputAcRadioButton.setText("Закр.");
        inputAcRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputAcRadioButtonActionPerformed(evt);
            }
        });
        jPanel6.add(inputAcRadioButton);

        buttonGroup1.add(inputGndRadioButton);
        inputGndRadioButton.setText("Земля");
        inputGndRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputGndRadioButtonActionPerformed(evt);
            }
        });
        jPanel6.add(inputGndRadioButton);

        buttonGroup1.add(inputDcRadioButton);
        inputDcRadioButton.setText("Откр.");
        inputDcRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputDcRadioButtonActionPerformed(evt);
            }
        });
        jPanel6.add(inputDcRadioButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel6, gridBagConstraints);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Синхронизация"));
        jPanel7.setLayout(new java.awt.GridLayout(1, 0));

        buttonGroup2.add(syncAutoRadioButton);
        syncAutoRadioButton.setSelected(true);
        syncAutoRadioButton.setText("Авто");
        syncAutoRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                syncAutoRadioButtonActionPerformed(evt);
            }
        });
        jPanel7.add(syncAutoRadioButton);

        buttonGroup2.add(synchNoneRadioButton);
        synchNoneRadioButton.setText("Нет");
        synchNoneRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchNoneRadioButtonActionPerformed(evt);
            }
        });
        jPanel7.add(synchNoneRadioButton);

        buttonGroup2.add(synchManualRadioButton);
        synchManualRadioButton.setText("Ручн.");
        synchManualRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchManualRadioButtonActionPerformed(evt);
            }
        });
        jPanel7.add(synchManualRadioButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel7, gridBagConstraints);

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("Триггер"));
        jPanel8.setLayout(new java.awt.GridLayout(1, 0));

        buttonGroup3.add(synchFrontRadioButton);
        synchFrontRadioButton.setSelected(true);
        synchFrontRadioButton.setText("Фронт");
        synchFrontRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchFrontRadioButtonActionPerformed(evt);
            }
        });
        jPanel8.add(synchFrontRadioButton);

        buttonGroup3.add(synchCutRadioButton);
        synchCutRadioButton.setText("Спад");
        synchCutRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                synchCutRadioButtonActionPerformed(evt);
            }
        });
        jPanel8.add(synchCutRadioButton);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel3.add(jPanel8, gridBagConstraints);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Уровень синхронизации"));
        jPanel2.setLayout(new java.awt.GridBagLayout());

        synchLevelSlider.setMajorTickSpacing(50);
        synchLevelSlider.setMaximum(200);
        synchLevelSlider.setMinorTickSpacing(10);
        synchLevelSlider.setPaintTicks(true);
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

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder("Смещение входа"));
        jPanel9.setLayout(new java.awt.GridBagLayout());

        dcOffsetSlider.setMajorTickSpacing(50);
        dcOffsetSlider.setMaximum(250);
        dcOffsetSlider.setMinorTickSpacing(10);
        dcOffsetSlider.setPaintTicks(true);
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

        autoDcCheckBox.setText("Авто");
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

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Устройство"));
        jPanel10.setLayout(new java.awt.GridBagLayout());

        startButton.setText("Старт");
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
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel10.add(startButton, gridBagConstraints);

        stopButton.setText("Стоп");
        stopButton.setEnabled(false);
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.5;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel10.add(stopButton, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 0);
        jPanel10.add(portsComboBox, gridBagConstraints);

        searchPortsButton.setText("?");
        searchPortsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                searchPortsButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 0, 5, 5);
        jPanel10.add(searchPortsButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel10, gridBagConstraints);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Режим работы"));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        continuousCheckBox.setSelected(true);
        continuousCheckBox.setText("Непрерывно");
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
        jPanel1.add(continuousCheckBox, gridBagConstraints);

        stepButton.setText("Шаг");
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
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(jPanel12, gridBagConstraints);

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder("Сохранить"));
        jPanel11.setLayout(new java.awt.GridBagLayout());

        pngButton.setText("PNG");
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
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel11.add(pngButton, gridBagConstraints);

        txtButton.setText("TXT");
        txtButton.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel11.add(txtButton, gridBagConstraints);

        htmlButton.setText("HTML");
        htmlButton.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
        jPanel11.add(htmlButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        jPanel3.add(jPanel11, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(jPanel3, gridBagConstraints);

        setSize(new java.awt.Dimension(835, 662));
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void inputAcRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputAcRadioButtonActionPerformed
        setInputMode(0);
        autoDcCheckBox.setEnabled(true);
    }//GEN-LAST:event_inputAcRadioButtonActionPerformed

    private void inputGndRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputGndRadioButtonActionPerformed
        setInputMode(1);
        autoDcCheckBox.setEnabled(true);
    }//GEN-LAST:event_inputGndRadioButtonActionPerformed

    private void inputDcRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_inputDcRadioButtonActionPerformed
        setInputMode(2);
        autoDcCheckBox.setEnabled(false);
        autoDcCheckBox.setSelected(false);
    }//GEN-LAST:event_inputDcRadioButtonActionPerformed

    private void syncAutoRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_syncAutoRadioButtonActionPerformed
        setSynchMode(0);
    }//GEN-LAST:event_syncAutoRadioButtonActionPerformed

    private void synchManualRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchManualRadioButtonActionPerformed
        setSynchMode(2);
    }//GEN-LAST:event_synchManualRadioButtonActionPerformed

    private void synchNoneRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_synchNoneRadioButtonActionPerformed
        setSynchMode(1);
    }//GEN-LAST:event_synchNoneRadioButtonActionPerformed

    private void timeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_timeComboBoxActionPerformed
        setTime(timeComboBox.getSelectedIndex());
    }//GEN-LAST:event_timeComboBoxActionPerformed

    private void rangeComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rangeComboBoxActionPerformed
        setVoltage(rangeComboBox.getSelectedIndex());
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

    private void decRangeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decRangeButtonActionPerformed
        int idx = rangeComboBox.getSelectedIndex();
        if (idx > 0) {
            rangeComboBox.setSelectedIndex(idx - 1);
        }
    }//GEN-LAST:event_decRangeButtonActionPerformed

    private void incRangeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incRangeButtonActionPerformed
        int idx = rangeComboBox.getSelectedIndex();
        if (idx < 10) {
            rangeComboBox.setSelectedIndex(idx + 1);
        }
    }//GEN-LAST:event_incRangeButtonActionPerformed

    private void decTimeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_decTimeButtonActionPerformed
        int idx = timeComboBox.getSelectedIndex();
        if (idx > 0) {
            timeComboBox.setSelectedIndex(idx - 1);
        }
    }//GEN-LAST:event_decTimeButtonActionPerformed

    private void incTimeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_incTimeButtonActionPerformed
        int idx = timeComboBox.getSelectedIndex();
        if (idx < 17) {
            timeComboBox.setSelectedIndex(idx + 1);
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

    private volatile boolean continuousMode = true;

    private volatile boolean makeStep;

    private void continuousCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_continuousCheckBoxActionPerformed
        continuousMode = continuousCheckBox.isSelected();
        enableStepButtons(!continuousMode);
    }//GEN-LAST:event_continuousCheckBoxActionPerformed

    private void stepButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepButtonActionPerformed
        makeStep = true;
        enableStepButtons(false);
    }//GEN-LAST:event_stepButtonActionPerformed

    private volatile boolean autoDcMode;

    private void autoDcCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_autoDcCheckBoxActionPerformed
        autoDcMode = autoDcCheckBox.isSelected();
    }//GEN-LAST:event_autoDcCheckBoxActionPerformed

    private void pngButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_pngButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_pngButtonActionPerformed

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
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JCheckBox continuousCheckBox;
    private javax.swing.JSlider dcOffsetSlider;
    private javax.swing.JButton decRangeButton;
    private javax.swing.JButton decTimeButton;
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
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JButton pngButton;
    private javax.swing.JComboBox portsComboBox;
    private javax.swing.JComboBox rangeComboBox;
    private javax.swing.JPanel scopeParentPanel;
    private javax.swing.JButton searchPortsButton;
    private javax.swing.JButton startButton;
    private javax.swing.JButton stepButton;
    private javax.swing.JButton stopButton;
    private javax.swing.JRadioButton syncAutoRadioButton;
    private javax.swing.JRadioButton synchCutRadioButton;
    private javax.swing.JRadioButton synchFrontRadioButton;
    private javax.swing.JSlider synchLevelSlider;
    private javax.swing.JRadioButton synchManualRadioButton;
    private javax.swing.JRadioButton synchNoneRadioButton;
    private javax.swing.JComboBox timeComboBox;
    private javax.swing.JButton txtButton;
    // End of variables declaration//GEN-END:variables

    private class ScopePanel extends JPanel {

        private BufferedImage image;

        @Override
        public void paint(Graphics g) {
            int w = this.getWidth();
            int h = this.getHeight();
            if (image != null) {
                int x = (w - image.getWidth()) / 2;
                int y = (h - image.getHeight()) / 2;
                g.drawImage(image, x, y, null);
            }
        }

    }

}
