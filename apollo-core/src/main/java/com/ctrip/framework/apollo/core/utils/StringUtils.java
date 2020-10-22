package com.ctrip.framework.apollo.core.utils;

import java.util.Collection;
import java.util.Iterator;

public class StringUtils {

  public static final String EMPTY = "";

  /**
   * <p>
   * Checks if a String is empty ("") or null.
   * </p>
   *
   * <pre>
   * StringUtils.isEmpty(null)      = true
   * StringUtils.isEmpty("")        = true
   * StringUtils.isEmpty(" ")       = false
   * StringUtils.isEmpty("bob")     = false
   * StringUtils.isEmpty("  bob  ") = false
   * </pre>
   *
   * <p>
   * NOTE: This method changed in Lang version 2.0. It no longer trims the String. That functionality is available in isBlank().
   * </p>
   *
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is empty or null
   */
  public static boolean isEmpty(String str) {
    return str == null || str.length() == 0;
  }


  public static boolean isContainEmpty(String... args){
    if (args == null){
      return false;
    }

    for (String arg: args){
      if (arg == null || "".equals(arg)){
        return true;
      }
    }

    return false;
  }
  /**
   * <p>
   * Checks if a String is whitespace, empty ("") or null.
   * </p>
   *
   * <pre>
   * StringUtils.isBlank(null)      = true
   * StringUtils.isBlank("")        = true
   * StringUtils.isBlank(" ")       = true
   * StringUtils.isBlank("bob")     = false
   * StringUtils.isBlank("  bob  ") = false
   * </pre>
   *
   * @param str the String to check, may be null
   * @return <code>true</code> if the String is null, empty or whitespace
   */
  public static boolean isBlank(String str) {
    int strLen;
    if (str == null || (strLen = str.length()) == 0) {
      return true;
    }
    for (int i = 0; i < strLen; i++) {
      if (!Character.isWhitespace(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * <p>
   * Removes control characters (char &lt;= 32) from both ends of this String returning <code>null</code> if the String is empty
   * ("") after the trim or if it is <code>null</code>.
   *
   * <p>
   * The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use
   * {@link #stripToNull(String)}.
   * </p>
   *
   * <pre>
   * StringUtils.trimToNull(null)          = null
   * StringUtils.trimToNull("")            = null
   * StringUtils.trimToNull("     ")       = null
   * StringUtils.trimToNull("abc")         = "abc"
   * StringUtils.trimToNull("    abc    ") = "abc"
   * </pre>
   *
   * @param str the String to be trimmed, may be null
   * @return the trimmed String, <code>null</code> if only chars &lt;= 32, empty or null String input
   * @since 2.0
   */
  public static String trimToNull(String str) {
    String ts = trim(str);
    return isEmpty(ts) ? null : ts;
  }

  /**
   * <p>
   * Removes control characters (char &lt;= 32) from both ends of this String returning an empty String ("") if the String is empty
   * ("") after the trim or if it is <code>null</code>.
   *
   * <p>
   * The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use
   * {@link #stripToEmpty(String)}.
   * </p>
   *
   * <pre>
   * StringUtils.trimToEmpty(null)          = ""
   * StringUtils.trimToEmpty("")            = ""
   * StringUtils.trimToEmpty("     ")       = ""
   * StringUtils.trimToEmpty("abc")         = "abc"
   * StringUtils.trimToEmpty("    abc    ") = "abc"
   * </pre>
   *
   * @param str the String to be trimmed, may be null
   * @return the trimmed String, or an empty String if <code>null</code> input
   * @since 2.0
   */
  public static String trimToEmpty(String str) {
    return str == null ? EMPTY : str.trim();
  }

  /**
   * <p>
   * Removes control characters (char &lt;= 32) from both ends of this String, handling <code>null</code> by returning
   * <code>null</code>.
   * </p>
   *
   * <p>
   * The String is trimmed using {@link String#trim()}. Trim removes start and end characters &lt;= 32. To strip whitespace use
   * {@link #strip(String)}.
   * </p>
   *
   * <p>
   * To trim your choice of characters, use the {@link #strip(String, String)} methods.
   * </p>
   *
   * <pre>
   * StringUtils.trim(null)          = null
   * StringUtils.trim("")            = ""
   * StringUtils.trim("     ")       = ""
   * StringUtils.trim("abc")         = "abc"
   * StringUtils.trim("    abc    ") = "abc"
   * </pre>
   *
   * @param str the String to be trimmed, may be null
   * @return the trimmed string, <code>null</code> if null String input
   */
  public static String trim(String str) {
    return str == null ? null : str.trim();
  }

  /**
   * <p>
   * Compares two Strings, returning <code>true</code> if they are equal.
   * </p>
   *
   * <p>
   * <code>null</code>s are handled without exceptions. Two <code>null</code> references are considered to be equal. The comparison
   * is case sensitive.
   * </p>
   *
   * <pre>
   * StringUtils.equals(null, null)   = true
   * StringUtils.equals(null, "abc")  = false
   * StringUtils.equals("abc", null)  = false
   * StringUtils.equals("abc", "abc") = true
   * StringUtils.equals("abc", "ABC") = false
   * </pre>
   *
   * @param str1 the first String, may be null
   * @param str2 the second String, may be null
   * @return <code>true</code> if the Strings are equal, case sensitive, or both <code>null</code>
   * @see java.lang.String#equals(Object)
   */
  public static boolean equals(String str1, String str2) {
    return str1 == null ? str2 == null : str1.equals(str2);
  }

  /**
   * <p>
   * Compares two Strings, returning <code>true</code> if they are equal ignoring the case.
   * </p>
   *
   * <p>
   * <code>null</code>s are handled without exceptions. Two <code>null</code> references are considered equal. Comparison is case
   * insensitive.
   * </p>
   *
   * <pre>
   * StringUtils.equalsIgnoreCase(null, null)   = true
   * StringUtils.equalsIgnoreCase(null, "abc")  = false
   * StringUtils.equalsIgnoreCase("abc", null)  = false
   * StringUtils.equalsIgnoreCase("abc", "abc") = true
   * StringUtils.equalsIgnoreCase("abc", "ABC") = true
   * </pre>
   *
   * @param str1 the first String, may be null
   * @param str2 the second String, may be null
   * @return <code>true</code> if the Strings are equal, case insensitive, or both <code>null</code>
   * @see java.lang.String#equalsIgnoreCase(String)
   */
  public static boolean equalsIgnoreCase(String str1, String str2) {
    return str1 == null ? str2 == null : str1.equalsIgnoreCase(str2);
  }

  /**
   * <p>
   * Check if a String starts with a specified prefix.
   * </p>
   *
   * <p>
   * <code>null</code>s are handled without exceptions. Two <code>null</code> references are considered to be equal. The comparison
   * is case sensitive.
   * </p>
   *
   * <pre>
   * StringUtils.startsWith(null, null)      = true
   * StringUtils.startsWith(null, "abc")     = false
   * StringUtils.startsWith("abcdef", null)  = false
   * StringUtils.startsWith("abcdef", "abc") = true
   * StringUtils.startsWith("ABCDEF", "abc") = false
   * </pre>
   *
   * @param str    the String to check, may be null
   * @param prefix the prefix to find, may be null
   * @return <code>true</code> if the String starts with the prefix, case sensitive, or both <code>null</code>
   * @see java.lang.String#startsWith(String)
   * @since 2.4
   */
  public static boolean startsWith(String str, String prefix) {
    return startsWith(str, prefix, false);
  }

  /**
   * <p>
   * Check if a String starts with a specified prefix (optionally case insensitive).
   * </p>
   *
   * @param str        the String to check, may be null
   * @param prefix     the prefix to find, may be null
   * @param ignoreCase inidicates whether the compare should ignore case (case insensitive) or not.
   * @return <code>true</code> if the String starts with the prefix or both <code>null</code>
   * @see java.lang.String#startsWith(String)
   */
  private static boolean startsWith(String str, String prefix, boolean ignoreCase) {
    if (str == null || prefix == null) {
      return str == null && prefix == null;
    }
    if (prefix.length() > str.length()) {
      return false;
    }
    return str.regionMatches(ignoreCase, 0, prefix, 0, prefix.length());
  }

  /**
   * <p>
   * Case insensitive check if a String starts with a specified prefix.
   * </p>
   *
   * <p>
   * <code>null</code>s are handled without exceptions. Two <code>null</code> references are considered to be equal. The comparison
   * is case insensitive.
   * </p>
   *
   * <pre>
   * StringUtils.startsWithIgnoreCase(null, null)      = true
   * StringUtils.startsWithIgnoreCase(null, "abc")     = false
   * StringUtils.startsWithIgnoreCase("abcdef", null)  = false
   * StringUtils.startsWithIgnoreCase("abcdef", "abc") = true
   * StringUtils.startsWithIgnoreCase("ABCDEF", "abc") = true
   * </pre>
   *
   * @param str    the String to check, may be null
   * @param prefix the prefix to find, may be null
   * @return <code>true</code> if the String starts with the prefix, case insensitive, or both <code>null</code>
   * @see java.lang.String#startsWith(String)
   * @since 2.4
   */
  public static boolean startsWithIgnoreCase(String str, String prefix) {
    return startsWith(str, prefix, true);
  }

  /**
   * <p>
   * Checks if the String contains only unicode digits. A decimal point is not a unicode digit and returns false.
   * </p>
   *
   * <p>
   * <code>null</code> will return <code>false</code>. An empty String (length()=0) will return <code>true</code>.
   * </p>
   *
   * <pre>
   * StringUtils.isNumeric(null)   = false
   * StringUtils.isNumeric("")     = true
   * StringUtils.isNumeric("  ")   = false
   * StringUtils.isNumeric("123")  = true
   * StringUtils.isNumeric("12 3") = false
   * StringUtils.isNumeric("ab2c") = false
   * StringUtils.isNumeric("12-3") = false
   * StringUtils.isNumeric("12.3") = false
   * </pre>
   *
   * @param str the String to check, may be null
   * @return <code>true</code> if only contains digits, and is non-null
   */
  public static boolean isNumeric(String str) {
    if (str == null) {
      return false;
    }
    int sz = str.length();
    for (int i = 0; i < sz; i++) {
      if (!Character.isDigit(str.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  public interface StringFormatter<T> {
    String format(T obj);
  }

  public static <T> String join(Collection<T> collection, String separator) {
    return join(collection, separator, new StringFormatter<T>() {
      @Override
      public String format(T obj) {
        return obj.toString();
      }
    });
  }

  public static <T> String join(Collection<T> collection, String separator,
                                StringFormatter<T> formatter) {
    Iterator<T> iterator = collection.iterator();
    // handle null, zero and one elements before building a buffer
    if (iterator == null) {
      return null;
    }
    if (!iterator.hasNext()) {
      return EMPTY;
    }
    T first = iterator.next();
    if (!iterator.hasNext()) {
      return first == null ? "" : formatter.format(first);
    }

    // two or more elements
    StringBuilder buf = new StringBuilder(256); // Java default is 16, probably too small
    if (first != null) {
      buf.append(formatter.format(first));
    }

    while (iterator.hasNext()) {
      buf.append(separator);
      T obj = iterator.next();
      if (obj != null) {
        buf.append(formatter.format(obj));
      }
    }

    return buf.toString();
  }
}
