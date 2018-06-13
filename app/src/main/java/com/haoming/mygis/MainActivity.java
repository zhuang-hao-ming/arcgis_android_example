package com.haoming.mygis;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;

import com.esri.arcgisruntime.mapping.view.Callout;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.IdentifyGraphicsOverlayResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {


    MapView mapView;
    GraphicsOverlay graphicsOverlay;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.mapView = (MapView) findViewById(R.id.mapview);
        ArcGISMap arcGISMap = new ArcGISMap(Basemap.Type.OPEN_STREET_MAP, 23, 121, 5);
        this.mapView.setMap(arcGISMap);
        this.mapView.setOnTouchListener(new EarthquakeSingleTapListener(this));
        graphicsOverlay = new GraphicsOverlay();
        mapView.getGraphicsOverlays().add(graphicsOverlay);

    }

    public void btnZoomIn(View view) {
        this.mapView.setViewpointScaleAsync(this.mapView.getMapScale() / 2);
    }

    public void btnZoomOut(View view) {
        this.mapView.setViewpointScaleAsync(this.mapView.getMapScale() * 2);
    }

    public void btnAddEarthquake(View view) {
        String eqType = (String)view.getTag();


        String url = "https://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/" + eqType +"_day.geojson";


        Toast.makeText(MainActivity.this, url, Toast.LENGTH_SHORT).show();
        getEarthquakeEvent(url);
    }

    void postGetData() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (graphicsOverlay != null && graphicsOverlay.getGraphics().size() == 0) {
            Toast.makeText(MainActivity.this, "没有地震事件", Toast.LENGTH_LONG).show();
        }
    }

    void preGetData() {
        if (mapView.getCallout().isShowing()) {
            mapView.getCallout().dismiss();
        }
        progressDialog = ProgressDialog.show(MainActivity.this, "", "取回geojson中。。。", true);
    }

    void getEarthquakeEvent(String url) {

        graphicsOverlay.getGraphics().clear();
        getEarthquake(url);
    }

    void getEarthquake(String url) {
        // 关闭弹出框，显示一个进度条
        preGetData();


        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    // 获得features字段，这个字段的值是数组
                    JSONArray featureArray = response.getJSONArray("features");
                    // 设置符号
                    SimpleMarkerSymbol simpleMarkerSymbol = new SimpleMarkerSymbol(SimpleMarkerSymbol.Style.CIRCLE, Color.RED, 15);


                    for (int i = 0; i < featureArray.length(); i++) {
                        // 获得几何信息
                        JSONObject geometry = featureArray.getJSONObject(i).getJSONObject("geometry");
                        double lon = Double.parseDouble(geometry.getJSONArray("coordinates").get(0).toString());
                        double lat = Double.parseDouble(geometry.getJSONArray("coordinates").get(1).toString());
                        // 新建点，此处是两步，新建点和投影，api返回的数据是wgs84投影，需要转换为何底图一致的投影
                        Point point = (Point) GeometryEngine.project(new Point(lon, lat,
                                        SpatialReference.create(4326)),
                                mapView.getSpatialReference());
                        // 获得属性信息
                        JSONObject properties = featureArray.getJSONObject(i).getJSONObject("properties");

                        Map<String, Object> attr = new HashMap<String, Object>();

                        String place = properties.getString("place").toString();
                        attr.put("place", place);

                        float mag = Float.valueOf(properties.getString("mag").toString());
                        attr.put("mag", mag);

                        float size = getSizeFromMag(mag);
                        simpleMarkerSymbol.setSize(size);
                        // 将属性信息中的unix时间戳转化为本地时间字符串
                        long time = Long.valueOf(properties.getString("time").toString());
                        Date date = new Date(time);
                        attr.put("date", date.toString());

                        attr.put("rms", properties.getString("rms").toString());
                        attr.put("gap", properties.getString("gap").toString());

                        // 新建并添加graphic元素，参数为坐标点，属性字典，符号
                        graphicsOverlay.getGraphics().add(new Graphic(point, attr, simpleMarkerSymbol));

                        postGetData();

                    }



                } catch (Exception e) {
                    e.printStackTrace();
                }




            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        // 发起请求
        MySingleton.getInstance(MainActivity.this).addToRequestQueue(jsonObjectRequest);
    }

    /**
     * 根据地震的大小返回符号的大小
     * @param mag
     * @return
     */
    private float getSizeFromMag(float mag) {
        float size;
        if (mag < 2.5)
            size = 7;
        else if (mag >= 2.5 && mag < 4.5)
            size = 10;
        else
            size = 15;
        return size;

    }

    private class EarthquakeSingleTapListener extends DefaultMapViewOnTouchListener {
        public EarthquakeSingleTapListener(Context ctx) {
            super(ctx, mapView);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {

            mMapView.getCallout().dismiss(); // 关闭弹窗
            for(int i = 0; i < graphicsOverlay.getGraphics().size(); i++) {
                graphicsOverlay.getGraphics().get(i).setSelected(false); // 将所有元素设置为未选中
            }

            final android.graphics.Point screenPoint = new android.graphics.Point((int)e.getX(), (int)e.getY()); // 获得屏幕点
            final Point mapPoint = mapView.screenToLocation(screenPoint); // 将屏幕点转为坐标点
            // 标准的识别方法， 指定在graphics图层中，屏幕点周围10单位,识别最多10个元素
            final ListenableFuture<IdentifyGraphicsOverlayResult> identifyFuture = mapView.identifyGraphicsOverlayAsync(graphicsOverlay, screenPoint, 10, false, 10);
            // 识别结果通过监听器函数获得
            identifyFuture.addDoneListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        // 获得识别结果
                        List<Graphic> identifiedGraphics = identifyFuture.get().getGraphics();

                        for (Graphic graphic : identifiedGraphics) {
                            // 遍历识别结果， 由于是点击，我们只处理一个识别到的元素

                            graphic.setSelected(true); // 选中元素
                            Map<String, Object> attrs = graphic.getAttributes(); // 获得元素上的属性
                            String text = "";
                            for (int i = 0; i < attrs.size(); i++) {
                                text = text + attrs.keySet().toArray()[i] + ": " + attrs.values().toArray()[i] + "\n";
                            } // 将属性拼接为字符串
                            TextView tv = new TextView(MainActivity.this); // 新建文本框
                            tv.setText(text);
                            tv.setTextSize(20);

                            Callout mapCallout = mMapView.getCallout(); // 新建弹出框
                            mapCallout.setContent(tv); // 将文本框加到弹出框中

                            mapCallout.setLocation(mapPoint); // 设置弹出框位置为点击处
                            mapCallout.show(); // 显示弹出框

                            break;

                        }
                    } catch (Exception e){
                    }
                }
            });


            return true;
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        mapView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }



}


