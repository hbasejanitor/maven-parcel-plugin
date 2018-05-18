package org.hbasejanitor.maven.parcelplugin;

import java.util.ArrayList;
import java.util.List;

public class ParcelInfo {
  public static class ComponentInfo {
    public String name;
    public String version;
    public String pkg_version;
    public String pkg_release;
  }
  
  public String name;
  public String version;
  public String depends;
  public String setActiveSymlink;
  
  List<ComponentInfo> components = new ArrayList<>();  
  
}
