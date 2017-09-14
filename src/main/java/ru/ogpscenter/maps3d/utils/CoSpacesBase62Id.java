package ru.ogpscenter.maps3d.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by Mikhail Senin (Mikhail.Senin@jetbrains.com) on 24.01.17.
 */
public class CoSpacesBase62Id {
  public static String calculateIdFromFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path));
    return CryptoUtil.hashBase62(bytes, "SHA-256");
  }

  // java -Dfile.encoding=UTF-8 -classpath /usr/lib/jvm/java-8-oracle/jre/lib/charsets.jar:/usr/lib/jvm/java-8-oracle/jre/lib/deploy.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/cldrdata.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/dnsns.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/jaccess.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/jfxrt.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/localedata.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/nashorn.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/sunec.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/sunjce_provider.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/sunpkcs11.jar:/usr/lib/jvm/java-8-oracle/jre/lib/ext/zipfs.jar:/usr/lib/jvm/java-8-oracle/jre/lib/javaws.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jce.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jfr.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jfxswt.jar:/usr/lib/jvm/java-8-oracle/jre/lib/jsse.jar:/usr/lib/jvm/java-8-oracle/jre/lib/management-agent.jar:/usr/lib/jvm/java-8-oracle/jre/lib/plugin.jar:/usr/lib/jvm/java-8-oracle/jre/lib/resources.jar:/usr/lib/jvm/java-8-oracle/jre/lib/rt.jar:/home/user/maps3d/target/classes:/home/user/.m2/repository/org/jetbrains/annotations/15.0/annotations-15.0.jar:/home/user/.m2/repository/junit/junit/4.12/junit-4.12.jar:/home/user/.m2/repository/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar:/home/user/.m2/repository/com/vividsolutions/jts/1.13/jts-1.13.jar:/home/user/.m2/repository/org/jgrapht/jgrapht-core/0.9.2/jgrapht-core-0.9.2.jar:/home/user/.m2/repository/com/google/code/gson/gson/2.2.4/gson-2.2.4.jar:/home/user/.local/share/JetBrains/Toolbox/apps/IDEA-U/ch-0/163.9166.29/lib/idea_rt.jar com.intellij.rt.execution.application.AppMain ru.ogpscenter.maps3d.utils.CoSpacesBase62Id *.png
  public static void main(String[] args) {
    for (String path : args) {
      try {
        String id = calculateIdFromFile(path);
        System.out.println(path + " ==> " + id);
      }
      catch (IOException e) {
        System.out.println(path + " ==> Skipped: " + e.getMessage());
      }
    }
  }
}