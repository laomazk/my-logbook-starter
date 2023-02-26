package com.magic.logbook.config;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magic.logbook.dto.LogbookReq;
import com.magic.logbook.dto.LogbookResp;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.zalando.logbook.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author: mazikai
 * @created: 2021-08-26 11:08
 */
@Slf4j
public class LogbookSink implements Sink {

    private final String appName;
    private final HttpLogWriter writer;
    private final ObjectMapper mapper;

    public LogbookSink(HttpLogWriter writer, String appName) {
        this.writer = writer;
        this.appName = appName;
        this.mapper = ObjectMapperFactory.createObjectMapper();
    }

    @Override
    public void write(Precorrelation precorrelation, HttpRequest request) throws IOException { // req
        writer.write(precorrelation, reqFormat(precorrelation, request));
    }

    @Override
    public void write(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException { // resp
        writer.write(correlation, respFormat(correlation, request, response));
    }

    private String reqFormat(Precorrelation precorrelation, HttpRequest request) throws IOException {
        final LogbookReq logbookReq = new LogbookReq();
        String uri = request.getRequestUri();
        logbookReq.setCorrelation(precorrelation.getId());
        logbookReq.setType(LogbookReq.TYPE_IN);
        logbookReq.setUri(uri);
        logbookReq.setMethod(request.getMethod());

        //客户端发起请求经httpclient拦截器org.zalando.logbook.httpclient.LogbookHttpRequestInterceptor处理成为LocalRequest, request.getOrigin()=LOCAL
        //服务端收到请求,经servlet过滤器org.zalando.logbook.servlet.LogbookFilter,处理为RemoteRequest, request.getOrigin()=REMOTE
        String originalStr = Origin.LOCAL.equals(request.getOrigin()) ? LogbookReq.ORIGIN_CONSUMER : LogbookReq.ORIGIN_PROVIDER;
        logbookReq.setOrigin(originalStr);
        //logbookReq.setOrigin(Origin.LOCAL.equals(request.getOrigin()) ? LogbookResp.ORIGIN_PROVIDER : LogbookResp.ORIGIN_CONSUMER);

        prepareBody(request).ifPresent(logbookReq::setBody);
        prepareHeaders(request).ifPresent(logbookReq::setHeaders);
        //收到来自外部的请求,尝试从httpRequestHeader获取外部调用方应用名
        if (Origin.REMOTE.equals(request.getOrigin())) {
            prepareOriginalCallAppName(request).ifPresent(logbookReq::setOriginalCallApp);
        }
        return mapper.writeValueAsString(logbookReq);
    }

    private String respFormat(Correlation correlation, HttpRequest request, HttpResponse response) throws IOException {
        final LogbookResp resp = new LogbookResp();
        String uri = request.getRequestUri();
        int status = response.getStatus();
        resp.setCorrelation(correlation.getId());
        resp.setType(LogbookResp.TYPE_OUT);
        resp.setUri(uri);

        //客户端发起请求经httpclient拦截器org.zalando.logbook.httpclient.LogbookHttpResponseInterceptor处理成为RemoteResponse, response.getOrigin()=REMOTE
        //服务端收到请求,经servlet过滤器org.zalando.logbook.servlet.LogbookFilter,处理为LocalResponse,response.getOrigin()=LOCAL
        String originalStr = Origin.REMOTE.equals(response.getOrigin()) ? LogbookReq.ORIGIN_CONSUMER : LogbookReq.ORIGIN_PROVIDER;
        resp.setOrigin(originalStr);
        //resp.setOrigin(Origin.LOCAL.equals(response.getOrigin()) ? LogbookResp.ORIGIN_PROVIDER : LogbookResp.ORIGIN_CONSUMER);

        resp.setDuration(correlation.getDuration().toMillis());
        resp.setStatus(status);
        String code = getRespCode(response);
        resp.setCode(code);
        prepareBody(response).ifPresent(resp::setBody);
        prepareHeaders(response).ifPresent(resp::setHeaders);
        //响应外部的请求时,尝试从httpRequestHeader获取外部调用方应用名
        if (Origin.LOCAL.equals(response.getOrigin())) {
            prepareOriginalCallAppName(request).ifPresent(resp::setOriginalCallApp);
        }
        return mapper.writeValueAsString(resp);
    }

    private String getRespCode(HttpResponse response) throws IOException {
        String strCode = findCode(response.getBodyAsString());
        int httpCode = response.getStatus();
        if (httpCode == 200) { // 正常响应
            return Optional.ofNullable(strCode).orElse("SUCCESS");
        }
        if (httpCode < 500) {// 非5xx异常, 缺失code时, 默认为无效调用.
            return Optional.ofNullable(strCode).orElse("GlobalExceptionHandler.INVALID_REQUEST");
        }
        //5xx
        if (StringUtils.isEmpty(strCode)) { //外部响应5xx
            return "外部响应5xx";
        }
        return strCode; //内部调用响应5xx
    }

    public Optional<Map<String, List<String>>> prepareHeaders(final HttpMessage message) {
        final Map<String, List<String>> headers = message.getHeaders();
        return Optional.ofNullable(headers.isEmpty() ? null : headers);
    }

    public Optional<Object> prepareBody(final HttpMessage message) throws IOException {
        final String contentType = message.getContentType();
        final String body = message.getBodyAsString();
        if (body.isEmpty()) {
            return Optional.empty();
        }
        if (ifJson(contentType)) {
            return Optional.of(new JsonBody(body));
        } else {
            return Optional.of(body);
        }
    }

    private Optional<String> prepareOriginalCallAppName(HttpMessage httpMessage) {
        HttpHeaders headers = httpMessage.getHeaders();
        List<String> values = headers.get("x-original-call-app");
        if (!CollectionUtils.isEmpty(values) && !values.get(0).equalsIgnoreCase(appName)) { //应用调用自身接口,不填充应用名称
            return Optional.ofNullable(values.get(0));
        }
        return Optional.empty();
    }

    private boolean ifJson(String contentType) {
        if (contentType == null) {
            return false;
        }
        // implementation note: manually coded for improved performance
        if (contentType.startsWith("application/")) {
            int index = contentType.indexOf(';', 12);
            if (index != -1) {
                if (index > 16) {
                    // application/some+json;charset=utf-8
                    return contentType.regionMatches(index - 5, "+json", 0, 5);
                }

                // application/json;charset=utf-8
                return contentType.regionMatches(index - 4, "json", 0, 4);
            } else {
                // application/json
                if (contentType.length() == 16) {
                    return contentType.endsWith("json");
                }
                // application/some+json
                return contentType.endsWith("+json");
            }
        }
        return false;
    }

    private String findCode(String json) {
        String regex = "\"code\"\\s?:\\s?(\"(.*?)\"|(\\d*))";
        Matcher matcher = Pattern.compile(regex).matcher(json);
        String value = null;
        if (matcher.find()) {
            value = matcher.group().replace("\"", "").replace("code:", "").trim();
        }
        return value;
    }

    @AllArgsConstructor
    private static final class JsonBody {
        String json;

        @JsonRawValue
        @JsonValue
        public String getJson() {
            return json;
        }
    }

}
