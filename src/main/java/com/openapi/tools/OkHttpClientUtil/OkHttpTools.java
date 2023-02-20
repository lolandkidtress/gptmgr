package com.openapi.tools.OkHttpClientUtil;

import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.openapi.Basic.JsonConvert;
import com.openapi.Basic.SystemConstant;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;


public class OkHttpTools {

    private static final Logger logger = LogManager.getLogger(OkHttpTools.class);

    private static Integer socket_timeout = 30;// response超时时间
    private static Integer connect_timeout = 3;// 连接超时时间
    private static Integer maxRequests = 128; // 最大的并发请求数
    private static Integer maxRequestsPerHost = 128; //单个主机最大请求并发数

    public static final String MEDIA_TYPE_JSON = "application/json; charset=utf-8";
    public static final String MEDIA_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded; charset=utf-8";
    public static final String MEDIA_TYPE_FORM_DATA = "multipart/form-data; charset=utf-8";//

    private static ArrayList<String> availableMediaType = new ArrayList();

    private static OkHttpClient.Builder defaultBuilder =
        new OkHttpClient.Builder().connectionPool(new ConnectionPool(1, 1, TimeUnit.SECONDS))
            .connectTimeout(connect_timeout, TimeUnit.SECONDS).writeTimeout(socket_timeout, TimeUnit.SECONDS)
            .readTimeout(socket_timeout, TimeUnit.SECONDS);

    private static final OkHttpClient zipkinClient = defaultBuilder.build();

    static {
        availableMediaType.add(MEDIA_TYPE_JSON);
        availableMediaType.add(MEDIA_TYPE_FORM_URLENCODED);
        availableMediaType.add(MEDIA_TYPE_FORM_DATA);

//    zipkinClient.dispatcher().setMaxRequests(maxRequests);
//    zipkinClient.dispatcher().setMaxRequestsPerHost(maxRequestsPerHost);
        logger.info("初始化HTTP CLIENT");
    }

    //TODO zipkin
    private static final Callback default_async_callback = new Callback() {
        @Override
        public void onFailure(Call call, IOException e) {
            logger.error("do_async_get 报错:".concat(call.request().url().toString()), e);
            if (!call.isCanceled()) {
                call.cancel();
            }
        }

        @Override
        public void onResponse(Call call, Response response)
            throws IOException {
            try (ResponseBody responseBody = response.body()) {
                if (!response.isSuccessful()) {
                    throw new IOException(response.request().url() + " Unexpected response: " + response);
                }
                logger.debug(responseBody.string());
            }
        }
    };

    //TODO 添加proxy和cache
    //https请求
    public static String sslget(String url, Map<String, Object> params, int readTimeoutSeconds, Map<String, String> header)
        throws Exception {

        url = buildURL(url);

        OkHttpClient client = buildClient(readTimeoutSeconds, "1");

        Request request = buildGetRequest(url, params, header).build();

        try (Response response = client.newCall(request).execute()) {
            return returnRespons(request, response);
        } catch (Exception e) {
            logger.error("okhttp get error", e);
            e.printStackTrace();
            return returnRespons(request, e.getMessage());
        }
    }

    //TODO 添加proxy和cache
    public static String get(String url, Map<String, Object> params, int readTimeoutSeconds, Map<String, String> header)
        throws Exception {

        url = buildURL(url);

        OkHttpClient client = buildClient(readTimeoutSeconds);

        Request request = buildGetRequest(url, params, header).build();

        try (Response response = client.newCall(request).execute()) {

            return returnRespons(request, response);
        } catch (Exception e) {
            logger.error("okhttp get error", e);
            e.printStackTrace();
            return returnRespons(request, e.getMessage());
        }
    }

    public static String get(String url, Map<String, Object> params)
        throws Exception {
        return get(url, params, socket_timeout, new HashMap<>());
    }

