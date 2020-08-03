package com.ctrip.framework.apollo.portal.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * jian.tan
 */
public class ConfigToFileUtils {

  @Deprecated
  public static void itemsToFile(OutputStream os, List<String> items) {
    try {
      PrintWriter printWriter = new PrintWriter(os);
      items.forEach(printWriter::println);
      printWriter.close();
    } catch (Exception e) {
      throw e;
    }
  }

  public static String fileToString(InputStream inputStream) {
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
    return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
  }
}
