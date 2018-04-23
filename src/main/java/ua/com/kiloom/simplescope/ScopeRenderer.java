package ua.com.kiloom.simplescope;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Класс для рисования графика
 *
 * @author Vasily Monakhov
 */
class ScopeRenderer {

    /**
     * Цветовая схема скопа
     */
    private ColorScheme colorScheme = ColorScheme.getScheme("Зелёная монохромная");

    /**
     * Устанавливает цветовую схему
     *
     * @param colorScheme новая цветовая схема
     */
    void setColorScheme(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

    /**
     * Схема шрифтов
     */
    private FontScheme fontScheme = FontScheme.STANDART;

    /**
     * Устанавливает новую схему шрифтов
     *
     * @param схема шрифтов
     */
    void setFontScheme(FontScheme fontScheme) {
        this.fontScheme = fontScheme;
    }

    /**
     * Нажим для рисования прямых
     */
    private final Stroke normalStroke = new BasicStroke(1);

    /**
     * Нажим для рисования сетки
     */
    private final static float[] dashingPattern = {3f, 3f};

    private final Stroke gridStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER, 1.0f, dashingPattern, 2.0f);

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
     * желаемый отступ для рисования графика по веритикали
     */
    private final static int V_GAP = 64;

    /**
     * желаемый отступ для рисования графика по горизонтали
     */
    private final static int H_GAP = 96;

    /**
     * ширина области, на которой рисуется график
     */
    int width;

    /**
     * высота области, на которой рисуется график
     */
    int height;

    /**
     * Отступ области, на которой рисуется график, от левого края изображения
     */
    int x_pos;

    /**
     * Отступ области, на которой рисуется график, от верхнего края изображения
     */
    int y_pos;

    /**
     * результат измерений
     */
    private Result result;

    /**
     * Сделать расчёты геометрии
     * @param imageWidth ширина области рисования
     * @param imageHeight высота области рисования
     */
    void calculateGeometry(int imageWidth, int imageHeight) {
        // сделать небольшие отступы и чтобы высота и ширина были кратны 10
        width = ((imageWidth - 2 * H_GAP) / 10) * 10;
        height = ((imageHeight - 2 * V_GAP) / 10) * 10;
        // вычислить координаты левого верхнего угла графика
        x_pos = (imageWidth - width) / 2;
        y_pos = (imageHeight - height) / 2;
    }

    /**
     * Рисует график сигнала
     *
     * @param imageWidth ширина области рисования
     * @param imageHeight высота области рисования
     * @param result набор данных от АЦП устройства
     */
    void renderScopeAndUpdateRulers(int imageWidth, int imageHeight, Result result) throws InterruptedException {
        this.result = result;
        BufferedImage image = getImage(imageWidth, imageHeight);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setFont(fontScheme.getScopeFont());
        // залить цветом фона
        g.setColor(colorScheme.getBackgroundColor());
        g.fillRect(0, 0, imageWidth - 1, imageHeight - 1);
        calculateGeometry(imageWidth, imageHeight);
        // нарисовать сетку
        drawScopeGrid(g);
        g.setStroke(normalStroke);
        // нарисовать линейки
        drawRulers(g);
        // нарисовать рамку
        g.setColor(colorScheme.getBorderColor());
        g.drawRect(x_pos, y_pos, width, height);
        // нарисовать луч
        drawRay(g);
        g.dispose();
        // result.processHarmonicsData(leftRuler, rightRuler);
        result.setScopeImage(image);
    }

    /**
     * Рисует сетку осциллоскопа
     *
     * @param g графический контекст
     */
    private void drawScopeGrid(Graphics2D g) {
        g.setStroke(gridStroke);
        int time = 0;
        int dtime = result.getTimePerCell();
        String timeStr = result.getTimeString();
        for (int i = 0; i <= width; i += width / 10) {
            g.setColor(colorScheme.getGridColor());
            g.drawLine(x_pos + i, y_pos, x_pos + i, y_pos + height);
            drawCenteredString(g, String.format("%d%s", time, timeStr), x_pos + i, y_pos + height + V_GAP / 2, colorScheme.getTextColor());
            time += dtime;
        }
        int dvoltage = result.getVoltagePerCell();
        int voltage = 5 * dvoltage;
        String voltageStr = result.getVoltageString();
        for (int i = 0; i <= height; i += height / 10) {
            g.setColor(colorScheme.getGridColor());
            g.drawLine(x_pos, y_pos + i, x_pos + width, y_pos + i);
            drawCenteredString(g, String.format("%d%s", voltage, voltageStr), x_pos - H_GAP / 2, y_pos + i, colorScheme.getTextColor());
            voltage -= dvoltage;
        }
    }

