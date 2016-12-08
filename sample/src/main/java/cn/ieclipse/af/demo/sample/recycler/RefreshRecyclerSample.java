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
package cn.ieclipse.af.demo.sample.recycler;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

import cn.ieclipse.af.adapter.AfRecyclerAdapter;
import cn.ieclipse.af.adapter.delegate.AdapterDelegate;
import cn.ieclipse.af.demo.R;
import cn.ieclipse.af.demo.common.AppFooterLoadingDelegate;
import cn.ieclipse.af.demo.common.AppRefreshRecyclerHelper;
import cn.ieclipse.af.demo.common.ui.H5Activity;
import cn.ieclipse.af.demo.sample.SampleBaseFragment;
import cn.ieclipse.af.view.refresh.RefreshLayout;
import cn.ieclipse.af.view.refresh.RefreshRecyclerHelper;
import cn.ieclipse.af.volley.RestError;

/**
 * Description
 *
 * @author Jamling
 */
public class RefreshRecyclerSample extends SampleBaseFragment implements NewsController.NewsListener,
    RefreshLayout.OnRefreshListener {
    RefreshLayout refreshLayout;
    RefreshRecyclerHelper helper;
    RecyclerView listView;
    AfRecyclerAdapter<NewsController.NewsInfo> adapter;
    NewsController controller = new NewsController(this);

    CheckBox rb1;

    @Override
    public CharSequence getTitle() {
        return "RefreshRecycler(New)";
    }

    @Override
    protected int getContentLayout() {
        return R.layout.sample_refresh_recycler;
    }

    @Override
    protected void initContentView(View view) {
        super.initContentView(view);

        refreshLayout = (RefreshLayout) view.findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(this);
        refreshLayout.setMode(RefreshLayout.REFRESH_MODE_BOTH);

        helper = new AppRefreshRecyclerHelper(refreshLayout) {
            @Override
            protected boolean isEmpty() {
                return getItemCount() - adapter.getHeaderCount() - adapter.getFooterCount() <= 0;
            }
        };
        helper.setKeepLoaded(true);
        listView = (RecyclerView) refreshLayout.findViewById(R.id.rv);
        adapter = new AfRecyclerAdapter<>();
        adapter.registerDelegate(new NewDelegate());
        adapter.setOnItemClickListener(new AfRecyclerAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                NewsController.NewsInfo info = adapter.getItem(position);
                if (info != null) {
                    startActivity(H5Activity.create(refreshLayout.getContext(), info.url, info.title));
                }
            }
        });

        helper.setAdapter(adapter);

        rb1 = (CheckBox) view.findViewById(R.id.rb1);
        rb1.setChecked(refreshLayout.isAutoLoad());
        chk3.setChecked(helper.isKeepLoaded());
        load(true);
    }

    @Override
    public void onRefresh() {
        load(false);
    }

    @Override
    public void onLoadMore() {
        load(false);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (chk1 == buttonView) {
            if (isChecked) {
                adapter.registerDelegate(-2, new HeaderDelegate());
            }
            else {
                adapter.removeDelegate(-2);
            }
            adapter.notifyDataSetChanged();
        }
        else if (chk2 == buttonView) {
            if (isChecked) {
                adapter.registerDelegate(-3, new AppFooterLoadingDelegate<NewsController.NewsInfo>(refreshLayout));
            }
            else {
                adapter.removeDelegate(-3);
            }
            adapter.notifyDataSetChanged();
        }
        else if (chk3 == buttonView) {
            helper.setKeepLoaded(isChecked);
        }
        else if (rb1 == buttonView) {
            refreshLayout.setAutoLoad(rb1.isChecked());
        }
    }

    private void load(boolean needCache) {
        NewsController.NewsRequest req = new NewsController.NewsRequest();
        req.page = helper.getCurrentPage();
        controller.loadNews(req, needCache);
    }

    @Override
    public void onLoadNewsFailure(RestError error) {
        helper.onLoadFailure(error);
    }

    @Override
    public void onLoadNewsSuccess(List<NewsController.NewsInfo> out, boolean fromCache) {
        if (chk5.isChecked()) {
            helper.onLoadFinish(null, 0, 0);
        }
        else if (chk6.isChecked()) {
            throw new NullPointerException("Mock error!");
        }
        else {
            helper.onLoadFinish(out, 50, 0);
        }
    }

    private class NewDelegate extends AdapterDelegate<NewsController.NewsInfo> {

        @Override
        public int getLayout() {
            return android.R.layout.simple_list_item_1;
        }

        @Override
        public void onUpdateView(RecyclerView.ViewHolder holder, NewsController.NewsInfo info, int position) {
            TextView tv = (TextView) holder.itemView;
            tv.setText(String.valueOf(position) + info.title);
        }
    }

    private class HeaderDelegate extends AdapterDelegate<NewsController.NewsInfo> {

        @Override
        public boolean isForViewType(NewsController.NewsInfo info, int position) {
            return position == 0;
        }

        @Override
        public int getLayout() {
            return android.R.layout.simple_list_item_1;
        }

        @Override
        public void onUpdateView(RecyclerView.ViewHolder holder, NewsController.NewsInfo info, int position) {
            TextView tv = (TextView) holder.itemView;
            tv.setText("Mock Header!");
        }
    }

    private class StringAdapter extends AfRecyclerAdapter<NewsController.NewsInfo> {

        public StringAdapter(Context context) {
            super(context);
        }

        @Override
        public int getLayout() {
            return android.R.layout.simple_list_item_1;
        }

        @Override
        public void onUpdateView(RecyclerView.ViewHolder holder, NewsController.NewsInfo data, int position) {
            TextView tv = (TextView) holder.itemView;
            tv.setText(String.valueOf(position) + data.title);
        }
    }
}
