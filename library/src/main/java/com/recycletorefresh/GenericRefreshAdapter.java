package com.recycletorefresh;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.recycletorefresh.util.AsyncWeakTask;

import java.util.List;

import static android.support.v7.widget.RecyclerView.OnScrollListener;

/**
 * RecycleView下拉刷新和上拉加载更多
 * viewType == 0 表示foot.所以其他类型必须从1开始
 * 需重写onCreateViewHolder方法
 * Created by 魏学军 on 2016/12/14.
 */
public abstract class GenericRefreshAdapter extends GenericAdapter{

    /**foot布局类型*/
    public final static int FOOT_TYPE = 0;
    /**获取最后可见位置*/
    private int[] lastPositions;
    protected LayoutManagerType mLayoutManagerType;
    /**最后显示布局的位置*/
    private int lastVisibleItemPosition;
    /**当前滑动的状态*/
    private int currentScrollState = 0;
    /**是否能加载更多*/
    public boolean mIsNoMore;
    /**加载监听*/
    private LoadCallback mCallback    = null;
    /**d当前页数*/
    private int mPage = 1;
    /**加载更多正在请求*/
    boolean mIsReqMore = false;
    boolean mIsReqInit = false;
    /**加载更多失败*/
    boolean mIsLoadFailedMore = false;
    /**分页数*/
    private int mResultCount = 0;
    private GridSpanSizeLookup mGridSpanSizeLookup;
    private GridLayoutManager gridManager;
    private RecyclerView mRecyclerView;

    /**
     * 线性布局
     * 网格布局
     * 不规格布局
     */
    public enum LayoutManagerType {
        LINEAR_LAYOUT,
        GRID_LAYOUT,
        STAGGERED_GRID_LAYOUT
    }

    public GenericRefreshAdapter(Context context,LoadCallback loadCallback) {
        super(context);
        mCallback = loadCallback;
    }