    public static String get(String url, Map<String, Object> params, int readTimeoutSeconds)
        throws Exception {
        return get(url, params, readTimeoutSeconds, new HashMap<>() );
    }

    public static void asyncGet(String url, Map<String, Object> params, int readTimeoutSeconds,
        Map<String, String> header, Callback callback, boolean ignoreURLValid)
        throws Exception {
        url = buildURL(url);

        if (!isTimeOutLegal(readTimeoutSeconds)) {
            return;
        }
        OkHttpClient client = buildClient(readTimeoutSeconds);

        Request request = buildGetRequest(url, params, header).build();

        if (callback == null) {
            client.newCall(request).enqueue(default_async_callback);
        } else {
            client.newCall(request).enqueue(callback);
        }
    }

    public static void asyncGet(String url, Map<String, Object> params, Callback callback)
        throws Exception {
        asyncGet(url, params, socket_timeout, null, callback, false);
    }

    public static void asyncGet(String url, Map<String, Object> params, int readTimeoutSeconds, Callback callback)
        throws Exception {
        asyncGet(url, params, readTimeoutSeconds, null, callback, false);
    }

    public static String post(String mediaType, String url, String params)
        throws Exception {
        return post(mediaType, url, params, 30, null, false);
    }

    public static String post(String mediaType, String url, String params, Map header)
        throws Exception {
        return post(mediaType, url, params, 30, header, false);
    }

    public static String sslpost(String mediaType, String url, String params, int readTimeoutSeconds,
        Map<String, String> header)
        throws Exception {

        String type = "application/json; charset=utf-8";
        if (mediaType != null && !"".equals(mediaType)) {
            type = mediaType;
        }

        if (!availableMediaType.contains(type)) {
            throw new IOException("Unsupported MediaType: " + mediaType);
        }

        url = buildURL(url);

        OkHttpClient client = buildClient(readTimeoutSeconds, "1");
        Request request = buildPostRequest(mediaType, url, params, header).build();

        try (Response response = client.newCall(request).execute()) {
            return returnRespons(request, response);
        } catch (Exception e) {
            logger.error("okhttp get error", e);
            return returnRespons(request, e.getMessage());
        }
    }

    public static String post(String mediaType, String url, String params, int readTimeoutSeconds,
        Map<String, String> header, boolean ignoreURLValid)
        throws Exception {

        String type = "application/json; charset=utf-8";
        if (mediaType != null && !"".equals(mediaType)) {
            type = mediaType;
        }

        if (!availableMediaType.contains(type)) {
            throw new IOException("Unsupported MediaType: " + mediaType);
        }

        url = buildURL(url);

        OkHttpClient client = buildClient(readTimeoutSeconds);
        Request request = buildPostRequest(mediaType, url, params, header).build();

        try (Response response = client.newCall(request).execute()) {
            return returnRespons(request, response);
        } catch (Exception e) {
            logger.error("okhttp get error", e);
            return returnRespons(request, e.getMessage());
        }
    }

    public static String post(String mediaType, String url, Map<String, Object> params)
        throws Exception {
        return post(mediaType, url, params, 30, null, false);
    }

    public static String post(String mediaType, String url, Map<String, Object> params, Map<String, String> header)
        throws Exception {
        return post(mediaType, url, params, 30, header, false);
    }

    public static String post(String mediaType, String url, Map<String, Object> params, int readTimeoutSeconds,
        Map<String, String> header, boolean ignoreURLValid)
        throws Exception {

        String type = "application/json; charset=utf-8";
        if (mediaType != null && !"".equals(mediaType)) {
            type = mediaType;
        }

        if (!availableMediaType.contains(type)) {
            throw new IOException("Unsupported MediaType: " + mediaType);
        }

        url = buildURL(url);

        OkHttpClient client = buildClient(readTimeoutSeconds);
        Request request = buildPostRequest(mediaType, url, params, header).build();

        try (Response response = client.newCall(request).execute()) {
            return returnRespons(request, response);
        } catch (Exception e) {
            return returnRespons(request, e.getMessage());
        }
    }

