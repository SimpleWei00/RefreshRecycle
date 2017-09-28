package com.recycletorefresh;

import android.content.Context;

/**
 * type必须大于0
 * Created by 魏学军 on 2016/12/10.
 */
public abstract class DataHolder {

    private Object   mData          = null;
    private int mType;

    public DataHolder(Object data, int type){
        mData = data;
        mType = type;
    }

    public abstract GenericViewHolder onCreateView(Context context);

    public abstract void onBindView(Context context, GenericViewHolder holder, int position, Object data);

    /**
     * 获取构造函数中传入的数据对象
     *
     * @return
     */
    public Object getData()
    {
        return mData;
    }

    public int getType() {
        return mType;
    }
}