    /**
     * swipeRefresh为null,不需要下拉刷新
     * @param swipeRefresh
     * @param recyclerView
     * @param resultCount  分页数
     */
    public void bindLoading(final SwipeRefreshLayout swipeRefresh, RecyclerView recyclerView, int resultCount){
        mRecyclerView = recyclerView;
        mResultCount = resultCount;
        if(swipeRefresh != null){
            //改变加载显示的颜色
            swipeRefresh.setColorSchemeColors(getContext().getResources().getColor(R.color.swipe_refresh_color));
            swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mPage = 1;
                    reqInitData(swipeRefresh);
                }
            });
        }
        recyclerView.addOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                currentScrollState = newState;
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                if (visibleItemCount > 0 && currentScrollState == RecyclerView.SCROLL_STATE_IDLE && lastVisibleItemPosition >= totalItemCount - 1) {
                    //可以加载更多或者加载更多失败
                    if (!mIsNoMore || mIsLoadFailedMore){
                        if(!isExistFoot()){
                            addFooterView(new FootDataHolder("",0));
                            recyclerView.smoothScrollToPosition(getItemCount());
                        }
                        reqMoreData();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (mLayoutManagerType == null) {
                    if (layoutManager instanceof LinearLayoutManager) {
                        mLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT;
                    } else if (layoutManager instanceof GridLayoutManager) {
                        mLayoutManagerType = LayoutManagerType.GRID_LAYOUT;
                    } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                        mLayoutManagerType = LayoutManagerType.STAGGERED_GRID_LAYOUT;
                    } else {
                        throw new RuntimeException("Unsupported LayoutManager used. Valid ones are LinearLayoutManager, GridLayoutManager and StaggeredGridLayoutManager");
                    }
                }
                switch (mLayoutManagerType) {
                    case LINEAR_LAYOUT:
                        lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
                        break;
                    case GRID_LAYOUT:
                        lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
                        break;
                    case STAGGERED_GRID_LAYOUT:
                        StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                        if (lastPositions == null) {
                            lastPositions = new int[staggeredGridLayoutManager.getSpanCount()];
                        }
                        staggeredGridLayoutManager.findLastVisibleItemPositions(lastPositions);
                        lastVisibleItemPosition = findMax(lastPositions);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public GenericViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == 0){
            View view = LayoutInflater.from(getContext()).inflate(R.layout.generic_foot_auto_loading,null);
            GenericViewHolder viewHolder = new GenericViewHolder(view);
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return viewHolder;
        }
        return super.onCreateViewHolder(parent,viewType);
    }

    @Override
    public final int getItemViewType(int position) {
        int size = getData().size();
        if(size == position){
            return FOOT_TYPE;
        }
        return super.getItemViewType(position);
    }

    /**
     * 设置是否可以加载更多
     * @param flag tre没有更多,false可以加载更多
     */
    public void setNoMore(boolean flag){
        mIsNoMore = flag;
    }

    /**
     * 添加Foot
     * @param holder
     */
    private void addFooterView(DataHolder holder) {
        int size = getData().size();
        addDataHolder(holder);
        notifyItemInserted(size);
    }

    /**
     * 移除Foot
     */
    public void removeFooterView(){
        List<DataHolder> holders = getData();
        if(holders == null || holders.isEmpty()){
            return;
        }
        int size = holders.size();
        //获取最后一个位置上的布局
        DataHolder lastHolder = holders.get(size - 1);
        //如果是Foot,就移除
        if(lastHolder instanceof FootDataHolder){
            //从数据源中移除掉Foot数据
            holders.remove(lastHolder);
            //从适配器移除该布局
            notifyItemRemoved(size - 1);
            return;
        }
    }

    /**
     * 是否存在Foot
     * @return
     */
    public boolean isExistFoot(){
        List<DataHolder> holders = getData();
        if(holders == null || holders.isEmpty()){
            return false;
        }
        int size = holders.size();
        //获取最后一个位置上的布局
        DataHolder lastHolder = holders.get(size - 1);
        if(lastHolder instanceof FootDataHolder){
            return true;
        }
        return false;
    }

    /**
     * 设置为下一页
     * @param curPage 当前页数
     */
    public void setPage(int curPage){
        mPage = mPage + curPage;
    }

    /**
     * 重设到初始化状态
     */
    public void resetPage(int curPage){
        mPage = 1;
        setPage(curPage);
    }

    /**
     * 添加foot
     */
    public void addFoot(){
        //可以加载更多或者加载更多失败
        if (!mIsNoMore || mIsLoadFailedMore){
            if(!isExistFoot() && mRecyclerView != null){
                addFooterView(new FootDataHolder("",0));
                mRecyclerView.smoothScrollToPosition(getItemCount());
            }
            reqMoreData();
        }
    }

    /**
     * 手动调用下拉刷新
     */
    public void refreshPullDown(SwipeRefreshLayout swipeRefresh){
        swipeRefresh.setRefreshing(true);
        mPage = 1;
        reqInitData(swipeRefresh);
    }

    /**
     * 下拉刷新
     * @param swipeRefresh
     */
    private void reqInitData(SwipeRefreshLayout swipeRefresh){
        //正在刷新，直接返回
        if(mIsReqInit){
            return;
        }
        mIsReqInit = true;
        new AsyncWeakTask<Object, Object, Object>(this,swipeRefresh) {

            protected void onPreExecute(Object[] objs) {
                super.onPreExecute(objs);
            }

            @Override
            protected Object doInBackgroundImpl(Object... arg0) throws Exception {
                int curPage = (Integer) arg0[0];
                return mCallback.onLoad(curPage,0);
            }

            protected void onPostExecute(Object[] objs, Object result) {
                super.onPostExecute(objs, result);
                GenericRefreshAdapter adapter = (GenericRefreshAdapter) objs[0];
                SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) objs[1];
                adapter.mIsReqInit = false;
                swipeRefresh.setRefreshing(false);
                Object[] datas = onHandleHolder(0,1,result);
                if(datas == null || datas.length<=0){
                    return;
                }
                adapter.clearDataHolders(false);
                //真是分页加载的数据,以免受多种类型的干扰
                int resultRealSize = (Integer) datas[0];
                //大于能分页条数
                if(resultRealSize >= adapter.mResultCount){
                    adapter.setNoMore(false);
                }else{
                    //移除foot
                    adapter.removeFooterView();
                    //没更多数据
                    adapter.setNoMore(true);
                }
                adapter.addDataHolders((List<DataHolder>)datas[1]);
                adapter.mPage++;
            }

            protected void onException(Object[] objs, Exception e) {
                GenericRefreshAdapter adapter = (GenericRefreshAdapter) objs[0];
                SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) objs[1];
                swipeRefresh.setRefreshing(false);
                adapter.onFailure(0);
                adapter.mIsReqInit = false;
            }

            @Override
            protected void onCancelled(Object[] objs) {
                GenericRefreshAdapter adapter = (GenericRefreshAdapter) objs[0];
                adapter.mIsReqInit = false;
                SwipeRefreshLayout swipeRefresh = (SwipeRefreshLayout) objs[1];
                swipeRefresh.setRefreshing(false);
            }
        }.execute(mPage);
    }

    /**
     * 加载更多
     */
    public void reqMoreData(){
        //正在加载更多，直接返回
        if(mIsReqMore){
            return;
        }
        mIsReqMore = true;
        new AsyncWeakTask<Object, Object, Object>(this) {

            @Override
            protected Object doInBackgroundImpl(Object... arg0) throws Exception {
                int curPage = (Integer) arg0[0];
                return mCallback.onLoad(curPage,1);
            }

            protected void onPostExecute(Object[] objs, Object result) {
                super.onPostExecute(objs, result);
                GenericRefreshAdapter adapter = (GenericRefreshAdapter) objs[0];
                adapter.mIsReqMore = false;
                adapter.mIsLoadFailedMore = false;
                Object[] datas = onHandleHolder(1,adapter.mPage,result);
                //没加载到数据
                if(datas == null || datas.length <= 0){
                    //移除foot
                    adapter.removeFooterView();
                    //没更多数据
                    adapter.setNoMore(true);
                    return;
                }
                //真是分页加载的数据,以免受多种类型的干扰
                int resultRealSize = (Integer) datas[0];
                //大于能分页条数
                if(resultRealSize >= adapter.mResultCount){
                    adapter.setNoMore(false);
                    //将数据插入到Foot前面
                    adapter.addDataHolders(adapter.getData().size() - 1,(List<DataHolder>)datas[1]);
                }else{
                    //移除foot
                    adapter.removeFooterView();
                    //没更多数据
                    adapter.setNoMore(true);
                    //添加数据
                    adapter.addDataHolders((List<DataHolder>)datas[1]);
                }
                adapter.mPage++;
            }

            protected void onException(Object[] objs, Exception e) {
                GenericRefreshAdapter adapter = (GenericRefreshAdapter) objs[0];
                adapter.mIsReqMore = false;
                adapter.mIsLoadFailedMore = true;
                adapter.onFailure(1);
            }

            @Override
            protected void onCancelled(Object[] objs) {
                //super.onCancelled(objs);
                GenericRefreshAdapter adapter = (GenericRefreshAdapter) objs[0];
                adapter.mIsReqMore = false;
            }
        }.execute(mPage);
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            gridManager = ((GridLayoutManager) manager);
            if (mGridSpanSizeLookup == null) {
                mGridSpanSizeLookup = new GridSpanSizeLookup();
            }
            gridManager.setSpanSizeLookup(mGridSpanSizeLookup);
        }
    }

    /**
     * 设置跨度
     */
    class GridSpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            //最后一个位置,并且是Foot,就设置为多少跨度
            if (position == getData().size() -1 && isExistFoot()) {
                return gridManager.getSpanCount();
            }
            return 1;
        }
    }

    @Override
    public void onViewAttachedToWindow(GenericViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (isStaggeredGridLayout(holder)) {
            handleLayoutIfStaggeredGridLayout(holder, holder.getLayoutPosition());
        }
    }

    private boolean isStaggeredGridLayout(android.support.v7.widget.RecyclerView.ViewHolder holder) {
        ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
        if (layoutParams != null && layoutParams instanceof StaggeredGridLayoutManager.LayoutParams) {
            return true;
        }
        return false;
    }

    protected void handleLayoutIfStaggeredGridLayout(android.support.v7.widget.RecyclerView.ViewHolder holder, int position) {
        if (position == getData().size() -1 && isExistFoot()) {
            StaggeredGridLayoutManager.LayoutParams p = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
            p.setFullSpan(true);
        }
    }

    private int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    /**
     *
     * @param type  类型  0表示下拉刷新 1加载更多
     * @param page  页数
     * @param data  加载到的数据
     * @return
     */
    protected abstract Object[] onHandleHolder(int type,int page,Object data);

    /**
     * UI线程
     * @param type 0表示下拉刷新 1加载更多
     */
    protected abstract void onFailure(int type);

    public abstract static class LoadCallback{
        /**
         * 数据加载 非UI线程
         * @param page 页数
         * @param type 类型  0表示下拉刷新 1加载更多
         */
        public abstract Object onLoad(int page,int type) throws Exception;
    }
}