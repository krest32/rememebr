package com.krest.admin.utils;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.krest.admin.entity.OkRequest;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpUtil {

    static OkHttpClient httpClient;

    public HttpUtil() {
    }

    public static String sendPostRequest(OkRequest okRequest) {
        if (okRequest.getRequestData() == null) {
            okRequest.setRequestData(new Object());
        }

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                JSON.toJSONString(okRequest.getRequestData(), SerializerFeature.DisableCircularReferenceDetect)
        );

        Request request = (new Request.Builder()).url(okRequest.getTargetUrl()).post(body).build();
        Response response = null;

        try {
            response = httpClient.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            log.warn("can not connect to : {} ", okRequest.getTargetUrl());
            return null;
        } finally {
            if (null != response) {
                response.close();
            }

        }

    }

    public static String sendGetRequest(OkRequest okRequest) {
        Request request = (new Request.Builder()).url(okRequest.getTargetUrl()).get().build();
        Response response = null;

        Object var4;
        try {
            response = httpClient.newCall(request).execute();
            String var3 = response.body().string();
            return var3;
        } catch (IOException var8) {
            log.error("can not connect to : {} ", okRequest.getTargetUrl());
            var4 = null;
        } finally {
            if (null != response) {
                response.close();
            }

        }

        return (String) var4;
    }

    static {
        httpClient = (new OkHttpClient()).newBuilder().connectTimeout(500L, TimeUnit.MILLISECONDS).build();
    }
}
