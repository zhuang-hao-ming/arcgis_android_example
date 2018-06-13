package com.haoming.mygis;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by Administrator on 2018/6/13/013.
 */

public class MySingleton  {

    private static MySingleton  myInstance;
    private static Context ctx;
    private RequestQueue requestQueue;

    private MySingleton (Context context) {
        ctx = context;
        requestQueue = getRequestQueue();
    }

    public RequestQueue getRequestQueue() {
        if (this.requestQueue == null) {
            this.requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());
        }
        return this.requestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }

    public static synchronized MySingleton  getInstance(Context context) {
        if (myInstance == null) {
            myInstance = new MySingleton (context);
        }
        return myInstance;
    }


}
