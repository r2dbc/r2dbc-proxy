# Reactive Relational Database Connectivity Proxy Framework [![Java CI with Maven](https://github.com/r2dbc/r2dbc-proxy/workflows/Java%20CI%20with%20Maven/badge.svg?branch=main)](https://github.com/r2dbc/r2dbc-proxy/actions?query=workflow%3A%22Java+CI+with+Maven%22+branch%3Amain) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.r2dbc/r2dbc-proxy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.r2dbc/r2dbc-proxy)

This project contains the proxy framework of the [R2DBC SPI][r]. R2DBC is a [Reactive Foundation][rf] project.

[r]: https://github.com/r2dbc/r2dbc-spi
[rf]: https://reactive.foundation

## Code of Conduct

This project is governed by the [R2DBC Code of Conduct](https://github.com/r2dbc/.github/blob/main/CODE_OF_CONDUCT.adoc). By participating, you are expected to uphold this code of conduct. Please report unacceptable behavior to [info@r2dbc.io](mailto:info@r2dbc.io).


## Maven configuration

Artifacts can be found on [Maven Central](https://search.maven.org/search?q=r2dbc-proxy):

```xml
<dependency>
  <groupId>io.r2dbc</groupId>
  <artifactId>r2dbc-proxy</artifactId>
  <version>${version}</version>
</dependency>
```

If you'd rather like the latest snapshots of the upcoming major version, use our Maven snapshot repository and declare the appropriate dependency version.

```xml
<dependency>
  <groupId>io.r2dbc</groupId>
  <artifactId>r2dbc-proxy</artifactId>
  <version>${version}.BUILD-SNAPSHOT</version>
</dependency>

<repository>
  <id>sonatype-nexus-snapshots</id>
  <name>Sonatype OSS Snapshot Repository</name>
  <url>https://oss.sonatype.org/content/repositories/snapshots</url>
</repository>
```

## R2DBC SPI versions

| R2DBC Proxy | R2DBC SPI
| ----------- | ----------
|     `1.0.x`, `1.1.x` | `1.0.x`
|     `0.9.x` | `0.9.x`
|     `0.8.x` | `0.8.x`


## Documentation

- Current release
  - [Reference Doc.][reference-current]
  - [API Doc.][javadoc-current]
  - [Changelog][changelog-current]
- Snapshot
  - [Reference Doc.][reference-snapshot]
  - [API Doc.][javadoc-snapshot]
  - [Changelog][changelog-snapshot]
- Other versions (TBD)

[reference-current]: http://r2dbc.io/r2dbc-proxy/docs/current/docs/html
[reference-snapshot]: http://r2dbc.io/r2dbc-proxy/docs/current-snapshot/docs/html
[javadoc-current]: http://r2dbc.io/r2dbc-proxy/docs/current/api/
[javadoc-snapshot]: http://r2dbc.io/r2dbc-proxy/docs/current-snapshot/api/
[changelog-current]: http://r2dbc.io/r2dbc-proxy/docs/current/CHANGELOG.txt
[changelog-snapshot]: http://r2dbc.io/r2dbc-proxy/docs/current-snapshot/CHANGELOG.txt


## Getting Started

Here shows how to create a proxy `ConnectionFactory`.

### URL Connection Factory Discovery

```java
ConnectionFactory connectionFactory = ConnectionFactories.get("r2dbc:proxy:<driver>//<host>:<port>>/<database>[?proxyListener=<fqdn>]");
```

Sample URLs:
```
# with driver
r2dbc:proxy:postgresql://localhost:5432/myDB?proxyListener=com.example.MyListener

# with pooling
r2dbc:proxy:pool:postgresql://localhost:5432/myDB?proxyListener=com.example.MyListener&maxIdleTime=PT60S
```

### Programmatic Connection Factory Discovery
```java
ConnectionFactory connectionFactory = ConnectionFactories.get(ConnectionFactoryOptions.builder()
   .option(DRIVER, "proxy")
   .option(PROTOCOL, "postgresql")
   .build());

Mono<Connection> connection = connectionFactory.create();
```

Supported Connection Factory Discovery options:

| Option | Description
| ------ | -----------
| `driver` | Must be `proxy`
| `protocol` | Delegating connection factory driver
| `proxyListener` | Comma separated list of fully qualified proxy listener class names  _(Optional)_

When programmatically `ConnectionFactoryOptions` are constructed, `proxyListener` option allows following values:
- Comma separated list of fully qualified proxy listener class names
- Proxy listener class
- Proxy listener instance
- Collection of above

### Programmatic creation with `ProxyConnectionFactory`

```java
ConnectionFactory original = ...

ConnectionFactory connectionFactory = ProxyConnectionFactory.builder(original)
    .onAfterQuery(queryInfo ->
        ...  // after query callback logic
    )
    .onBeforeMethod(methodInfo ->
        ...  // before method callback logic
    )
    .listener(...)  // add listener
    .build();

Publisher<? extends Connection> connectionPublisher = connectionFactory.create();

// Alternative: Creating a Mono using Project Reactor
Mono<Connection> connectionMono = Mono.from(connectionFactory.create());
```


## Samples

[r2dbc-proxy-samples][r2dbc-proxy-samples] repository contains sample listener implementations.

- Distributed tracing - [TracingExecutionListener][TracingExecutionListener]
- Micrometer metrics - [MetricsExecutionListener][MetricsExecutionListener]


[r2dbc-proxy-samples]: https://github.com/ttddyy/r2dbc-proxy-examples
[TracingExecutionListener]: https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/listener-example/src/main/java/io/r2dbc/examples/TracingExecutionListener.java
[MetricsExecutionListener]: https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/listener-example/src/main/java/io/r2dbc/examples/MetricsExecutionListener.java

## Getting Help

Having trouble with R2DBC? We'd love to help!

* Check the [spec documentation](https://r2dbc.io/spec/0.8.1.RELEASE/spec/html/), and [Javadoc](https://r2dbc.io/spec/0.8.1.RELEASE/api/).
* If you are upgrading, check out the [changelog](https://r2dbc.io/spec/0.8.1.RELEASE/CHANGELOG.txt) for "new and noteworthy" features.
* Ask a question - we monitor [stackoverflow.com](https://stackoverflow.com) for questions
  tagged with [`r2dbc`](https://stackoverflow.com/tags/r2dbc). 
  You can also chat with the community on [Gitter](https://gitter.im/r2dbc/r2dbc).
* Report bugs with R2DBC Proxy at [github.com/r2dbc/r2dbc-proxy/issues](https://github.com/r2dbc/r2dbc-proxy/issues).

## Reporting Issues

R2DBC uses GitHub as issue tracking system to record bugs and feature requests. 
If you want to raise an issue, please follow the recommendations below:

* Before you log a bug, please search the [issue tracker](https://github.com/r2dbc/r2dbc-proxy/issues) to see if someone has already reported the problem.
* If the issue doesn't already exist, [create a new issue](https://github.com/r2dbc/r2dbc-proxy/issues/new).
* Please provide as much information as possible with the issue report, we like to know the version of R2DBC Proxy that you are using and JVM version.
* If you need to paste code, or include a stack trace use Markdown ``` escapes before and after your text.
* If possible try to create a test-case or project that replicates the issue. 
Attach a link to your code or a compressed file containing your code.

## Building from Source

You don't need to build from source to use R2DBC Proxy (binaries in Maven Central), but if you want to try out the latest and greatest, R2DBC Proxy can be easily built with the
[maven wrapper](https://github.com/takari/maven-wrapper). You also need JDK 1.8 and Docker to run integration tests.

```bash
 $ ./mvnw clean install
```

If you want to build with the regular `mvn` command, you will need [Maven v3.5.0 or above](https://maven.apache.org/run-maven/index.html).

_Also see [CONTRIBUTING.adoc](https://github.com/r2dbc/.github/blob/main/CONTRIBUTING.adoc) if you wish to submit pull requests. Commits require `Signed-off-by` (`git commit -s`) to ensure [Developer Certificate of Origin](https://developercertificate.org/)._

### Building the documentation

Building the documentation uses [maven asciidoctor plugin][asciidoctor-maven-plugin].

```bash
 $ ./mvnw clean exec:java@generate-micrometer-docs asciidoctor:process-asciidoc
```

[asciidoctor-maven-plugin]: https://github.com/asciidoctor/asciidoctor-maven-plugin


## Staging to Maven Central

To stage a release to Maven Central, you need to create a release tag (release version) that contains the desired state and version numbers (`mvn versions:set versions:commit -q -o -DgenerateBackupPoms=false -DnewVersion=x.y.z.(RELEASE|Mnnn|RCnnn`) and force-push it to the `release-0.x` branch. This push will trigger a Maven staging build (see `build-and-deploy-to-maven-central.sh`).

## License
This project is released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0
