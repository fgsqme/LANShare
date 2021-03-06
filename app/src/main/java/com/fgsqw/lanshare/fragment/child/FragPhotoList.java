package com.fgsqw.lanshare.fragment.child;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.activity.preview.ReviewImages;
import com.fgsqw.lanshare.activity.video.VideoPlayer;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.fragment.adapter.PhotoAdapter;
import com.fgsqw.lanshare.fragment.adapter.SortPhotoAdapter;
import com.fgsqw.lanshare.fragment.minterface.ChildBaseMethod;
import com.fgsqw.lanshare.pojo.MediaInfo;
import com.fgsqw.lanshare.pojo.PhotoFolder;
import com.fgsqw.lanshare.utils.DateUtils;
import com.fgsqw.lanshare.utils.FIleSerachUtils;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.ViewUpdate;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class FragPhotoList extends BaseFragment implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, ChildBaseMethod {

    private View view;
    private ImageView backImg;
    private TextView sizImgTv;
    private TextView timeTv;
    private CheckBox selectAll;
    private CheckBox selectMode;
    private SwipeRefreshLayout swipe;
    private RecyclerView recyclerView;
    private RelativeLayout backLayout;
    private LinearLayout selectLayout;
    private boolean isOpenFolder;
    private boolean isShowTime;

    private int posiition;
    private PhotoAdapter mPhotoAdapter;
    private GridLayoutManager mLayoutManager;
    private List<PhotoFolder> mFolders;
    public final List<MediaInfo> mSelectList = new LinkedList<>();


    private final Handler mHideHandler = new Handler();
    private final Runnable mHide = this::hideTime;

    public DataCenterActivity dataCenterActivity;

    private List<MediaInfo> cruuentPhotoList;

    private PrefUtil prefUtil;

    private boolean isPhotoView;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_child_photo, container, false);
            prefUtil = new PrefUtil(getContext());
            initView();
            loadImageForSDCard();
        }
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void initView() {
        selectLayout = view.findViewById(R.id.photo_select_layout);
        backImg = view.findViewById(R.id.photo_back_img);
        sizImgTv = view.findViewById(R.id.photo_size_img_tv);
        recyclerView = view.findViewById(R.id.photo_recy);
        timeTv = view.findViewById(R.id.photo_time_tv);
        swipe = view.findViewById(R.id.photo_swipe);
        backLayout = view.findViewById(R.id.photo_back_layout);
        selectAll = view.findViewById(R.id.photo_check_select_all);
        selectMode = view.findViewById(R.id.photo_check_select_mode);
        backLayout.setOnClickListener(this);
        selectAll.setOnClickListener(this);
        selectMode.setOnCheckedChangeListener(this);
        swipe.setOnRefreshListener(this::loadImageForSDCard);
        selectMode.setChecked(prefUtil.getBoolean(PreConfig.MEDIA_SELECT_MODEL));
    }

    private void loadImageForSDCard() {
        ViewUpdate.runThread(() -> {
            ViewUpdate.threadUi(() -> {
                sizImgTv.setText("?????????");
                swipe.setRefreshing(true);
            });
            mFolders = FIleSerachUtils.loadImageForSDCard(getContext());
            ViewUpdate.threadUi(() -> {
                if (mFolders != null && !mFolders.isEmpty()) {
                    isOpenFolder = true;
                    posiition = 0;
                    folderview();//?????????????????????
                    swipe.setRefreshing(false);//?????????????????????
                }
            });
        });

    }

    public void initPhotoList(PhotoFolder photoFolder) {
        cruuentPhotoList = photoFolder.getImages();

        if (mLayoutManager == null) {
            mLayoutManager = new GridLayoutManager(getActivity(), 4);
        }

        if (mPhotoAdapter == null) {
            mPhotoAdapter = new PhotoAdapter(this, !selectMode.isChecked());
        }
        isPhotoView = true;

        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(mPhotoAdapter);
        mPhotoAdapter.refresh();


        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (isOpenFolder) {
                    changeTime();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //if(isOpenFolder);
                //changeTime();
            }
        });

        mPhotoAdapter.setOnImageSelectListener((photoInfo, isSelect, view) -> {
            selectAll.setChecked(mPhotoAdapter.isSelectAll());
        });

        mPhotoAdapter.setOnItemClickListener(new PhotoAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(MediaInfo mediaInfo, int position) {
                if (mediaInfo.isVideo()) {
                    VideoPlayer.toPreviewVideoActivity(dataCenterActivity, mediaInfo);
                } else {
                    toPreviewActivity(cruuentPhotoList, position);
                }
            }

            @Override
            public void OnLongItenClick(final MediaInfo mediaInfo, final int position) {
                final String path = mediaInfo.getPath();
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("???????????????");
                String[] items;
                // ???????????????(??????/???
                final File f = new File(path);
                if (f.canWrite()) {
                    items = new String[]{"??????", "??????", "??????", "??????",};
                } else {
                    items = new String[]{"??????"};
                }
                // ???????????????????????????
                builder.setItems(items, (arg0, arg1) -> {
                    switch (arg1) {
                        case 0:
                            // ??????
                            dataCenterActivity.sendOneFile(mediaInfo);
                            break;
                        case 1:
                            Toast.makeText(getContext(), f.getPath(), Toast.LENGTH_LONG).show();
                            break;
                        case 2:
                            //??????
                            if (mediaInfo.isVideo()) {
                                VideoPlayer.toPreviewVideoActivity(dataCenterActivity, mediaInfo);
                            } else {
                                toPreviewActivity(Collections.singletonList(mediaInfo), 1);
                            }
                            break;
                        case 4:
                            //??????
                            break;
                    }
                }).show();
            }
        });
    }

    private void initfolderlist() {
        isPhotoView = false;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        SortPhotoAdapter adapter = new SortPhotoAdapter(getContext(), mFolders);
        adapter.setOnFolderSelectListener(new SortPhotoAdapter.OnFolderSelectListener() {
            @Override
            public void OnImageFoderSelect(PhotoFolder folder) {


            }

            @Override
            public void OnFolderSelect(PhotoFolder folder) {
                posiition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstVisibleItemPosition();
                //????????????????????????????????????item
                imageview(folder);

            }
        });

        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(posiition);
    }


    /**
     * ??????????????????
     */
    @SuppressLint("SetTextI18n")
    private void folderview() {
        if (isOpenFolder) {
            initfolderlist();
            sizImgTv.setText("??????" + "(" + mFolders.size() + ")");
            backImg.setVisibility(View.GONE);
            selectLayout.setVisibility(View.GONE);
            isOpenFolder = false;
        }
    }

    /**
     * ??????????????????
     **/
    @SuppressLint("SetTextI18n")
    private void imageview(PhotoFolder folder) {
        if (!isOpenFolder) {
            initPhotoList(folder);
            selectAll.setChecked(mPhotoAdapter.isSelectAll());
            sizImgTv.setText(folder.getName() + "(" + folder.getImages().size() + ")");
            isOpenFolder = true;
            backImg.setVisibility(View.VISIBLE);
            selectLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * ???????????????
     */
    private void hideTime() {
        if (isShowTime) {
            ObjectAnimator.ofFloat(timeTv, "alpha", 1, 0).setDuration(300).start();
            isShowTime = false;
        }
    }

    /**
     * ???????????????
     */
    private void showTime() {
        if (!isShowTime) {
            ObjectAnimator.ofFloat(timeTv, "alpha", 0, 1).setDuration(300).start();
            isShowTime = true;
        }
    }

    /**
     * ??????????????????????????????????????????????????????????????????????????????????????????
     */
    private void changeTime() {
        int firstVisibleItem = getFirstVisibleItem();    //?????????????????????item ??????
        MediaInfo mediaInfo = mPhotoAdapter.getFirstVisibleImage(firstVisibleItem); //???????????????????????????
        if (mediaInfo != null) {
            String time = DateUtils.getImageTime(mediaInfo.getTime() * 1000);
            timeTv.setText(time);
            showTime();
            mHideHandler.removeCallbacks(mHide);
            mHideHandler.postDelayed(mHide, 500);
        }
    }

    private int getFirstVisibleItem() {
        return mLayoutManager.findFirstVisibleItemPosition();//?????????????????????item ??????
    }


    private void toPreviewActivity(List<MediaInfo> mediaInfos, int position) {
        if (mediaInfos != null && !mediaInfos.isEmpty()) {
            ReviewImages.openActivity(getActivity(), mediaInfos,
                    mPhotoAdapter.getSelectImages(), false, 0, position);
        }
    }


    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (isOpenFolder) {
            folderview();
            return true;
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.photo_back_layout: {
                folderview();
                break;
            }
            case R.id.photo_check_select_all: {
                CheckBox checkBox = (CheckBox) v;
                if (checkBox.isChecked()) {
                    mPhotoAdapter.setSelecteAll(cruuentPhotoList);
                    mSelectList.clear();
                    mSelectList.addAll(cruuentPhotoList);
                } else {
                    mPhotoAdapter.clearThisFolderAllSelect();
                }
                break;
            }

        }
    }

    public List<MediaInfo> getSelectList() {
        return mSelectList;
    }

    public List<MediaInfo> getCruuentPhotoList() {
        return cruuentPhotoList;
    }

    @Override
    public void clearSelect() {

        if (mSelectList.size() > 0 && isVisible()) {
            mSelectList.clear();
            mPhotoAdapter.refresh();
            selectAll.setChecked(mPhotoAdapter.isSelectAll());
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.photo_check_select_mode: {
                if (isPhotoView) {
                    mPhotoAdapter.setViewImage(!isChecked);
                }
                break;
            }

        }
    }
}



















