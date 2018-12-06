package com.rohidekar.callgraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class RelationshipsClassNames {

  RelationshipsClassNames(Map<String, JavaClass> javaClasses) {

    this.classNameToJavaClassMap = ImmutableMap.copyOf(javaClasses);
  }
  // nodes
  private ImmutableMap<String, JavaClass> classNameToJavaClassMap;

  public JavaClass getClassDef(String aClassFullName) {
    JavaClass jc = null;
    try {
      jc = Repository.lookupClass(aClassFullName);
    } catch (ClassNotFoundException e) {
      if (this.classNameToJavaClassMap.get(aClassFullName) != null) {
        System.err.println("We do need our own homemade repository. I don't know why");
      }
    }
    if (jc == null) {
      jc = this.classNameToJavaClassMap.get(aClassFullName);
    }
    return jc;
  }

  public Collection<JavaClass> getParentClassesAndInterfaces(JavaClass childClass) {
    Collection<JavaClass> superClassesAndInterfaces = new HashSet<JavaClass>();
    String[] interfaceNames = childClass.getInterfaceNames();
    for (String interfaceName : interfaceNames) {
      JavaClass anInterface = this.classNameToJavaClassMap.get(interfaceName);
      if (anInterface == null) {
        // Do it later
        deferParentContainment(interfaceName, childClass);
      } else {
        superClassesAndInterfaces.add(anInterface);
      }
    }
    String superclassNames = childClass.getSuperclassName();
    if (!superclassNames.equals("java.lang.Object")) {
      JavaClass theSuperclass = this.classNameToJavaClassMap.get(superclassNames);
      if (theSuperclass == null) {
        // Do it later
        deferParentContainment(superclassNames, childClass);
      } else {
        superClassesAndInterfaces.add(theSuperclass);
      }
    }
    if (superClassesAndInterfaces.size() > 0) {
      System.err.println("Has a parent (" + childClass.getClassName() + ")");
    }
    return ImmutableSet.copyOf(superClassesAndInterfaces);
  }

  private Set<DeferredParentContainment> deferredParentContainments =
      new HashSet<DeferredParentContainment>();

  public void deferParentContainment(String parentClassName, JavaClass javaClass) {
    System.err.println("Deferring " + parentClassName + " --> " + javaClass.getClassName());
    this.deferredParentContainments.add(new DeferredParentContainment(parentClassName, javaClass));
  }

  public Collection<JavaClass> getClassNameToJavaClassMapValues() {
    return this.classNameToJavaClassMap.values();
  }

  public Set<DeferredParentContainment>
      getDeferredParentContainments() { // TODO Auto-generated method stub
    return ImmutableSet.copyOf(deferredParentContainments);
  }
}
