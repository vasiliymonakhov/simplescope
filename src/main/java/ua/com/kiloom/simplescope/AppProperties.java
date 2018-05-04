package ua.com.kiloom.simplescope;

import java.awt.Font;
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
public abstract class AppProperties {

    /**
     * Настройки
     */
    private final static Properties prop = new Properties();

    static {
        load();
    }

    /**
     * Получить число
     *
     * @param key ключ
     * @param defaultValue значение по-умолчанию
     * @return значение
     */
    static int getInteger(Keys key, int defaultValue) {
        return Integer.parseInt(prop.getProperty(key.name(), String.valueOf(defaultValue)));
    }

    /**
     * Задать число
     *
     * @param key ключ
     * @param value значение
     */
    static void setInteger(Keys key, int value) {
        prop.setProperty(key.name(), String.valueOf(value));
    }

    /**
     * Получить логическое значение
     *
     * @param key ключ
     * @param defaultValue значение по-умолчанию
     * @return значение
     */
    static boolean getBoolean(Keys key, boolean defaultValue) {
        return "true".equals(prop.getProperty(key.name(), String.valueOf(defaultValue)));
    }

    /**
     * Задать логическое значение
     *
     * @param key ключ
     * @param value значение
     */
    static void setBoolean(Keys key, boolean value) {
        prop.setProperty(key.name(), String.valueOf(value));
    }

    /**
     * Получить строку
     *
     * @param key ключ
     * @param defaultValue значение по-умолчанию
     * @return значение
     */
    static String getString(Keys key, String defaultValue) {
        return prop.getProperty(key.name(), defaultValue);
    }

    /**
     * Задать строку
     *
     * @param key ключ
     * @param value значение
     */
    static void setString(Keys key, String value) {
        if (value != null) {
            prop.setProperty(key.name(), value);
        } else {
            prop.remove(key.name());
        }
    }

    /**
     * Получить цветовую схему
     *
     * @return цветовая схема
     */
    static ColorScheme getColorScheme() {
        return ColorScheme.getScheme(getString(Keys.COLOR_SCHEME, null));
    }

    /**
     * Установить цветовую схему
     *
     * @param value цветовая схема
     */
    static void setColorScheme(ColorScheme value) {
        setString(Keys.COLOR_SCHEME, value.getName());
    }

    /**
     * Получить шрифтовую схему
     *
     * @return шрифтовая схема
     */
    static FontScheme getFontScheme() {
        return new FontScheme(createFont(Keys.GUI_FONT_NAME, Keys.GUI_FONT_SIZE, Keys.GUI_FONT_BOLD, Keys.GUI_FONT_ITALIC),
                createFont(Keys.VAL_FONT_NAME, Keys.VAL_FONT_SIZE, Keys.VAL_FONT_BOLD, Keys.VAL_FONT_ITALIC),
                createFont(Keys.BORDER_FONT_NAME, Keys.BORDER_FONT_SIZE, Keys.BORDER_FONT_BOLD, Keys.BORDER_FONT_ITALIC),
                createFont(Keys.SCOPE_FONT_NAME, Keys.SCOPE_FONT_SIZE, Keys.SCOPE_FONT_BOLD, Keys.SCOPE_FONT_ITALIC));
    }

    /**
     * Сохранить шрифтовую схему
     *
     * @param fontScheme новая шрифтовая схема
     */
    static void setFontScheme(FontScheme fontScheme) {
        saveFont(fontScheme.getGuiFont(), Keys.GUI_FONT_NAME, Keys.GUI_FONT_SIZE, Keys.GUI_FONT_BOLD, Keys.GUI_FONT_ITALIC);
        saveFont(fontScheme.getValFont(), Keys.VAL_FONT_NAME, Keys.VAL_FONT_SIZE, Keys.VAL_FONT_BOLD, Keys.VAL_FONT_ITALIC);
        saveFont(fontScheme.getBorderFont(), Keys.BORDER_FONT_NAME, Keys.BORDER_FONT_SIZE, Keys.BORDER_FONT_BOLD, Keys.BORDER_FONT_ITALIC);
        saveFont(fontScheme.getScopeFont(), Keys.SCOPE_FONT_NAME, Keys.SCOPE_FONT_SIZE, Keys.SCOPE_FONT_BOLD, Keys.SCOPE_FONT_ITALIC);
    }

