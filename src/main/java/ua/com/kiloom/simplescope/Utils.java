package ua.com.kiloom.simplescope;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import static ua.com.kiloom.simplescope.AppProperties.Keys.IMAGE_FORMAT;
import static ua.com.kiloom.simplescope.AppProperties.Keys.TEXT_CHARSET;

/**
 * Утилиты
 * @author Vasily Monakhov
 */
public class Utils {

    private final static double EPS = 0.001d;

    /**
     * Преобразует напряжение в строку с точностью 2 знака после запятой. Если
     * абсолютная величина напряжения менее 1В, то результат выводится в
     * милливольтах, иначе в вольтах
     *
     * @param voltage напряжение
     * @return результирующая строка
     */
    static String voltageToString(double voltage) {
        if (Math.abs(voltage) < EPS) {
            return "0V";
        }
        String prefix = "";
        double val = voltage;
        if (Math.abs(voltage) < 1) {
            prefix = "m";
            val = voltage * 1000d;
        }
        return String.format("%.2f%sV", val, prefix).replace('.', ',');
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
        return String.format("%.2f%sS", val, prefix).replace('.', ',');
    }

    /**
     * Преобразует частоту в строку с точностью 2 знака после запятой. Если
     * частота более 1 кГц, то выводится в килогерцах
     *
     * @param freq частота
     * @return результирующая строка
     */
    static String frequencyToString(double freq) {
        if (Math.abs(freq) < EPS) {
            return "0Hz";
        }
        String prefix = "";
        double val = freq;
        if (freq >= 1000d) {
            prefix = "k";
            val = freq / 1000d;
        }
        return String.format("%.2f%sHz", val, prefix).replace('.', ',');
    }

    /**
     * Превраяет долю в % с точностью 1 знак после запятой.
     *
     * @param val доля, где 1 соответсвует 100%
     * @return строка с процентами
     */
    static String valueToPercent(double val) {
        if (Math.abs(val) < EPS) {
            return "0%";
        }
        if (val >= 1) {
            return String.format("%.0f%%", val * 100d);
        }
        return String.format("%.1f%%", val * 100d).replace('.', ',');
    }

    /**
     * Превраяет долю в дБ с точностью 1 знак после запятой.
     *
     * @param val доля, где 0 соответсвует 0dB
     * @return строка децибелами
     */
    static String dbToString(double val) {
        return String.format("%.1fdB", val).replace('.', ',');
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

    /**
     * Создаёт имя для файла на основе текущей даты
     * @return имя файла
     */
    private static String createFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date(System.currentTimeMillis()));
    }

    /**
     * Сохраняет изображение в файл
     * @param name имя файла
     * @param image изображение
     * @return результат сохранения
     */
    static boolean saveImage(RenderedImage image) {
        String name = createFileName();
        String format = AppProperties.getString(IMAGE_FORMAT, "PNG");
        try {
            ImageIO.write(image, format, new File("image" + name + "." + format.toLowerCase()));
            return true;
        } catch (IOException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "Ошибка сохранения изображения", ex);
            return false;
        }
    }

    /**
     * Сохраняет в текстовый файл данные скопа
     * @param result результат
     * @return true если успешно
     */
    static boolean saveScopeText(Result result) {
        String name = createFileName();
        try (PrintWriter pw = new PrintWriter("scope" + name + ".txt", AppProperties.getString(TEXT_CHARSET, "UTF-16"))) {
            pw.println("Номер;Значение АЦП;Напряжение");
            for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
                pw.println("" + (i + 1) + ";" + result.getAdcData()[i] + ";" + voltageToString(result.getVoltages()[i]));
            }
            pw.flush();
            return true;
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "Ошибка записи данных", ex);
            return false;
        }
    }

    /**
     * Сохраняет в текстовый файл данные анализа гармоник
     * @param result результат
     * @return true если успешно
     */
    static boolean saveHarmText(Result result) {
        String name = createFileName();
        try (PrintWriter pw = new PrintWriter("harm" + name + ".txt", AppProperties.getString(TEXT_CHARSET, "UTF-16"))) {
            pw.println("Номер;Величина");
            boolean db = AppProperties.isHarmonicsInDb();
            for (int i = 0; i < result.getHarmonics().length; i++) {
                pw.println("" + (i + 1) + ";" + (db ? dbToString(result.getHarmonics()[i]) : valueToPercent(result.getHarmonics()[i])));
            }
            pw.flush();
            return true;
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "Ошибка записи данных", ex);
            return false;
        }
    }

    /**
     * Сохраняет в веб-страницу данные скопа
     * @param result результат
     * @return true если успешно
     */
    static boolean saveScopeWebPage(Result result) {
        String name = createFileName();
        try (PrintWriter pw = new PrintWriter("scope" + name + ".html", AppProperties.getString(TEXT_CHARSET, "UTF-16"))) {
            pw.println("<table>");
            pw.println("  <tr>");
                pw.println("    <td>Номер</td><td>Значение АЦП</td><td>Напряжение</td>");
            pw.println("  </tr>");
            for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
                pw.println("  <tr>");
                pw.println("    <td>" + (i + 1) + "</td><td>" + result.getAdcData()[i] + "</td><td>" + voltageToString(result.getVoltages()[i]) + "</td>");
                pw.println("  </tr>");
            }
            pw.println("</table>");
            pw.flush();
            return true;
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "Ошибка записи данных", ex);
            return false;
        }
    }

    /**
     * Сохраняет в веб-страницу данные анализа гармоник
     * @param result результат
     * @return true если успешно
     */
    static boolean saveHarmWebPage(Result result) {
        String name = createFileName();
        try (PrintWriter pw = new PrintWriter("harm" + name + ".html", AppProperties.getString(TEXT_CHARSET, "UTF-16"))) {
            pw.println("<table>");
            pw.println("  <tr>");
                pw.println("    <td>Номер</td><td>Значение</td>");
            pw.println("  </tr>");
            boolean db = AppProperties.isHarmonicsInDb();
            for (int i = 0; i < result.getHarmonics().length; i++) {
                pw.println("  <tr>");
                pw.println("    <td>" + (i + 1) + "</td><td>" + (db ? dbToString(result.getHarmonics()[i]) : valueToPercent(result.getHarmonics()[i])) + "</td>");
                pw.println("  </tr>");
            }
            pw.println("</table>");
            pw.flush();
            return true;
        } catch (FileNotFoundException | UnsupportedEncodingException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, "Ошибка записи данных", ex);
            return false;
        }
    }

}
