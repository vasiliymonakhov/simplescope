package ua.com.kiloom.simplescope;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;

/**
 * Класс для рисования графика
 *
 * @author Vasily Monakhov
 */
class ScopeRenderer {

    static class ColorScheme {

        private Color backgroundColor;
        private Color borderColor;
        private Color gridColor;
        private Color rayColor;
        private Color textColor;
        private Color rulerColor;

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

    private final static ColorScheme GREEN_MONO_SCHEME;

    private final static Color DARK_GREEN = new Color(0, 128, 0);
    private final static Color GREEN = Color.GREEN;
    private final static Color BLACK = Color.BLACK;
    private final static Color LIGHT_GREEN = new Color(128, 255, 128);

    static {
        GREEN_MONO_SCHEME = new ColorScheme(BLACK, DARK_GREEN, DARK_GREEN, GREEN, LIGHT_GREEN, LIGHT_GREEN);
    }

    private ColorScheme colorScheme = GREEN_MONO_SCHEME;

    private final Stroke rayStroke = new BasicStroke(2);

    private static class Point {
        int x;
        int y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    private Point[] convertAdcResultToScopePoints(int width, int height,DeviceController.ADCResult adcResult) {
        int length = adcResult.getAdcData().length;
        Point[] points = new Point[length];
        double xScale = ((double)width) / length;
        double yScale = ((double)height) / 4096;
        for (int i = 0; i < length; i++) {
            int x = (int)(Math.round(xScale * i));
            int y = (int)(Math.round(yScale * (4095 - adcResult.getAdcData()[i])));
            points[i] = new Point(x, y);
        }
        return points;
    }

    BufferedImage render(int width, int height, DeviceController.ADCResult adcResult) {
        width = ((width - 32) / 10) * 10;
        height = ((height - 32) / 10) * 10;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setColor(colorScheme.backgroundColor);
        g.fillRect(0, 0, width, height);

        g.setColor(colorScheme.borderColor);
        g.drawRect(0, 0, width - 1, height - 1);

        g.setColor(colorScheme.gridColor);
        for (int i = 0; i < width; i+= width / 10) {
            g.drawLine(i, 0, i, height);
        }
        for (int i = 0; i < height; i+= height / 10) {
            g.drawLine(0, i, width, i);
        }

        g.setColor(colorScheme.rayColor);
        g.setStroke(rayStroke);
        Point[] points = convertAdcResultToScopePoints(width, height, adcResult);
        for (int i = 0; i < points.length - 1; i++) {
            int x1 = points[i].x;
            int y1 = points[i].y;
            int x2 = points[i + 1].x;
            int y2 = points[i + 1].y;
            g.drawLine(x1, y1, x2, y2);
        }


        g.dispose();
        return image;
    }

}
