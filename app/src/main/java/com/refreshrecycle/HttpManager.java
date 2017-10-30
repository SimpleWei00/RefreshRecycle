package com.refreshrecycle;

import android.os.Handler;
import android.os.Looper;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Set;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class HttpManager {

    private static HttpManager mInstance;
    private OkHttpClient mClient;
    private Handler mHandler;

    private HttpManager(){
        mClient = new OkHttpClient();
        mHandler = new Handler(Looper.getMainLooper());
    }

    public static HttpManager getInstance() {
        if (mInstance == null) {
            synchronized (HttpManager.class) {
                if (mInstance == null) {
                    mInstance = new HttpManager();
                }
            }
        }
        return mInstance;
    }

    public String doGet(String url, LinkedHashMap<String, String> params) throws Exception{
        final StringBuilder urls = new StringBuilder(url);
        if(params != null && !params.isEmpty()){
            Set<String> keys = params.keySet();
            urls.append("?");
            for (String key : keys) {
                String value = params.get(key);
                try {
                    value = URLEncoder.encode(value,"utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                urls.append(key).append('=').append(value).append('&');
            }
            int len = urls.length();
            if(urls.lastIndexOf("&") == len - 1){
                urls.delete(len - 1,len);
            }
        }
        Request.Builder builder = new Request.Builder();
//        builder.addHeader("Accept", accept.toString()).addHeader("Content-Type", "application/x-www-form-urlencoded");
        Request request = builder.url(urls.toString()).build();
        Response response = mClient.newCall(request).execute();
        String result = response.body().string();
        int statusCode = response.code();
        if(statusCode == 200){
            return result;
        }
       return null;
    }

}