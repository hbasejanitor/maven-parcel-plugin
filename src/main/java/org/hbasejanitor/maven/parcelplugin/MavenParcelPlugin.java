package org.hbasejanitor.maven.parcelplugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Generate a parcel from the assembly tar (assuming the meta/ directory and needed files are there)
 */
@Mojo(name = "makeparcel", requiresDependencyResolution = ResolutionScope.COMPILE)
public class MavenParcelPlugin extends AbstractMojo  {

  @Parameter( property = "makeparcel.targetTypeOfParcel")
  private String targetTypeOfParcel;
  
//  @Parameter( property = "makeparcel.parcelName")
//  private String parcelName;

  @Parameter( property = "makeparcel.rhelVersion")
  private String rhelVersion = "el6";

  @Parameter( property = "makeparcel.parcelName")
  private String parcelName;

  @Parameter( property = "makeparcel.cdhVersion")
  private String cdhVersion;

  @Component
  MavenProject project;
  
  @Component
  MavenSession mavenSession;
  
  @Component
  BuildPluginManager pluginManager;

  public static String PARCEL_TYPE_FLUME_PLIUGIN = "flume";
  
  private Map<String, String> typeToTemplate;

  private Map<String, List<String>> typeToTemplateFiles;



  public MavenParcelPlugin() {
    typeToTemplate = new HashMap<>();
    typeToTemplate.put(PARCEL_TYPE_FLUME_PLIUGIN, PARCEL_TYPE_FLUME_PLIUGIN+"-assembly-template.xml");

    typeToTemplateFiles = new HashMap<>();
    typeToTemplateFiles.put(PARCEL_TYPE_FLUME_PLIUGIN, new ArrayList<>());
    typeToTemplateFiles.get(PARCEL_TYPE_FLUME_PLIUGIN).add("templates/flume/alternatives.json");
    typeToTemplateFiles.get(PARCEL_TYPE_FLUME_PLIUGIN).add("templates/flume/parcel.json");
    typeToTemplateFiles.get(PARCEL_TYPE_FLUME_PLIUGIN).add("templates/flume/env.sh");
  }
  
  public static void copy(InputStream in, OutputStream out,String parcelNameAndVersion) throws Exception {
    StringBuilder sbuff = new StringBuilder();
    //${parcelName}
    byte buff[] = new byte[2048];
    int count = in.read(buff);
    while (count > -1) {
      //out.write(buff,0,count);
      sbuff.append(new String(buff,0,count));
      count = in.read(buff);
    }

    int where = sbuff.indexOf("${parcelName}");
    while (where > -1) {
      sbuff.delete(where, where+"${parcelName}".length());
      sbuff.insert(where, parcelNameAndVersion);
      where = sbuff.indexOf("${parcelName}");
    }
    out.write(sbuff.toString().getBytes());
  }
  
  
  public List<String> getDirectoryNames(String entryName){
    List<String> directoryNames = new ArrayList<>();
    String splits[] = entryName.split("/");
    for (int i = 0 ; i < splits.length-1;i++) {
      directoryNames.add(StringUtils.join(Arrays.copyOfRange(splits, 0, i+1), "/")+"/");      
    }
    
    return directoryNames;
  }
  
