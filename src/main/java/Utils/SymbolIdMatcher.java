package Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Artyom.Fomenko on 05.09.2016.
 */
public class SymbolIdMatcher {

    public static boolean matches(int symbol, String str) {

        if (str.length() == 0) return false;

        Pattern p = Pattern.compile(str);
        Matcher m = p.matcher(Integer.toString(symbol));
        return m.matches();

    }

}
