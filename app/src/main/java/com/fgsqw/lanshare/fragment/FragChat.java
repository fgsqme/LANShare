package com.fgsqw.lanshare.fragment;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fgsqw.lanshare.R;
import com.fgsqw.lanshare.activity.DataCenterActivity;
import com.fgsqw.lanshare.activity.preview.ReviewImages;
import com.fgsqw.lanshare.activity.video.VideoPlayer;
import com.fgsqw.lanshare.base.BaseFragment;
import com.fgsqw.lanshare.config.PreConfig;
import com.fgsqw.lanshare.dialog.DeviceSelectDialog;
import com.fgsqw.lanshare.fragment.adapter.ChatAdabper;
import com.fgsqw.lanshare.fragment.adapter.viewolder.FileMsgHolder;
import com.fgsqw.lanshare.pojo.Device;
import com.fgsqw.lanshare.pojo.MediaInfo;
import com.fgsqw.lanshare.pojo.MessageContent;
import com.fgsqw.lanshare.pojo.MessageFileContent;
import com.fgsqw.lanshare.pojo.MessageFolderContent;
import com.fgsqw.lanshare.pojo.MessageMediaContent;
import com.fgsqw.lanshare.pojo.mCmd;
import com.fgsqw.lanshare.pojo.mSocket;
import com.fgsqw.lanshare.service.LANService;
import com.fgsqw.lanshare.toast.T;
import com.fgsqw.lanshare.utils.FileUtil;
import com.fgsqw.lanshare.utils.PrefUtil;
import com.fgsqw.lanshare.utils.mUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class FragChat extends BaseFragment implements View.OnClickListener, View.OnLongClickListener, ChatAdabper.OnItemClickListener, ChatAdabper.OnItemLongClickListener {


    public DataCenterActivity dataCenterActivity;
    InputMethodManager mInputManager;

    private View view;
    private Button btnSned;
    private EditText editContent;
    private LinearLayout messageLayout;
    private TextView devSelectLTv;
    private RecyclerView recyclerView;
    private ChatAdabper chatAdabper;
    private LinearLayoutManager layoutManager;
    private final List<MessageContent> messageContentList = new ArrayList<>();
    private Device selectedDevice;
    private PrefUtil prefUtil;

    @Override
    public void onAttach(Context context) {
        dataCenterActivity = (DataCenterActivity) context;
        super.onAttach(context);
    }

    // ??????LANService?????????
    @SuppressWarnings("all")
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == mCmd.SERVICE_IF_RECIVE_FILES) {   // ????????????????????????
                showIsRecyDialog(msg);
            } else if (msg.what == mCmd.SERVICE_SHOW_PROGRESS) {     // ??????????????????
                addListData(msg);
            } else if (msg.what == mCmd.SERVICE_PROGRESS) {          // ??????????????????
                updateLocalItemProgress(msg);
            } else if (msg.what == mCmd.SERVICE_COMPLETE_COUNT) {    // ????????????????????????????????????
                updateLocalItemFolderCount(msg);
            } else if (msg.what == mCmd.SERVICE_CLOSE_PROGRESS) {    // ????????????
                updateLocalItemInfo(msg);
            } else if (msg.what == mCmd.SERVICE_ADD_MESSGAGE) {      // ????????????
                MessageContent messageContent = (MessageContent) msg.obj;
                addMessage(messageContent);
            } else if (msg.what == mCmd.SERVICE_NETWORK_CHANGES) {    // ????????????
                Device device = (Device) msg.obj;
                dataCenterActivity.updateIP(device.getDevIP());
            }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    public void updateLocalItemInfo(Message message) {
        MessageFileContent fileContent = (MessageFileContent) message.obj;
        // ?????????????????????????????????
        int dataPosition = chatAdabper.getDataPosition(fileContent);
        if (fileContent.getViewType() == ChatAdabper.TYPE_FILE_MSG_LEFT || fileContent.getViewType() == ChatAdabper.TYPE_FILE_MSG_RIGHT) {
            // ?????????????????????????????????
            FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
            if (viewHolder != null) {
                viewHolder.progressBar.setProgress(fileContent.getProgress());
                if (fileContent.getSuccess() != null) {
                    viewHolder.progressBar.setVisibility(View.GONE);
                    viewHolder.stateTv.setVisibility(View.VISIBLE);
                    viewHolder.stateTv.setText(fileContent.getStateMessage());
                    if (!fileContent.getSuccess()) {
                        viewHolder.stateTv.setTextColor(Color.RED);
                    } else {
                        viewHolder.stateTv.setTextColor(getContext().getColor(R.color.item_text));
                    }
                } else {
                    viewHolder.progressBar.setVisibility(View.VISIBLE);
                    viewHolder.stateTv.setVisibility(View.GONE);
                    viewHolder.stateTv.setTextColor(getContext().getColor(R.color.item_text));
                }
            } else {
                chatAdabper.notifyItemChanged(dataPosition);
            }
        } else {
            chatAdabper.notifyItemChanged(dataPosition);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void addListData(Message message) {
        List<MessageFileContent> messageContents = (List<MessageFileContent>) message.obj;
        messageContentList.addAll(messageContents);
        chatAdabper.notifyDataSetChanged();
        recyclerView.scrollToPosition(chatAdabper.getItemCount() - 1);
    }

    public void updateLocalItemFolderCount(Message message) {
        MessageFolderContent folderContent = (MessageFolderContent) message.obj;
        int dataPosition = chatAdabper.getDataPosition(folderContent);
        FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
        if (viewHolder != null) {
            viewHolder.content.setText(folderContent.getContent());
        } else {
            chatAdabper.notifyItemChanged(dataPosition);
        }
    }

    public void updateLocalItemProgress(Message message) {
        MessageFileContent fileContent = (MessageFileContent) message.obj;
        // ?????????????????????????????????
        int dataPosition = chatAdabper.getDataPosition(fileContent);
        // ?????????????????????????????????
        FileMsgHolder viewHolder = (FileMsgHolder) recyclerView.findViewHolderForAdapterPosition(dataPosition);
        if (viewHolder != null) {
            viewHolder.progressBar.setProgress(fileContent.getProgress());
        } else {
            chatAdabper.notifyItemChanged(dataPosition);
        }
    }

    public void showIsRecyDialog(Message msg) {
        Object[] dataObject = (Object[]) msg.obj;
        Device device = (Device) dataObject[0];
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setIcon(R.mipmap.ic_launcher)
                .setCancelable(false)
                .setTitle("????????????")
                .setMessage("??????????????????" + device.getDevName() + "???" + msg.arg1 + "?????????")
                .setPositiveButton("??????", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    LANService.service.startRecvFile(dataObject, true);
                }).setNegativeButton("??????", (dialogInterface, i) -> {
                    dialogInterface.dismiss();
                    LANService.service.startRecvFile(dataObject, false);
                });
        builder.create().show();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            prefUtil = new PrefUtil(getContext());
            view = inflater.inflate(R.layout.fragment_chat, container, false);
            initView();
            initList();
        }

        mInputManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    public void initView() {
        recyclerView = view.findViewById(R.id.chat_recy);
        btnSned = view.findViewById(R.id.chat_btn_send);
        editContent = view.findViewById(R.id.chat_et_content);
        messageLayout = view.findViewById(R.id.chat_send_message_layout);
        devSelectLTv = view.findViewById(R.id.chat_dev_select_tv);

        btnSned.setOnClickListener(this);
        btnSned.setOnLongClickListener(this);
        devSelectLTv.setOnClickListener(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void initList() {
        layoutManager = new LinearLayoutManager(getContext());
        chatAdabper = new ChatAdabper(this);
        chatAdabper.setOnItemClickListener(this);
        chatAdabper.setOnItemLongClickListener(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdabper);

        recyclerView.setOnTouchListener((view, motionEvent) -> {
            hideSoftInput();
            editContent.clearFocus();
            return false;
        });


        editContent.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // ????????????
                dataCenterActivity.hideBottom();
            } else {
                // ????????????
                dataCenterActivity.showBottom();
            }
        });
    }


    @Override
    public void onItemClick(MessageContent messageContent, int position) {
        boolean POEN_MEDIA_PLAYER = prefUtil.getBoolean(PreConfig.POEN_MEDIA_PLAYER, true);
        if (messageContent instanceof MessageFileContent) {
            MessageFileContent fileContent = (MessageFileContent) messageContent;
            if (fileContent.getSuccess() != null && fileContent.getSuccess()) {

                if (POEN_MEDIA_PLAYER && messageContent instanceof MessageMediaContent) {
                    MessageMediaContent mediaContent = (MessageMediaContent) fileContent;

                    MediaInfo mediaInfo = new MediaInfo();
                    mediaInfo.setPath(mediaContent.getPath());
                    mediaInfo.setLength(mediaContent.getLength());
                    mediaInfo.setName(mediaContent.getContent());

                    if (mediaContent.isVideo()) {
                        VideoPlayer.toPreviewVideoActivity(dataCenterActivity, mediaInfo);
                    } else {
                        List<MediaInfo> mediaInfos = Arrays.asList(mediaInfo);
                        ReviewImages.openActivity(getActivity(), mediaInfos,
                                mediaInfos, false, 0, 1);
                    }
                } else {

                    if (messageContent instanceof MessageFolderContent) {
                        T.s("???????????????????????????,???????????????????????????");
                    } else {
                        if (fileContent.getSuccess() != null && fileContent.getSuccess()) {
                            FileUtil.openFile(dataCenterActivity, new File(fileContent.getPath()));
                        }
                    }
                }


            } else {
                T.s("?????????????????????");
            }

        }
    }



    @Override
    public boolean onItemLongClick(MessageContent messageContent, View view, int position) {
        PopupMenu popupMenu = new PopupMenu(Objects.requireNonNull(getContext()), view);
        popupMenu.getMenuInflater().inflate(R.menu.recy_menu, popupMenu.getMenu());
        popupMenu.setGravity(messageContent.isLeft() ? Gravity.START : Gravity.END);
        // ???????????????????????????????????????
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_delete) {

                if (messageContent instanceof MessageFileContent) {
                    MessageFileContent fileContent = (MessageFileContent) messageContent;
                    mSocket socket = fileContent.getSocket();
                    if (fileContent.isLeft()) {
                        LANService.service.sendCloseCmd(fileContent, socket.getOut());
                    } else {
                        socket.mClose();
                    }
                }
                // ????????????
                messageContentList.remove(position);
//                chatAdabper.notifyItemRemoved(position);
                // ????????????
                chatAdabper.refresh();
            } else if (item.getItemId() == R.id.menu_cppy) {
                // ????????????
                mUtil.copyString(messageContent.getContent(), getContext());
            }
            return false;
        });
        popupMenu.show();
        return true;
    }

    public List<MessageContent> getMessageContents() {
        return messageContentList;
    }

    public Handler getHandler() {
        return handler;
    }

    public void addMessage(MessageContent messageContent) {
        messageContentList.add(messageContent);
        chatAdabper.refresh();
        recyclerView.scrollToPosition(chatAdabper.getItemCount() - 1);
    }

    @Override
    public boolean onKeyDown(int n, KeyEvent keyEvent) {
        if (editContent.isFocused()) {
            editContent.clearFocus();
            return true;
        }
        return false;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_btn_send: {
                String message = editContent.getText().toString();
                if (message.isEmpty()) {
                    T.s("??????????????????");
                    return;
                } else if (message.length() > 700) {
                    T.s("????????????????????????700???");
                    return;
                }
                MessageContent messageContent = new MessageContent();
                messageContent.setLeft(false);
                messageContent.setContent(message);
                messageContent.setUserName(LANService.service.getDevName());
                messageContent.setToUser(selectedDevice == null ? "????????????" : selectedDevice.getDevName());
                LANService.service.broadcastMessage(selectedDevice, message, false);
                addMessage(messageContent);
                editContent.setText("");
                break;
            }
            case R.id.chat_dev_select_tv: {
                selectDevice();
                break;
            }
        }
    }

    public void selectDevice() {
        DeviceSelectDialog deviceSelectDialog = new DeviceSelectDialog(getContext());
        deviceSelectDialog.setOnDeviceSelect(device -> {
            if (device.getDevIP() == null) {
                editContent.setHint(getString(R.string.send_message_hint));
                devSelectLTv.setText(getString(R.string.all_device));
                selectedDevice = null;
            } else {
                selectedDevice = device;
                editContent.setHint("???" + device.getDevName() + "????????????");
                devSelectLTv.setText(device.getDevName());
            }
        });
        deviceSelectDialog.show();
    }


    /**
     * ???????????????
     */
    public void hideSoftInput() {
        mInputManager.hideSoftInputFromWindow(editContent.getWindowToken(), 0);
    }

    /**
     * ???????????????
     */
    public void showSoftInput() {
        editContent.requestFocus();
        editContent.post(() -> mInputManager.showSoftInput(editContent, 0));
    }


    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onLongClick(View v) {
        switch (v.getId()) {
            case R.id.chat_btn_send: {
                String message = editContent.getText().toString();
                if (message.isEmpty()) {
                    T.s("??????????????????");
                    return false;
                } else if (message.length() > 700) {
                    T.s("????????????????????????700???");
                    return false;
                }
                MessageContent messageContent = new MessageContent();
                messageContent.setLeft(false);
                messageContent.setContent(message);
                messageContent.setUserName(LANService.service.getDevName());
                messageContent.setToUser(selectedDevice == null ? "????????????" : selectedDevice.getDevName());
                LANService.service.broadcastMessage(selectedDevice, message, true);
                addMessage(messageContent);
                editContent.setText("");
                break;
            }
        }
        return true;
    }
}
