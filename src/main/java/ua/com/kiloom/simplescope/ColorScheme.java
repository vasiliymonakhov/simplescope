package ua.com.kiloom.simplescope;

import java.awt.Color;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Класс для цветов графика
 *
 * @author Vasily Monakhov
 */
class ColorScheme {

    /**
     * Цвет фона скопа
     */
    private final Color backgroundColor;
    /**
     * Цвет рамки вокруг поля скопа
     */
    private final Color borderColor;
    /**
     * Цвет сетки скопа
     */
    private final Color gridColor;
    /**
     * Цвет луча
     */
    private final Color rayColor;
    /**
     * Цвет текста
     */
    private final Color textColor;
    /**
     * Цвет линеек
     */
    private final Color rulerColor;

    /**
     * Имя схемы
     */
    private final String name;

    /**
     * Создаёт цветовую схему
     *
     * @param backgroundColor цвет фона скопа
     * @param borderColor цвет рамки вокруг поля скопа
     * @param gridColor цвет сетки скопа
     * @param rayColor цвет луча
     * @param textColor цвет текста
     * @param rulerColor цвет линеек
     */
    private ColorScheme(String name, Color backgroundColor,
            Color borderColor,
            Color gridColor,
            Color rayColor,
            Color textColor,
            Color rulerColor) {
        this.name = name;
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.gridColor = gridColor;
        this.rayColor = rayColor;
        this.textColor = textColor;
        this.rulerColor = rulerColor;
    }

    private final static Color BLACK = Color.BLACK;
    /**
     * Оттенки зелёного
     */
    private final static Color DARK_GREEN = new Color(0, 128, 0);
    private final static Color GREEN = Color.GREEN;
    private final static Color LIGHT_GREEN = new Color(128, 255, 128);
    /**
     * Оттенки оранжевого
     */
    private final static Color DARK_ORANGE = new Color(192, 192, 0);
    private final static Color ORANGE = Color.ORANGE;
    private final static Color LIGHT_ORANGE = new Color(255, 255, 128);
    /**
     * Оттенки серого (и да, их тут не 50!)
     */
    private final static Color DARK_GRAY = new Color(128, 128, 128);
    private final static Color GRAY = new Color(192, 192, 192);
    private final static Color WHITE = Color.WHITE;

    private final static Color RED = Color.RED;
    private final static Color BLUE = Color.BLUE;

    /**
     * Оранжевая почти монохромная схема
     */
    private final static ColorScheme ORANGE_MONO_SCHEME;
    /**
     * Зелёная почти монохромная схема
     */
    final static ColorScheme GREEN_MONO_SCHEME;
    /**
     * Серая монохромная схема
     */
    private final static ColorScheme GRAY_MONO_SCHEME;
    /**
     * Белая монохромная схема
     */
    private final static ColorScheme WHITE_MONO_SCHEME;

    /**
     * Тёмная цветная
     */
    private final static ColorScheme DARK_COLOR_SCHEME;

    /**
     * Светлая цветная
     */
    private final static ColorScheme LIGHT_COLOR_SCHEME;

    /**
     * Карта для хранени я схем по именам
     */
    private final static Map<String, ColorScheme> SCHEMES = new TreeMap<>();

    static {
        GREEN_MONO_SCHEME = new ColorScheme("Изумрудная", BLACK, DARK_GREEN, DARK_GREEN, LIGHT_GREEN, GREEN, GREEN);
        ORANGE_MONO_SCHEME = new ColorScheme("Янтарная", BLACK, DARK_ORANGE, DARK_ORANGE, LIGHT_ORANGE, ORANGE, ORANGE);
        GRAY_MONO_SCHEME = new ColorScheme("Серая", BLACK, DARK_GRAY, DARK_GRAY, WHITE, GRAY, GRAY);
        WHITE_MONO_SCHEME = new ColorScheme("Белая", WHITE, DARK_GRAY, DARK_GRAY, BLACK, DARK_GRAY, DARK_GRAY);
        DARK_COLOR_SCHEME = new ColorScheme("Тёмная", BLACK, GREEN, GREEN, WHITE, BLUE, RED);
        LIGHT_COLOR_SCHEME = new ColorScheme("Светлая", WHITE, GREEN, GREEN, BLACK, BLUE, RED);
        registerScheme(GREEN_MONO_SCHEME);
        registerScheme(ORANGE_MONO_SCHEME);
        registerScheme(GRAY_MONO_SCHEME);
        registerScheme(WHITE_MONO_SCHEME);
        registerScheme(DARK_COLOR_SCHEME);
        registerScheme(LIGHT_COLOR_SCHEME);
    }

    /**
     * Возвращает коллекцию имен схем
     *
     * @return коллекция имен схем
     */
    static Collection<String> getNames() {
        return SCHEMES.keySet();
    }

    /**
     * Возвращает схему по имени
     *
     * @param name имя схемы
     * @return схема
     */
    static ColorScheme getScheme(String name) {
        if (name == null) {
            return GREEN_MONO_SCHEME;
        }
        return SCHEMES.get(name);
    }

    /**
     * Зарегистрировать схему
     *
     * @param scheme схема
     */
    private static void registerScheme(ColorScheme scheme) {
        SCHEMES.put(scheme.name, scheme);
    }

    /**
     * Возвращает цвет фона
     *
     * @return цвет фона
     */
    Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * Возвращает цвет рамки графика
     *
     * @return цвет рамки графика
     */
    Color getBorderColor() {
        return borderColor;
    }

    /**
     * Возвращает цвет сетки
     *
     * @return цвет сетки
     */
    Color getGridColor() {
        return gridColor;
    }

    /**
     * Возвращает цвет луча
     *
     * @return цвет луча
     */
    Color getRayColor() {
        return rayColor;
    }

    /**
     * Возвращает цвет надписей
     *
     * @return цвет надписей
     */
    Color getTextColor() {
        return textColor;
    }

    /**
     * Возвращает цвет линеек
     *
     * @return цвет линеек
     */
    Color getRulerColor() {
        return rulerColor;
    }

    /**
     * Возвращает имя схемы
     *
     * @return имя схемы
     */
    String getName() {
        return name;
    }

}
