package com.zqb.refreshlayout;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Scroller;
import java.util.HashMap;

/**
 * 刷新
 * Created by  on 2018/1/21.
 */

public class RefreshLayout extends ViewGroup {

    private Scroller mScroller;
    private RecyclerView mRecyclerView;
    private RefreshListener mListener;

    private int mTotalHeight; //子view加在一起总高度
    private  HashMap<Integer,Integer> mViewMarginTop; //所有子view marginTop距离
    private  HashMap<Integer,Integer> mViewMarginBottom;//所有子view marginBottom距离
    private float mStartY;//手指落下位置 移动之后更新
    private float mStartX;
    private int mMoveHeight; //移动的总高度
    private int mState; // 刷新状态
    private static  int NORMAL=0; // 正常状态
    private static  int REFRESH=1; // 刷新中
    private static  int LOAD=2; //上拉加载中
    private boolean isPull; //是否拖动中
    private int mCanScrollDistance=-1; //可拖动最远距离
    private boolean mCanLoadMore;//可否上拉加载
    //private int mCanLoadMore;//可否上拉加载


    public RefreshLayout(Context context) {
        this(context,null);
    }

    public RefreshLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mViewMarginTop= new HashMap<>();
        mViewMarginBottom= new HashMap<>();
        mScroller = new Scroller(context);
        View headView = LayoutInflater.from(context).inflate(R.layout.item_refresh_head, this, false);
        addView(headView,0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        for (int i = 0; i < getChildCount(); i++) {
            //测量每一个子
            measureChild(getChildAt(i),widthMeasureSpec,heightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int layoutWidth = getMeasuredWidth();
        mTotalHeight=t;//初始化高度
        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            if (view.getVisibility() != GONE) {
                LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
                int viewHeight = view.getMeasuredHeight();
                int viewWidth = view.getMeasuredWidth();
                if (i != 0) {
                    mTotalHeight += viewHeight;
                    if (mViewMarginTop.get(i) != null) {//总高度加上子view距离上方的距离
                        mTotalHeight += mViewMarginTop.get(i);
                    }
                }
                if(layoutParams.mCenter) {
                    int marginHorizontal = (layoutWidth - viewWidth-getPaddingLeft()-getPaddingRight()) / 2;
                    if(mTotalHeight>getMeasuredHeight()) {//如果子view总高度大于父控件总高度则最大值为父控件高度
                        int viewMarginBottom=0;
                        if (mViewMarginBottom.get(i) != null) {
                            viewMarginBottom = mViewMarginBottom.get(i);
                        }
                        if((mTotalHeight-viewHeight)>=getMeasuredHeight()) {//判断如果子view在父控件高度之外 绘制的位置
                            view.layout(l + marginHorizontal + getPaddingLeft(), mTotalHeight - viewHeight, viewWidth + marginHorizontal + l + getPaddingLeft(), mTotalHeight);
                        }else {
                            view.layout(l + marginHorizontal + getPaddingLeft(), mTotalHeight - viewHeight, viewWidth + marginHorizontal + l + getPaddingLeft(), getMeasuredHeight() - viewMarginBottom);
                        }
                    }else{
                        view.layout(l + marginHorizontal + getPaddingLeft(), mTotalHeight - viewHeight, viewWidth + marginHorizontal + l + getPaddingLeft(), mTotalHeight);
                    }
                }else{
                    if(mTotalHeight>getMeasuredHeight()) {//如果子view总高度大于父控件总高度则最大值为父控件高度
                        int viewMarginBottom=0;
                        if (mViewMarginBottom.get(i) != null) {
                             viewMarginBottom = mViewMarginBottom.get(i);
                        }
                        if((mTotalHeight-viewHeight)>=getMeasuredHeight()) {//判断如果子view在父控件高度之外 绘制的位置
                            view.layout(l + paddingLeft, mTotalHeight - viewHeight, viewWidth + l + paddingLeft + paddingRight, mTotalHeight);
                        }else{
                            view.layout(l + paddingLeft, mTotalHeight - viewHeight, viewWidth + l + paddingLeft + paddingRight, getMeasuredHeight() - viewMarginBottom);
                            mTotalHeight=getMeasuredHeight() - viewMarginBottom;
                        }
                    }else{
                        view.layout(l + paddingLeft, mTotalHeight - viewHeight, viewWidth + l + paddingLeft + paddingRight, mTotalHeight);
                    }
                }
                if (mViewMarginBottom.get(i) != null) {//总高度加上子view距离下方的距离
                    mTotalHeight += mViewMarginBottom.get(i);
                }
            }
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (mScroller != null && !mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                mStartY = event.getRawY();
                mStartX = event.getRawY();
                super.dispatchTouchEvent(event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if(Math.abs(mMoveHeight)>mCanScrollDistance && mCanScrollDistance>0){//判断是否到达设置的极限滚动距离
                    return true;
                }
                int round = Math.round(mStartY - event.getRawY());
                int roundX = Math.round(mStartX - event.getRawX());
                if(mListener!=null){
                    mListener.moveDy(round);
                }
                if(Math.abs(roundX)>Math.abs(round)){//保证横向能够滚动
                    mStartY -= round;
                    mStartX -= roundX;
                    return super.dispatchTouchEvent(event);
                }
                if(mRecyclerView!=null) {
                    if (!mRecyclerView.canScrollVertically(-1)) {//竖直方向recyclerView可否下拉
                        if (round < 0) {
                            isPull = true;
                            dealMove(round,event);
                            return true;
                        }
                    }
                    if (!mRecyclerView.canScrollVertically(1)) {//竖直方向recyclerView可否上拉
                        if (round > 0) {//上拉
                            isPull = true;
                            dealMove(round,event);
                            return true;
                        }
                    }
                    if (mState == REFRESH || isPull) {//刷新状态，拖动状态下拦截，自己处理移动
                        dealMove(round,event);
                        return true;
                    }
                    if(mMoveHeight>0){//如果底部view还在显示则下拉是从底部view开始
                        dealMove(round,event);
                        return true;
                    }
                    mStartY -= round;
                    mStartX -= roundX;
                }else{
                    dealMove(round,event);
                    return true;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                isPull=false;
                int height = getChildAt(0).getHeight();
                if(mMoveHeight<0 || mState == REFRESH) {
                    if (-mMoveHeight > height && mState == NORMAL) { // 正常状态下下拉超过第一个view高度，改为刷新状态
                        mState = REFRESH;
                        if (mListener != null) {
                            mListener.refresh();
                        }
                        smoothScrollBy( -mMoveHeight - height);
                    } else if (mMoveHeight > height / 2 && mState == REFRESH) {//刷新状态下移动到第一个view显示小于一半会滚到到正常显示状态
                        mState = NORMAL;
                        smoothScrollBy( -mMoveHeight + height);
                    } else {
                        smoothScrollBy( -mMoveHeight);
                    }
                    mMoveHeight = 0;
                }else{
                    if(mMoveHeight>mTotalHeight-getMeasuredHeight()) {//判断上拉加载
                        if(mCanLoadMore) {
                            mState = LOAD;
                            if(mListener!=null){
                                mListener.loadMore();
                            }
                        }
                        if(mTotalHeight>getMeasuredHeight()) {
                            //滚动到最后一个view显示的位置
                            smoothScrollBy(-mMoveHeight + mTotalHeight - getMeasuredHeight());
                            //计算滚回到最后一个view显示位置需要移动的距离
                            mMoveHeight = mTotalHeight - getMeasuredHeight();
                        }else{
                            smoothScrollBy( -mMoveHeight );
                            mMoveHeight = 0;
                        }
                    }
                }
                super.dispatchTouchEvent(event);
                return true;
        }
        return super.dispatchTouchEvent(event);
    }

    /**
     * 处理拖动 的高度计算和回调
     */
    private void dealMove(int round, MotionEvent event) {
        if(mCanScrollDistance!=-1 && +Math.abs(mMoveHeight)>mCanScrollDistance/2){
            round=round/2;
        }
        mMoveHeight += round;
        if(mListener!=null) {
            if (Math.abs(mMoveHeight) > getChildAt(0).getHeight()) {
                mListener.pullDown();
            }else{
                mListener.pullUp();
            }
        }
        smoothScrollBy( round);
        mStartY = event.getRawY();
        mStartX = event.getRawX();
    }

    /**
     * 滑动到指定位置
     * @param dy 竖直方向偏移量
     */
    private void smoothScrollBy(int dy) {
        mScroller.startScroll(mScroller.getFinalX(), mScroller.getFinalY(), 0, dy);
        postInvalidate();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return super.generateLayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(),attrs);
    }

    /**
     * 自定义LayoutParams 添加自定义属性
     * 并可以获取到子view的属性
     */
    private  class LayoutParams extends ViewGroup.LayoutParams {
        private boolean mCenter;
        private LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            for (int i = 0; i < attrs.getAttributeCount(); i++) {
                String attributeName = attrs.getAttributeName(i);
                String attributeValue = attrs.getAttributeValue(i);
                switch (attributeName){
                    case "layout_marginTop"://单位px 保存所有子view marginTop高度
                        if(attributeValue.length()>2) {
                            mViewMarginTop.put(getChildCount(),(int) Double.parseDouble(attributeValue.substring(0, attributeValue.length() - 2)));
                        }
                        break;
                    case "layout_marginBottom"://单位px 保存所有子view marginBottom高度
                        if(attributeValue.length()>2) {
                            mViewMarginBottom.put(getChildCount(),(int) Double.parseDouble(attributeValue.substring(0, attributeValue.length() - 2)));
                        }
                        break;
                    case "center_horizontal"://自定义属性水平居中
                        mCenter = Boolean.valueOf(attributeValue);
                        break;
                }
            }
        }
    }

    /**
     * 使用Scroller的时候需要重写该方法
     */
    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            postInvalidate();
        }
    }

