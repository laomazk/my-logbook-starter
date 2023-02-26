package com.magic.logbook.config;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor;
import org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor;

@Configuration
public class HttpClientConfiguration {
    @Autowired
    private LogbookHttpRequestInterceptor logbookHttpRequestInterceptor;
    @Autowired
    private LogbookHttpResponseInterceptor logbookHttpResponseInterceptor;

    private HttpClient httpClient() {

        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();

        // 池化
        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(registry);
        poolingHttpClientConnectionManager.setMaxTotal(30);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(100);
        poolingHttpClientConnectionManager.setValidateAfterInactivity(3000);

        RequestConfig requestConfig = RequestConfig.custom()
                // 超时链接设置，todo 走配置+默认形式
                .setSocketTimeout(6000)
                .setConnectTimeout(6000)
                .setConnectionRequestTimeout(6000)
                .build();

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingHttpClientConnectionManager)
                .addInterceptorLast(logbookHttpRequestInterceptor)
                .addInterceptorLast(logbookHttpResponseInterceptor)
                .build();

    }

}
