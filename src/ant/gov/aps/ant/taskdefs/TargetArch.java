
package gov.aps.ant.taskdefs;

import gov.aps.jca.jni.JNITargetArch;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.Project;


public class TargetArch extends Task {
  private String name;

  // The method executing the task
  public void execute() throws BuildException {
    Project project=getProject();

    String targetArch= JNITargetArch.getTargetArch();

    project.setProperty( name, targetArch );
    project.setProperty( name+"."+targetArch, "true" );
  }

  public void setName( String name ) {
    this.name=name;
  }

}
