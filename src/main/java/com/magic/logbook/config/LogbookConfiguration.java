package com.magic.logbook.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.zalando.logbook.*;
import org.zalando.logbook.autoconfigure.LogbookProperties;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import static org.zalando.logbook.BodyFilters.truncate;
import static org.zalando.logbook.HeaderFilters.replaceHeaders;


@Import({LogbookCustomRequestProperties.class, LogbookCustomResponseProperties.class})
@Configuration
@ConditionalOnProperty(prefix = "logbook.custom", name = "enabled", havingValue = "true", matchIfMissing = false)
public class LogbookConfiguration {

    private static final List<String> DEFAULT_REQUEST_HEADERS = Arrays.asList("Accept", "Content-Type", "X-Gravitee-Api-Key");
    private static final List<String> DEFAULT_RESPONSE_HEADERS = Collections.singletonList("Content-Type");

    @Autowired
    private LogbookCustomRequestProperties requestProperties;
    @Autowired
    private LogbookCustomResponseProperties responseProperties;
    @Autowired
    private LogbookProperties properties;


    @Bean
    @ConditionalOnMissingBean(RequestFilter.class)
    public RequestFilter requestFilter() {
        RequestFilter requestFilter = RequestFilters.defaultValue();
        return RequestFilter.merge(requestFilter, r -> new HttpRequest() {
            @Override
            public String getRequestUri() {
                return r.getRequestUri();
            }

            @Override
            public String getRemote() {
                return r.getRemote();
            }

            @Override
            public String getMethod() {
                return r.getMethod();
            }

            @Override
            public String getScheme() {
                return r.getScheme();
            }

            @Override
            public String getHost() {
                return r.getHost();
            }

            @Override
            public Optional<Integer> getPort() {
                return r.getPort();
            }

            @Override
            public String getPath() {
                return r.getPath();
            }

            @Override
            public String getQuery() {
                return r.getQuery();
            }

            @Override
            public HttpRequest withBody() throws IOException {
                return r.withBody();
            }

            @Override
            public HttpRequest withoutBody() {
                return r.withoutBody();
            }

            @Override
            public String getBodyAsString() throws IOException {
                // https://github.com/zalando/logbook/issues/870
                return new String(r.getBody());
            }

            @Override
            public String getProtocolVersion() {
                return r.getProtocolVersion();
            }

            @Override
            public Origin getOrigin() {
                return r.getOrigin();
            }

            @Override
            public HttpHeaders getHeaders() {
                return requestProperties.getHeaderStrategy().filterHeader(r.getHeaders(), DEFAULT_REQUEST_HEADERS,
                        requestProperties.getCustomHeaders());
            }

            @Override
            @Nullable
            public String getContentType() {
                return r.getContentType();
            }

            @Override
            public Charset getCharset() {
                return r.getCharset();
            }

            @Override
            public byte[] getBody() throws IOException {
                return r.getBody();
            }
        });
    }

    @Bean
    @ConditionalOnMissingBean(ResponseFilter.class)
    public ResponseFilter responseFilter() {
        ResponseFilter responseFilter = ResponseFilters.defaultValue();
        return ResponseFilter.merge(responseFilter, r -> new HttpResponse() {
            @Override
            public String getReasonPhrase() {
                return r.getReasonPhrase();
            }

            @Override
            public String getBodyAsString() throws IOException {
                // https://github.com/zalando/logbook/issues/870
                return new String(r.getBody());
            }

            @Override
            public int getStatus() {
                return r.getStatus();
            }

            @Override
            public HttpResponse withBody() throws IOException {
                return r.withBody();
            }

            @Override
            public HttpResponse withoutBody() {
                return r.withoutBody();
            }

            @Override
            public String getProtocolVersion() {
                return r.getProtocolVersion();
            }

            @Override
            public Origin getOrigin() {
                return r.getOrigin();
            }

            @Override
            public HttpHeaders getHeaders() {
                return responseProperties.getHeaderStrategy().filterHeader(r.getHeaders(), DEFAULT_RESPONSE_HEADERS,
                        responseProperties.getCustomHeaders());
            }

            @Override
            @Nullable
            public String getContentType() {
                return r.getContentType();
            }

            @Override
            public Charset getCharset() {
                return r.getCharset();
            }

            @Override
            public byte[] getBody() throws IOException {
                return r.getBody();
            }
        });
    }

    @Bean
    public HttpLogWriter httpLogWriter() {
        return new HttpInfoLevelLogWriter();
    }

    @Bean
    @ConditionalOnMissingBean(Sink.class)
    public Sink sink(@Autowired HttpLogWriter httpLogWriter, @Value("${spring.application.name:unknown}") String appName) {
        return new LogbookSink(httpLogWriter, appName);
    }

    @Bean
    @ConditionalOnMissingBean(HeaderFilter.class)
    public HeaderFilter headerFilter() {
        final Set<String> headers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        headers.addAll(properties.getObfuscate().getHeaders());

        return headers.isEmpty() ?
                HeaderFilter.none() :
                replaceHeaders(headers, "XXX");
    }

    @Bean
    @ConditionalOnMissingBean(PathFilter.class)
    public PathFilter pathFilter() {
        final List<String> paths = properties.getObfuscate().getPaths();
        return paths.isEmpty() ?
                PathFilter.none() :
                paths.stream()
                        .map(path -> PathFilters.replace(path, "XXX"))
                        .reduce(PathFilter::merge)
                        .orElseGet(PathFilter::none);
    }

    @Bean
    @ConditionalOnMissingBean(BodyFilter.class)
    public BodyFilter bodyFilter() {
        final LogbookProperties.Write write = properties.getWrite();
        final int maxBodySize = write.getMaxBodySize();

        if (maxBodySize < 0) {
            return BodyFilter.none();
        }

        return BodyFilter.merge(BodyFilter.none(), truncate(maxBodySize));
    }


}
