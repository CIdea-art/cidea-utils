package cn.cidea.core.utils;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Http请求构建工具
 *
 * @author CIdea
 */
@Slf4j
public abstract class HttpRequestBuilder {

    /**
     * 请求名称
     */
    @Setter
    @Accessors(chain = true, fluent = true)
    protected String name = "";

    /**
     * http地址
     */
    protected String url;

    /**
     * http方法
     */
    protected RequestMethod method;

    /**
     * 参数
     */
    protected Object param;

    /**
     * 编码
     */
    @Accessors(chain = true, fluent = true)
    protected String charset;

    /**
     * 头部
     */
    @Accessors(chain = true, fluent = true)
    protected List<Header> headerList = new ArrayList<>();

    @Setter
    @Accessors(chain = true, fluent = true)
    protected Integer connectionTimeout = 3000;

    @Setter
    @Accessors(chain = true, fluent = true)
    protected Integer socketTimeout = 30000;

    @Setter
    @Accessors(chain = true, fluent = true)
    protected ProxyHost proxyHost;

    @Setter
    @Getter
    @Accessors(chain = true, fluent = true)
    protected Collection<String> cookies;

    private HttpRequestBuilder() {
    }

    /**
     * post
     *
     * @param url
     * @return
     */
    public static Post post(String url) {
        Post builder = new Post();
        builder.url = url;
        builder.method = RequestMethod.POST;
        return builder;
    }

    public static Get get(String url) {
        Get builder = new Get();
        builder.url = url;
        builder.method = RequestMethod.GET;
        return builder;
    }

    /**
     * 预设的一些编码
     */
    public HttpRequestBuilder utf8() {
        this.charset = Charset.forName("UTF-8").displayName();
        return this;
    }

    public HttpRequestBuilder gb2313() {
        this.charset = Charset.forName("GB2312").displayName();
        return this;
    }

    public HttpRequestBuilder gbk() {
        this.charset = Charset.forName("GBK").displayName();
        return this;
    }

    /**
     * 添加Header
     */
    public HttpRequestBuilder addHeader(String name, String value) {
        this.headerList.add(new Header(name, value));
        return this;
    }

    public HttpRequestBuilder addHeader(List<Header> headerList) {
        this.headerList.addAll(headerList);
        return this;
    }

