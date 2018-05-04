package ua.com.kiloom.simplescope;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Тест класса результата
 *
 * @author Vasily Monakhov
 */
public class ResultTest {

    public ResultTest() {
    }

    private byte[] integersToBytes(int[] input) {
        byte[] output = new byte[Const.BYTES_BLOCK_SIZE];
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            output[i * 2] = (byte) ((input[i] >> 8) & 0x00FF);
            output[i * 2 + 1] = (byte) (input[i] & 0x00FF);
        }
        return output;
    }

    private byte[] makeZero() {
        int[] adc = new int[Const.ADC_DATA_BLOCK_SIZE];
        Arrays.fill(adc, 0);
        return integersToBytes(adc);
    }

    private byte[] makeFull() {
        int[] adc = new int[Const.ADC_DATA_BLOCK_SIZE];
        Arrays.fill(adc, Const.ADC_MAX);
        return integersToBytes(adc);
    }

    private byte[] makeMiddle() {
        int[] adc = new int[Const.ADC_DATA_BLOCK_SIZE];
        Arrays.fill(adc, Const.ADC_MIDDLE);
        return integersToBytes(adc);
    }

    private byte[] makeError() {
        int[] adc = new int[Const.ADC_DATA_BLOCK_SIZE];
        Arrays.fill(adc, 0xFFFFFFFF);
        return integersToBytes(adc);
    }

    private byte[] makeMeandr() {
        int[] adc = new int[Const.ADC_DATA_BLOCK_SIZE];
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            if ((i / 100) % 2 == 1) {
                adc[i] = Const.ADC_MIDDLE + 1000;
            } else {
                adc[i] = Const.ADC_MIDDLE - 1000;
            }
        }
        return integersToBytes(adc);
    }

    private byte[] makeSinus() {
        int[] adc = new int[Const.ADC_DATA_BLOCK_SIZE];
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            adc[i] = Const.ADC_MIDDLE + (int) Math.round((Const.ADC_MIDDLE - 1) * Math.sin(i * Math.PI * 8 / Const.ADC_DATA_BLOCK_SIZE));
        }
        return integersToBytes(adc);
    }

    private byte[] makeMultiSinus() {
        int[] adc = new int[Const.ADC_DATA_BLOCK_SIZE];
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            adc[i] = Const.ADC_MIDDLE + (int) Math.round(400 * Math.sin(i * Math.PI * 4 / Const.ADC_DATA_BLOCK_SIZE)
                    + 300 * Math.sin(i * Math.PI * 8 / Const.ADC_DATA_BLOCK_SIZE)
                    + 200 * Math.sin(i * Math.PI * 12 / Const.ADC_DATA_BLOCK_SIZE)
                    + 100 * Math.sin(i * Math.PI * 16 / Const.ADC_DATA_BLOCK_SIZE));
        }
        return integersToBytes(adc);
    }

    /**
     * Test of getAdcData method, of class Result.
     */
    @Test
    public void testGetAdcData() {
        Result r = new Result(10, 0);
        assertTrue(r.processADCData(makeZero(), true, true));
        int[] result = r.getAdcData();
        assertNotNull(result);
        assertEquals(result.length, Const.ADC_DATA_BLOCK_SIZE);
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            assertEquals(result[i], 0);
        }
        assertTrue(r.processADCData(makeFull(), true, true));
        result = r.getAdcData();
        assertNotNull(result);
        assertEquals(result.length, Const.ADC_DATA_BLOCK_SIZE);
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            assertEquals(result[i], Const.ADC_MAX);
        }
        assertTrue(r.processADCData(makeMiddle(), true, true));
        result = r.getAdcData();
        assertNotNull(result);
        assertEquals(result.length, Const.ADC_DATA_BLOCK_SIZE);
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            assertEquals(result[i], Const.ADC_MIDDLE);
        }
        assertFalse(r.processADCData(makeError(), true, true));
    }

    /**
     * Test of getVoltages method, of class Result.
     */
    @Test
    public void testGetVoltages() {
        Result r = new Result(10, 0);
        assertTrue(r.processADCData(makeZero(), true, true));
        double[] result = r.getVoltages();
        assertNotNull(result);
        assertEquals(result.length, Const.ADC_DATA_BLOCK_SIZE);
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            assertEquals(result[i], -100, 0.1);
        }
        assertTrue(r.processADCData(makeFull(), true, true));
        result = r.getVoltages();
        assertNotNull(result);
        assertEquals(result.length, Const.ADC_DATA_BLOCK_SIZE);
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            assertEquals(result[i], 100, 0.1);
        }
        assertTrue(r.processADCData(makeMiddle(), true, true));
        result = r.getVoltages();
        assertNotNull(result);
        assertEquals(result.length, Const.ADC_DATA_BLOCK_SIZE);
        for (int i = 0; i < Const.ADC_DATA_BLOCK_SIZE; i++) {
            assertEquals(result[i], 0, 0.1);
        }
    }

    /**
     * Test of getVMax method, of class Result.
     */
    @Test
    public void testGetV() {
        Result r = new Result(10, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(100, r.getVMax(), 0.1);
        assertEquals(-100, r.getVMin(), 0.1);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.1);

        r = new Result(9, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(50, r.getVMax(), 0.1);
        assertEquals(-50, r.getVMin(), 0.1);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.1);

        r = new Result(8, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(25, r.getVMax(), 0.1);
        assertEquals(-25, r.getVMin(), 0.1);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.1);

        r = new Result(7, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(10, r.getVMax(), 0.1);
        assertEquals(-10, r.getVMin(), 0.1);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.1);

        r = new Result(6, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(5, r.getVMax(), 0.1);
        assertEquals(-5, r.getVMin(), 0.1);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.1);

        r = new Result(5, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(2.5, r.getVMax(), 0.01);
        assertEquals(-2.5, r.getVMin(), 0.01);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.01);

        r = new Result(4, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(1, r.getVMax(), 0.01);
        assertEquals(-1, r.getVMin(), 0.01);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.01);

        r = new Result(3, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(0.5, r.getVMax(), 0.001);
        assertEquals(-0.5, r.getVMin(), 0.001);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.001);

        r = new Result(2, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(0.25, r.getVMax(), 0.001);
        assertEquals(-0.25, r.getVMin(), 0.001);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.001);

        r = new Result(1, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(0.1, r.getVMax(), 0.001);
        assertEquals(-0.1, r.getVMin(), 0.001);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.001);

        r = new Result(0, 0);
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertEquals(0.05, r.getVMax(), 0.001);
        assertEquals(-0.05, r.getVMin(), 0.001);
        assertEquals(r.getVMax() / Math.sqrt(2), r.getVRms(), 0.001);
    }

    @Test
    public void testAuto() {
        Result r = new Result(10, 15);
        assertTrue(r.processADCData(makeMeandr(), true, true));
        assertEquals(48.83, r.getVMax(), 0.01);
        assertEquals(-48.83, r.getVMin(), 0.01);
        assertEquals(48.83, r.getVRms(), 0.01);
        assertEquals(97.66, r.getDeltaV(), 0.01);
        assertEquals(0.4, r.getDeltaT(), 0.001);
        assertEquals(Const.ADC_MIDDLE + 1000, r.getUpperRulerPos());
        assertEquals(Const.ADC_MIDDLE - 1000, r.getLowerRulerPos());
        assertEquals(100, r.getLeftRulerPos());
        assertEquals(300, r.getRightRulerPos());
    }

    @Test
    public void testHarmonics() {
        Result r = new Result(10, 15);
        assertTrue(r.processADCData(makeMultiSinus(), true, true));
        AppProperties.setBoolean(AppProperties.Keys.HARMONICS_DECIBELLS, false);
        r.processHarmonicsData(Const.ADC_DATA_BLOCK_SIZE / 4, 3 * Const.ADC_DATA_BLOCK_SIZE / 4 - 1);
        double[] h = r.getHarmonics();
        assertEquals(0.40d, h[0], 0.01d);
        assertEquals(0.30d, h[1], 0.01d);
        assertEquals(0.20d, h[2], 0.01d);
        assertEquals(0.10d, h[3], 0.01d);
        assertEquals(0, h[4], 0.01d);
        assertEquals(0, h[5], 0.01d);
        assertEquals(0, h[6], 0.01d);
        assertEquals(0, h[7], 0.01d);
        assertEquals(0, h[8], 0.01d);
        assertEquals(0, h[9], 0.01d);
        AppProperties.setBoolean(AppProperties.Keys.HARMONICS_DECIBELLS, true);
        r.processHarmonicsData(Const.ADC_DATA_BLOCK_SIZE / 4, 3 * Const.ADC_DATA_BLOCK_SIZE / 4 - 1);
        h = r.getHarmonics();
        assertEquals(20d * Math.log10(0.4d), h[0], 0.5);
        assertEquals(20d * Math.log10(0.3d), h[1], 0.5);
        assertEquals(20d * Math.log10(0.2d), h[2], 0.5);
        assertEquals(20d * Math.log10(0.1d), h[3], 0.5);
        assertTrue(h[4] < -60d);
        assertTrue(h[5] < -60d);
        assertTrue(h[6] < -60d);
        assertTrue(h[7] < -60d);
        assertTrue(h[8] < -60d);
        assertTrue(h[9] < -60d);
    }

    public void testOverload() {
        Result r = new Result(10, 15);
        assertTrue(r.processADCData(makeFull(), true, true));
        assertTrue(r.isOverloadSignal());
        assertFalse(r.isTooLowSignal());
        assertTrue(r.processADCData(makeZero(), true, true));
        assertTrue(r.isOverloadSignal());
        assertFalse(r.isTooLowSignal());
        assertTrue(r.processADCData(makeMiddle(), true, true));
        assertFalse(r.isOverloadSignal());
        assertTrue(r.isTooLowSignal());
        assertTrue(r.processADCData(makeSinus(), true, true));
        assertFalse(r.isOverloadSignal());
        assertFalse(r.isTooLowSignal());
    }

}
