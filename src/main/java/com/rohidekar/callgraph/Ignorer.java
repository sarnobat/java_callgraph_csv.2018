package com.rohidekar.callgraph;

import org.apache.bcel.classfile.JavaClass;

public class Ignorer {
  private static final String[] substringsToIgnore = {
    "java", "Logger", ".toString", "Exception",
  };

  public static boolean shouldIgnore(JavaClass iClass) {
    return shouldIgnore(iClass.getClassName());
  }

  public static boolean shouldIgnore(String classFullName) {
    for (String substringToIgnore : substringsToIgnore) {
      if (classFullName.contains(substringToIgnore)) {
        return true;
      }
    }
    System.err.println(classFullName + " was not ignored");
    return false;
  }
}
