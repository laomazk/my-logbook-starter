package com.magic.logbook.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author: mazikai
 * @created: 2021-08-26 13:58
 */
@Getter
@Setter
@JsonInclude
//保持content中各key的顺序, 后续会按照该顺序匹配各key的值
@JsonPropertyOrder({"correlation", "type", "uri", "origin", "duration", "status", "code", "body", "headers", "originalCallApp"})
public class LogbookResp implements Serializable {

    public static final String TYPE_IN = "in";
    public static final String TYPE_OUT = "out";

    public static final String ORIGIN_CONSUMER = "CONSUMER";
    public static final String ORIGIN_PROVIDER = "PROVIDER";

    private String correlation;
    private String type = TYPE_OUT;
    private Integer status;
    private Long duration;
    private String code;
    private String uri;
    private String origin = ORIGIN_CONSUMER;
    private Object body;
    private Object headers;

    /**
     * 当服务端应用获取到调用方应用名称时, 填充到该属性以标识请求来源.
     */
    private String originalCallApp = "_NO_VALUE";

}
