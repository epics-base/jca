# jca [![Build Status](https://travis-ci.org/epics-base/jca.svg?branch=master)](https://travis-ci.org/epics-base/jca)

Java Channel Access client and server API and a pure java implementation for both.

### Java Requirements

- Java 8 or later JDK (e.g., [Oracle Java SE](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html))
- [Maven 2.x](https://maven.apache.org/)

### Download jca

**Latest release**  

The jca artifacts are hosted on maven centeral and can be downloaded from https://mvnrepository.com/artifact/org.epics/jca  

You can also directly download the jca jars along with their sources and javadocs from    
[jca-2.4.3 jars](https://repo1.maven.org/maven2/org/epics/jca/2.4.3/)

The javadocs are also avaiable online:  
[jca-2.4.3-javadocs](https://www.javadoc.io/doc/org.epics/jca/latest/index.html)

**Development Release**  
You can also download the lastest development snapshots from the sonatype snapshot repository

[Download jca-2.4.4-SNAPSHOT jars](https://oss.sonatype.org/content/repositories/snapshots/org/epics/jca/2.4.4-SNAPSHOT/)

or add the sonatype repository to your pom

```
<!-- Explicitly declare snapshot repository -->
<repositories>
  <repository>
    <id>sonatype-nexus-snapshots</id>
    <name>OSS Snapshots</name>
    <url>https://oss.sonatype.org/content/repositories/snapshots</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>org.epics</groupId>
    <artifactId>jca</artifactId>
    <version>2.4.3-SNAPSHOT</version>
  </dependency>
</dependencies>
```

**Archived Release**  

Older released of jca are archived [here](https://repo1.maven.org/maven2/org/epics/jca/)

### Build/Install

Clone the Git repository and run a Maven `install` in the top directory.
```
$ git clone https://github.com/epics-base/jca.git
$ cd jca
$ mvn install
```
