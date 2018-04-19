package ua.com.kiloom.simplescope;

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
}
