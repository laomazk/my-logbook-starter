package com.magic.logbook.config;

import org.springframework.util.CollectionUtils;
import org.zalando.logbook.HttpHeaders;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum LogbookHeaderStrategy {

    ALL {
        public HttpHeaders filterHeader(HttpHeaders httpHeaders, List<String> defaultHeaders, List<String> customHeaders) {
            return httpHeaders;
        }
    },
    NONE {
        public HttpHeaders filterHeader(HttpHeaders httpHeaders, List<String> defaultHeaders, List<String> customHeaders) {
            return HttpHeaders.empty();
        }
    },
    DEFAULT {
        public HttpHeaders filterHeader(HttpHeaders httpHeaders, List<String> defaultHeaders, List<String> customHeaders) {
            return filterVisibleHeader(httpHeaders, defaultHeaders);
        }
    },
    CUSTOM {
        public HttpHeaders filterHeader(HttpHeaders httpHeaders, List<String> defaultHeaders, List<String> customHeaders) {
            return filterVisibleHeader(httpHeaders, customHeaders);
        }
    },
    DEFAULT_AND_CUSTOM {
        public HttpHeaders filterHeader(HttpHeaders httpHeaders, List<String> defaultHeaders, List<String> customHeaders) {
            List<String> limitVisibleHeaders = new ArrayList<>();
            if (!CollectionUtils.isEmpty(defaultHeaders)) {
                limitVisibleHeaders.addAll(defaultHeaders);
            }
            if (!CollectionUtils.isEmpty(customHeaders)) {
                limitVisibleHeaders.addAll(customHeaders);
            }
            return filterVisibleHeader(httpHeaders, limitVisibleHeaders);
        }
    },
    ;

    public abstract HttpHeaders filterHeader(HttpHeaders httpHeaders, List<String> defaultHeaders, List<String> customHeaders);

    private static HttpHeaders filterVisibleHeader(HttpHeaders httpHeaders, List<String> limitVisibleHeaders) {
        if (CollectionUtils.isEmpty(limitVisibleHeaders)) {
            return HttpHeaders.empty();
        }
        Set<String> keySet = new HashSet<>(httpHeaders.keySet());
        keySet.removeIf(limitVisibleHeaders::contains);
        return httpHeaders.delete(keySet);
    }

}
