package ua.com.kiloom.simplescope;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Класс для хранения настроек приложения
 *
 * @author Vasily Monakhov
 */
public class AppProperties {

    /**
     * Настройки
     */
    private final Properties prop = new Properties();

    /**
     * Получить число
     *
     * @param key ключ
     * @param defaultValue значение по-умолчанию
     * @return значение
     */
    int getInteger(Keys key, int defaultValue) {
        return Integer.parseInt(prop.getProperty(key.name(), String.valueOf(defaultValue)));
    }

    /**
     * Задать число
     *
     * @param key ключ
     * @param value значение
     */
    void setInteger(Keys key, int value) {
        prop.setProperty(key.name(), String.valueOf(value));
    }

    /**
     * Получить логическое значение
     *
     * @param key ключ
     * @param defaultValue значение по-умолчанию
     * @return значение
     */
    boolean getBoolean(Keys key, boolean defaultValue) {
        return "true".equals(prop.getProperty(key.name(), String.valueOf(defaultValue)));
    }

    /**
     * Задать логическое значение
     *
     * @param key ключ
     * @param value значение
     */
    void setBoolean(Keys key, boolean value) {
        prop.setProperty(key.name(), String.valueOf(value));
    }

    /**
     * Получить строку
     *
     * @param key ключ
     * @param defaultValue значение по-умолчанию
     * @return значение
     */
    String getString(Keys key, String defaultValue) {
        return prop.getProperty(key.name(), defaultValue);
    }

    /**
     * Задать строку
     *
     * @param key ключ
     * @param value значение
     */
    void setString(Keys key, String value) {
        prop.setProperty(key.name(), value);
    }

    /**
     * Получить цветовую схему
     *
     * @return цветовая схема
     */
    ColorScheme getColorScheme() {
        return ColorScheme.getScheme(getString(Keys.COLOR_SCHEME, null));
    }

    /**
     * Установить цветовую схему
     *
     * @param value цветовая схема
     */
    void setColorScheme(ColorScheme value) {
        setString(Keys.COLOR_SCHEME, value.getName());
    }

    /**
     * Имя файла для настроек
     */
    private final static String PROPERTIES_FILE_NAME = "simplescope.properties";

    /**
     * Загрузить из файла
     */
    void load() {
        try (FileInputStream fis = new FileInputStream(PROPERTIES_FILE_NAME)) {
            prop.load(fis);
        } catch (IOException ex) {
            Logger.getLogger(AppProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Записать в файл
     *
     * @return true если запись успешна
     */
    boolean save() {
        try (FileOutputStream fos = new FileOutputStream(PROPERTIES_FILE_NAME)) {
            prop.store(fos, "Simplescope properties");
            return true;
        } catch (IOException ex) {
            Logger.getLogger(AppProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Ключи для настроек
     */
    static enum Keys {

        /**
         * Имя порта
         */
        PORT_NAME,
        /**
         * режим автовыбора предела измерения
         */
        AUTO_RANGE_MODE,
        /**
         * Предел измерения
         */
        RANGE,
        /**
         * Период
         */
        PERIOD,
        /**
         * Вход
         */
        INPUT,
        /**
         * Автосмещение входа
         */
        AUTO_DC,
        /**
         * Смещение входа
         */
        DC_OFFSET,
        /**
         * Режим синхронизации
         */
        SYNCH,
        /**
         * Синхронизация по фронту
         */
        SYNCH_FRONT,
        /**
         * Уровень синхронизации
         */
        SYNCH_LEVEL,
        /**
         * Автоизмерение периода и частоты
         */
        AUTO_FREQ,
        /**
         * Автоизмерение напряжений сигнала
         */
        AUTO_MEASURE,
        /**
         * Координата окна на экране по горизонтали
         */
        X,
        /**
         * Координата окна на экране по вертикали
         */
        Y,
        /**
         * Ширина окна
         */
        W,
        /**
         * Высота окна
         */
        H,
        /**
         * Окно на весь экран
         */
        MAXIMIZED,
        /**
         * Цветовая схема
         */
        COLOR_SCHEME
    }

}
