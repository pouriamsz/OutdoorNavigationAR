package com.example.outdoordirections.model;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class API {
    String url;
    String method;
    Response response;

    public API(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public Response getResponse() {
        return response;
    }

    public void sendRequest() {
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        Response response_ = null;
        Request request = new Request.Builder().url(this.url)
                .method(this.method, null)
                .build();
        try {
            response_ = client.newCall(request).execute();
        }catch (IOException e) {
            e.printStackTrace();
        }

        this.response = response_;
    }
}