    /**
     * 如果含有recyclerView需要绑定之后才能滚动
     */
    public void bindRecyclerView(RecyclerView recyclerView){
        mRecyclerView=recyclerView;
    }

    /**
     * 刷新完成调用
     */
    public void refreshComplete(){
        if(mState==REFRESH) {
            int height = getChildAt(0).getHeight();
            mState = NORMAL;
            smoothScrollBy( -mMoveHeight + height);
            mMoveHeight = 0;
        }
    }

    /**
     * 上拉加载完成调用
     * @param type 0为还有数据需要加载 1 为数据全部加载完成
     */
    public void loadComplete(int type){
        if(mState==LOAD) {
            mState = NORMAL;
            if(type==0) {
                smoothScrollBy(-mMoveHeight);
                mMoveHeight = 0;
            }else{
                //滚动到最后一个view显示的位置
                smoothScrollBy( -mMoveHeight + mTotalHeight - getMeasuredHeight());
                //计算滚回到最后一个view显示位置需要移动的距离
                mMoveHeight =mTotalHeight - getMeasuredHeight();
            }
        }
    }

    /**
     * 最大可弹性滚动的距离
     * @param scrollDistance 距离单位px
     */
    public void canScrollDistance(int scrollDistance){
        mCanScrollDistance=scrollDistance;
    }

    /**
     * 绑定刷新状态监听
     */
    public void setRefreshListener(RefreshListener listener,boolean canLoadMore){
        mListener=listener;
        mCanLoadMore=canLoadMore;
    }
}
