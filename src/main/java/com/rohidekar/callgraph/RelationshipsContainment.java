package com.rohidekar.callgraph;

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;

import com.google.common.collect.ImmutableSet;

public class RelationshipsContainment {

	  // Objects that cannot yet be found
	  private Set<DeferredChildContainment> deferredChildContainments =
	      new HashSet<DeferredChildContainment>();

	  public boolean deferContainmentVisit(
	      JavaClass parentClassToVisit, String childClassQualifiedName) {
	    return this.deferredChildContainments.add(
	        new DeferredChildContainment(parentClassToVisit, childClassQualifiedName));
	  }

	  public Set<DeferredChildContainment> getDeferredChildContainment() {
	    return ImmutableSet.copyOf(this.deferredChildContainments);
	  }

}
