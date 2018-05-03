package ua.com.kiloom.simplescope;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
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
    private ColorScheme colorScheme = AppProperties.getColorScheme();

    /**
     * Схема шрифтов
     */
    private FontScheme fontScheme = AppProperties.getFontScheme();

    /**
     * Устанавливает цветовую схему
     *
     * @param colorScheme новая цветовая схема
     */
    void setColorScheme(ColorScheme colorScheme) {
        this.colorScheme = colorScheme;
    }

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
    final static Stroke NORMAL_STROKE = new BasicStroke(1);

    /**
     * Нажим для рисования луча
     */
    final static Stroke RAY_STROKE = new BasicStroke(3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND);

    /**
     * Точка на экране
     */
    static class Point {

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
     *
     * @param imageWidth ширина области рисования
     * @param imageHeight высота области рисования
     */
    void calculateGeometry(int imageWidth, int imageHeight) {
        // сделать небольшие отступы и чтобы высота и ширина были кратны 10
        width = ((imageWidth - 2 * Const.H_GAP) / 10) * 10;
        height = ((imageHeight - 2 * Const.V_GAP) / 10) * 10;
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
    void renderScope(int imageWidth, int imageHeight, Result result) throws InterruptedException {
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
        g.setStroke(NORMAL_STROKE);
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
        g.setStroke(NORMAL_STROKE);
        double time = 0;
        double dtime = result.getTime() / 10;
        for (int i = 0; i <= width; i += width / 10) {
            g.setColor(colorScheme.getGridColor());
            g.drawRect(x_pos + i, y_pos, 0, height);
            Utils.drawCenteredString(g, Utils.timeToString(time), x_pos + i, y_pos + height + Const.V_GAP / 2, colorScheme.getTextColor());
            time += dtime;
        }
        double voltage = result.getVoltage();
        double dvoltage = voltage / 5;
        for (int i = 0; i <= height; i += height / 10) {
            g.setColor(colorScheme.getGridColor());
            g.drawRect(x_pos, y_pos + i, width, 0);
            Utils.drawCenteredString(g, Utils.voltageToString(voltage), x_pos - Const.H_GAP / 2, y_pos + i, colorScheme.getTextColor());
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
        g.setStroke(RAY_STROKE);
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
     * @return 0 - ничего не менять, 1 - захвачена вертикальная линейка, 2 -
     * захвачена горизонтальная линейка
     */
    int addMouseClick(int x, int y) {
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
            if (min > Const.MOUSE_MAX_DISTANCE_TO_RULER) {
                // мышь далеко от линеек
                return 0;
            }
            // в зависимости от того, кто ближе, переместить линейку в эту точку
            if (min == dLeftRuler) {
                leftRuler = xToHScale(x);
                return 1;
            } else if (min == dRightRuler) {
                rightRuler = xToHScale(x);
                return 1;
            } else if (min == dUpperRuler) {
                upperRuler = yToVScale(y);
                return 2;
            } else {
                // min == dLowerRuler
                lowerRuler = yToVScale(y);
                return 2;
            }
        }
        return 0;
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
        if (result.isAutoFreq()) {
            leftRuler = result.getLeftRulerPos();
            rightRuler = result.getRightRulerPos();
        }
        if (result.isAutoMeasure()) {
            upperRuler = result.getUpperRulerPos();
            lowerRuler = result.getLowerRulerPos();
        }
        // вычислить координаты линеек на изображении
        xLeftRuler = (int) Math.round(xScale * leftRuler + x_pos);
        xRightRuler = (int) Math.round(xScale * rightRuler + x_pos);
        yUpperRuler = (int) Math.round(yScale * (Const.ADC_MAX - upperRuler) + y_pos);
        yLowerRuler = (int) Math.round(yScale * (Const.ADC_MAX - lowerRuler) + y_pos);
        // нарисовать линии
        g.setColor(colorScheme.getRulerColor());
        g.drawRect(xLeftRuler, y_pos, 0, height);
        g.drawRect(xRightRuler, y_pos, 0, height);
        g.drawRect(x_pos, yUpperRuler, width, 0);
        g.drawRect(x_pos, yLowerRuler, width, 0);

        // нанести надписи со значением времени
        Utils.drawCenteredString(g, vRulerToString(leftRuler), xLeftRuler, Const.V_GAP / 2, colorScheme.getTextColor());
        Utils.drawCenteredString(g, vRulerToString(rightRuler), xRightRuler, Const.V_GAP / 2, colorScheme.getTextColor());
        // нанести надписи со значением напряжения
        Utils.drawCenteredString(g, hRulerToString(upperRuler), x_pos + width + Const.H_GAP / 2, yUpperRuler, colorScheme.getTextColor());
        Utils.drawCenteredString(g, hRulerToString(lowerRuler), x_pos + width + Const.H_GAP / 2, yLowerRuler, colorScheme.getTextColor());
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
        if (!result.isAutoMeasure()) {
            result.setDeltaV(upperRuler, lowerRuler);
        }
    }

    /**
     * Рисует изображение анализа гармоник
     *
     * @param imageWidth ширина области рисования
     * @param imageHeight высота области рисования
     * @param result набор данных от АЦП устройства
     * @throws InterruptedException
     */
    void renderHarmAnalyse(int imageWidth, int imageHeight, Result result) throws InterruptedException {
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
        g.setStroke(NORMAL_STROKE);
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
        g.setStroke(NORMAL_STROKE);
        for (int i = 0; i <= width; i += width / AppProperties.getHarmonicsRender()) {
            g.setColor(colorScheme.getGridColor());
            g.drawRect(x_pos + i, y_pos, 0, height);
        }
        // в dB или %
        boolean db = AppProperties.isHarmonicsInDb();
        double deltaPercent = db ? 10 : 0.1d;
        double percent = db ? 0 : 1d;
        int div = db ? 6 : 10;
        for (int i = 0; i <= height; i += height / div) {
            g.setColor(colorScheme.getGridColor());
            g.drawRect(x_pos, y_pos + i, width, 0);
            // градуировка с шагом -10dB или 10% слева
            String s = db ? Utils.dbToString(percent) : Utils.valueToPercent(percent);
            Utils.drawCenteredString(g, s, x_pos - Const.H_GAP / 2, y_pos + i, colorScheme.getTextColor());
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
        int harmonicsCount = AppProperties.getHarmonicsRender();
        // ширина клетки
        int cw = width / harmonicsCount;
        // ширина столбика
        int bw = 8 * cw / 10;
        // смещение столбиков
        int xl = x_pos + (cw - bw) / 2;
        // смещение центра надписи по горизонтали
        int cx = x_pos + cw / 2;
        // нижняя координата столбиков
        int yl = y_pos + height;
        // частота основной гармоники
        double fr = 1 / result.getDeltaT();
        // в dB или %
        boolean db = AppProperties.isHarmonicsInDb();
        // рисуем столбики
        for (int i = 0; i < harmonicsCount; i++) {
            g.setColor(colorScheme.getGridColor());
            // высота столбика
            int bl;
            if (db) {
                if (harms[i] < -60d) {
                    bl = 0;
                } else {
                    bl = (int) (Math.round((60 + harms[i]) / 60 * height));
                }
            } else {
                bl = (int) (Math.round(harms[i] * height));
            }
            g.fillRect(xl, yl - bl, bw, bl);
            g.setColor(colorScheme.getRayColor());
            g.drawRect(xl, yl - bl, bw, bl);
            // над столбиком нарисовать величину гармоники в dB или %
            String s = db ? Utils.dbToString(harms[i]) : Utils.valueToPercent(harms[i]);
            Utils.drawCenteredString(g, s, cx, y_pos + height - bl - Const.V_GAP / 2, colorScheme.getTextColor());
            // под столбиком частоту гармоники
            Utils.drawCenteredString(g, Utils.frequencyToString(fr * (i + 1)), cx, y_pos + height + Const.V_GAP / 2, colorScheme.getTextColor());
            // сверху написать номер гармоники
            Utils.drawCenteredString(g, String.valueOf(i + 1), cx, y_pos - Const.V_GAP / 2, colorScheme.getTextColor());
            // сдвинуть на следующий столбик
            xl += cw;
            cx += cw;
        }
    }

}
