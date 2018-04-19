package ua.com.kiloom.simplescope;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Класс для рисования графика
 *
 * @author Vasily Monakhov
 */
class ScopeRenderer {

    /**
     * Класс для поддержки цветовых схем
     */
    static class ColorScheme {

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
         * Создаёт цветовую схему
         *
         * @param backgroundColor цвет фона скопа
         * @param borderColor цвет рамки вокруг поля скопа
         * @param gridColor цвет сетки скопа
         * @param rayColor цвет луча
         * @param textColor цвет текста
         * @param rulerColor цвет линеек
         */
        ColorScheme(Color backgroundColor,
                Color borderColor,
                Color gridColor,
                Color rayColor,
                Color textColor,
                Color rulerColor) {
            this.backgroundColor = backgroundColor;
            this.borderColor = borderColor;
            this.gridColor = gridColor;
            this.rayColor = rayColor;
            this.textColor = textColor;
            this.rulerColor = rulerColor;
        }
    }

    /**
     * Зелёная почти монохромная схема
     */
    private final static ColorScheme GREEN_MONO_SCHEME;

    /**
     * Оттенки зелёного
     */
    private final static Color DARK_GREEN = new Color(0, 128, 0);
    private final static Color GREEN = Color.GREEN;
    private final static Color BLACK = Color.BLACK;
    private final static Color LIGHT_GREEN = new Color(128, 255, 128);

    static {
        GREEN_MONO_SCHEME = new ColorScheme(BLACK, DARK_GREEN, DARK_GREEN, GREEN, LIGHT_GREEN, LIGHT_GREEN);
    }

    /**
     * Цветовая схема скопа
     */
    private ColorScheme colorScheme = GREEN_MONO_SCHEME;

    /**
     * Нажим для ристования луча
     */
    private final Stroke rayStroke = new BasicStroke(2);

    /**
     * Точка на экране
     */
    private static class Point {

        /**
         * Ось абцисс, слева направо
         */
        int x;
        /**
         * Ось ординат сверху вниз
         */
        int y;

        /**
         * Создаёт точку
         *
         * @param x ось абцисс
         * @param y ось ординат
         */
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Преобразует набор результатов измерения АЦП устройства в набор точек
     * графика
     *
     * @param x_pos смещение графика по оси абцисс
     * @param y_pos смещение графика по оси ординат
     * @param width ширина области рисования
     * @param height высота области рисования
     * @param adcResult резултаты измерения сигнала АЦП устроства
     * @return массив точек графика сигнала для отрисовки
     */
    private Point[] convertAdcResultToScopePoints(int x_pos, int y_pos, int width, int height, ADCResult adcResult) {
        int length = adcResult.getAdcData().length;
        Point[] points = new Point[length];
        // вычислить масштаб по оси абцисс в зависимости от ширины области рисования
        double xScale = ((double) width) / length;
        // вычислить масштаб по оси ординат в зависимости от высоты области рисования
        double yScale = ((double) height) / 4096;
        for (int i = 0; i < length; i++) {
            int x = (int) (Math.round(xScale * i));
            int y = (int) (Math.round(yScale * (4095 - adcResult.getAdcData()[i])));
            points[i] = new Point(x + x_pos, y + y_pos);
        }
        return points;
    }

    /**
     * Отступ для рисования графика по веритикали
     */
    private final static int V_GAP = 64;

    /**
     * Отступ для рисования графика по горизонтали
     */
    private final static int H_GAP = 96;

    /**
     * Размер шрифта для шкалы
     */
    private final static int fontSize = 14;
    private final Font scopeFont = new Font("Arial", 0, fontSize);

