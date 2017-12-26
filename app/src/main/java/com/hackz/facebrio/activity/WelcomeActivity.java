package com.hackz.facebrio.activity;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;

import com.hackz.facebrio.R;
import com.hackz.facebrio.intro_util.PreviewIndicator;
import com.hackz.facebrio.intro_util.PreviewVideoView;

public class WelcomeActivity extends Activity {

    private PreviewVideoView mVideoView;
    private ViewPager mVpImage;
    private PreviewIndicator mIndicator;

    private List<View> mViewList = new ArrayList<>();
    private int[] mImageResIds = new int[]{R.mipmap.intro_text_1, R.mipmap.intro_text_2, R.mipmap.intro_text_3};
    private CustomPagerAdapter mAdapter;

    private int mCurrentPage = 0;
    private Subscription mLoop;

    @Override
    protected void onCreate (Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        mVideoView = findViewById(R.id.vv_preview);
        mVpImage =  findViewById(R.id.vp_image);
        mIndicator =  findViewById(R.id.indicator);

        mVideoView.setVideoURI(Uri.parse(getVideoPath()));

        for (int i = 0; i < mImageResIds.length; i++) {
            View view = LayoutInflater.from(this).inflate(R.layout.preview_item, null, false);
            ((ImageView) view.findViewById(R.id.iv_intro_text)).setImageResource(mImageResIds[i]);
            mViewList.add(view);

        }

        mAdapter = new CustomPagerAdapter(mViewList);
        mVpImage.setAdapter(mAdapter);
        mVpImage.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                mCurrentPage = position;
                mIndicator.setSelected(mCurrentPage);
                startLoop();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        startLoop();

        TextView skip_text= findViewById(R.id.skip_text);
        skip_text.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent= new Intent(WelcomeActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });

        Handler mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {

            @Override
            public void run() {
                //start your activity here
                Intent intent= new Intent(WelcomeActivity.this,MainActivity.class);
                startActivity(intent);
            }

        }, 9000L);

    }

    /**
     * 获取video文件的路径
     *
     * @return 路径
     */

    private String getVideoPath() {
        return "android.resource://" + this.getPackageName() + "/" + R.raw.intro_video;
    }

    /**
     * 开启轮询
     */
    private void startLoop() {
        long timer= 0;
        if (null != mLoop) {
            mLoop.unsubscribe();
        }
        mLoop = Observable.interval( 6 * 1000, TimeUnit.MILLISECONDS).startWith(timer)
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        mVideoView.seekTo(mCurrentPage * 6 * 1000);
                        if (!mVideoView.isPlaying()) {
                            mVideoView.start();

                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (null != mLoop) {
            mLoop.unsubscribe();
        }
        super.onDestroy();
    }

    public static class CustomPagerAdapter extends PagerAdapter {

        private List<View> mViewList;

        public CustomPagerAdapter(List<View> viewList) {
            mViewList = viewList;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(mViewList.get(position));
            return mViewList.get(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(mViewList.get(position));
        }

        @Override
        public int getCount() {
            return mViewList.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }



}

