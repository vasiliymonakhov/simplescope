package ua.com.kiloom.simplescope;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Тест утилит
 *
 * @author Vasily Monakhov
 */
public class UtilsTest {

    /**
     * Test of voltageToString method, of class Utils.
     */
    @Test
    public void testVoltageToString() {
        assertEquals("0V", Utils.voltageToString(0));
        assertEquals("100,00V", Utils.voltageToString(100));
        assertEquals("10,00V", Utils.voltageToString(10));
        assertEquals("1,00V", Utils.voltageToString(1));
        assertEquals("100,00mV", Utils.voltageToString(0.1));
        assertEquals("10,00mV", Utils.voltageToString(0.01));
        assertEquals("1,00mV", Utils.voltageToString(0.001));
        assertEquals("-100,00V", Utils.voltageToString(-100));
        assertEquals("-10,00V", Utils.voltageToString(-10));
        assertEquals("-1,00V", Utils.voltageToString(-1));
        assertEquals("-100,00mV", Utils.voltageToString(-0.1));
        assertEquals("-10,00mV", Utils.voltageToString(-0.01));
        assertEquals("-1,00mV", Utils.voltageToString(-0.001));
    }

    /**
     * Test of timeToString method, of class Utils.
     */
    @Test
    public void testTimeToString() {
        assertEquals("1,00S", Utils.timeToString(1));
        assertEquals("10,00S", Utils.timeToString(10));
        assertEquals("100,00S", Utils.timeToString(100));
        assertEquals("100,00mS", Utils.timeToString(0.1));
        assertEquals("10,00mS", Utils.timeToString(0.01));
        assertEquals("1,00mS", Utils.timeToString(0.001));
        assertEquals("100,00µS", Utils.timeToString(0.0001));
        assertEquals("10,00µS", Utils.timeToString(0.00001));
        assertEquals("1,00µS", Utils.timeToString(0.000001));
    }

    /**
     * Test of frequencyToString method, of class Utils.
     */
    @Test
    public void testFrequencyToString() {
        assertEquals("0Hz", Utils.frequencyToString(0));
        assertEquals("0,10Hz", Utils.frequencyToString(0.1));
        assertEquals("0,01Hz", Utils.frequencyToString(0.01));
        assertEquals("10,00Hz", Utils.frequencyToString(10));
        assertEquals("100,00Hz", Utils.frequencyToString(100));
        assertEquals("1,00kHz", Utils.frequencyToString(1000));
        assertEquals("10,00kHz", Utils.frequencyToString(10000));
        assertEquals("100,00kHz", Utils.frequencyToString(100000));

    }

    /**
     * Test of valueToPercent method, of class Utils.
     */
    @Test
    public void testValueToPercent() {
        assertEquals("0%", Utils.valueToPercent(0));
        assertEquals("100%", Utils.valueToPercent(1));
        assertEquals("146%", Utils.valueToPercent(1.46));
        assertEquals("10,0%", Utils.valueToPercent(0.1));
        assertEquals("1,0%", Utils.valueToPercent(0.01));
        assertEquals("0,1%", Utils.valueToPercent(0.001));
    }

}
