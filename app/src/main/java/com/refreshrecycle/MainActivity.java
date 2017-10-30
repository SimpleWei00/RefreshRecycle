package com.refreshrecycle;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmable;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.memory.NoOpMemoryTrimmableRegistry;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.recycletorefresh.DataHolder;
import com.recycletorefresh.GenericRefreshAdapter;
import com.refreshrecycle.holder.NewsDataHolder;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import okhttp3.OkHttpClient;

public class MainActivity extends Activity {

    private static final String API = "http://api.dagoogle.cn/news/get-news";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipeRefresh);
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.rv);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(null);
        GenericRefreshAdapter mAdapter = new GenericRefreshAdapter(this, new GenericRefreshAdapter.LoadCallback() {
            @Override
            public Object onLoad(int page, int type) throws Exception {
                //数据加载
                LinkedHashMap<String, String> params = new LinkedHashMap<>();
                params.put("tableNum","1");
                params.put("page",String.valueOf(page));
                params.put("pagesize","15");
                params.put("justList","1");
                String result = HttpManager.getInstance().doGet(API,params);
                JSONObject jsonObject = new JSONObject(result);
                Gson gson = new Gson();
                List<News> newses = gson.fromJson(jsonObject.getString("data"),new TypeToken<List<News>>(){}.getType());
                return newses;
            }
        }) {
            @Override
            protected Object[] onHandleHolder(int type, int page, Object data) {
                List<News> newses = (List<News>) data;
                if(newses == null || newses.isEmpty()){
                    return null;
                }
                return new Object[]{newses.size(),getItems(newses)};
            }

            @Override
            protected void onFailure(int type) {

            }
        };
        mAdapter.bindLoading(swipeRefresh, recyclerView, 15);
        mAdapter.setPage(1);
//        mAdapter.addDataHolders(getItems());
        recyclerView.setAdapter(mAdapter);
        mAdapter.setNoMore(true);
        mAdapter.removeFooterView();



        Fresco.initialize(this,getImagePipelineConfig(getApplicationContext()));
    }

    private List<DataHolder> getItems(List<News> newses){
        List<DataHolder> holders = new ArrayList<>();
        for(News news : newses){
            holders.add(new NewsDataHolder(news,1));
        }
        return holders;
    }

    private ImagePipelineConfig getImagePipelineConfig(Context context){
        // 当内存紧张时采取的措施
        MemoryTrimmableRegistry memoryTrimmableRegistry = NoOpMemoryTrimmableRegistry.getInstance();
        memoryTrimmableRegistry.registerMemoryTrimmable(new MemoryTrimmable() {
            @Override
            public void trim(MemoryTrimType trimType) {
                final double suggestedTrimRatio = trimType.getSuggestedTrimRatio();
                if (MemoryTrimType.OnCloseToDalvikHeapLimit.getSuggestedTrimRatio() == suggestedTrimRatio
                        || MemoryTrimType.OnSystemLowMemoryWhileAppInBackground.getSuggestedTrimRatio() == suggestedTrimRatio
                        || MemoryTrimType.OnSystemLowMemoryWhileAppInForeground.getSuggestedTrimRatio() == suggestedTrimRatio
                        ) {
                    // 清除内存缓存
                    Fresco.getImagePipeline().clearMemoryCaches();
                }
            }
        });
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        ImagePipelineConfig config = OkHttpImagePipelineConfigFactory.newBuilder(context, okHttpClient)
                .setBitmapMemoryCacheParamsSupplier(new LolipopBitmapMemoryCacheSupplier((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)))
                .setDownsampleEnabled(true)
                .setMemoryTrimmableRegistry(memoryTrimmableRegistry)
                .setBitmapsConfig(Bitmap.Config.RGB_565)
                .build();
        return config;
    }
}