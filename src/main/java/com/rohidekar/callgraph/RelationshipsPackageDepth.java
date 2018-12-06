package com.rohidekar.callgraph;

import org.apache.bcel.classfile.JavaClass;
import org.apache.commons.lang.ClassUtils;
import org.apache.commons.lang.StringUtils;

public class RelationshipsPackageDepth {

  // The top level package with classes in it
  private int minPackageDepth = Integer.MAX_VALUE;

  public int getMinPackageDepth() {
    return minPackageDepth;
  }

  public void updateMinPackageDepth(JavaClass javaClass) {
    int packageDepth = getPackageDepth(javaClass.getClassName());
    if (packageDepth < minPackageDepth) {
      minPackageDepth = packageDepth;
    }
  }

  public static int getPackageDepth(String qualifiedClassName) {
    String packageName = ClassUtils.getPackageName(qualifiedClassName);
    int periodCount = StringUtils.countMatches(packageName, ".");
    int packageDepth = periodCount + 1;
    return packageDepth;
  }
}
