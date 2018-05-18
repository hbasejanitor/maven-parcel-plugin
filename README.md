# maven-parcel-plugin
A maven plugin to create parcels.

I wrote this to make it as easy as possible to create a parcel from a java project.   The idea is to 

to use it to build a flume plugin, there is one step:

Add the following to the <build> 

<plugin>
	<groupId>org.hbasejanitor</groupId>
	<artifactId>parcel-maven-plugin</artifactId>
	<version>0.0.1-SNAPSHOT</version>
	<configuration>
	    <targetTypeOfParcel>flume</targetTypeOfParcel>
	    <!-- parcel names contain the linux version, if it doesn't matter put el7/el6/weezy depending on the RHEL/centos/whatever version -->
	    <rhelVersion>${rh.version}</rhelVersion>
	    <!-- parcelName must be upper case -->
	    <parcelName>UPPERCASE_PARCEL_NAME</parcelName>
	    <cdhDepend>CDH (&gt;= 5.2), CDH (&lt;&lt; 6.0)</cdhDepend>
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

