package com.refreshrecycle;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.recycletorefresh.GenericRefreshAdapter;

public class MainActivity extends Activity {

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
                return null;
            }
        }) {
            @Override
            protected Object[] onHandleHolder(int type, int page, Object data) {

                return new Object[]{0,null};
            }

            @Override
            protected void onFailure(int type) {

            }
        };
        mAdapter.bindLoading(swipeRefresh, recyclerView, 10);
        mAdapter.setPage(1);
//        mAdapter.addDataHolders(getItems());
        recyclerView.setAdapter(mAdapter);
        mAdapter.setNoMore(true);
        mAdapter.removeFooterView();
    }
}