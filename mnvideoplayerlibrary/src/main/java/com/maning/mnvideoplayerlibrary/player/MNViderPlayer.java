package com.maning.mnvideoplayerlibrary.player;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.maning.mnvideoplayerlibrary.R;
import com.maning.mnvideoplayerlibrary.listener.OnCompletionListener;
import com.maning.mnvideoplayerlibrary.listener.OnNetChangeListener;
import com.maning.mnvideoplayerlibrary.listener.OnScreenOrientationListener;
import com.maning.mnvideoplayerlibrary.permissions.OnPermission;
import com.maning.mnvideoplayerlibrary.permissions.Permission;
import com.maning.mnvideoplayerlibrary.permissions.XXPermissions;
import com.maning.mnvideoplayerlibrary.utils.LightnessControl;
import com.maning.mnvideoplayerlibrary.utils.PlayerUtils;
import com.maning.mnvideoplayerlibrary.view.ProgressWheel;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by maning on 16/6/14.
 * ?????????
 */
public class MNViderPlayer extends FrameLayout implements View.OnClickListener, SeekBar.OnSeekBarChangeListener,
        SurfaceHolder.Callback, GestureDetector.OnGestureListener, MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnBufferingUpdateListener {

    private static final String TAG = "MNViderPlayer";
    private Context context;
    private Activity activity;

    static final Handler myHandler = new Handler(Looper.getMainLooper());

    // SurfaceView?????????????????????????????????
    private SurfaceHolder surfaceHolder;
    private MediaPlayer mediaPlayer;

    //???????????????????????????
    private boolean isPlaying = true;

    private boolean isFirstPlay = false;

    //??????
    private String videoPath;
    private String videoTitle;
    private int video_position = 0;

    //?????????????????????
    private float mediaPlayerX;
    private float mediaPlayerY;
    private int playerViewW;
    private int playerViewH;

    // ?????????
    private Timer timer_video_time;
    private TimerTask task_video_timer;
    private Timer timer_controller;
    private TimerTask task_controller;

    //???????????????
    private boolean isFullscreen = false;
    private boolean isLockScreen = false;
    private boolean isPrepare = false;
    private boolean isNeedBatteryListen = true;
    private boolean isNeedNetChangeListen = true;

    //??????
    private LinearLayout mn_player_surface_bg;
    private RelativeLayout mn_rl_bottom_menu;
    private SurfaceView mn_palyer_surfaceView;
    private ImageView mn_iv_play_pause;
    private ImageView mn_iv_fullScreen;
    private TextView mn_tv_time;
    private SeekBar mn_seekBar;
    private ImageView mn_iv_back;
    private TextView mn_tv_title;
    private TextView mn_tv_system_time;
    private RelativeLayout mn_rl_top_menu;
    private RelativeLayout mn_player_rl_progress;
    private ImageView mn_player_iv_lock;
    private LinearLayout mn_player_ll_error;
    private TextView tv_error_content;
    private LinearLayout mn_player_ll_net;
    private ProgressWheel mn_player_progressBar;
    private ImageView mn_iv_battery;
    private ImageView mn_player_iv_play_center;
    private ImageView iv_video_thumbnail;

    public MNViderPlayer(Context context) {
        this(context, null);
    }

    public MNViderPlayer(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MNViderPlayer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        activity = (Activity) this.context;
        PlayerUtils.hideBottomUIMenu(activity);
        //?????????????????????
        initAttrs(context, attrs);
        //??????
        init();
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        //?????????????????????
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MNViderPlayer);
        //???????????????????????????
        for (int i = 0; i < typedArray.getIndexCount(); i++) {
            int index = typedArray.getIndex(i);
            if (index == R.styleable.MNViderPlayer_mnFirstNeedPlay) {
                isFirstPlay = typedArray.getBoolean(R.styleable.MNViderPlayer_mnFirstNeedPlay, false);
            }
        }
        //??????
        typedArray.recycle();
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

    }

    // ??????????????????
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        int screenWidth = PlayerUtils.getScreenWidth(activity);
        int screenHeight = PlayerUtils.getScreenHeight(activity);
        Log.d(TAG, "screenWidth:" + screenWidth);
        ViewGroup.LayoutParams layoutParams = getLayoutParams();

        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {

            layoutParams.width = screenWidth;
            layoutParams.height = screenHeight;

            //????????????
            if (onScreenOrientationListener != null) {
                onScreenOrientationListener.orientation_portrait();
            }

        } else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            layoutParams.width = screenWidth - PlayerUtils.getStatusBarHeight(activity);
            layoutParams.height = PlayerUtils.getScreenHeight(activity);

            //????????????
            if (onScreenOrientationListener != null) {
                onScreenOrientationListener.orientation_landscape();
            }
        }

        setLayoutParams(layoutParams);

        playerViewW = screenWidth;
        playerViewH = layoutParams.height;

        //????????????
        fitVideoSize();
    }


    //?????????
    private void init() {
        View inflate = View.inflate(context, R.layout.mn_player_view, this);
        mn_rl_bottom_menu = inflate.findViewById(R.id.mn_rl_bottom_menu);
        mn_palyer_surfaceView = inflate.findViewById(R.id.mn_palyer_surfaceView);
        mn_iv_play_pause = inflate.findViewById(R.id.mn_iv_play_pause);
        mn_iv_fullScreen = inflate.findViewById(R.id.mn_iv_fullScreen);
        mn_tv_time = inflate.findViewById(R.id.mn_tv_time);
        mn_tv_system_time = inflate.findViewById(R.id.mn_tv_system_time);
        mn_seekBar = inflate.findViewById(R.id.mn_seekBar);
        mn_iv_back = inflate.findViewById(R.id.mn_iv_back);
        mn_tv_title = inflate.findViewById(R.id.mn_tv_title);
        mn_rl_top_menu = inflate.findViewById(R.id.mn_rl_top_menu);
        mn_player_rl_progress = inflate.findViewById(R.id.mn_player_rl_progress);
        mn_player_iv_lock = inflate.findViewById(R.id.mn_player_iv_lock);
        mn_player_ll_error = inflate.findViewById(R.id.mn_player_ll_error);
        tv_error_content = inflate.findViewById(R.id.tv_error_content);
        mn_player_ll_net = inflate.findViewById(R.id.mn_player_ll_net);
        mn_player_progressBar = inflate.findViewById(R.id.mn_player_progressBar);
        mn_iv_battery = inflate.findViewById(R.id.mn_iv_battery);
        mn_player_iv_play_center = inflate.findViewById(R.id.mn_player_iv_play_center);
        mn_player_surface_bg = inflate.findViewById(R.id.mn_player_surface_bg);
        iv_video_thumbnail = inflate.findViewById(R.id.iv_video_thumbnail);

        mn_seekBar.setOnSeekBarChangeListener(this);
        mn_iv_play_pause.setOnClickListener(this);
        mn_iv_fullScreen.setOnClickListener(this);
        mn_iv_back.setOnClickListener(this);
        mn_player_iv_lock.setOnClickListener(this);
        mn_player_ll_error.setOnClickListener(this);
        mn_player_ll_net.setOnClickListener(this);
        mn_player_iv_play_center.setOnClickListener(this);

        //?????????
        initViews();

        if (!isFirstPlay) {
            mn_player_iv_play_center.setVisibility(View.VISIBLE);
            mn_player_progressBar.setVisibility(View.GONE);
        }

        //?????????SurfaceView
        initSurfaceView();

        //???????????????
        initGesture();

        //???????????????????????????
        getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (playerViewW == 0) {
                mediaPlayerX = getX();
                mediaPlayerY = getY();
                playerViewW = getWidth();
                playerViewH = getHeight();
            }
        });
    }

    private void initViews() {

        mn_palyer_surfaceView.setAlpha(0);

        mn_tv_system_time.setText(PlayerUtils.getCurrentHHmmTime());
        mn_rl_bottom_menu.setVisibility(View.GONE);
        mn_rl_top_menu.setVisibility(View.GONE);
        mn_player_iv_lock.setVisibility(View.GONE);
        initLock();
        mn_player_rl_progress.setVisibility(View.VISIBLE);
        mn_player_progressBar.setVisibility(View.VISIBLE);
        mn_player_ll_error.setVisibility(View.GONE);
        mn_player_ll_net.setVisibility(View.GONE);
        mn_player_iv_play_center.setVisibility(View.GONE);
        initTopMenu();

    }

    private void setVideoThumbnail() {
        if (PlayerUtils.isNetworkConnected(context)) {
            new Thread(() -> {
                final Bitmap videoThumbnail = PlayerUtils.createVideoThumbnail(videoPath, getWidth(), getHeight());
                myHandler.post(() -> {
                    if (videoThumbnail != null) {
                        iv_video_thumbnail.setVisibility(View.VISIBLE);
                        iv_video_thumbnail.setImageBitmap(videoThumbnail);
                    } else {
                        iv_video_thumbnail.setVisibility(View.GONE);
                    }
                });
            }).start();
        }
    }

    private void initLock() {
      /*  if (isFullscreen) {
            mn_player_iv_lock.setVisibility(View.VISIBLE);
        } else {
            mn_player_iv_lock.setVisibility(View.GONE);
        }*/
    }

    private void initSurfaceView() {
        // ??????SurfaceView?????????????????????????????????????????????????????????
        surfaceHolder = mn_palyer_surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        // SurfaceView?????????????????????
        surfaceHolder.addCallback(this);
    }

    private void initTopMenu() {
        mn_tv_title.setText(videoTitle);
        if (isFullscreen) {
            mn_rl_top_menu.setVisibility(View.VISIBLE);
        } else {
            mn_rl_top_menu.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.mn_iv_play_pause) {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    pauseVideo();
                } else {
                    startVideo();
                }
            }
        } else if (i == R.id.mn_iv_fullScreen) {
            if (isFullscreen) {
                setProtrait();
            } else {
                setLandscape();
            }
        } else if (i == R.id.mn_iv_back) {
            setProtrait();
        } else if (i == R.id.mn_player_iv_lock) {
            if (isLockScreen) {
                unLockScreen();
                initBottomMenuState();
            } else {
                lockScreen();
                destroyControllerTask(true);
            }
        } else if (i == R.id.mn_player_ll_error || i == R.id.mn_player_ll_net || i == R.id.mn_player_iv_play_center) {
            if (!videoPath.startsWith("http") && !hasWritePermission()) {
                XXPermissions.with(activity)
                        .permission(Permission.Group.STORAGE)
                        .request(new OnPermission() {

                            @Override
                            public void hasPermission(List<String> granted, boolean isAll) {
                                playVideo(videoPath, videoTitle, video_position);
                            }

                            @Override
                            public void noPermission(List<String> denied, boolean quick) {
                                Toast.makeText(activity, "??????????????????", Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                playVideo(videoPath, videoTitle, video_position);
            }
        }
    }

    //--------------------------------------------------------------------------------------
    // ######## ??????View????????? ########
    //--------------------------------------------------------------------------------------

    private void unLockScreen() {
        isLockScreen = false;
        mn_player_iv_lock.setImageResource(R.drawable.mn_player_landscape_screen_lock_open);
    }

    private void lockScreen() {
        isLockScreen = true;
        mn_player_iv_lock.setImageResource(R.drawable.mn_player_landscape_screen_lock_close);
    }

    //??????????????????????????????
    private void initBottomMenuState() {
        mn_tv_system_time.setText(PlayerUtils.getCurrentHHmmTime());
        if (mn_rl_bottom_menu.getVisibility() == View.GONE) {
            initControllerTask();
            mn_rl_bottom_menu.setVisibility(View.VISIBLE);
            mn_rl_top_menu.setVisibility(View.VISIBLE);
            //if (isFullscreen) {
            //   mn_rl_top_menu.setVisibility(View.VISIBLE);
//            mn_player_iv_lock.setVisibility(View.VISIBLE);
            //  }
        } else {
            destroyControllerTask(true);
        }
    }

    private void dismissControllerMenu() {
//        if (!isLockScreen) {
//            mn_player_iv_lock.setVisibility(View.GONE);
//        }
        mn_rl_top_menu.setVisibility(View.GONE);
        mn_rl_bottom_menu.setVisibility(View.GONE);
    }

    private void showErrorView() {
        showErrorView("");
    }

    private void showErrorView(String errorMsg) {
        mn_player_iv_play_center.setVisibility(View.GONE);
        mn_player_ll_net.setVisibility(View.GONE);
        mn_player_progressBar.setVisibility(View.GONE);
        iv_video_thumbnail.setVisibility(View.GONE);
        mn_player_ll_error.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(errorMsg)) {
            errorMsg = "????????????\n??????????????????";
        }
        tv_error_content.setText(errorMsg);
    }

    private void showNoNetView() {
        mn_player_iv_play_center.setVisibility(View.GONE);
        mn_player_progressBar.setVisibility(View.GONE);
        mn_player_ll_error.setVisibility(View.GONE);
        iv_video_thumbnail.setVisibility(View.GONE);
        mn_player_ll_net.setVisibility(View.VISIBLE);
    }

    private void setLandscape() {
        isFullscreen = true;
        //????????????
        ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        if (mn_rl_bottom_menu.getVisibility() == View.VISIBLE) {
            mn_rl_top_menu.setVisibility(View.VISIBLE);
        }
        initLock();
    }

    private void setProtrait() {
        if (isFullscreen) {
            isFullscreen = false;
            //??????
            ((Activity) context).setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            unLockScreen();
            initLock();
        } else {
            ((Activity) context).finish();
        }

    }

    //--------------------------------------------------------------------------------------
    // ######## ????????????????????? ########
    //--------------------------------------------------------------------------------------

    private void initTimeTask() {
        timer_video_time = new Timer();
        task_video_timer = new TimerTask() {
            @Override
            public void run() {
                myHandler.post(() -> {
                    if (mediaPlayer == null) {
                        return;
                    }
                    //????????????
                    mn_tv_time.setText(String.valueOf(PlayerUtils.converLongTimeToStr(mediaPlayer.getCurrentPosition()) + " / " + PlayerUtils.converLongTimeToStr(mediaPlayer.getDuration())));
                    //?????????
                    int progress = mediaPlayer.getCurrentPosition();
                    mn_seekBar.setProgress(progress);
                });
            }
        };
        timer_video_time.schedule(task_video_timer, 0, 1000);
    }

    private void destroyTimeTask() {
        if (timer_video_time != null && task_video_timer != null) {
            timer_video_time.cancel();
            task_video_timer.cancel();
            timer_video_time = null;
            task_video_timer = null;
        }
    }

    private void initControllerTask() {
        // ???????????????,???????????????????????????
        timer_controller = new Timer();
        task_controller = new TimerTask() {
            @Override
            public void run() {
                destroyControllerTask(false);
            }
        };
        timer_controller.schedule(task_controller, 5000);
        initTimeTask();
    }

    private void destroyControllerTask(boolean isMainThread) {
        if (isMainThread) {
            dismissControllerMenu();
        } else {
            myHandler.post(this::dismissControllerMenu);
        }
        if (timer_controller != null && task_controller != null) {
            timer_controller.cancel();
            task_controller.cancel();
            timer_controller = null;
            task_controller = null;
        }
        destroyTimeTask();
    }

    //--------------------------------------------------------------------------------------
    // ######## ?????????????????? ########
    //--------------------------------------------------------------------------------------
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            int maxCanSeekTo = seekBar.getMax() - 5 * 1000;
            if (seekBar.getProgress() < maxCanSeekTo) {
                mediaPlayer.seekTo(seekBar.getProgress());
            } else {
                //??????????????????
                mediaPlayer.seekTo(maxCanSeekTo);
            }
        }
    }

    //??????
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setDisplay(holder); // ??????????????????
        //?????????????????????
        mediaPlayer.setOnCompletionListener(this);
        // ????????????????????????????????????????????????????????????????????????
        mediaPlayer.setOnPreparedListener(this);
        //?????????????????????
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        //??????????????????????????????????????????
        if (isFirstPlay) {
            playVideo(videoPath, videoTitle);
        }
        isFirstPlay = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //??????????????????
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mn_iv_play_pause.setImageResource(R.drawable.mn_player_play);
            video_position = mediaPlayer.getCurrentPosition();
        }
        destroyControllerTask(true);
    }

    //MediaPlayer
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        mn_iv_play_pause.setImageResource(R.drawable.mn_player_play);
        isPlaying = false;
        destroyControllerTask(true);
        video_position = 0;
        if (onCompletionListener != null) {
            onCompletionListener.onCompletion(mediaPlayer);
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
//        Log.i(TAG, "????????????onBufferingUpdate: " + percent);
        if (percent >= 0 && percent <= 100) {
            int secondProgress = mp.getDuration() * percent / 100;
            mn_seekBar.setSecondaryProgress(secondProgress);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
//        Log.i(TAG, "????????????error:" + what);
        if (what != -38) {  //??????????????????
            showErrorView();
        }
        return true;
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onPrepared(final MediaPlayer mediaPlayer) {
        mediaPlayer.start(); // ????????????
        //??????????????????
        if (!isPlaying) {
            mediaPlayer.pause();
            mn_iv_play_pause.setImageResource(R.drawable.mn_player_play);
        } else {
            mn_iv_play_pause.setImageResource(R.drawable.mn_player_pause);
        }
        isPrepare = true;
        // ??????????????????????????????????????????
        mn_seekBar.setMax(mediaPlayer.getDuration());
        mn_tv_time.setText(PlayerUtils.converLongTimeToStr(mediaPlayer.getCurrentPosition())
                + "/" + PlayerUtils.converLongTimeToStr(mediaPlayer.getDuration()));
        //???????????????????????????????????????,?????????
        mn_player_rl_progress.setVisibility(View.GONE);
        initBottomMenuState();
        //??????????????????
        if (video_position > 0) {
            Log.i(TAG, "onPrepared---video_position:" + video_position);
            MNViderPlayer.this.mediaPlayer.seekTo(video_position);
            video_position = 0;
        }
        //????????????
        fitVideoSize();
        //????????????,???????????????
        mn_palyer_surfaceView.setAlpha(1);
        iv_video_thumbnail.setVisibility(View.GONE);

    }

    private void fitVideoSize() {
        if (mediaPlayer == null) {
            return;
        }
        if (playerViewW == 0) {
            playerViewW = getWidth();
            playerViewH = getHeight();
        }
        //?????????????????????
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        int parentWidth = playerViewW;
        int parentHeight = playerViewH;
        //???????????????????????????????????????
        int surfaceViewW;
        int surfaceViewH;
        if ((float) videoWidth / (float) videoHeight > (float) parentWidth / (float) parentHeight) {
            surfaceViewW = parentWidth;
            surfaceViewH = videoHeight * surfaceViewW / videoWidth;
        } else {
            surfaceViewH = parentHeight;
            surfaceViewW = videoWidth * parentHeight / videoHeight;
        }
        //??????surfaceView?????????
        ViewGroup.LayoutParams params = mn_player_surface_bg.getLayoutParams();
        params.height = surfaceViewH;
        params.width = surfaceViewW;
        mn_player_surface_bg.setLayoutParams(params);
    }

    //--------------------------------------------------------------------------------------
    // ######## ???????????? ########
    //--------------------------------------------------------------------------------------
    private RelativeLayout gesture_volume_layout;// ??????????????????
    private TextView geture_tv_volume_percentage;// ???????????????
    private ProgressBar geture_tv_volume_percentage_progress;// ???????????????
    private ImageView gesture_iv_player_volume;// ????????????
    private RelativeLayout gesture_light_layout;// ????????????
    private TextView geture_tv_light_percentage;// ???????????????
    private ProgressBar geture_tv_light_percentageProgress;// ???????????????
    private RelativeLayout gesture_progress_layout;// ????????????
    private TextView geture_tv_progress_time;// ??????????????????
    private ImageView gesture_iv_progress;// ?????????????????????
    private GestureDetector gestureDetector;
    private AudioManager audiomanager;
    private int maxVolume, currentVolume;
    private static final float STEP_PROGRESS = 2f;// ?????????????????????????????????????????????????????????????????????????????????
    private static final float STEP_VOLUME = 2f;// ?????????????????????????????????????????????????????????????????????????????????
    private static final float STEP_LIGHT = 2f;// ?????????????????????????????????????????????????????????????????????????????????
    private int GESTURE_FLAG = 0;// 1,???????????????2???????????????
    private static final int GESTURE_MODIFY_PROGRESS = 1;
    private static final int GESTURE_MODIFY_VOLUME = 2;
    private static final int GESTURE_MODIFY_BRIGHTNESS = 3;

    private void initGesture() {
        gesture_volume_layout = (RelativeLayout) findViewById(R.id.mn_gesture_volume_layout);
        geture_tv_volume_percentage = (TextView) findViewById(R.id.mn_gesture_tv_volume_percentage);
        geture_tv_volume_percentage_progress = (ProgressBar) findViewById(R.id.mn_gesture_tv_volume_percentage_progress);
        gesture_iv_player_volume = (ImageView) findViewById(R.id.mn_gesture_iv_player_volume);

        gesture_progress_layout = (RelativeLayout) findViewById(R.id.mn_gesture_progress_layout);
        geture_tv_progress_time = (TextView) findViewById(R.id.mn_gesture_tv_progress_time);
        gesture_iv_progress = (ImageView) findViewById(R.id.mn_gesture_iv_progress);

        //???????????????
        gesture_light_layout = (RelativeLayout) findViewById(R.id.mn_gesture_light_layout);
        geture_tv_light_percentage = (TextView) findViewById(R.id.mn_geture_tv_light_percentage);
        geture_tv_light_percentageProgress = (ProgressBar) findViewById(R.id.mn_geture_tv_light_percentage_progress);

        gesture_volume_layout.setVisibility(View.GONE);
        gesture_progress_layout.setVisibility(View.GONE);
        gesture_light_layout.setVisibility(View.GONE);

        gestureDetector = new GestureDetector(getContext(), this);
        setLongClickable(true);
        gestureDetector.setIsLongpressEnabled(true);
        audiomanager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        maxVolume = audiomanager.getStreamMaxVolume(AudioManager.STREAM_MUSIC); // ????????????????????????
        currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC); // ???????????????
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if (!isPrepare || isLockScreen) {
            return false;
        }
        initBottomMenuState();
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

        if (!isPrepare || isLockScreen) {
            return false;
        }

        int FLAG = 0;

        // ???????????????????????????????????????????????????????????????????????????
        if (Math.abs(distanceX) >= Math.abs(distanceY)) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                FLAG = GESTURE_MODIFY_PROGRESS;
            }
        } else {
            int intX = (int) e1.getX();
            int screenWidth = PlayerUtils.getScreenWidth((Activity) context);
            if (intX > screenWidth / 2) {
                FLAG = GESTURE_MODIFY_VOLUME;
            } else {
                //???????????????
                FLAG = GESTURE_MODIFY_BRIGHTNESS;
            }
        }

        if (GESTURE_FLAG != 0 && GESTURE_FLAG != FLAG) {
            return false;
        }

        GESTURE_FLAG = FLAG;

        if (FLAG == GESTURE_MODIFY_PROGRESS) {
            //?????????????????????,??????????????????
            // distanceX=lastScrollPositionX-currentScrollPositionX???????????????????????????
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_light_layout.setVisibility(View.GONE);
            gesture_progress_layout.setVisibility(View.VISIBLE);
            try {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (Math.abs(distanceX) > Math.abs(distanceY)) {// ??????????????????????????????
                        if (distanceX >= PlayerUtils.dip2px(context, STEP_PROGRESS)) {// ????????????????????????????????????????????????
                            gesture_iv_progress
                                    .setImageResource(R.drawable.mn_player_backward);
                            if (mediaPlayer.getCurrentPosition() > 3 * 1000) {// ????????????
                                int cpos = mediaPlayer.getCurrentPosition();
                                mediaPlayer.seekTo(cpos - 3000);
                                mn_seekBar.setProgress(mediaPlayer.getCurrentPosition());
                            } else {
                                //???????????????
                                mediaPlayer.seekTo(3000);
                            }
                        } else if (distanceX <= -PlayerUtils.dip2px(context, STEP_PROGRESS)) {// ??????
                            gesture_iv_progress
                                    .setImageResource(R.drawable.mn_player_forward);
                            if (mediaPlayer.getCurrentPosition() < mediaPlayer.getDuration() - 5 * 1000) {// ?????????????????????
                                int cpos = mediaPlayer.getCurrentPosition();
                                mediaPlayer.seekTo(cpos + 3000);
                                // ?????????????????????????????????
                                mn_seekBar.setProgress(mediaPlayer.getCurrentPosition());
                            }
                        }
                    }
                    String timeStr = PlayerUtils.converLongTimeToStr(mediaPlayer.getCurrentPosition()) + " / "
                            + PlayerUtils.converLongTimeToStr(mediaPlayer.getDuration());
                    geture_tv_progress_time.setText(timeStr);

                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        // ????????????????????????????????????scroll??????????????????????????????scroll?????????????????????????????????????????????????????????????????????
        else if (FLAG == GESTURE_MODIFY_VOLUME) {
            //???????????????
            gesture_volume_layout.setVisibility(View.VISIBLE);
            gesture_light_layout.setVisibility(View.GONE);
            gesture_progress_layout.setVisibility(View.GONE);
            currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC); // ???????????????
            if (Math.abs(distanceY) > Math.abs(distanceX)) {// ??????????????????????????????
                if (currentVolume == 0) {// ????????????????????????????????????
                    gesture_iv_player_volume.setImageResource(R.drawable.mn_player_volume_close);
                }
                if (distanceY >= PlayerUtils.dip2px(context, STEP_VOLUME)) {// ????????????,??????????????????????????????,???????????????????????????????????????????????????distanceY??????
                    if (currentVolume < maxVolume) {// ????????????????????????distanceY????????????????????????
                        currentVolume++;
                    }
                    gesture_iv_player_volume.setImageResource(R.drawable.mn_player_volume_open);
                } else if (distanceY <= -PlayerUtils.dip2px(context, STEP_VOLUME)) {// ????????????
                    if (currentVolume > 0) {
                        currentVolume--;
                        if (currentVolume == 0) {// ????????????????????????????????????
                            gesture_iv_player_volume.setImageResource(R.drawable.mn_player_volume_close);
                        }
                    }
                }
                int percentage = (currentVolume * 100) / maxVolume;
                geture_tv_volume_percentage.setText(String.valueOf(percentage + "%"));
                geture_tv_volume_percentage_progress.setProgress(percentage);
                audiomanager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            }
        }
        //????????????
        else if (FLAG == GESTURE_MODIFY_BRIGHTNESS) {
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_light_layout.setVisibility(View.VISIBLE);
            gesture_progress_layout.setVisibility(View.GONE);
            currentVolume = audiomanager.getStreamVolume(AudioManager.STREAM_MUSIC); // ???????????????
            if (Math.abs(distanceY) > Math.abs(distanceX)) {// ??????????????????????????????
                // ????????????,??????????????????????????????,???????????????????????????????????????????????????distanceY??????
                if (distanceY >= PlayerUtils.dip2px(context, STEP_LIGHT)) {
                    LightnessControl.SetLightness((Activity) context, 10);
                } else if (distanceY <= -PlayerUtils.dip2px(context, STEP_LIGHT)) {// ????????????
                    LightnessControl.SetLightness((Activity) context, -10);
                }
                //??????????????????
                int currentLight = LightnessControl.GetLightness((Activity) context);
                int percentage = (currentLight * 100) / 255;
                geture_tv_light_percentage.setText(percentage + "%");
                geture_tv_light_percentageProgress.setProgress(percentage);
            }
        }
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // ???????????????singleTapUp?????????????????????up?????????
        if (event.getAction() == MotionEvent.ACTION_UP) {
            GESTURE_FLAG = 0;// ????????????????????????????????????????????????????????????
            gesture_volume_layout.setVisibility(View.GONE);
            gesture_progress_layout.setVisibility(View.GONE);
            gesture_light_layout.setVisibility(View.GONE);
        }
        return gestureDetector.onTouchEvent(event);
    }

    //--------------------------------------------------------------------------------------
    // ######## ????????????????????? ########
    //--------------------------------------------------------------------------------------

    /**
     * ??????????????????
     *
     * @param url   ????????????
     * @param title ????????????
     */
    public void setDataSource(String url, String title) {
        //??????
        videoPath = url;
        videoTitle = title;
        setVideoThumbnail();
    }

    /**
     * ????????????
     *
     * @param url   ????????????
     * @param title ????????????
     */
    public void playVideo(String url, String title) {
        playVideo(url, title, video_position);
    }

    /**
     * ??????????????????????????????????????????
     * ???????????????????????????????????????????????????position??????????????????
     *
     * @param url      ????????????
     * @param title    ????????????
     * @param position ?????????????????????(??????)
     */
    public void playVideo(String url, String title, int position) {
        //??????????????????
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(context, context.getString(R.string.mnPlayerUrlEmptyHint), Toast.LENGTH_SHORT).show();
            return;
        }
        //??????ControllerView
        destroyControllerTask(true);

        //??????
        videoPath = url;
        videoTitle = title;
        video_position = position;
        isPrepare = false;
        isPlaying = true;

        //?????????View
        initViews();

        //?????????????????????????????????????????????????????????
        if (!PlayerUtils.isNetworkConnected(context) && url.startsWith("http")) {
            Toast.makeText(context, context.getString(R.string.mnPlayerNoNetHint), Toast.LENGTH_SHORT).show();
            showNoNetView();
            return;
        }
        //?????????????????????
        if (PlayerUtils.isMobileConnected(context) && url.startsWith("http")) {
            Toast.makeText(context, context.getString(R.string.mnPlayerMobileNetHint), Toast.LENGTH_SHORT).show();
        }

        //??????????????????
        if (!url.startsWith("http") && !hasWritePermission()) {
            showErrorView("??????????????????\n??????????????????");
            return;
        }

        //??????MediaPlayer
        resetMediaPlayer();

        //??????????????????
        if (isNeedBatteryListen) {
            registerBatteryReceiver();
        } else {
            unRegisterBatteryReceiver();
            mn_iv_battery.setVisibility(View.GONE);
        }
        //?????????????????????
        if (isNeedNetChangeListen) {
            registerNetReceiver();
        } else {
            unregisterNetReceiver();
        }

    }

    public boolean hasWritePermission() {
        int perm = activity.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE");
        return perm == PackageManager.PERMISSION_GRANTED;
    }

    private void resetMediaPlayer() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    mediaPlayer.stop();
                }
                //??????mediaPlayer
                mediaPlayer.reset();
                //??????????????????
                mediaPlayer.setDataSource(videoPath);
                // ????????????,????????????????????????????????????
                mediaPlayer.prepareAsync();
                //??????????????????
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mediaPlayer.setVideoScalingMode(MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT);
                }
            } else {
                //TODO:???????????????????????????????????????
                //Toast.makeText(context, "????????????????????????", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ????????????
     */
    public void startVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
            mn_iv_play_pause.setImageResource(R.drawable.mn_player_pause);
            isPlaying = true;
        }
    }

    /**
     * ????????????
     */
    public void pauseVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            mn_iv_play_pause.setImageResource(R.drawable.mn_player_play);
            video_position = mediaPlayer.getCurrentPosition();
            isPlaying = false;
        }
    }

    /**
     * ??????
     */
    public void setOrientationPortrait() {
        setProtrait();
    }

    /**
     * ??????
     */
    public void setOrientationLandscape() {
        setLandscape();
    }

    /**
     * ??????????????????????????????
     */
    public void setIsNeedBatteryListen(boolean isNeedBatteryListen) {
        this.isNeedBatteryListen = isNeedBatteryListen;
    }

    /**
     * ????????????????????????????????????
     */
    public void setIsNeedNetChangeListen(boolean isNeedNetChangeListen) {
        this.isNeedNetChangeListen = isNeedNetChangeListen;
    }

    /**
     * ???????????????????????????
     *
     * @return
     */
    public boolean isFullScreen() {
        return isFullscreen;
    }

    /**
     * ???????????????????????????
     */
    public int getVideoCurrentPosition() {
        int position = 0;
        if (mediaPlayer != null) {
            position = mediaPlayer.getCurrentPosition();
        }
        return position;
    }

    /**
     * ?????????????????????
     */
    public int getVideoTotalDuration() {
        int position = 0;
        if (mediaPlayer != null) {
            position = mediaPlayer.getDuration();
        }
        return position;
    }

    /**
     * ???????????????
     */
    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    /**
     * ????????????
     */
    public void destroyVideo() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();// ????????????
            mediaPlayer = null;
        }
        surfaceHolder = null;
        mn_palyer_surfaceView = null;
        video_position = 0;
        unRegisterBatteryReceiver();
        unregisterNetReceiver();
        removeAllListener();
        destroyTimeTask();
        myHandler.removeCallbacksAndMessages(null);
    }


    //--------------------------------------------------------------------------------------
    // ######## ???????????? ########
    //--------------------------------------------------------------------------------------

    /**
     * ?????????????????????
     */
    class BatteryReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //????????????????????????????????????Broadcast Action
            if (Intent.ACTION_BATTERY_CHANGED.equals(intent.getAction())) {
                //??????????????????
                int level = intent.getIntExtra("level", 0);
                //??????????????????
                int scale = intent.getIntExtra("scale", 100);

                int battery = (level * 100) / scale;

                //?????????????????????
                Log.i(TAG, "???????????????" + battery + "%");

                mn_iv_battery.setVisibility(View.VISIBLE);
                if (battery > 0 && battery < 20) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_01);
                } else if (battery >= 20 && battery < 40) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_02);
                } else if (battery >= 40 && battery < 65) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_03);
                } else if (battery >= 65 && battery < 90) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_04);
                } else if (battery >= 90 && battery <= 100) {
                    mn_iv_battery.setImageResource(R.drawable.mn_player_battery_05);
                } else {
                    mn_iv_battery.setVisibility(View.GONE);
                }
            }
        }
    }

    private BatteryReceiver batteryReceiver;

    private void registerBatteryReceiver() {
        if (batteryReceiver == null) {
            //?????????????????????
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            //???????????????????????????
            batteryReceiver = new BatteryReceiver();
            //??????receiver
            context.registerReceiver(batteryReceiver, intentFilter);
        }
    }

    private void unRegisterBatteryReceiver() {
        if (batteryReceiver != null) {
            context.unregisterReceiver(batteryReceiver);
        }
    }

    //-------------------------??????????????????
    public class NetChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (onNetChangeListener == null || !isNeedNetChangeListen) {
                return;
            }
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isAvailable()) {
                if (netInfo.getType() == ConnectivityManager.TYPE_WIFI) { //WiFi??????
                    onNetChangeListener.onWifi(mediaPlayer);
                } else if (netInfo.getType() == ConnectivityManager.TYPE_MOBILE) {   //3g??????
                    onNetChangeListener.onMobile(mediaPlayer);
                } else {    //??????
                    Log.i(TAG, "????????????");
                }
            } else {
                onNetChangeListener.onNoAvailable(mediaPlayer);
            }
        }
    }

    private NetChangeReceiver netChangeReceiver;

    private void registerNetReceiver() {
        if (netChangeReceiver == null) {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            netChangeReceiver = new NetChangeReceiver();
            context.registerReceiver(netChangeReceiver, filter);
        }
    }

    private void unregisterNetReceiver() {
        if (netChangeReceiver != null) {
            context.unregisterReceiver(netChangeReceiver);
        }
    }


    //--------------------------------------------------------------------------------------
    // ######## ??????????????? ########
    //--------------------------------------------------------------------------------------

    private void removeAllListener() {
        if (onNetChangeListener != null) {
            onNetChangeListener = null;
        }
        if (onCompletionListener != null) {
            onCompletionListener = null;
        }
    }


    //??????????????????
    private OnNetChangeListener onNetChangeListener;

    public void setOnNetChangeListener(OnNetChangeListener onNetChangeListener) {
        this.onNetChangeListener = onNetChangeListener;
    }

    //-----------------------???????????????
    private OnCompletionListener onCompletionListener;

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        this.onCompletionListener = onCompletionListener;
    }

    //-----------------------?????????????????????
    private OnScreenOrientationListener onScreenOrientationListener;

    public void setOnScreenOrientationListener(OnScreenOrientationListener onScreenOrientationListener) {
        this.onScreenOrientationListener = onScreenOrientationListener;
    }

}