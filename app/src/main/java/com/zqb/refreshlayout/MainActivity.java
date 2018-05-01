package com.zqb.refreshlayout;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity  {

    private RecyclerView mRecyclerView;
    private RefreshLayout mRefreshLayout;
    private TextView mHeaderHintTextView;
    private TextView mLoadTextView;
    private int mCount=15;
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRecyclerView =  findViewById(R.id.recycler_view);
        mRefreshLayout = findViewById(R.id.refresh_layout);
        mHeaderHintTextView = findViewById(R.id.header_hint_text);
        mLoadTextView = findViewById(R.id.load_text_view);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mAdapter = new MyAdapter();
        mRecyclerView.setAdapter(mAdapter);
        mRefreshLayout.bindRecyclerView(mRecyclerView);
        mRefreshLayout.canScrollDistance(1000);
        mRefreshLayout.setRefreshListener(new RefreshListener() {
            @Override
            void refresh() {
                mHeaderHintTextView.setText(R.string.header_hint_refresh_loading);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mRefreshLayout.refreshComplete();
                    }

                }, 5000);
            }

            @Override
            void pullUp() {
                mHeaderHintTextView.setText(R.string.header_hint_refresh_normal);
            }

            @Override
            void pullDown() {
                mHeaderHintTextView.setText(R.string.header_hint_refresh_ready);
            }

            @Override
            void loadMore() {
                super.loadMore();
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if(mCount>30) {
                            mLoadTextView.setText("加载完毕");
                            mRefreshLayout.loadComplete(1);
                        }else {
                            mCount += 10;
                            mAdapter.notifyDataSetChanged();
                            mRefreshLayout.loadComplete(0);
                        }
                    }

                }, 2000);
            }
        },true);
    }

    private class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_recycler_view, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            holder.mTextView.setText(position+"--");
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(MainActivity.this,"haha",Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mCount;
        }
    }

    private class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView mTextView;

        private MyViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.item_text_view);
        }
    }
}
