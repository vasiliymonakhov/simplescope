package ua.com.kiloom.simplescope;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
        GREEN_MONO_SCHEME = new ColorScheme(BLACK, DARK_GREEN, DARK_GREEN, LIGHT_GREEN, GREEN, DARK_GREEN);
    }

    /**
     * Цветовая схема скопа
     */
    private ColorScheme colorScheme = GREEN_MONO_SCHEME;

    /**
     * Нажим для рисования прямых
     */
    private final Stroke normalStroke = new BasicStroke(1);

    /**
     * Нажим для рисования сетки
     */
    private final static float[] dashingPattern1 = {3f, 3f};

    private final Stroke gridStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_MITER, 1.0f, dashingPattern1, 2.0f);

    /**
     * Нажим для рисования луча
     */
    private final Stroke rayStroke = new BasicStroke(3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND);

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
     * масштаб по оси абцисс в зависимости от ширины области рисования
     */
    private double xScale;

    /**
     * масштаб по оси ординат в зависимости от высоты области рисования
     */
    private double yScale;

    /**
     * Преобразует набор результатов измерения АЦП устройства в набор точек
     * графика
     *
     * @return массив точек графика сигнала для отрисовки
     */
    private Point[] convertAdcResultToScopePoints() {
        Point[] points = new Point[Const.ADC_DATA_BLOCK_SIZE];
        // вычислить масштаб по оси абцисс в зависимости от ширины области рисования
        xScale = ((double) width) / Const.ADC_DATA_BLOCK_SIZE;
        // вычислить масштаб по оси ординат в зависимости от высоты области рисования
        yScale = ((double) height) / Const.ADC_RANGE;
        // вычислить все точки графика
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            int x = (int) Math.round(xScale * i + x_pos);
            int y = (int) Math.round(yScale * (Const.ADC_MAX - result.getAdcData()[i]) + y_pos);
            points[i] = new Point(x, y);
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
    private final static int fontSize = 13;

    private final Font scopeFont = new Font("Arial", Font.BOLD, fontSize);

    int width;

    int height;

    int x_pos;

    int y_pos;

    private Result result;

    /**
     * Рисует график сигнала
     *
     * @param imageWidth ширина области рисования
     * @param imageHeight высота области рисования
     * @param result набор данных от АЦП устройства
     */
    void renderAndUpdateRulers(int imageWidth, int imageHeight, Result result) throws InterruptedException {
        this.result = result;
        BufferedImage image = getImage(imageWidth, imageHeight);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setFont(scopeFont);

        // залить цветом фона
        g.setColor(colorScheme.backgroundColor);
        g.fillRect(0, 0, imageWidth - 1, imageHeight - 1);

        // сделать небольшие отступы и чтобы высота и ширина были кратны 10
        width = ((imageWidth - 2 * H_GAP) / 10) * 10;
        height = ((imageHeight - 2 * V_GAP) / 10) * 10;
        // вычислить координаты левого верхнего угла графика
        x_pos = (imageWidth - width) / 2;
        y_pos = (imageHeight - height) / 2;

        // нарисовать сетку
        g.setStroke(gridStroke);
        int time = 0;
        int dtime = result.getTimePerCell();
        String timeStr = result.getTimeString();
        for (int i = 0; i <= width; i += width / 10) {
            g.setColor(colorScheme.gridColor);
            g.drawLine(x_pos + i, y_pos, x_pos + i, y_pos + height);
            drawCenteredString(g, String.format("%d%s", time, timeStr), x_pos + i, y_pos + height + V_GAP / 2, colorScheme.textColor);
            time += dtime;
        }

        int dvoltage = result.getVoltagePerCell();
        int voltage = 5 * dvoltage;
        String voltageStr = result.getVoltageString();
        for (int i = 0; i <= height; i += height / 10) {
            g.setColor(colorScheme.gridColor);
            g.drawLine(x_pos, y_pos + i, x_pos + width, y_pos + i);
            drawCenteredString(g, String.format("%d%s", voltage, voltageStr), x_pos - H_GAP / 2, y_pos + i, colorScheme.textColor);
            voltage -= dvoltage;
        }

        g.setStroke(normalStroke);
        // нарисовать линейки
        drawRulers(g);

        // нарисовать рамку
        g.setColor(colorScheme.borderColor);
        g.drawRect(x_pos, y_pos, width, height);

        // Нарисовать луч
        g.setColor(colorScheme.rayColor);
        g.setStroke(rayStroke);
        Point[] points = convertAdcResultToScopePoints();
        for (int i = 0; i < points.length - 1; i++) {
            int x1 = points[i].x;
            int y1 = points[i].y;
            int x2 = points[i + 1].x;
            int y2 = points[i + 1].y;
            g.drawLine(x1, y1, x2, y2);
        }

        g.dispose();
        result.setImage(image);
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
     * Очередь для повторного использования изображений. Это позволяет экономить
     * память и меньше мусорить.
     */
    private final BlockingQueue<BufferedImage> imagesQueue = new LinkedBlockingQueue<>();

    /**
     * Возвращает изображение из очереди, если там нет или изображения не
     * подходят по размеру (изменились размеры главного окна), очередь очищается
     * и создаётся новое изображение.
     *
     * @param imageWidth требуемая ширина
     * @param imageHeight требуемая высота
     * @return изображение
     * @throws InterruptedException
     */
    private BufferedImage getImage(int imageWidth, int imageHeight) throws InterruptedException {
        if (imagesQueue.isEmpty()) {
            return createNewImage(imageWidth, imageHeight);
        } else {
            BufferedImage getted = imagesQueue.take();
            if (getted.getWidth() != imageWidth || getted.getHeight() != imageHeight) {
                imagesQueue.clear();
                return createNewImage(imageWidth, imageHeight);
            }
            return getted;
        }
    }

    /**
     * Создаёт новое извображение
     *
     * @param imageWidth требуемая ширина
     * @param imageHeight требуемая высота
     * @return созданное изображение
     */
    private BufferedImage createNewImage(int imageWidth, int imageHeight) {
        GraphicsConfiguration gfx_config = GraphicsEnvironment.
                getLocalGraphicsEnvironment().getDefaultScreenDevice().
                getDefaultConfiguration();
        BufferedImage image = gfx_config.createCompatibleImage(imageWidth, imageHeight, Transparency.OPAQUE);
        image.setAccelerationPriority(1);
        return image;
    }

    /**
     * Возвращает изображение в очередь
     *
     * @param image изображение
     * @throws InterruptedException
     */
    void returnUsedImage(BufferedImage image) throws InterruptedException {
        imagesQueue.put(image);
    }

    private int leftRuler = Const.ADC_DATA_BLOCK_SIZE / 4;
    private int rightRuler = Const.ADC_DATA_BLOCK_SIZE * 3 / 4;
    private int upperRuler = Const.ADC_RANGE * 3 / 4;
    private int lowerRuler = Const.ADC_RANGE / 4;

    void addMouseClick(int x, int y) {
        if (x >= x_pos && x <= x_pos + width && y >= y_pos && y <= y_pos + height) {

            int dLeftRuler = Math.abs(xLeftRuler - x);
            int dRightRuler = Math.abs(xRightRuler - x);
            int dUpperRuler = Math.abs(yUpperRuler - y);
            int dLowerRuler = Math.abs(yLowerRuler - y);

            int[] distances = new int[4];
            distances[0] = dLeftRuler;
            distances[1] = dRightRuler;
            distances[2] = dUpperRuler;
            distances[3] = dLowerRuler;
            Arrays.sort(distances);
            int min = distances[0];

            if (min == dLeftRuler) {
                leftRuler = xToHScale(x);
            } else if (min == dRightRuler) {
                rightRuler = xToHScale(x);
            } else if (min == dUpperRuler) {
                upperRuler = yToVScale(y);
            } else {
                // min == dLowerRuler
                lowerRuler = yToVScale(y);
            }

        }
    }

    private int xToHScale(int x) {
        return (int) Math.round((x - x_pos) / xScale);
    }

    private int yToVScale(int y) {
        return Const.ADC_MAX - (int) Math.round((y - y_pos) / yScale);
    }

    private int xLeftRuler;
    private int xRightRuler;
    private int yUpperRuler;
    private int yLowerRuler;

    void drawRulers(Graphics2D g) {
        xLeftRuler = (int) Math.round(xScale * leftRuler + x_pos);
        xRightRuler = (int) Math.round(xScale * rightRuler + x_pos);
        yUpperRuler = (int) Math.round(yScale * (Const.ADC_MAX - upperRuler) + y_pos);
        yLowerRuler = (int) Math.round(yScale * (Const.ADC_MAX - lowerRuler) + y_pos);
        g.setColor(colorScheme.rulerColor);
        g.drawLine(xLeftRuler, y_pos, xLeftRuler, y_pos + height);
        g.drawLine(xRightRuler, y_pos, xRightRuler, y_pos + height);
        g.drawLine(x_pos, yUpperRuler, x_pos + width, yUpperRuler);
        g.drawLine(x_pos, yLowerRuler, x_pos + width, yLowerRuler);

        drawCenteredString(g, hRulerToString(leftRuler), xLeftRuler, V_GAP / 2, colorScheme.textColor);
        drawCenteredString(g, hRulerToString(rightRuler), xRightRuler, V_GAP / 2, colorScheme.textColor);

        drawCenteredString(g, vRulerToString(upperRuler), x_pos + width + H_GAP / 2, yUpperRuler, colorScheme.textColor);
        drawCenteredString(g, vRulerToString(lowerRuler), x_pos + width + H_GAP / 2, yLowerRuler, colorScheme.textColor);

        result.setDeltaT(leftRuler, rightRuler);
        result.setDeltaV(upperRuler, lowerRuler);
    }

    private String vRulerToString(int ruler) {
        return Utils.voltageToString(result.adcValueToVoltage(ruler));
    }

    private String hRulerToString(int ruler) {
        return Utils.timeToString(result.adcTimeToRealTime(ruler));
    }

}