    public static String post(String mediaType, String url, List params)
        throws Exception {
        return post(mediaType, url, params, 30, null, false);
    }

    public static String post(String mediaType, String url, List params, Map header)
        throws Exception {
        return post(mediaType, url, params, 30, header, false);
    }

    public static String post(String mediaType, String url, List params, int readTimeoutSeconds,
        Map<String, String> header, boolean ignoreURLValid)
        throws Exception {

        String type = "application/json; charset=utf-8";
        if (mediaType != null && !"".equals(mediaType)) {
            type = mediaType;
        }

        if (!availableMediaType.contains(type)) {
            throw new IOException("Unsupported MediaType: " + mediaType);
        }

        url = buildURL(url);

        OkHttpClient client = buildClient(readTimeoutSeconds);
        Request request = buildPostRequest(mediaType, url, params, header).build();

        try (Response response = client.newCall(request).execute()) {
            return returnRespons(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static void asyncPost(String mediaType, String url, Map<String, String> params, int readTimeoutSeconds,
        Map<String, String> header, Callback callback, boolean ignoreURLValid)
        throws Exception {

        String type = "application/json; charset=utf-8";
        if (mediaType != null && !"".equals(mediaType)) {
            type = mediaType;
        }

        if (!availableMediaType.contains(type)) {
            throw new IOException("Unsupported MediaType: " + mediaType);
        }

        url = buildURL(url);

        OkHttpClient client = buildClient(readTimeoutSeconds);
        Request request = buildPostRequest(mediaType, url, params, header).build();

        if (callback == null) {
            client.newCall(request).enqueue(default_async_callback);
        } else {
            client.newCall(request).enqueue(callback);
        }
    }

    //处理response的body
    private static String returnRespons(Request request, Response response)
        throws Exception {
        if (!response.isSuccessful()) {
            logger.error(request.url() + "  returnRespons" + response);
            throw new IOException("Unexpected code: " + response);
        }
        //TODO 判断body大小是否大于1M
        return response.body().string();
    }

    //处理response的body
    private static String returnRespons(Request request, String e)
        throws Exception {

        logger.error(request.url() + " return Respons:" + e);
        throw new IOException("Unexpected code: " + e);
    }



    private static String buildURL(String url)
        throws Exception {

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://".concat(url);
        }
        return url;
    }

    //检查超时参数
    private static boolean isTimeOutLegal(int readTimeoutSeconds) {
        if (readTimeoutSeconds <= 0) {
            logger.error("ReadTimeout:" + readTimeoutSeconds + "不正确");
            return false;
        }
        return true;
    }

    private static OkHttpClient getUnsafeOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {
        // 创建一个信任所有证书的 TrustManager
        final TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }
        };

        // 创建一个 SSLContext，并指定信任所有证书
        SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        // 创建一个 OkHttpClient，并指定 SSLContext
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);