    /**
     * Нарисовать луч
     *
     * @param g графический контекст
     */
    private void drawRay(Graphics2D g) {
        g.setColor(colorScheme.getRayColor());
        g.setStroke(rayStroke);
        Point[] points = convertAdcResultToScopePoints();
        for (int i = 0; i < points.length - 1; i++) {
            int x1 = points[i].x;
            int y1 = points[i].y;
            int x2 = points[i + 1].x;
            int y2 = points[i + 1].y;
            g.drawLine(x1, y1, x2, y2);
        }
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

    /**
     * Левая линейка
     */
    private int leftRuler = Const.ADC_DATA_BLOCK_SIZE / 4;
    /**
     * Правая линейка
     */
    private int rightRuler = Const.ADC_DATA_BLOCK_SIZE * 3 / 4;
    /**
     * Верхняя линейка
     */
    private int upperRuler = Const.ADC_RANGE * 3 / 4;
    /**
     * Нижняя линейка
     */
    private int lowerRuler = Const.ADC_RANGE / 4;

    /**
     * Добавить координаты события от мыши относительно левого верхнего угла
     * изображения
     *
     * @param x по оси абсцисс
     * @param y по оси ординат
     */
    boolean addMouseClick(int x, int y) {
        boolean offAutoFreq = false;
        // проверить, попадает ли точка в область рисования графика
        if (x >= x_pos && x <= x_pos + width && y >= y_pos && y <= y_pos + height) {
            // найти расстояние от точки до каждой из линеек
            int dLeftRuler = Math.abs(xLeftRuler - x);
            int dRightRuler = Math.abs(xRightRuler - x);
            int dUpperRuler = Math.abs(yUpperRuler - y);
            int dLowerRuler = Math.abs(yLowerRuler - y);
            // найти минимальное расстояние
            int[] distances = new int[4];
            distances[0] = dLeftRuler;
            distances[1] = dRightRuler;
            distances[2] = dUpperRuler;
            distances[3] = dLowerRuler;
            Arrays.sort(distances);
            int min = distances[0];
            // в зависимости от того, кто ближе, переместить линейку в эту точку
            if (min == dLeftRuler) {
                leftRuler = xToHScale(x);
                offAutoFreq = true;
            } else if (min == dRightRuler) {
                rightRuler = xToHScale(x);
                offAutoFreq = true;
            } else if (min == dUpperRuler) {
                upperRuler = yToVScale(y);
            } else {
                // min == dLowerRuler
                lowerRuler = yToVScale(y);
            }
        }
        return offAutoFreq;
    }

    /**
     * Преобразует ось абсцисс на изображении в позицию в выборке
     *
     * @param x координата
     * @return позиция в выборке из АЦП
     */
    private int xToHScale(int x) {
        return (int) Math.round((x - x_pos) / xScale);
    }

    /**
     * Преобразует ось ординат на изображении в значение выборки
     *
     * @param x координата
     * @return значение выборки из АЦП
     */
    private int yToVScale(int y) {
        return Const.ADC_MAX - (int) Math.round((y - y_pos) / yScale);
    }

    /**
     * положение вертикальных линеек на изображении
     */
    private int xLeftRuler;
    private int xRightRuler;
    /**
     * положение горизонтальных линеек на изображении
     */
    private int yUpperRuler;
    private int yLowerRuler;

    /**
     * Нарисовать линейки
     *
     * @param g графический контекст
     */
    void drawRulers(Graphics2D g) {
        // проверить, не двигаются ли наши линейки автоматически
        if (result.getLeftRulerPos() != -1) {
            leftRuler = result.getLeftRulerPos();
        }
        if (result.getRightRulerPos() != -1) {
            rightRuler = result.getRightRulerPos();
        }
        // вычислить координаты линеек на изображении
        xLeftRuler = (int) Math.round(xScale * leftRuler + x_pos);
        xRightRuler = (int) Math.round(xScale * rightRuler + x_pos);
        yUpperRuler = (int) Math.round(yScale * (Const.ADC_MAX - upperRuler) + y_pos);
        yLowerRuler = (int) Math.round(yScale * (Const.ADC_MAX - lowerRuler) + y_pos);
        // нарисовать линии
        g.setColor(colorScheme.getRulerColor());
        g.drawLine(xLeftRuler, y_pos, xLeftRuler, y_pos + height);
        g.drawLine(xRightRuler, y_pos, xRightRuler, y_pos + height);
        g.drawLine(x_pos, yUpperRuler, x_pos + width, yUpperRuler);
        g.drawLine(x_pos, yLowerRuler, x_pos + width, yLowerRuler);

        // нанести надписи созначением времени
        drawCenteredString(g, vRulerToString(leftRuler), xLeftRuler, V_GAP / 2, colorScheme.getTextColor());
        drawCenteredString(g, vRulerToString(rightRuler), xRightRuler, V_GAP / 2, colorScheme.getTextColor());
        // нанести надписи со значением напряжения
        drawCenteredString(g, hRulerToString(upperRuler), x_pos + width + H_GAP / 2, yUpperRuler, colorScheme.getTextColor());
        drawCenteredString(g, hRulerToString(lowerRuler), x_pos + width + H_GAP / 2, yLowerRuler, colorScheme.getTextColor());
        // записать в результат положение линеек
        updateRulers();
    }

    /**
     * Преобразует положение горизонтальной линейки в строку с напряжением
     *
     * @param ruler положение линейки
     * @return строка со временем
     */
    private String hRulerToString(int ruler) {
        return Utils.voltageToString(result.adcValueToVoltage(ruler));
    }

    /**
     * Преобразует положение вертикальной линейки в строку со временем
     *
     * @param ruler положение линейки
     * @return строка со временем
     */
    private String vRulerToString(int ruler) {
        return Utils.timeToString(result.adcTimeToRealTime(ruler));
    }

    /**
     * записать в результат положение линеек
     */
    private void updateRulers() {
        if (!result.isAutoFreq()) {
            result.setDeltaT(leftRuler, rightRuler);
        }
        result.setDeltaV(upperRuler, lowerRuler);
    }

    /**
     * Рисует изображение анализа гармоник
     *
     * @param imageWidth ширина области рисования
     * @param imageHeight высота области рисования
     * @param result набор данных от АЦП устройства
     * @throws InterruptedException
     */
    void renderHarmAnalyser(int imageWidth, int imageHeight, Result result) throws InterruptedException {
        this.result = result;
        updateRulers();
        result.processHarmonicsData(leftRuler, rightRuler);
        BufferedImage image = getImage(imageWidth, imageHeight);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setFont(fontScheme.getScopeFont());
        // залить цветом фона
        g.setColor(colorScheme.getBackgroundColor());
        g.fillRect(0, 0, imageWidth - 1, imageHeight - 1);
        calculateGeometry(imageWidth, imageHeight);
        // нарисовать сетку анализатора гармоник
        drawHarmAnalyserGrid(g);
        // нарисовать рамку
        g.setStroke(normalStroke);
        g.setColor(colorScheme.getBorderColor());
        g.drawRect(x_pos, y_pos, width, height);
        // наристовать столбцы
        drawHarmAnalyserBars(g);
        result.setHarmImage(image);
    }

    /**
     * Рисует сетку анализатора гармоник
     *
     * @param g графический контекст
     */
    private void drawHarmAnalyserGrid(Graphics2D g) {
        g.setStroke(gridStroke);
        for (int i = 0; i <= width; i += width / Const.HARMONICS_COUNT) {
            g.setColor(colorScheme.getGridColor());
            g.drawLine(x_pos + i, y_pos, x_pos + i, y_pos + height);
        }
        double deltaPercent = 0.1d;
        double percent = 1d;
        for (int i = 0; i <= height; i += height / 10) {
            g.setColor(colorScheme.getGridColor());
            g.drawLine(x_pos, y_pos + i, x_pos + width, y_pos + i);
            // градуировка с шагом 10% слева
            drawCenteredString(g, Utils.valueToPercent(percent), x_pos - H_GAP / 2, y_pos + i, colorScheme.getTextColor());
            percent -= deltaPercent;
        }
    }

    /**
     * Рисует столбцы анализатора
     *
     * @param g графический контекст
     */
    private void drawHarmAnalyserBars(Graphics2D g) {
        double[] harms = result.getHarmonics();
        // ширина клетки
        int cw = width / Const.HARMONICS_COUNT;
        // ширина столбика
        int bw = 6 * cw / Const.HARMONICS_COUNT;
        // смещение столбиков
        int xl = x_pos + (cw - bw) / 2;
        // смещение центра надписи по горизонтали
        int cx = x_pos + cw / 2;
        // нижняя координата столбиков
        int yl = y_pos + height;
        // частота основной гармоники
        double fr = 1 / result.getDeltaT();
        // рисуем только первые 10 столбиков
        for (int i = 0; i < Const.HARMONICS_COUNT; i ++) {
            g.setColor(colorScheme.getGridColor());
            // высота столбика
            int bl = (int)(Math.round(harms[i] * height));
            g.fillRect(xl, yl - bl, bw, bl);
            g.setColor(colorScheme.getRayColor());
            g.drawRect(xl, yl - bl, bw, bl);
            // над столбиком нарисовать величину гармоники в %
            drawCenteredString(g, Utils.valueToPercent(harms[i]), cx, y_pos + height - bl - V_GAP / 2, colorScheme.getTextColor());
            // под столбиком частоту гармоники
            drawCenteredString(g, Utils.frequencyToString(fr * (i + 1)), cx, y_pos + height + V_GAP / 2, colorScheme.getTextColor());
            // сдвинуть на следующий столбик
            xl += cw;
            cx += cw;
        }
    }

}
