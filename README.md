# maven-parcel-plugin
A maven plugin to create parcels.

I wrote this to make it as easy as possible to create a parcel from a java project.

to use it to build a flume plugin, there is one step:

Add the following to the build section of your pom.xml:

```
<plugin>
        <groupId>org.hbasejanitor</groupId>
        <artifactId>parcel-maven-plugin</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <configuration>
            <targetTypeOfParcel>flume</targetTypeOfParcel>
            <parcelName>${parcel base name}</parcelName>
            <cdhVersion>${cdh.version}</cdhVersion>
            <rhelVersion>${rh.version}</rheglVersion>
        </configuration>
        <executions>
                <execution>
                        <phase>package</phase>
                        <goals>
                                <goal>makeparcel</goal>
                        </goals>
                </execution>
        </executions>
</plugin>
```

This will build the parcel automagically as part of your build process.

The plugin will:
1. Generate the evn.sh, alternatives.json, parcel.json,
2. Create a tar file with the correct directory structure, based on the values for ${parcelName}, ${cdh.version}, and ${rh.version}
3. It then generates the filelist.json file
4. Finally, it creates the file .parcel file with the correct naming conventions.


Limitations:
1. You can only have one component per parcel


Have fun!