        return builder.build();
    }
    public static OkHttpClient buildClient(int readTimeoutSeconds, String str_cert) {
        if("1".equals(str_cert)) {
            try{
                return getUnsafeOkHttpClient();
            }catch(Exception e){
                e.printStackTrace();
            }
        }else{
            return zipkinClient;
        }

        if (readTimeoutSeconds != socket_timeout) {

            if (str_cert != null && str_cert.length() > 0) {
                try {
                    X509TrustManager x509TrustManager = SSLSocketClient.trustManagerForCertificates(str_cert);
                    return defaultBuilder.writeTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                        .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                        .sslSocketFactory(SSLSocketClient.getSSLSocketFactory(), x509TrustManager).build();
                } catch (Exception e) {
                    logger.error("trustManager异常", e);
                }
            }
            return defaultBuilder.writeTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS).build();
        } else {
            return zipkinClient;
        }
    }

    private static OkHttpClient buildClient(int readTimeoutSeconds) {
        return buildClient(readTimeoutSeconds, null);
    }

    private static Request.Builder buildGetRequest(String url, Map<String, Object> param, Map<String, String> header) {
        Request.Builder rqBuilder = new Request.Builder();
        if (param == null) {
            param = new HashMap<>();
        }
        url = concat_url_params(url, param);
        rqBuilder.url(url);
        try {

            addHeaders(rqBuilder, new URL(url).getPath(), header, String.valueOf(param.getOrDefault(SystemConstant.accessToken, "")));
        } catch (Exception e) {
            addHeaders(rqBuilder, header, String.valueOf(param.getOrDefault(SystemConstant.accessToken, "")));
        }

        return rqBuilder;
    }


    public static Request.Builder buildPostRequest(String mediaType, String url, Object params,
        Map<String, String> header) {
        Request.Builder rqBuilder = new Request.Builder();
        rqBuilder.url(url);

        try {
            addHeaders(rqBuilder, new URL(url).getPath(), header, "");
        } catch (Exception e) {
            addHeaders(rqBuilder, header, "");
        }

        switch (mediaType) {
            default:
                if (params instanceof String) {
                    rqBuilder.post(RequestBody.create(MediaType.parse(MEDIA_TYPE_JSON), String.valueOf(params)));
                } else {
                    rqBuilder.post(RequestBody.create(MediaType.parse(MEDIA_TYPE_JSON), JsonConvert.toJson(params)));
                }

                break;
            case MEDIA_TYPE_FORM_DATA:
                MultipartBody.Builder multipartBuilder = new MultipartBody.Builder();
                multipartBuilder.setType(MultipartBody.FORM);
                if (params instanceof Map) {
                    try {
                        Map<String, String> p = JsonConvert.toObject(JsonConvert.toJson(params), Map.class);
                        if (p.isEmpty()) {
                            multipartBuilder.addFormDataPart("", "");
                        }
                        for (String key : p.keySet()) {
                            multipartBuilder.addFormDataPart(key, p.get(key));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                RequestBody requestBody = multipartBuilder.build();

                rqBuilder.post(requestBody);

                break;
            case MEDIA_TYPE_FORM_URLENCODED:
                FormBody.Builder formBodyBuilder = new FormBody.Builder();
                if (params instanceof Map) {
                    try {
                        Map<String, String> p = JsonConvert.toObject(JsonConvert.toJson(params), Map.class);
                        for (Map.Entry<String, String> entry : p.entrySet()) {
                            formBodyBuilder.add(entry.getKey(), entry.getValue());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    FormBody formBody = formBodyBuilder.build();
                    rqBuilder.post(formBody);
                }
                break;
        }

        return rqBuilder;
    }

    public static String concat_url_params(String url, Map<String, Object> params) {

        if (params == null || params.isEmpty()) {
            return url;
        }
        Iterator<String> iterator = params.keySet().iterator();
        StringBuilder do_get_sbf = new StringBuilder();

        String first_key = iterator.next();
        do_get_sbf.append(url).append("?").append(first_key).append("=").append(params.get(first_key));
        while (iterator.hasNext()) {
            String key = iterator.next();
            do_get_sbf.append("&").append(key).append("=").append(params.get(key));
        }
        url = do_get_sbf.toString();
        return url;
    }

    private static void addHeaders(Request.Builder builder, Map<String, String> headers, String accessToken) {
        addHeaders(builder, "", headers, accessToken);
    }

    private static void addHeaders(Request.Builder builder, String path, Map<String, String> headers, String accessToken) {

        if (headers == null || headers.isEmpty()) {
            headers = new HashMap<>();
        }

        headers.put("Connection", "close");

        headers.forEach((key, value) -> {
            if (value != null) {
                builder.header(key, value);
            }
        });
    }
}