  public void execute() throws MojoExecutionException {
//    String artifactId = project.getArtifactId();
    
    // if snapshot, we'll add currenttimemills() to the version.
//    String version=project.getVersion();
//    long extra = System.currentTimeMillis();
    
//    if (version.endsWith("SNAPSHOT")) {
//      version += "-";
//      version += String.valueOf(extra);
//    }

    String assemblyDescriptor = typeToTemplate.get(targetTypeOfParcel);
    
    File tempAssemblyDir = new File("target/parcelplugin/tempassembly");
    tempAssemblyDir.mkdirs();
    File tempAssembly = new File(tempAssemblyDir,"assembly.xml");

//    File tempAssemblyOut = new File("target/parcelplugin/tempassemblydone");
//    tempAssemblyOut.mkdirs();
    
    getLog().info("using template file "+assemblyDescriptor);
    

    
    
    try (InputStream resource = MavenParcelPlugin.class.getResourceAsStream("/"+assemblyDescriptor);
        FileOutputStream fout = new FileOutputStream(tempAssembly);
        ) {


        String ver = mavenSession.getCurrentProject().getVersion();

        if (ver.endsWith("-SNAPSHOT")){
            long time=System.currentTimeMillis();
            ver = ver.replace("-SNAPSHOT","_"+String.valueOf(time));
        }

      String finalParcelName=parcelName+"-"+ver;
      
      MavenParcelPlugin.copy(resource,fout,finalParcelName+"-"+cdhVersion);
      
      getLog().info("parcel name "+parcelName+" version "+ver);

      FileWriter filter = new FileWriter("target/parcel-filters.txt");
      Properties p = new Properties();
      p.put("parcelName",parcelName);
      p.put("parcelVersion",ver+"-"+cdhVersion);
      p.store(filter,"substitutions");
      IOUtils.closeQuietly(filter);

      File templateDir = new File("target/classes/templates");
      templateDir.mkdirs();

      for (String templateFile : typeToTemplateFiles.get(targetTypeOfParcel)){
        getLog().info("copy template file "+templateFile);

        try (InputStream templateFileIn= MavenParcelPlugin.class.getResourceAsStream("/"+templateFile);
             FileOutputStream templateOutput = new FileOutputStream(
                     new File(templateDir,templateFile.substring(templateFile.lastIndexOf("/")+1)));){
          IOUtils.copy(templateFileIn,templateOutput);
        }
      }

//      FileWriter testMySubs = new FileWriter(new File(templateDir,"woot.txt"));
//      testMySubs.write("my name is ${foobar}, yo version ${project.version}");
//      IOUtils.closeQuietly(testMySubs);

      //maven-assembly-plugin to build the tar
      executeMojo(plugin("org.apache.maven.plugins","maven-assembly-plugin","3.1.0"), 
      "single", 
      configuration(element("descriptors", element("descriptor", tempAssembly.toString())),
          element("attach","false"),
          element("finalName",finalParcelName),
          element("filters",element("filter","target/parcel-filters.txt"))
          ),
      executionEnvironment(project,mavenSession,pluginManager));
      
      File myAssembledTar = new File("target/"+finalParcelName+"-distribution.tar.gz");


      // get the parcel.json file
      try (TarArchiveInputStream tin =
                   new TarArchiveInputStream(
                           new GZIPInputStream(
                                   new FileInputStream(myAssembledTar)));
          FileOutputStream myparcel = new FileOutputStream("target/classes/templates/final.parcel.json")){
        TarArchiveEntry entry = tin.getNextTarEntry();
        while (entry !=null) {
          if (entry.getName().endsWith("meta/parcel.json")){
            IOUtils.copy(tin,myparcel);
            break;
          }
          entry = tin.getNextTarEntry();
        }
      }

      FileReader parcelReader = new FileReader(new File("target/classes/templates/final.parcel.json"));
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      ParcelInfo info =gson.fromJson(parcelReader, ParcelInfo.class);

      Map<String, FileListComponent> componentToFiles = new HashMap<>();
      String componentName=info.components.get(0).name;
      componentToFiles.put(componentName,new FileListComponent());
      componentToFiles.get(componentName).name=componentName;
      componentToFiles.get(componentName).files= new HashMap<>();
      
      File parcelFile= new File("target/"+finalParcelName+"-"+cdhVersion+"-"+rhelVersion+".parcel");
      
      try (TarArchiveInputStream tin = 
            new TarArchiveInputStream(
              new GZIPInputStream(
                new FileInputStream(myAssembledTar)));
          TarArchiveOutputStream tout = 
              new TarArchiveOutputStream(
                new GZIPOutputStream(
                  new FileOutputStream(parcelFile)))) {
        tout.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

        Set<String> directoryNames = new HashSet<>();

        TarArchiveEntry entry = tin.getNextTarEntry();
        while (entry !=null) {
          componentToFiles.get(componentName).files.put(entry.getName(), new String[] {});
          
          List<String> dirNamesForEntry = getDirectoryNames(entry.getName());
          for (String item : dirNamesForEntry) {
            if (!directoryNames.contains(item)) {
              TarArchiveEntry tentry = new TarArchiveEntry(item);
              tentry.setGroupId(0);
              tentry.setUserId(0);
              tout.putArchiveEntry(tentry);
              tout.closeArchiveEntry();
              directoryNames.add(item);
            }
          }
          tout.putArchiveEntry(entry);
          IOUtils.copy(tin, tout);
          tout.closeArchiveEntry();
          entry = tin.getNextTarEntry();
        }
        
       
        
        TarArchiveEntry filesEntry = new TarArchiveEntry(finalParcelName+"-"+cdhVersion+"/meta/filelist.json");
        byte fileList[]=gson.toJson(componentToFiles).getBytes();
        filesEntry.setSize(fileList.length);
        tout.putArchiveEntry(filesEntry);
        
        tout.write(fileList);
        tout.closeArchiveEntry();
        
        
      }

      myAssembledTar.delete();
      
      // generate the sha1 hash
      String hash = getSha1Hash(parcelFile);
      
      File hashFile = new File(parcelFile.getParent(),parcelFile.getName()+".sha");
      FileOutputStream hfout = new FileOutputStream(hashFile);
      hfout.write(hash.getBytes());
      org.apache.commons.io.IOUtils.closeQuietly(hfout);
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new MojoExecutionException(e.getMessage());
    }
  }

  public static String getSha1Hash(File file) throws Exception {
    
    MessageDigest sha1 = MessageDigest.getInstance("SHA1");
    try (FileInputStream fis = new FileInputStream(file);  ){
      byte[] data = new byte[1024];
      int read = 0; 
      while ((read = fis.read(data)) != -1) {
          sha1.update(data, 0, read);
      };
      byte[] hashBytes = sha1.digest();

      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < hashBytes.length; i++) {
        sb.append(Integer.toString((hashBytes[i] & 0xff) + 0x100, 16).substring(1));
      }
       
      return sb.toString();
    } 
  }
}