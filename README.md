# Reactive Relational Database Connectivity Proxy Framework

This project contains the proxy framework of the [R2DBC SPI][r].

[r]: https://github.com/r2dbc/r2dbc-spi

## Maven
Both milestone and snapshot artifacts (library, source, and javadoc) can be found in Maven repositories.

```xml
<dependency>
  <groupId>io.r2dbc</groupId>
  <artifactId>r2dbc-proxy</artifactId>
  <version>1.0.0.M5</version>
</dependency>
```

Artifacts can be found at the following repositories.

### Repositories
```xml
<repository>
    <id>spring-snapshots</id>
    <name>Spring Snapshots</name>
    <url>https://repo.spring.io/snapshot</url>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

```xml
<repository>
    <id>spring-milestones</id>
    <name>Spring Milestones</name>
    <url>https://repo.spring.io/milestone</url>
    <snapshots>
        <enabled>false</enabled>
    </snapshots>
</repository>
```

## Usage
Configuration of the `ConnectionFactory` can be accomplished in two ways:

### Connection Factory Discovery
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
| `proxyListener` | Fully qualified proxy listener class name  _(Optional)_

When programmatically `ConnectionFactoryOptions` are constructed, `proxyListener` option allows following values:
- Fully qualified proxy listener class name
- Proxy listener class
- Proxy listener instance
- Collection of above

### Programmatic
```java
ConnectionFactory original = ...

ConnectionFactory connectionFactory = ProxyConnectionFactory.builder(original)
    .onAfterQuery(mono ->
        ...  // after query callback logic
    )
    .onBeforeMethod(mono ->
        ...  // before method callback logic
    )
    .listener(...)  // add listener
    .build();

Mono<Connection> connection = connectionFactory.create();
```

## License
This project is released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0
