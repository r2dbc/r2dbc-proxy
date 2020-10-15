# Reactive Relational Database Connectivity Proxy Framework [![Java CI with Maven](https://github.com/r2dbc/r2dbc-proxy/workflows/Java%20CI%20with%20Maven/badge.svg?branch=main)](https://github.com/r2dbc/r2dbc-proxy/actions?query=workflow%3A%22Java+CI+with+Maven%22+branch%3Amain) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.r2dbc/r2dbc-proxy/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.r2dbc/r2dbc-proxy)

This project contains the proxy framework of the [R2DBC SPI][r].

[r]: https://github.com/r2dbc/r2dbc-spi

## Code of Conduct

This project is governed by the [R2DBC Code of Conduct](https://github.com/r2dbc/.github/blob/main/CODE_OF_CONDUCT.adoc). By participating, you are expected to uphold this code of conduct. Please report unacceptable behavior to [info@r2dbc.io](mailto:info@r2dbc.io).


### Maven configuration

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
| `proxyListener` | Comma separated list of fully qualified proxy listener class names  _(Optional)_

When programmatically `ConnectionFactoryOptions` are constructed, `proxyListener` option allows following values:
- Comma separated list of fully qualified proxy listener class names
- Proxy listener class
- Proxy listener instance
- Collection of above

### Programmatic
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

----

## Use cases

### Query logging

When query is executed by `Batch#execute()` or `Statement#execute()`, listener receives query
callbacks.
The callback contains query execution information(`QueryExecutionInfo`) such as query string,
execution type, bindings, execution time, etc.  
Users can format this contextual information and perform logging.

*Sample Output (wrapped for display purpose):*
```sql
# Statement with no bindings
#
Thread:reactor-tcp-nio-1(30) Connection:1
Transaction:{Create:1 Rollback:0 Commit:0}
Success:True Time:34
Type:Statement BatchSize:0 BindingsSize:0
Query:["SELECT value FROM test"], Bindings:[]

# Batch query
#
Thread:reactor-tcp-nio-3(32) Connection:2
Transaction:{Create:1 Rollback:0 Commit:0}
Success:True Time:4
Type:Batch BatchSize:2 BindingsSize:0
Query:["INSERT INTO test VALUES(200)","SELECT value FROM test"], Bindings:[]

# Statement with multiple bindings
#
Thread:reactor-tcp-nio-1(30) Connection:3
Transaction:{Create:1 Rollback:0 Commit:0}
Success:True Time:21
Type:Statement BatchSize:0 BindingsSize:2
Query:["INSERT INTO test VALUES ($1,$2)"], Bindings:[(100,101),(200,null(int))]
```

### Method tracing

When any methods on proxy classes(`ConnectionFactory`, `Connection`, `Batch`, `Statement`, or `Result`)
are called, listeners receive callbacks on before and after invocations.

Below output simply printed out the method execution information(`MethodExecutionInfo`)
at each method invocation.  
Essentially, this shows interaction with R2DBC SPI.

*Sample: Execution with transaction:*
```sql
  1: Thread:34 Connection:1 Time:16  PostgresqlConnectionFactory#create()
  2: Thread:34 Connection:1 Time:0  PostgresqlConnection#createStatement()
  3: Thread:34 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#bind()
  4: Thread:34 Connection:1 Time:0  ExtendedQueryPostgresqlStatement#add()
  5: Thread:34 Connection:1 Time:5  PostgresqlConnection#beginTransaction()
  6: Thread:34 Connection:1 Time:5  ExtendedQueryPostgresqlStatement#execute()
  7: Thread:34 Connection:1 Time:3  PostgresqlConnection#commitTransaction()
  8: Thread:34 Connection:1 Time:4  PostgresqlConnection#close()
```

### Slow query detection

There are two types of slow query detection.
- Detect slow query *AFTER* query has executed.
- Detect slow query *WHILE* query is running.

Former is simple. On `afterQuery` callback, check the execution time.
If it took more than threshold, perform an action such as logging, send notification, etc.

To perform some action _while_ query is still executing and it has passed the threshold time, one implementation
is to create a watcher that checks running queries and notify ones exceeded the threshold.
In [datasource-proxy](datasource-proxy), [`SlowQueryListener` is implemented in this way][slow-query-doc].

[datasource-proxy]: https://github.com/ttddyy/datasource-proxy
[slow-query-doc]: https://ttddyy.github.io/datasource-proxy/docs/current/user-guide/#_slow_query_logging_listener


### Distributed Tracing

Using before and after callbacks with contextual information, it can easily construct tracing spans.

Sample implementation: [TracingExecutionListener][TracingExecutionListener]

*Tracing*
![Tracing][zipkin-tracing-rollback]

*Connection Span*
![Connection Span][zipkin-span-connection]

*Query Span*
![Query Span][zipkin-span-batch-query]

[zipkin-tracing-rollback]: https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/listener-example/images/zipkin-tracing-rollback.png
[zipkin-span-connection]: https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/listener-example/images/zipkin-span-connection.png
[zipkin-span-batch-query]: https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/listener-example/images/zipkin-span-batch-query.png


### Metrics

Similar to distributed tracing, on every callback, any obtained information can be used to update metrics.

For example:
- Number of opened connections
- Number of rollbacks
- Method execution time
- Number of queries
- Type of query (SELECT, DELETE, ...)
- Query execution time
- etc.

Sample implementation: [MetricsExecutionListener][MetricsExecutionListener]

This listener populates following metrics:
- Time took to create a connection
- Commit and rollback counts
- Executed query count
- Slow query count

In addition, this listener logs slow queries.


*Connection metrics on JMX*
![Connection JMX][metrics-jmx-connection]

*Query metrics on JMX:*
![Query JMX][metrics-jmx-query]

*Transaction metrics on actuator (`/actuator/metrics/r2dbc.transaction`):*
![Transaction Actuator][metrics-actuator-connection]

[metrics-jmx-connection]: https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/listener-example/images/metrics-jmx-connection.png
[metrics-jmx-query]: https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/listener-example/images/metrics-jmx-query.png
[metrics-actuator-connection]: https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/listener-example/images/metrics-actuator-connection.png

### Assertion/Verification

By inspecting invoked methods and/or executed queries, you can verify the target logic has performed
as expected.

For example, by keeping track of connection open/close method calls, connection leaks can be
detected or verified.

Another example is to check group of queries are executed on the same connection.
This could verify the premise of transaction - queries need to be performed on the same
connection in order to be in the same transaction.


### Custom logic injection

Any logic can be performed on callbacks.
Users can write own logic that performs any actions, such as audit logging, sending
notifications, calling external system, etc.


## Implementing custom listener

In order to create a custom listener, simply implement `ProxyExecutionListener` or `ProxyMethodExecutionListener`
interface.

```java
static class MyListener implements ProxyMethodExecutionListener {
	@Override
	public void afterCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
		System.out.println("connection created");
	}
}
```

```java
ConnectionFactory proxyConnectionFactory =
    ProxyConnectionFactory.builder(connectionFactory)
    	.listener(new MyListener())
		.build();
```


## API

Currently, there are two listener interfaces - `ProxyExecutionListener` and `ProxyMethodExecutionListener`.
These listeners define callback APIs for method and query executions.

Formatters are used for converting execution information object to `String`.
Mainly used for preparing log entries.

### ProxyExecutionListener

`ProxyExecutionListener` is the foundation listener interface.
This listener defines callbacks for method invocation, query execution, and query
result processing.

```java
// invoked before any method on proxy is called
void beforeMethod(MethodExecutionInfo executionInfo);

// invoked after any method on proxy is called
void afterMethod(MethodExecutionInfo executionInfo);

// invoked before query gets executed
void beforeQuery(QueryExecutionInfo execInfo);

// invoked after query is executed
void afterQuery(QueryExecutionInfo execInfo);

// invoked on processing(subscribing) each query result
void eachQueryResult(QueryExecutionInfo execInfo);
```

`MethodExecutionInfo` and `QueryExecutionInfo` contains contextual information about the
method/query execution.

Any method calls on proxy triggers method callbacks - `beforeMethod()` and `afterMethod()`.  
`Batch#execute()` and `Statement#execute()` triggers query callbacks - `beforeQuery()`
and `afterQuery()`.(Specifically, when returned result-publisher is subscribed.)  
`eachQueryResult()` is called on each mapped query result when `Result#map()` is subscribed.


### ProxyMethodExecutionListener

`ProxyMethodExecutionListener` is an extension of `ProxyExecutionListener`.
In addition to the methods defined in `ProxyExecutionListener`, `ProxyMethodExecutionListener` provides
before/after methods for all methods defined on `ConnectionFactory`, `Connection`, `Batch`,
`Statement`, and `Result`.

For example, if you want know the creation of connection and close of it:

```java
public class ConnectionStartToEndListener implements ProxyMethodExecutionListener {

  @Override
  public void beforeCreateOnConnectionFactory(MethodExecutionInfo methodExecutionInfo) {
    // callback at ConnectionFactory#create()
  }

  @Override
  public void afterCloseOnConnection(MethodExecutionInfo methodExecutionInfo) {
    // callback at Connection#close()
  }

}
```


### QueryExecutionInfoFormatter

This class converts `QueryExecutionInfo` to `String`. Mainly used for preparing log entries.  
Internally, this class has multiple consumers for `QueryExecutionInfo` and loop through them to
populate the output `StringBuilder`.

This class implements `Function<QueryExecutionInfo,String>` and can be used in functional style as well.

```java
// convert all info
QueryExecutionInfoFormatter formatter = QueryExecutionInfoFormatter#showAll();
String str = formatter.format(queryExecutionInfo);

// customize conversion
QueryExecutionInfoFormatter formatter = new QueryExecutionInfoFormatter();
formatter.addConsumer((execInfo, sb) -> {
  sb.append("MY-QUERY-EXECUTION="); // add prefix
};
formatter.newLine();  // new line
formatter.showSuccess();
formatter.showConnection((execInfo, sb)  -> {
    // custom conversion
    sb.append("MY-ID=" + executionInfo.getConnectionInfo().getConnectionId());
});
formatter.showQuery();

// convert it
String str = formatter.format(queryExecutionInfo);
```

### MethodExecutionInfoFormatter

Similar to `QueryExecutionInfoFormatter`, `MethodExecutionInfoFormatter` converts `MethodExecutionInfo` to
`String`.

```java
MethodExecutionInfoFormatter formatter = MethodExecutionInfoFormatter.withDefault();

ProxyConnectionFactoryBuilder.create(connectionFactory)
  .onAfterMethod(execInfo ->
     System.out.println(formatter.format(execInfo)))  // convert & print out to sysout
  .build();
```

----

## Configuration examples

### Query logging

Query logging can be achieved by logging executed query information on after-query callback.
This can be done in **before** query callback(`beforeQuery`); however, some of the attributes are only
available at **after** query callback(`afterQuery`) such as execution time, successfully executed, etc.

`QueryExecutionInfoFormatter`, which converts `QueryExecutionInfo` to `String`, can be used
out of the box to generate log statements.

```java
QueryExecutionInfoFormatter queryExecutionFormatter = QueryExecutionInfoFormatter.showAll();


ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactory.builder(connectionFactory)  // wrap original ConnectionFactory
    // on every query execution
    .onAfterQuery(execInfo ->
      System.out.println(formatter.format(execInfo)))  // convert & print out to sysout
    .build();
```

### Slow query detection

#### Detect slow query AFTER query has executed

On after query execution, check whether the query execution time has exceeded the threshold
time, then perform any action.

```java
Duration threshold = Duration.of(...);

ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactory.builder(connectionFactory)  // wrap original ConnectionFactory
    .onAfterQuery(execInfo -> {
       if(threshold.minus(execInfo.getExecuteDuration()).isNegative()) {
         // slow query logic
       }
    })
    .build();
```


### Method tracing

At each invocation of methods, perform action such as printing out the invoked method,
create a span, or update metrics.

`MethodExecutionInfoFormatter` is used to generate log string.

```java
MethodExecutionInfoFormatter methodExecutionFormatter = MethodExecutionInfoFormatter.withDefault();

ConnectionFactory proxyConnectionFactory =
  ProxyConnectionFactory.builder(connectionFactory)  // wrap original ConnectionFactory
    // on every method invocation
    .onAfterMethod(execInfo ->
      System.out.println(formatter.format(execInfo)))  // print out method execution (method tracing)
    .build();
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

_Also see [CONTRIBUTING.adoc](https://github.com/r2dbc/.github/blob/main/CONTRIBUTING.adoc) if you wish to submit pull requests, and in particular please sign the [Contributor's Agreement](https://cla.pivotal.io/sign/reactor) before your first change, however trivial._

## Staging to Maven Central

To stage a release to Maven Central, you need to create a release tag (release version) that contains the desired state and version numbers (`mvn versions:set versions:commit -q -o -DgenerateBackupPoms=false -DnewVersion=x.y.z.(RELEASE|Mnnn|RCnnn`) and force-push it to the `release-0.x` branch. This push will trigger a Maven staging build (see `build-and-deploy-to-maven-central.sh`).

## License
This project is released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0
