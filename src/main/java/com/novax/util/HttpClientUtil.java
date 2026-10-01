package com.novax.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.net.SocketTimeoutException;
import java.util.*;

public class HttpClientUtil {
    static Logger logger = Logger.getLogger(HttpClientUtil.class);

    private static final PoolingHttpClientConnectionManager cm;
    private static final CloseableHttpClient httpClient;

    public String HTTPSTATUS = "HttpStatus";
    private final RequestConfig requestConfig;

    static {
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(50);
        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setConnectionManagerShared(true)
                .build();
    }

    public HttpClientUtil() {
        requestConfig = RequestConfig.custom().setConnectTimeout(5000).
                setConnectionRequestTimeout(5000).setSocketTimeout(30000).build();
    }

    /**
     * @param connectTimeout           设置连接超时时间，单位毫秒。
     * @param connectionRequestTimeout 设置从connect Manager(连接池)获取 Connection
     *                                 超时时间，单位毫秒。这个属性是新加的属性，
     * @param socketTimeout            请求获取数据的超时时间(即响应时间)，单位 毫秒。
     *                                 如果访问一个接口，多少时间内无法返回数据， 就直接放弃此次调用。
     */
    public HttpClientUtil(int connectTimeout, int connectionRequestTimeout, int socketTimeout) {
        requestConfig = RequestConfig.custom().setConnectTimeout(connectTimeout).
                setConnectionRequestTimeout(connectionRequestTimeout).
                setSocketTimeout(socketTimeout).build();
    }

    private void applyHeaders(HttpRequestBase request, HashMap<String, String> headers) {
        if (headers == null) return;
        AllureParam.set("headers", JSON.toJSONString(headers));
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue());
        }
    }

    public JSONObject sendGet(String url, HashMap<String, String> params, HashMap<String, String> headers)
            throws Exception {
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> pairs = new ArrayList<>();
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String value = entry.getValue();
                if (!value.isEmpty()) {
                    pairs.add(new BasicNameValuePair(entry.getKey(), value));
                }
            }
            url += "?" + EntityUtils.toString(new UrlEncodedFormEntity(pairs), "UTF-8");
        }

        AllureParam.set("reqAdr", url);
        AllureParam.set("params", JSON.toJSONString(params));
        HttpGet httpGet = new HttpGet(url);
        httpGet.setConfig(requestConfig);
        applyHeaders(httpGet, headers);

        JSONObject responseJson = null;
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "UTF-8");
                responseJson = JSON.parseObject(responseEntityString);
                responseJson.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时：" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return responseJson;
    }

    public JSONObject sendGet(String url, HashMap<String, String> params) throws Exception {
        return this.sendGet(url, params, null);
    }

    public JSONObject sendGet(String url) throws Exception {
        return this.sendGet(url, null, null);
    }

    public JSONObject sendPostByJson(String url, Object object, HashMap<String, String> headers) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);
        String requestBody = JSON.toJSONString(object);
        AllureParam.set("reqAdr", url);
        AllureParam.set("params", requestBody);
        StringEntity requestEntity = new StringEntity(requestBody, "utf-8");
        requestEntity.setContentType("application/json");
        httpPost.setEntity(requestEntity);
        applyHeaders(httpPost, headers);

        JSONObject responseJson = null;
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                responseJson = JSON.parseObject(responseEntityString);
                responseJson.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时：" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return responseJson;
    }

    public int sendPostByJsonReturnHttpStatus(String url, Object object, HashMap<String, String> headers) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);
        String requestBody = JSON.toJSONString(object);
        AllureParam.set("reqAdr", url);
        AllureParam.set("params", requestBody);
        StringEntity requestEntity = new StringEntity(requestBody, "utf-8");
        requestEntity.setContentType("application/json");
        httpPost.setEntity(requestEntity);
        applyHeaders(httpPost, headers);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            return response.getStatusLine().getStatusCode();
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时：" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
    }

    public JSONObject sendPostByJson(String url, Object object) throws Exception {
        return sendPostByJson(url, object, null);
    }

    public JSONObject sendPostByForm(String url, Map<String, String> formData, HashMap<String, String> headers) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setConfig(requestConfig);
        UrlEncodedFormEntity entity = null;
        if (formData != null) {
            ArrayList<BasicNameValuePair> list = new ArrayList<>();
            formData.forEach((key, value) -> list.add(new BasicNameValuePair(key, value)));
            entity = new UrlEncodedFormEntity(list, "utf-8");
        }
        Objects.requireNonNull(entity).setContentType("application/x-www-form-urlencoded");
        httpPost.setEntity(entity);
        AllureParam.set("reqAdr", url);
        AllureParam.set("params", JSON.toJSONString(formData));
        applyHeaders(httpPost, headers);

        JSONObject jsonObject = null;
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                jsonObject = JSON.parseObject(responseEntityString);
                jsonObject.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时：" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return jsonObject;
    }

    public JSONObject sendPostByForm(String url, Map<String, String> formData) throws Exception {
        return sendPostByForm(url, formData, null);
    }

    public JSONObject sendPut(String url, String entityString, HashMap<String, String> headers) throws Exception {
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(requestConfig);
        AllureParam.set("reqAdr", url);
        AllureParam.set("params", entityString);
        httpPut.setEntity(new StringEntity(entityString, "utf-8"));
        applyHeaders(httpPut, headers);

        JSONObject jsonObject = null;
        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                jsonObject = JSON.parseObject(responseEntityString);
                jsonObject.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时：" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return jsonObject;
    }

    public JSONObject sendPut(String url, Object object, HashMap<String, String> headers) throws Exception {
        HttpPut httpPut = new HttpPut(url);
        httpPut.setConfig(requestConfig);
        String requestBody = JSON.toJSONString(object);
        AllureParam.set("reqAdr", url);
        AllureParam.set("params", requestBody);
        StringEntity requestEntity = new StringEntity(requestBody, "utf-8");
        requestEntity.setContentType("application/json");
        httpPut.setEntity(requestEntity);
        applyHeaders(httpPut, headers);

        JSONObject jsonObject = null;
        try (CloseableHttpResponse response = httpClient.execute(httpPut)) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null) {
                String responseEntityString = EntityUtils.toString(responseEntity, "utf-8");
                jsonObject = JSON.parseObject(responseEntityString);
                jsonObject.put(HTTPSTATUS, response.getStatusLine().getStatusCode());
            }
            EntityUtils.consume(responseEntity);
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时：" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
        return jsonObject;
    }

    public int sendDelete(String url) throws Exception {
        HttpDelete httpDelete = new HttpDelete(url);
        httpDelete.setConfig(requestConfig);

        try (CloseableHttpResponse response = httpClient.execute(httpDelete)) {
            return response.getStatusLine().getStatusCode();
        } catch (SocketTimeoutException | ConnectTimeoutException e) {
            logger.error("请求连接超时：" + e.getMessage());
            throw new RuntimeException("HTTP超时，URL: " + url, e);
        } catch (Exception e) {
            logger.error("请求异常，异常信息：" + e.getMessage());
            throw new Exception("HTTP请求失败，URL: " + url, e);
        }
    }

}