    /**
     * Рисует график сигнала
     *
     * @param imageWidth ширина области рисования
     * @param imageHeight высота области рисования
     * @param adcResult набор данных от АЦП устройства
     * @return отрисованное изображение
     */
    BufferedImage render(int imageWidth, int imageHeight, ADCResult adcResult) {
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setFont(scopeFont);

        // залить цветом фона
        g.setColor(colorScheme.backgroundColor);
        g.fillRect(0, 0, imageWidth - 1, imageHeight - 1);

        // сделать небольшие отступы и чтобы высота и ширина были кратны 10
        int width = ((imageWidth - 2 * H_GAP) / 10) * 10;
        int height = ((imageHeight - 2 * V_GAP) / 10) * 10;
        // вычислить координаты левого верхнего угла графика
        int x_pos = (imageWidth - width) / 2;
        int y_pos = (imageHeight - height) / 2;

        // нарисовать сетку
        int time = 0;
        int dtime = adcResult.getTimePerCell();
        String timeStr = adcResult.getTimeString();
        for (int i = 0; i <= width; i += width / 10) {
            g.setColor(colorScheme.gridColor);
            g.drawLine(x_pos + i, y_pos, x_pos + i, y_pos + height);
            drawCenteredString(g, String.format("%d%s", time, timeStr), x_pos + i, y_pos + height + V_GAP / 2, colorScheme.textColor);
            time += dtime;
        }

        int dvoltage = adcResult.getVoltagePerCell();
        int voltage = 5 * dvoltage;
        String voltageStr = adcResult.getVoltageString();
        for (int i = 0; i <= height; i += height / 10) {
            g.setColor(colorScheme.gridColor);
            g.drawLine(x_pos, y_pos + i, x_pos + width, y_pos + i);
            drawCenteredString(g, String.format("%d%s", voltage, voltageStr), x_pos - H_GAP / 2, y_pos + i, colorScheme.textColor);
            voltage -= dvoltage;
        }

        // нарисовать рамку
        g.setColor(colorScheme.borderColor);
        g.drawRect(x_pos, y_pos, width, height);

        // Нарисовать луч
        g.setColor(colorScheme.rayColor);
        g.setStroke(rayStroke);
        Point[] points = convertAdcResultToScopePoints(x_pos, y_pos, width, height, adcResult);
        for (int i = 0; i < points.length - 1; i++) {
            int x1 = points[i].x;
            int y1 = points[i].y;
            int x2 = points[i + 1].x;
            int y2 = points[i + 1].y;
            g.drawLine(x1, y1, x2, y2);
        }

        // Нарисовать напряжения
        int dx = imageWidth / 5;
        int tx = 0;
        drawCenteredString(g, "Vmin = " + voltageToString(adcResult.getVMin()), tx += dx, V_GAP / 2, colorScheme.textColor);
        drawCenteredString(g, "Vmax = " + voltageToString(adcResult.getVMax()), tx += dx, V_GAP / 2, colorScheme.textColor);
        drawCenteredString(g, "Vpp = " + voltageToString(adcResult.getVMax() - adcResult.getVMin()), tx += dx, V_GAP / 2, colorScheme.textColor);
        drawCenteredString(g, "Vrms = " + voltageToString(adcResult.getVRms()), tx += dx, V_GAP / 2, colorScheme.textColor);

        g.dispose();
        return image;
    }

    /**
     * Рисует строку
     *
     * @param g графический контекст
     * @param str строка
     * @param centerX координата оси абцисс центра надписи
     * @param centerY координата оси ординат центра надписи
     * @param faceColor цвет надписи
     */
    void drawCenteredString(Graphics2D g, String str, int centerX, int centerY, Color faceColor) {
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(str, g);
        g.setColor(faceColor);
        g.drawString(str, (int) (centerX - r.getCenterX()), (int) (centerY - r.getCenterY()));
    }

    /**
     * Преобразует напряжение в строку с точностью 2 знака после запятой. Если
     * абсолютная величина напряжения менее 1В, то результат выводится в
     * милливольтах, иначе в вольтах
     *
     * @param voltage напряжение
     * @return результирующая строка
     */
    String voltageToString(double voltage) {
        String prefix = "";
        double val = voltage;
        if (Math.abs(voltage) < 1) {
            prefix = "m";
            val = voltage * 1000d;
        }
        return String.format("%3.2f%sV", val, prefix);
    }

}
