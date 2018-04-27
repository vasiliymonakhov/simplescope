package ua.com.kiloom.simplescope;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

/**
 *
 * @author Vasily Monakhov
 */
public class Utils {

    /**
     * Преобразует напряжение в строку с точностью 2 знака после запятой. Если
     * абсолютная величина напряжения менее 1В, то результат выводится в
     * милливольтах, иначе в вольтах
     *
     * @param voltage напряжение
     * @return результирующая строка
     */
    static String voltageToString(double voltage) {
        String prefix = "";
        double val = voltage;
        if (Math.abs(voltage) < 1) {
            prefix = "m";
            val = voltage * 1000d;
        }
        return String.format("%.2f%sV", val, prefix);
    }

    /**
     * Преобразует время в строку с точностью 2 знака после запятой. Если
     * величина времени менее 1 с, то результат выводится в миллисекундах, если
     * время менее 1 мсек, то в микросекундах, иначе в секундах
     *
     * @param time время
     * @return результирующая строка
     */
    static String timeToString(double time) {
        String prefix = "";
        double val = time;
        if (time < 1) {
            prefix = "m";
            val = time * 1000d;
            if (time < 0.001) {
                prefix = "µ";
                val = time * 1000000d;
            }
        }
        return String.format("%.2f%sS", val, prefix);
    }

    /**
     * Преобразует частоту в строку с точностью 2 знака после запятой. Если
     * частота более 1 кГц, то выводится в килогерцах
     *
     * @param freq частота
     * @return результирующая строка
     */
    static String frequencyToString(double freq) {
        String prefix = "";
        double val = freq;
        if (freq > 1000d) {
            prefix = "k";
            val = freq / 1000d;
        }
        return String.format("%.2f%sHz", val, prefix);
    }

    /**
     * Превраяет долю в % с точностью 1 знак после запятой.
     *
     * @param val доля, где 1 соответсвует 100%
     * @return строка с процентами
     */
    static String valueToPercent(double val) {
        return String.format("%.1f%%", val * 100d);
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
    static void drawCenteredString(Graphics2D g, String str, int centerX, int centerY, Color faceColor) {
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D r = fm.getStringBounds(str, g);
        g.setColor(faceColor);
        g.drawString(str, (int) (centerX - r.getCenterX()), (int) (centerY - r.getCenterY()));
    }

}
