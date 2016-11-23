/*
 * Copyright (C) 2015-2016 QuickAF
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.ieclipse.af.view.refresh;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.HashMap;
import java.util.Map;

import cn.ieclipse.af.R;
import cn.ieclipse.af.common.Logger;
import cn.ieclipse.af.view.VScrollView;

/**
 * RefreshLayout is the parent view of {@link android.support.v4.widget.SwipeRefreshLayout SwipeRefreshLayout}.
 * <p>
 * The struct is:
 * <pre>
 *     &lt;RefreshLayout&gt;
 *          &lt;android.support.v4.widget.SwipeRefreshLayout&gt;
 *              &lt;content_view... &gt;
 *          &lt;android.support.v4.widget.SwipeRefreshLayout&gt;
 *              &lt;empty_view... &gt;
 * </pre>
 * </p>
 *
 * @author Jamling
 */
public class RefreshLayout extends FrameLayout implements SwipeRefreshLayout.OnRefreshListener {

    /**
     * 禁用刷新和加载
     */
    public static final int REFRESH_MODE_NONE = 0x00;
    /**
     * 可下拉刷新
     */
    public static final int REFRESH_MODE_TOP = 0x01;
    /**
     * 可上拉加载
     */
    public static final int REFRESH_MODE_BOTTOM = 0x02;
    /**
     * 可上拉加载和下拉刷新
     */
    public static final int REFRESH_MODE_BOTH = 0x03;

    // 无刷新
    private static final int LOADING_NONE = 0;
    // 下拉刷新
    private static final int LOADING_REFRESH = 1;
    // 上拉加载更多
    private static final int LOADING_MORE = -1;

    private int mLoading = LOADING_NONE;

    private int mRefreshMode = REFRESH_MODE_TOP;

    /**
     * 是否滚动到底部自动加载
     */
    private boolean mAutoLoad = true;

