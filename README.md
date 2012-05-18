# pallet.common

A library containing general purpose functions and macros for use across other
pallet libraries.

See [reference documentation](http://pallet.github.com/common/autodoc/index.html)
and [annotated source](http://pallet.github.com/common/marginalia/uberdoc.html).


## Installation

pallet-common is distributed as a jar, and is available in the
[sonatype repository](http://oss.sonatype.org/content/repositories/releases/org/cloudhoist).

Installation is with maven or your favourite maven repository aware build tool.

### lein/cake project.clj

    :dependencies [[org.cloudhoist/pallet-common "0.2.3"]]
    :repositories {"sonatype"
                   "http://oss.sonatype.org/content/repositories/releases"}

### maven pom.xml

    <dependencies>
      <dependency>
        <groupId>org.cloudhoist</groupId>
        <artifactId>pallet-common</artifactId>
        <version>0.2.3</version>
      </dependency>
    <dependencies>

    <repositories>
      <repository>
        <id>sonatype</id>
        <url>http://oss.sonatype.org/content/repositories/releases</url>
      </repository>
    </repositories>

## License

Licensed under [EPL](http://www.eclipse.org/legal/epl-v10.html)

Copyright 2011, 2012 Hugo Duncan.
