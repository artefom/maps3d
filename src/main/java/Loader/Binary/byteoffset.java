package Loader.Binary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by Artem on 21.07.2016.
 */


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface byteoffset {
    int offset();
}
