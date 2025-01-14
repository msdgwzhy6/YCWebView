/*
Copyright 2017 yangchong211（github.com/yangchong211）

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package com.ycbjie.webviewlib;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.tencent.smtt.export.external.interfaces.IX5WebChromeClient;
import com.tencent.smtt.sdk.ValueCallback;
import com.tencent.smtt.sdk.WebChromeClient;
import com.tencent.smtt.sdk.WebView;
import static android.app.Activity.RESULT_OK;

/**
 * <pre>
 *     @author yangchong
 *     blog  : https://github.com/yangchong211
 *     time  : 2019/9/10
 *     desc  : 自定义x5的WebChromeClient
 *     revise: 如果自定义WebChromeClient，建议继承该类，后期添加视频播放的处理方法
 *             demo地址：https://github.com/yangchong211/YCWebView
 * </pre>
 */
public class X5WebChromeClient extends WebChromeClient {

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mUploadMessageForAndroid5;
    /**
     * 注意h5中调用上传图片，resultCode保持一致性
     */
    public static int FILE_CHOOSER_RESULT_CODE = 1;
    public static int FILE_CHOOSER_RESULT_CODE_5 = 2;
    private InterWebListener webListener;
    private VideoWebListener videoWebListener;
    private boolean isShowContent = false;
    private Activity activity;
    private View progressVideo;
    private View customView;
    private IX5WebChromeClient.CustomViewCallback customViewCallback;
    private FullscreenHolder videoFullView;

    /**
     * 设置监听时间，包括常见状态页面切换，进度条变化等
     * @param listener                          listener
     */
    public void setWebListener(InterWebListener listener){
        this.webListener = listener;
    }

    /**
     * 设置视频播放监听，主要是比如全频，取消全频，隐藏和现实webView
     * @param videoWebListener                  listener
     */
    public void setVideoWebListener(VideoWebListener videoWebListener){
        this.videoWebListener = videoWebListener;
    }

    /**
     * 构造方法
     * @param activity                          上下文
     */
    public X5WebChromeClient(Activity activity) {
        this.activity = activity;
    }

