> [https://github.com/zalando/logbook](https://github.com/zalando/logbook)

## 1 背景
目前通过日志排查业务问题会出现难以定位的情况，缺少一些必要的请求响应日志来分析调用链路并快速定位问题。
解决方案：

- 每个方法调用都手动打印 log。（繁琐）
- 手写过滤器。（造轮子要兼顾通用性）
- 引入外部工具包。（开箱即用、维护更新、社区活跃度）
## 2 用法
zalando logbook 是一个可扩展的 Java 库，提供了 SpringBoot 的自动装配，可用于打印请求和响应日志，并可做一些自定义的操作。从长远的角度看，也可以提供一些日志审计分析上的帮助。

- pom 依赖：
```xml
<dependency>
  <groupId>org.zalando</groupId>
  <artifactId>logbook-spring-boot-starter</artifactId>
  <version>${logbook.version}</version>
</dependency>
```

- application.properties
```properties
logging.level.org.zalando.logbook = trace
```
如果是 SpringBoot 应用，完成以上的设置 logbook 即可生效。之后即可按需参照 logbook github 上面的文档做自己的设置。

- OpenFeign 上日志的的拦截，logbook 支持比较主流的请求工具的日志拦截，包括 HttpClient、Okhttp 等。这里以 HttpClient 为例，Okhttp 可以参照 github
```java
private HttpClient createClient(HttpClientBuilder builder,
                               HttpClientConnectionManager httpClientConnectionManager,
                               FeignHttpClientProperties httpClientProperties) {
   RequestConfig defaultRequestConfig = RequestConfig.custom()
           .setConnectTimeout(httpClientProperties.getConnectionTimeout())
           .setConnectionRequestTimeout(httpClientProperties.getConnectionTimeout())
           .setSocketTimeout(httpClientProperties.getConnectionTimeout())
           .setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build();
   return builder
           .setDefaultRequestConfig(defaultRequestConfig)
           .setConnectionManager(httpClientConnectionManager)
           // 请求拦截器
           .addInterceptorLast(logbookHttpRequestInterceptor)
           // 响应拦截器
           .addInterceptorLast(logbookHttpResponseInterceptor).build();
}
```
## 3 代码说明
根据需要，做了一些自定义的修改

- LogbookConfiguration 
   1. 日志提供总开关
   2. 提供一些默认定义的配置
   3. 为重写一些请求过滤器\输入输出提供入口（顺带解决了中文日志乱码的问题）
- HttpInfoLevelLogWriter 目前日志的级别是 trace ，重写为 info
- LogbookHeaderStrategy 提供一些策略，用于自定义请求响应的 header ，策略可于 application.properties 配置，配置方式见 LogbookCustomRequestProperties 和 LogbookCustomResponseProperties
- LogbookSink 
   1. 自定义了请求日志体和响应日志体 LogbookReq LogbookResp，目的是为了减少日志的体积和方便统一化操作
   2. 可在这里根据响应的 code 自定义抛出异常
   3. 格式化日志，顺便拿到了请求的时长
   4. ...
## 4 配置参考
```yaml
logbook:
  custom:
    # 开启 logbook
    enabled: true
    request:
      # 请求头 header 策略 
      headerStrategy: DEFAULT
    response:
      # 响应体 header 策略 
      headerStrategy: DEFAULT
  write:
    # 输出大小
    max-body-size: 2048
  exclude:
    # 排除输出日志的路径
    - /actuator/health
  format:
    # 日志格式
    style: json
```