    public RefreshLayout(Context context) {
        this(context, null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    protected OnRefreshListener mOnRefreshListener;
    protected SwipeRefreshLayout mContentViewWrapper;
    protected SwipeRefreshLayout mEmptyViewWrapper;
    protected View mContentView;
    protected EmptyView mEmptyView;
    protected LayoutInflater mLayoutInflater;
    protected Logger mLogger = Logger.getLogger(getClass());

    protected void init(Context context, AttributeSet attrs) {
        mContentViewWrapper = new SwipeRefreshLayout(context);
        mEmptyViewWrapper = new SwipeRefreshLayout(context);
        mContentViewWrapper.setOnRefreshListener(this);
        mEmptyViewWrapper.setOnRefreshListener(this);

        addView(mContentViewWrapper,
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mEmptyViewWrapper,
            new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mLayoutInflater = LayoutInflater.from(context);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
            handleStyledAttributes(a);
            a.recycle();
        }

        if (mEmptyView == null) {
            mEmptyViewWrapper.setVisibility(View.GONE);
        }
        else {
            mContentViewWrapper.setVisibility(GONE);
        }
        registerProxy(VScrollView.class, new RefreshScrollProxy());
        registerProxy(RecyclerView.class, new RefreshRecyclerProxy());
    }

    protected void handleStyledAttributes(TypedArray a) {
        int contentId = a.getResourceId(R.styleable.RefreshLayout_ptr_content, 0);
        if (contentId > 0) {
            mContentView = mLayoutInflater.inflate(contentId, mContentViewWrapper, false);
            mContentViewWrapper.addView(mContentView);
        }
        int emptyId = a.getResourceId(R.styleable.RefreshLayout_ptr_empty, 0);
        if (emptyId > 0) {
            mEmptyView = (EmptyView) mLayoutInflater.inflate(emptyId, mEmptyViewWrapper, false);
            mEmptyViewWrapper.addView(mEmptyView);
        }
    }

    protected void detectProxy() {
        if (mContentView == null) {
            return;
        }
        if (getProxy() != null) {
            getProxy().setEnabled(false);
        }
        RefreshProxy proxy = mProxyMap.get(mContentView.getClass());
        if (proxy != null) {
            this.mProxy = proxy;
            proxy.setEnabled(true);
        }
    }

    @Override
    public void onRefresh() {
        mLoading = LOADING_REFRESH;
        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    public void loadMore() {
        boolean isRefreshing = mContentViewWrapper.isRefreshing();
        if (!isRefreshing // 是否正在刷新
            && mLoading == LOADING_NONE // 是否正在加载
            && (mRefreshMode & REFRESH_MODE_BOTTOM) != 0) {
            mLogger.d("load more");
            mContentViewWrapper.setRefreshing(true);
            mLoading = LOADING_MORE;
            if (mOnRefreshListener != null) {
                mOnRefreshListener.onLoadMore();
            }
        }
    }

    public void onRefreshComplete() {
        mLogger.v("onRefreshComplete");
        // 清除RecyclerView的加载状态
        if (mContentViewWrapper.isRefreshing()) {
            mContentViewWrapper.setRefreshing(false);
        }

        // 清除empty的加载状态
        if (mEmptyViewWrapper.isRefreshing()) {
            mEmptyViewWrapper.setRefreshing(false);
        }

        mLoading = LOADING_NONE;
        // 上拉完成，恢复下拉可用状态
        // 防止mRefreshMode = REFRESH_MODE_NONE,恢复可刷新状态
        if (mRefreshMode != REFRESH_MODE_NONE) {
            mContentViewWrapper.setEnabled(true);
        }
    }

    public void showEmptyView() {
        mEmptyViewWrapper.setVisibility(View.VISIBLE);
        mContentViewWrapper.setVisibility(View.GONE);
    }

    public void hideEmptyView() {
        mEmptyViewWrapper.setVisibility(View.GONE);
        mContentViewWrapper.setVisibility(View.VISIBLE);
    }

    public static abstract class RefreshProxy<T> {
        protected RefreshLayout mRefresh;
        protected T view;

        private void setRefresh(RefreshLayout refreshLayout) {
            this.mRefresh = refreshLayout;
        }

        public RefreshLayout getRefresh() {
            return mRefresh;
        }

        private void setView(T t) {
            this.view = t;
        }

        public T getView() {
            return view;
        }

        public abstract void setEnabled(boolean enable);
    }

    public static class ViewProxy<T> {

    }

    public enum Mode {
        /**
         * Disable all Pull-to-Refresh gesture and Refreshing handling
         */
        DISABLED(0x0),

        /**
         * Only allow the user to Pull from the start of the Refreshable View to
         * refresh. The start is either the Top or Left, depending on the
         * scrolling direction.
         */
        PULL_FROM_START(0x1),

        /**
         * Only allow the user to Pull from the end of the Refreshable View to
         * refresh. The start is either the Bottom or Right, depending on the
         * scrolling direction.
         */
        PULL_FROM_END(0x2),

        /**
         * Allow the user to both Pull from the start, from the end to refresh.
         */
        BOTH(0x3);
        private int mIntValue;

        Mode(int modeInt) {
            mIntValue = modeInt;
        }
    }

    private Map<Class, RefreshProxy> mProxyMap = new HashMap<>();
    private RefreshProxy mProxy;

    public void registerProxy(Class clazz, RefreshProxy proxy) {
        if (proxy != null) {
            proxy.setView(mContentView);
            proxy.setRefresh(this);
            mProxyMap.put(clazz, proxy);
            detectProxy();
        }
    }

    public RefreshProxy getProxy() {
        return mProxy;
    }

    public interface OnRefreshListener extends SwipeRefreshLayout.OnRefreshListener {
        void onLoadMore();
    }

    // getter & setter
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.mOnRefreshListener = onRefreshListener;
    }

    /**
     * set the refresh mode to control the refresh direction
     *
     * @param refreshMode {@link #REFRESH_MODE_NONE} or
     *                    {@link #REFRESH_MODE_TOP} or
     *                    {@link #REFRESH_MODE_BOTTOM} or
     *                    {@link #REFRESH_MODE_BOTH}
     */
    public void setMode(int refreshMode) {
        this.mRefreshMode = refreshMode;
        if (refreshMode == REFRESH_MODE_NONE) {
            mContentViewWrapper.setEnabled(false);
            mEmptyViewWrapper.setEnabled(false);
        }
        else {
            mContentViewWrapper.setEnabled(true);
            mEmptyViewWrapper.setEnabled(true);
        }
    }

    public boolean isAutoLoad() {
        return mAutoLoad;
    }

    public EmptyView getEmptyView() {
        return mEmptyView;
    }

    public View getContentView() {
        return mContentView;
    }

    /**
     * 设置刷新indicator的颜色，默认黑色
     *
     * @param colorResIds
     */
    public void setColorSchemeResources(int... colorResIds) {
        mContentViewWrapper.setColorSchemeResources(colorResIds);
        mEmptyViewWrapper.setColorSchemeResources(colorResIds);
    }

    public boolean isRefresh() {
        return mLoading == LOADING_REFRESH;
    }
}
