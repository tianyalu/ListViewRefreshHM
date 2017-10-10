package com.sty.listview.refresh.view;

import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.sty.listview.refresh.R;

import java.text.SimpleDateFormat;

/**
 * 包含下拉刷新功能的ListView
 * Created by Shi Tianyi on 2017/10/8/0008.
 */

public class RefreshListView extends ListView implements AbsListView.OnScrollListener{
    private static final String TAG = RefreshListView.class.getSimpleName();

    private View mHeaderView;
    private View mFooterView;
    private int mHeaderViewHeight;
    private int mFooterViewHeight;
    private float downY;
    private float moveY;
    public static final int PULL_TO_REFRESH = 0; //下拉刷新
    public static final int RELEASE_REFRESH = 1; //释放刷新
    public static final int REFRESHING = 2; //刷新中
    private int currentState = PULL_TO_REFRESH; //当前的刷新模式

    private int paddingTop;

    private RotateAnimation rotateUpAnim;
    private RotateAnimation rotateDownAnim;
    private ImageView mArrowView;
    private TextView mTitleText;
    private TextView mLastRefreshTime;
    private ProgressBar pb;

    private OnRefreshListener listener;
    private boolean isLoadingMore; //是否正在加载更多

    public RefreshListView(Context context) {
        super(context);
        init();
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 初始化头布局/脚布局
     * 滚动监听
     */
    private void init(){
        initHeaderView();

        initAnimation();

        initFooterView();

        setOnScrollListener(this);
    }

    /**
     * 初始化头布局
     */
    private void initHeaderView(){
        mHeaderView = View.inflate(getContext(), R.layout.layout_header_list, null);
        mArrowView = mHeaderView.findViewById(R.id.iv_arrow);
        pb = (ProgressBar) mHeaderView.findViewById(R.id.pb);
        mTitleText = (TextView) mHeaderView.findViewById(R.id.tv_title);
        mLastRefreshTime = (TextView) mHeaderView.findViewById(R.id.tv_desc_last_refresh);

        //提取收到测量宽高
        mHeaderView.measure(0, 0); //按照设置的规则测量

        int height = mHeaderView.getHeight();
        mHeaderViewHeight = mHeaderView.getMeasuredHeight(); //获取到测量后的高度
        Log.i(TAG, "height: " + height + "  measureHeight: " + mHeaderViewHeight);

        //设置内边距，可以隐藏当前控件, -自身高度
        mHeaderView.setPadding(0, -mHeaderViewHeight, 0, 0);

        //在设置数据适配器之前执行添加头布局/脚布局的方法
        addHeaderView(mHeaderView);
    }

    /**
     * 初始化头布局动画
     */
    private void initAnimation(){
        //向上转，围绕着自己的中心，逆时针旋转0° -> -180°
        rotateUpAnim = new RotateAnimation(0f, -180f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateUpAnim.setDuration(300);
        rotateUpAnim.setFillAfter(true);  //动画停留在结束位置

        //向下转，围绕着自己的中心，逆时针旋转-180° -> -360°
        rotateDownAnim = new RotateAnimation(-180f, -360f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        rotateDownAnim.setDuration(300);
        rotateDownAnim.setFillAfter(true);  //动画停留在结束位置
    }

    private void initFooterView(){
        mFooterView = View.inflate(getContext(), R.layout.layout_footer_list, null);

        mFooterView.measure(0, 0);
        mFooterViewHeight = mFooterView.getMeasuredHeight();

        //隐藏脚布局
        mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);

        addFooterView(mFooterView);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        //判断滑动距离，给Header设置paddingTop
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = ev.getY();
                //Log.i(TAG, "moveY: " + moveY);
                // 如果是正在刷新中，则执行父类的处理
                if(currentState == REFRESHING){
                    return super.onTouchEvent(ev);
                }

                float offset = moveY - downY; //移动的偏移量
                // 只有偏移量>0,并且当前第一个可见条目索引是0,才放大头部
                if(offset > 0 && getFirstVisiblePosition() == 0) {
                    paddingTop = (int) (-mHeaderViewHeight + offset); //paddingTop = -自身高度 + 偏移量
                    mHeaderView.setPadding(0, paddingTop, 0, 0);

                    if(paddingTop >= 0 && currentState != RELEASE_REFRESH) { //头布局完全显示
                        //变成释放刷新模式
                        currentState = RELEASE_REFRESH;
                        Log.i(TAG, "切换成释放刷新模式： " + paddingTop);
                        updateHeader(); //根据最新的状态值更新头布局内容
                    }else if(paddingTop < 0 && currentState != PULL_TO_REFRESH){ //头布局不完全显示
                        //切换成下拉刷新模式
                        currentState = PULL_TO_REFRESH;
                        Log.i(TAG, "切换成下拉刷新模式： " + paddingTop);
                        updateHeader(); //根据最新的状态值更新头布局内容
                    }
                    return true; //当前事件被我们处理并消费
                }

                break;
            case MotionEvent.ACTION_UP:
                if(currentState == PULL_TO_REFRESH){ //不完全显示，恢复
                    mHeaderView.setPadding(0, -mHeaderViewHeight, 0, 0);
                }else{ //完全显示，执行正在刷新...
                    mHeaderView.setPadding(0, 0, 0, 0);
                    currentState = REFRESHING;
                    updateHeader();
                }
                break;
            default:
                break;
        }
        
        return super.onTouchEvent(ev);
    }

    /**
     * 根据状态更新头布局内容
     */
    private void updateHeader(){
        switch (currentState){
            case PULL_TO_REFRESH:  //切换回下拉刷新
                mArrowView.startAnimation(rotateDownAnim);
                mTitleText.setText("下拉刷新");
                break;
            case RELEASE_REFRESH:  //切换成释放刷新
                mArrowView.startAnimation(rotateUpAnim);
                mTitleText.setText("释放刷新");
                break;
            case REFRESHING:  //刷新中...
                mArrowView.clearAnimation();
                mArrowView.setVisibility(View.INVISIBLE);
                pb.setVisibility(View.VISIBLE);
                mTitleText.setText("正在刷新中...");

                if(listener != null){
                    listener.onRefresh();  //通知调用者，让其到网络加载数据
                }
                break;
            default:
                break;
        }
    }

    /**
     * 刷新结束，恢复界面效果
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onRefreshComplete(){
        if(isLoadingMore){ //加载更多
            mFooterView.setPadding(0, -mFooterViewHeight, 0, 0);
            isLoadingMore = false;
        }else { //下拉刷新
            currentState = PULL_TO_REFRESH;
            mTitleText.setText("下拉刷新"); // 切换文本
            mHeaderView.setPadding(0, -mHeaderViewHeight, 0, 0); // 隐藏头布局
            pb.setVisibility(View.INVISIBLE);
            mArrowView.setVisibility(View.VISIBLE);

            String time = getTime();
            mLastRefreshTime.setText("最后刷新时间：" + time);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private String getTime(){
        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(currentTimeMillis);
    }

    /**
     * 状态更新的时候
     * SCROLL_STATE_IDLE: 0  空闲状态
     * SCROLL_STATE_TOUCH_SCROLL: 1 触摸状态
     * SCROLL_STATE_FLING: 2 滑翔状态
     * @param absListView
     * @param scrollState
     */
    @Override
    public void onScrollStateChanged(AbsListView absListView, int scrollState) {
        Log.i(TAG, "scrollState:---->" + scrollState);
        if(isLoadingMore){
            return; //已经在加载更多了，返回
        }

        //最新状态是空闲状态，并且当界面显示了所有数据的最后一条，加载更多
        if(scrollState == SCROLL_STATE_IDLE && getLastVisiblePosition() >= (getCount() - 1)){
            Log.i(TAG, "scrollState: 开始加载更多" );
            isLoadingMore = true;
            mFooterView.setPadding(0, 0, 0, 0);

            setSelection(getCount()); //停留在最后

            if(listener != null){
                listener.onLoadMore();
            }
        }
    }

    /**
     * 滑动过程中
     * @param absListView
     * @param firstVisibleItem
     * @param visibleItemCount
     * @param totalItemCount
     */
    @Override
    public void onScroll(AbsListView absListView, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

    }

    public interface OnRefreshListener{
        void onRefresh(); //下拉刷新

        void onLoadMore(); //加载更多
    }

    public void setRefreshListener(OnRefreshListener listener){
        this.listener = listener;
    }
}