    public HttpRequestBuilder addHeader(Map<String, String> header) {
        if (header == null) {
            return this;
        }
        for (Map.Entry<String, String> entry : header.entrySet()) {
            headerList.add(new Header(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    public HttpRequestBuilder cookie(String cookie) {
        if (cookies == null) {
            cookies = new HashSet<>();
        }
        cookies.add(cookie);
        return this;
    }

    public HttpRequestBuilder systemProxyHost() {
        String proxyHostKey = "http.proxyHost";
        String proxyHost = System.getProperty(proxyHostKey, System.getenv(proxyHostKey));
        if (StringUtils.isBlank(proxyHost)) {
            return this;
        }
        String proxyPortKey = "http.proxyPort";
        Integer proxyPort = Integer.valueOf(System.getProperty(proxyPortKey, System.getenv(proxyPortKey)));
        log.info("proxy host={}, port={}", proxyHost, proxyPort);
        proxyHost(new ProxyHost(proxyHost, proxyPort));
        return this;
    }

    public JSONObject executeJson() throws IOException {
        String rspStr = execute();
        if (StringUtils.isBlank(rspStr)) {
            return null;
        }
        return JSONObject.parseObject(rspStr);
    }

    public String execute() throws IOException {
        HttpMethod httpMethod = buildHttpMethod();
        HttpMethodParams httpMethodParams = httpMethod.getParams();
        if (charset != null) {
            httpMethodParams.setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, charset);
        }

        for (Header header : headerList) {
            httpMethod.addRequestHeader(header);
        }

        HttpClient client = new HttpClient();
        HttpConnectionManagerParams connectionManagerParams = client.getHttpConnectionManager().getParams();
        connectionManagerParams.setConnectionTimeout(connectionTimeout);
        connectionManagerParams.setSoTimeout(socketTimeout);

        if (proxyHost != null) {
            log.info("proxy = {}", JSONObject.toJSONString(proxyHost));
            client.getHostConfiguration().setProxyHost(proxyHost);
        }
        if (CollectionUtils.isNotEmpty(cookies)) {
            httpMethod.setRequestHeader("cookie", cookies.stream().collect(Collectors.joining(";")));
        }

        int status;
        String responseBody;
        String paramJson = (param != null && param instanceof String) ? (String) param : JSONObject.toJSONString(param);
        log.info("{}request: url = {}, param = {}", name, url, paramJson);
        status = client.executeMethod(httpMethod);
        responseBody = httpMethod.getResponseBodyAsString();
        log.info("{}response: status = {}, body = {}", name, status, responseBody);
        if (status != HttpStatus.SC_OK) {
            throw new HttpException(name + "HTTP调用状态异常: " + responseBody);
        }
        if (ArrayUtils.isNotEmpty(client.getState().getCookies())) {
            cookies = Arrays.stream(client.getState().getCookies()).map(Cookie::toString).collect(Collectors.toList());
        }
        return responseBody;
    }

    abstract HttpMethod buildHttpMethod();

    private enum RequestMethod {
        POST,
        GET,
    }

    @Slf4j
    public static class Post extends HttpRequestBuilder {

        protected ContentType contentType = ContentType.NONE;

        private Post() {
        }

        public Post json(Object param) {
            this.contentType = ContentType.JSON;
            this.param = param;
            return this;
        }

        public Post formUrlencoded(String key, Object value) {
            this.contentType = ContentType.FORM_URLENCODED;
            if (param == null) {
                param = new HashMap<>();
            }
            if (!(param instanceof Map)) {
                param = JSONObject.parseObject(JSONObject.toJSONString(param));
            }
            ((Map) param).put(key, value);
            return this;
        }

        public Post formUrlencoded(Object param) {
            this.contentType = ContentType.FORM_URLENCODED;
            if (this.param == null) {
                this.param = param;
            } else {
                if (!(param instanceof Map)) {
                    param = JSONObject.toJSONString(param);
                }
                ((Map) param).putAll(JSONObject.parseObject(JSONObject.toJSONString(param)));
            }
            return this;
        }

        public HttpRequestBuilder xml(String param) {
            this.contentType = ContentType.XML;
            this.param = param;
            return this;
        }

        public HttpRequestBuilder textXml(String param) {
            this.contentType = ContentType.TEST_XML;
            this.param = param;
            return this;
        }

        // private Post formData(Object param) {
        //     this.contentType = ContentType.FORM_DATA;
        //     // TODO
        //     return this;
        // }

        @Override
        public HttpMethod buildHttpMethod() {
            PostMethod httpMethod = new PostMethod(url);
            if (param == null) {
                return httpMethod;
            }
            try {
                switch (contentType) {
                    case NONE:
                    case FORM_URLENCODED:
                        Map<String, Object> paramMap;
                        if (param instanceof Map) {
                            paramMap = (Map) param;
                        } else {
                            paramMap = JSONObject.parseObject(JSONObject.toJSONString(this.param));
                        }
                        List<NameValuePair> nameValuePairList = paramMap.entrySet().stream()
                                .map(e -> new NameValuePair(e.getKey(), Optional.ofNullable(e.getValue()).map(Object::toString).orElse(null)))
                                .collect(Collectors.toList());
                        NameValuePair[] array = new NameValuePair[nameValuePairList.size()];
                        for (int i = 0; i < nameValuePairList.size(); i++) {
                            array[i] = nameValuePairList.get(i);
                        }
                        httpMethod.setRequestBody(array);
                        break;
                    case JSON:
                        if (!(param instanceof String)) {
                            param = JSONObject.toJSONString(param);
                        }
                    case TEST_XML:
                    case XML:
                        StringRequestEntity requestEntity = new StringRequestEntity(
                                (String) param,
                                contentType.value(),
                                charset);
                        httpMethod.setRequestEntity(requestEntity);
                        break;
                    default:
                        throw new RuntimeException("暂不支持此类型");
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("HTTP参数封装异常: " + e.getMessage());
            }
            return httpMethod;
        }

        @AllArgsConstructor
        public enum ContentType {
            NONE("0"),
            FORM_DATA("multipart/form-data"),
            FORM_URLENCODED("application/x-www-form-urlencoded"),
            JSON("application/json"),
            XML("application/xml"),
            TEST_XML("text/xml"),
            ;

            private String value;

            public String value() {
                return value;
            }
        }

    }

    @Slf4j
    public static class Get extends HttpRequestBuilder {

        @Override
        HttpMethod buildHttpMethod() {
            GetMethod httpMethod = new GetMethod(url);
            return httpMethod;
        }
    }

    public static void main(String[] args) throws IOException {
        Map<String, String> param1 = new HashMap<>();
        param1.put("userName", "admin");
        param1.put("password", "123456");
        String url1 = "http://127.0.0.1:9527/xxl-job-admin";
        Post post = HttpRequestBuilder.post(url1 + "/login").formUrlencoded(param1);
        String execute = post.execute();
        Collection<String> cookies1 = post.cookies();


        Map<String, Object> param2 = new HashMap<>();
        // param2.put("appname", executorProperties.getAppname());
        // param2.put("title", executorProperties.getAppname());
        JSONObject response = HttpRequestBuilder.post(url1 + "/jobgroup/pageList")
                .formUrlencoded(param2)
                .cookies(cookies1)
                .executeJson();
        return;
    }

}
