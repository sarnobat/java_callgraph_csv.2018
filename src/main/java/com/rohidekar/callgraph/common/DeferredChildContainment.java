// Copyright 2012 Google Inc. All Rights Reserved.

package com.rohidekar.callgraph.common;

import org.apache.bcel.classfile.JavaClass;

/**
 * When the child does not exist
 * 
 * @author ssarnobat@google.com (Sridhar Sarnobat)
 */
class DeferredChildContainment {
  private String childClassQualifiedName ;
  private JavaClass parentClass;
  public DeferredChildContainment(JavaClass parentClass, String childClassQualifiedName){
    this.childClassQualifiedName = childClassQualifiedName;
    this.parentClass = parentClass;
  }

  public String getClassQualifiedName() {
    return childClassQualifiedName;
  }

  public JavaClass getParentClass() {
    return parentClass;
  }
}