    /**
     * 这个方法是监听加载进度变化的，当加载到百分之八十五的时候，页面一般就出来呢
     * 作用：获得网页的加载进度并显示
     * @param view                              view
     * @param newProgress                       进度值
     */
    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        if (webListener!=null){
            webListener.startProgress(newProgress);
            int max = 85;
            if (newProgress> max && !isShowContent){
                webListener.hindProgressBar();
                isShowContent = true;
            }
        }
    }

    /**
     * 这个方法主要是监听标题变化操作的
     * @param view                              view
     * @param title                             标题
     */
    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        if (title.contains("404") || title.contains("网页无法打开")){
            if (webListener!=null){
                webListener.showErrorView();
            }
        } else {
            // 设置title
        }
    }

    /**
     * 视频加载时进程loading
     */
    @Override
    public View getVideoLoadingProgressView() {
        if (progressVideo == null && activity!=null) {
            LayoutInflater inflater = LayoutInflater.from(activity);
            progressVideo = inflater.inflate(R.layout.view_web_video_progress, null);
        }
        return progressVideo;
    }

    /**
     * 播放网络视频时全屏会被调用的方法，播放视频切换为横屏
     */
    @Override
    public void onShowCustomView(View view, IX5WebChromeClient.CustomViewCallback callback) {
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        videoWebListener.hindWebView();
        // 如果一个视图已经存在，那么立刻终止并新建一个
        if (customView != null) {
            callback.onCustomViewHidden();
            return;
        }
        fullViewAddView(view);
        customView = view;
        customViewCallback = callback;
        videoWebListener.showVideoFullView();
    }

    /**
     * 添加view到decorView容齐中
     * @param view                              view
     */
    private void fullViewAddView(View view) {
        //增强逻辑判断，尤其是getWindow()
        if (activity!=null && activity.getWindow()!=null){
            FrameLayout decor = (FrameLayout) activity.getWindow().getDecorView();
            videoFullView = new FullscreenHolder(activity);
            videoFullView.addView(view);
            decor.addView(videoFullView);
        }
    }

    /**
     * 获取视频控件view
     * @return                                  view
     */
    private FrameLayout getVideoFullView() {
        return videoFullView;
    }

    /**
     * 销毁的时候需要移除一下视频view
     */
    public void removeVideoView(){
        if (videoFullView!=null){
            videoFullView.removeAllViews();
        }
    }

    /**
     * 视频播放退出全屏会被调用的
     */
    @Override
    public void onHideCustomView() {
        if (customView == null) {
            // 不是全屏播放状态
            return;
        }
        if (activity!=null){
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        customView.setVisibility(View.GONE);
        if (getVideoFullView() != null) {
            getVideoFullView().removeView(customView);
        }
        customView = null;
        if (videoWebListener!=null){
            videoWebListener.hindVideoFullView();
        }
        customViewCallback.onCustomViewHidden();
        if (videoWebListener!=null){
            videoWebListener.showWebView();
        }
    }

    /**
     * 判断是否是全屏
     */
    public boolean inCustomView() {
        return (customView != null);
    }

    /**
     * 逻辑是：先判断是否全频播放，如果是，则退出全频播放
     * 全屏时按返加键执行退出全屏方法
     */
    public void hideCustomView() {
        this.onHideCustomView();
        if (activity!=null){
            activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    /**
     * 打开文件夹，扩展浏览器上传文件，3.0++版本
     * @param uploadMsg                         msg
     * @param acceptType                        type
     */
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
        openFileChooserImpl(uploadMsg);
    }

    /**
     * 3.0--版本
     * @param uploadMsg                         msg
     */
    public void openFileChooser(ValueCallback<Uri> uploadMsg) {
        openFileChooserImpl(uploadMsg);
    }

    /**
     * 打开文件夹
     * @param uploadMsg                         msg
     * @param acceptType                        type
     * @param capture                           capture
     */
    @Override
    public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
        openFileChooserImpl(uploadMsg);
    }

    /**
     * For Android > 5.0
     * @param webView                           webview
     * @param uploadMsg                         msg
     * @param fileChooserParams                 参数
     * @return
     */
    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> uploadMsg,
                                     FileChooserParams fileChooserParams) {
        openFileChooserImplForAndroid5(uploadMsg);
        return true;
    }

    /**
     * 打开文件夹
     * @param uploadMsg                         msg
     */
    private void openFileChooserImpl(ValueCallback<Uri> uploadMsg) {
        if (activity!=null){
            mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            activity.startActivityForResult(
                    Intent.createChooser(i, "文件选择"), FILE_CHOOSER_RESULT_CODE);
        }
    }

    /**
     * 打开文件夹，Android5.0以上
     * @param uploadMsg                         msg
     */
    private void openFileChooserImplForAndroid5(ValueCallback<Uri[]> uploadMsg) {
        if (activity!=null){
            mUploadMessageForAndroid5 = uploadMsg;
            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("image/*");
            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "图片选择");
            activity.startActivityForResult(chooserIntent, FILE_CHOOSER_RESULT_CODE_5);
        }
    }

    /**
     * 5.0以下 上传图片成功后的回调
     */
    public void uploadMessage(Intent intent, int resultCode) {
        if (null == mUploadMessage) {
            return;
        }
        Uri result = intent == null || resultCode != RESULT_OK ? null : intent.getData();
        mUploadMessage.onReceiveValue(result);
        mUploadMessage = null;
    }

    /**
     * 5.0以上 上传图片成功后的回调
     */
    public void uploadMessageForAndroid5(Intent intent, int resultCode) {
        if (null == mUploadMessageForAndroid5) {
            return;
        }
        Uri result = (intent == null || resultCode != RESULT_OK) ? null : intent.getData();
        if (result != null) {
            mUploadMessageForAndroid5.onReceiveValue(new Uri[]{result});
        } else {
            mUploadMessageForAndroid5.onReceiveValue(new Uri[]{});
        }
        mUploadMessageForAndroid5 = null;
    }
}