    /**
     * Создаёт шрифт из настроек
     *
     * @param nameKey ключ имени
     * @param sizeKey ключ размера
     * @param boldKey ключ признака жироного
     * @param italicKey ключ признака наклонного
     * @return созданный шрифт
     */
    private static Font createFont(Keys nameKey, Keys sizeKey, Keys boldKey, Keys italicKey) {
        return new Font(getString(nameKey, "Dialog"),
                (getBoolean(boldKey, false) ? Font.BOLD : 0)
                | (getBoolean(italicKey, false) ? Font.ITALIC : 0),
                getInteger(sizeKey, 12));
    }

    /**
     * Сохраняет шрифт в настройки
     *
     * @param font шрифт
     * @param nameKey ключ имени
     * @param sizeKey ключ размера
     * @param boldKey ключ признака жироного
     * @param italicKey ключ признака наклонного
     */
    private static void saveFont(Font font, Keys nameKey, Keys sizeKey, Keys boldKey, Keys italicKey) {
        setString(nameKey, font.getFamily());
        setBoolean(boldKey, font.isBold());
        setBoolean(italicKey, font.isItalic());
        setInteger(sizeKey, font.getSize());
    }

    /**
     * Имя файла для настроек
     */
    private final static String PROPERTIES_FILE_NAME = "simplescope.properties";

    /**
     * Загрузить из файла
     */
    private static void load() {
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
    static boolean save() {
        try (FileOutputStream fos = new FileOutputStream(PROPERTIES_FILE_NAME)) {
            prop.store(fos, "Simplescope properties");
            return true;
        } catch (IOException ex) {
            Logger.getLogger(AppProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    /**
     * Возвращает количество гармоник для расчёта
     * @return количество гармоник для расчёта
     */
    static int getHarmonicsCount() {
        return getInteger(Keys.HARMONICS_COUNT, Const.HARMONICS_COUNT);
    }

    /**
     * Возвращает количество гармоник для отображения
     * @return количество гармоник для отображения
     */
    static int getHarmonicsRender() {
        return getInteger(Keys.HARMONICS_RENDER, Const.HARMONICS_COUNT);
    }

    /**
     * Отображать гармоники в дБ или %
     * @return true если гармоники отображать в дБ
     */
    static boolean isHarmonicsInDb() {
        return getBoolean(Keys.HARMONICS_DECIBELLS, true);
    }

    /**
     * Устанавливает количество гармоник для расчёта. Чем больше, тем точнее, но и больше
     * требования к вычислительной мощности процессора. Устанавливать в разумных пределах, т.к.
     * из-за ограничений на размер выборки и разрядность АЦП слишком большие значения уже не
     * будут давать точности и даже наоборот.
     * @param cnt количество гармоник для расчёта
     */
    static void setHarmonicsCount(int cnt) {
        setInteger(Keys.HARMONICS_COUNT, cnt);
    }

    /**
     * Устанавливает количество гармоник для отображения на графике. Слишком большое количество
     * может некрасиво отображаться на графике
     * @param cnt количество гармоник для отображения на графике
     */
    static void setHarmonicsRender(int cnt) {
        setInteger(Keys.HARMONICS_RENDER, cnt);
    }

    /**
     * Устанавливает отображение гармоник в дБ или %
     * @param val true если гармоники отображать в дБ
     */
    static void setHarmonicsInDb(boolean val) {
        setBoolean(Keys.HARMONICS_DECIBELLS, val);
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
        COLOR_SCHEME,
        /**
         * Основной шрифт приложения
         */
        GUI_FONT_NAME,
        GUI_FONT_SIZE,
        GUI_FONT_BOLD,
        GUI_FONT_ITALIC,
        /**
         * Шрифт для значений
         */
        VAL_FONT_NAME,
        VAL_FONT_SIZE,
        VAL_FONT_BOLD,
        VAL_FONT_ITALIC,
        /**
         * Шрифт для рамок
         */
        BORDER_FONT_NAME,
        BORDER_FONT_SIZE,
        BORDER_FONT_BOLD,
        BORDER_FONT_ITALIC,
        /**
         * Шрифт для графиков
         */
        SCOPE_FONT_NAME,
        SCOPE_FONT_SIZE,
        SCOPE_FONT_BOLD,
        SCOPE_FONT_ITALIC,
        /**
         * Количество вычисляемых гармоник
         */
        HARMONICS_COUNT,
        /**
         * Количество отображаемых гармоник
         */
        HARMONICS_RENDER,
        /**
         * Отображать шкалу гармоник в децибелах
         */
        HARMONICS_DECIBELLS,
        /**
         * Формат для сохранения изображений
         */
        IMAGE_FORMAT,
        /**
         * Кодировка для файлов с результатами
         */
        TEXT_CHARSET

    }

}
