package Utils;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Artyom.Fomenko on 05.09.2016.
 */
public class SymbolIdMatcherTest {

    private static void testTrue(int symbol, String pattern) {
        assertTrue(SymbolIdMatcher.matches(symbol,pattern));
    }
    private static void testFalse(int symbol, String pattern) {
        assertFalse(SymbolIdMatcher.matches(symbol,pattern));
    }

    @Test
    public void matches() throws Exception {
        testTrue(100001,"100...");
        testTrue(100002,"100...");
        testTrue(100010,"100...");
        testTrue(100200,"100...");
        testFalse(101000,"100...");
        testFalse(10000,"100...");

    }

}