# jca [![Build Status](https://travis-ci.org/epics-base/jca.svg?branch=master)](https://travis-ci.org/epics-base/jca)

Java Channel Access client and server API and a pure java implementation for both.

### Java Requirements

- Java 8 or later JDK (e.g., [Oracle Java SE](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html))
- [Maven 2.x](https://maven.apache.org/)

### Download jca

**Jar:**

[Download jca-2.4.2 jars](https://repo1.maven.org/maven2/org/epics/jca/2.4.2/)

**Maven:**

```
<dependency>
  <groupId>org.epics</groupId>
  <artifactId>jca</artifactId>
  <version>2.4.2</version>
</dependency>
```

**Development Release**  
You can also download the lastest development snapshots from the sonatype snapshot repository

[Download jca-2.4.3-SNAPSHOT jars](https://oss.sonatype.org/content/repositories/snapshots/org/epics/jca/2.4.3-SNAPSHOT/)

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


### Build/Install

Clone the Git repository and run a Maven `install` in the top directory.
```
$ git clone https://github.com/epics-base/jca.git
$ cd jca
$ mvn install
```
