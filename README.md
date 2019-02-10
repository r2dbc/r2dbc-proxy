# Reactive Relational Database Connectivity Proxy Framework

This project contains the proxy framework of the [R2DBC SPI][r].

[r]: https://github.com/r2dbc/r2dbc-spi

## Maven
Both milestone and snapshot artifacts (library, source, and javadoc) can be found in Maven repositories.

```xml
<dependency>
  <groupId>io.r2dbc</groupId>
  <artifactId>r2dbc-proxy</artifactId>
  <version>1.0.0.M7</version>
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

Publisher<? extends Connection> connectionPublisher = connectionFactory.create();

// Alternative: Creating a Mono using Project Reactor
Mono<Connection> connectionMono = Mono.from(connectionFactory.create());
```

## License
This project is released under version 2.0 of the [Apache License][l].

[l]: https://www.apache.org/licenses/LICENSE-2.0

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
[slow-query-doc]: http://ttddyy.github.io/datasource-proxy/docs/current/user-guide/#_slow_query_logging_listener


### Distributed Tracing

Using before and after callbacks with contextual information, it can easily construct tracing spans.

Sample implementation: [TracingExecutionListener](https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/src/main/java/io/r2dbc/examples/TracingExecutionListener.java):

*Tracing*
![Tracing](https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/images/zipkin-tracing-rollback.png)

*Connection Span*
![Connection Span](https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/images/zipkin-span-connection.png)

*Query Span*
![Query Span](https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/images/zipkin-span-batch-query.png)


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

Sample implementation: [MetricsExecutionListener](https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/src/main/java/io/r2dbc/examples/MetricsExecutionListener.java)

This listener populates following metrics:
- Time took to create a connection
- Commit and rollback counts
- Executed query count
- Slow query count

In addition, this listener logs slow queries.


*Connection metrics on JMX*
![Connection JMX](https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/images/metrics-jmx-connection.png)

*Query metrics on JMX:*
![Query JMX](https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/images/metrics-jmx-query.png)

*Transaction metrics on actuator (`/actuator/metrics/r2dbc.transaction`):*
![Transaction Actuator](https://github.com/ttddyy/r2dbc-proxy-examples/raw/master/images/metrics-actuator-connection.png)


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


## API

Currently, there are two listener interfaces - `ProxyExecutionListener` and `LifeCycleListener`.
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


### LifeCycleListener

`LifeCycleListener` provides before/after methods for all methods defined on `ConnectionFactory`,
`Connection`, `Batch`, `Statement`, and `Result`, as well as method executions(`beforeMethod`, `afterMethod`),
query executions(`beforeQuery`, `afterQuery`) and result processing(`onEachQueryResult`).
This listener is built on top of method and query interceptor API on `ProxyExecutionListener`.

For example, if you want know the creation of connection and close of it:

```java
public class ConnectionStartToEndListener implements LifeCycleListener {

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
     execInfo.map(methodExecutionFormatter::format)  // convert
       .doOnNext(System.out::println)  // print out to sysout
       .subscribe())
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
      execInfo.map(queryExecutionFormatter::format)    // convert QueryExecutionInfo to String
              .doOnNext(System.out::println)       // print out executed query
              .subscribe())
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
    .onAfterQuery(mono -> mono
       .filter(execInfo -> threshold.minus(execInfo.getExecuteDuration()).isNegative())
       .doOnNext(execInfo -> {
         // slow query logic
       })
       .subscribe())
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
      execInfo.map(methodExecutionFormatter::format)    // convert MethodExecutionInfo to String
              .doOnNext(System.out::println)        // print out method execution (method tracing)
              .subscribe())
    .build();
```


## Samples

[r2dbc-proxy-samples][r2dbc-proxy-samples] repository contains sample listener implementations.

- Distributed tracing - [TracingExecutionListener][TracingExecutionListener]
- Micrometer metrics - [MetricsExecutionListener][MetricsExecutionListener]


[r2dbc-proxy-samples]: https://github.com/ttddyy/r2dbc-proxy-examples
[TracingExecutionListener]: https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/src/main/java/io/r2dbc/examples/TracingExecutionListener.java
[MetricsExecutionListener]: https://github.com/ttddyy/r2dbc-proxy-examples/blob/master/src/main/java/io/r2dbc/examples/MetricsExecutionListener.java
