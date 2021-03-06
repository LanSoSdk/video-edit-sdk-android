package com.lansosdk.videoeditor;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewConfiguration;


import com.lansosdk.LanSongFilter.LanSongFilter;
import com.lansosdk.box.ILayerInterface;
import com.lansosdk.box.LSOAsset;
import com.lansosdk.box.LSOCamLayer;
import com.lansosdk.box.LSOCamRelativeLayout;
import com.lansosdk.box.LSOCameraRunnable;
import com.lansosdk.box.LSOCameraSizeType;
import com.lansosdk.box.LSOFrameLayout;
import com.lansosdk.box.LSOLog;
import com.lansosdk.box.LSORecordFile;
import com.lansosdk.box.OnCameraResumeErrorListener;
import com.lansosdk.box.OnCreateListener;
import com.lansosdk.box.OnLanSongSDKErrorListener;
import com.lansosdk.box.OnRecordCompletedListener;
import com.lansosdk.box.OnRecordProgressListener;
import com.lansosdk.box.OnResumeListener;
import com.lansosdk.box.OnTakePictureListener;
import com.lansosdk.box.OnTextureAvailableListener;

import java.util.List;

public class LSOCamera extends LSOFrameLayout implements ILSOTouchInterface{

    private int compWidth = 1080;
    private int compHeight = 1920;

    private LSOCameraRunnable render;

    public LSOCamera(Context context) {
        super(context);
    }

    public LSOCamera(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LSOCamera(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public LSOCamera(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }
    //---------------------copy code start---------------------
    protected void sendOnCreateListener() {
        super.sendOnCreateListener();
        if (render != null) {
            render.setSurface(compWidth, compHeight, getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    public void sendOnResumeListener() {

        super.sendOnResumeListener();
        if (render != null) {
            render.setSurface(compWidth, compHeight, getSurfaceTexture(), getViewWidth(), getViewHeight());
        }
    }

    //旋转移动缩放
    public boolean onTextureViewTouchEvent(MotionEvent event) {
        if(isEnableTouch){
            super.onTextureViewTouchEvent(event);
            return onTouchEvent(event);
        }else{
            return false;
        }
    }


    public void onCreateAsync(OnCreateListener listener) {
        setup();
        setPlayerSizeAsync(compWidth, compHeight, listener);
    }


    private OnCreateListener onCreateListener;


    public void onResumeAsync(OnResumeListener listener) {
        super.onResumeAsync(listener);
        if (render != null) {
            render.onActivityPaused(false);
        }
    }

    public void onPause() {
        super.onPause();
        if (render != null) {
            if(isRecording()){
                pauseRecord();
            }
            render.onActivityPaused(true);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        cancel();
    }

    //---------------render start-----------------------------------------
    private static boolean isCameraOpened = false;

    private boolean frontCamera = false;
    private long recordDurationUs = Long.MAX_VALUE;

    private OnFocusEventListener onFocusListener;

    /**
     * 设置前置摄像头,在开始前设置;默认是后置摄像头;在录制前设置;
     * Set the front camera, set it before starting; the default is the back camera; set it before recording;
     * @param is
     */
    public void setFrontCamera(boolean is) {
        if (!isRunning()) {
            frontCamera = is;
        } else {
            LSOLog.e("setFrontCamera error render have been setup .");
        }
    }

    /**
     * 设置录制的时长, 默认无限长; 在录制前设置;
     * @param durationUs 录制时长
     */
    public void setRecordDuration(long durationUs) {
        if (durationUs > 0 && !isRunning() && getRecordDurationUs()==0) {
            recordDurationUs = durationUs;
        }
    }

    /**
     * 禁止麦克风的声音;
     * 增加外部音频时,自动禁止mic声音;
     *
     * @param is
     */
    public void setMicMute(boolean is) {

        if (render != null && !isRecording()) {
            render.setMicMute(is);
        }
    }

    /**
     * 设置预览尺寸, 不建议设置.
     * 如设置,则在start()前设置;
     * @param type
     */
    public void setPreviewSize(LSOCameraSizeType type){
        if(render!=null && !render.isRunning()){
            render.setPreviewSize(type);
        }
    }


    public boolean isRunning() {
        return render != null && render.isRunning();
    }


    /**
     * 录制完成监听
     * @param listener
     */
    public void setOnRecordCompletedListener(OnRecordCompletedListener listener) {
        if (render != null) {
            render.setOnRecordCompletedListener(listener);
        }
    }

    /**
     * OnRecordProgressListener中的两个参数: 当前录制的时长, 总录制的时长;
     *
     * @param listener
     */
    public void setOnRecordProgressListener(OnRecordProgressListener listener) {
        if (render != null) {
            render.setOnRecordProgressListener(listener);
        }
    }
    /**
     * 当camera从后台回来时, 如果相机被占用则会触发此错误回调;
     * @param listener
     */
    public void setOnCameraResumeErrorListener(OnCameraResumeErrorListener listener){
        if(render!=null){
            render.setOnCameraResumeErrorListener(listener);
        }
    }

    /**
     * 错误监听
     * @param listener
     */
    public void setOnLanSongSDKErrorListener(OnLanSongSDKErrorListener listener) {
        if (render != null) {
            render.setOnLanSongSDKErrorListener(listener);
        }
    }

    public boolean start() {
        super.start();
        if (isCameraOpened) {
            LSOLog.d("LSOCamera  start error. is opened...");
            return true;
        }
        if (getSurfaceTexture() != null) {
            render.setFrontCamera(frontCamera);
            render.setRecordDurationUs(recordDurationUs);

            if (render != null) {
                render.setDisplaySurface(getSurfaceTexture(), getViewWidth(), getViewHeight());
                isCameraOpened = render.start();
                if (!isCameraOpened) {
                    LSOLog.e("open LSOCamera error.\n");
                } else {
                    LSOLog.d("LSOCamera start preview...");
                }
            }
        } else {
            LSOLog.w("mSurfaceTexture error.");
        }
        return isCameraOpened;
    }

    public void setFilter(LanSongFilter filter) {
        if (render != null) {
            render.setFilter(filter);
        }
    }


    /**
     * 美颜, 范围是0.0---1.0; 0.0 不做磨皮, 1.0:完全磨皮;
     *
     * @param level
     */
    public void setBeautyLevel(float level) {
        if (render != null) {
            render.setBeautyLevel(level);
        }
    }

    /**
     * 禁止美颜;
     */
    public void setDisableBeauty() {
        if (render != null) {
            render.setBeautyLevel(0.0f);
        }
    }

    //--------------

    /**
     * 是否在绿幕抠图
     */
    public boolean isGreenMatting() {
        return render != null && render.isGreenMatting();
    }

    /**
     * 设置绿幕抠图
     * (绿幕抠图需要另外授权才有效)
     */
    public void setGreenMatting() {
        if (render != null) {
            render.setGreenMatting();
        }else{
            LSOLog.e("setGreenMatting error. render is null");
        }
    }

    /**
     * 取消绿幕抠图
     */
    public void cancelGreenMatting() {
        if (render != null) {
            render.cancelGreenMatting();
        }
    }


    private String bgPath=null;

    public String getBackGroundPath(){
        return bgPath;
    }
    /**
     * 设置背景路径, 路径可以是图片或视频
     * path  support  image and video.
     * @param path 路径
     */
    public void setBackGroundPath(String path) {
        if(bgPath!=null && bgPath.equals(path)){
            return;
        }

        if (render != null && isRunning() && path != null) {
            try {
                String suffix=getFileSuffix(path);
                if(isBitmapSuffix(suffix)){
                    setBackGroundBitmapPath(path);
                }else if(isVideoSuffix(suffix)){
                    setBackGroundVideoPath(path);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setBackGroundPath error, input is:"+ path);
            }
        }
    }

    /**
     * 设置背景图片;
     * @param path
     */
    public void setBackGroundBitmapPath(String path) {
        if(bgPath!=null && bgPath.equals(path)){
            return;
        }

        if (render != null && isRunning() && path != null) {
            try {
                bgPath=path;
                render.setBackGroundBitmapPath(path);
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setBackGroundPath error, input is:"+ path);
                bgPath=null;
            }
        }
    }

    public void setBackGroundVideoPath(String path) {
        if(bgPath!=null && bgPath.equals(path)){
            return;
        }

        if (render != null && isRunning() && path != null) {
            try {
                bgPath=path;
                render.setBackGroundVideoPath(path,1.0f);
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setBackGroundPath error, input is:"+ path);
                bgPath=null;
            }
        }
    }

    public void setBackGroundVideoPath(String path, float audioVolume) {
        if(bgPath!=null && bgPath.equals(path)){
            return;
        }
        if (render != null && isRunning() && path != null) {
            try {
                bgPath=path;
                render.setBackGroundVideoPath(path,audioVolume);
            } catch (Exception e) {
                e.printStackTrace();
                LSOLog.e("setBackGroundPath error, input is:"+ path);
            }
        }
    }


    /**
     * 获取背景图层.
     * 在每次setBackGroundPath会更新背景图层对象, 需要重新获取;
     * 设置后需等待30毫秒后获取
     * 不建议使用;
     * @return
     */
    public LSOCamLayer getBackGroundLayer(){
        if (render != null) {
            return render.getBackGroundLayer();
        }
        return null;
    }

    /**
     * 删除背景层;
     */
    public void removeBackGroundLayer() {
        if (render != null) {
            bgPath=null;
            render.removeBackGroundLayer();
        }
    }


    public LSOCamLayer getCameraLayer(){
        if(render!=null){
            return render.getCameraLayer();
        }else {
            return null;
        }
    }

    public MediaPlayer getMediaPlayer(){
        if(render!=null){
            return render.getMediaPlayer();
        }else{
            return null;
        }
    }



    private String fgBitmapPath=null;
    private String fgColorPath=null;

    /**
     * 设置前景图片;
     * @param path 图片路径
     */
    public void setForeGroundBitmap(String path) {

        if(fgBitmapPath!=null && fgBitmapPath.equals(path)){
            return;
        }

        if (render != null && isRunning() && !isRecording()) {
            try {
                fgBitmapPath=path;
                fgColorPath=null;
                LSOLog.d("Camera setForeGroundBitmap...");
                render.setForeGroundBitmap(new LSOAsset(path));
            } catch (Exception e) {
                e.printStackTrace();
                fgBitmapPath=null;
            }
        }
    }

    /**
     * 设置前景透明动画,
     * @param colorPath mv color path
     * @param maskPath mv mask path
     */
    public void setForeGroundVideoPath(String colorPath, String maskPath) {

        if(fgColorPath!=null && fgColorPath.equals(colorPath)){
            return;
        }

        if (render != null && isRunning() && !isRecording() && getRecordDurationUs()==0) {
            fgBitmapPath=null;
            fgColorPath=colorPath;
            render.setForeGroundVideoPath(colorPath, maskPath);
        } else {
            LSOLog.e("add MVLayer error!");
        }
    }

    /**
     * 删除前景视频
     */
    public void removeForeGroundLayer() {
        fgBitmapPath=null;
        fgColorPath=null;

        if (render != null) {
            render.removeForeGroundLayer();
        }
    }
    /**
     * 拍照
     * @param listener
     */
    public void takePictureAsync(OnTakePictureListener listener) {
        if (render != null && render.isRunning()) {
            render.takePictureAsync(listener);
        } else if (listener != null) {
            listener.onTakePicture(null);
        }
    }

    /**
     * 切换摄像头.
     * change front or back camera;
     */
    public void changeCamera() {
        if (render != null && !isRecording() && LSOCameraRunnable.isSupportFrontCamera()) {
            frontCamera = !frontCamera;
            render.changeCamera();
        }
    }

    /**
     * 是否是前置摄像头
     * @return
     */
    public boolean isFrontCamera() {
        return frontCamera;
    }

    /**
     * 开启或关闭闪光灯; 默认是不开启;
     * Turn on or off the flash; the default is not to turn on;
     */
    public void changeFlash() {
        if (render != null) {
            render.changeFlash();
        }
    }


    /**
     * 开始录制
     */
    public void startRecord() {
        if (render != null && !render.isRecording() ) {
            render.startRecord();
        }
    }
    /**
     * 是否在录制中.
     * @return
     */
    public boolean isRecording() {
        return render != null && render.isRecording();
    }

    /**
     * 暂停录制
     * 暂停后, 会录制一段视频, 录制的这段视频在onDestory中释放;
     */
    public void pauseRecord() {
        if (render != null && render.isRecording()) {
            render.pauseRecord();
        }
    }

    /**
     * 异步停止录制, 停止后会通过完成回调返回录制后的路径;
     * Stop the recording asynchronously,
     * and return to the recorded path through the completion callback after stopping;
     */
    public void stopRecordAsync() {
        if (render != null) {
            render.stopRecordAsync();
        }
    }
    /**
     * 删除上一段的录制
     *
     */
    public void deleteLastRecord() {
        if (render != null) {
            render.deleteLastRecord();
        }
    }

    /**
     * 获取录制总时长;
     *
     * @return
     */
    public long getRecordDurationUs() {
        if (render != null) {
            return render.getRecordDurationUs();
        } else {
            return 10;
        }
    }
    public List<LSORecordFile> getRecordFiles(){
        if(render!=null){
            return render.getRecordFiles();
        }else{
            LSOLog.e("getRecordFile error. render is null. ");
            return null;
        }
    }

    /**
     * 增加外部声音.
     * @param path
     * @param looping 是否循环;
     * @return
     */
    public void setAudioLayer(String path, boolean looping) {
        if (render!=null && !render.isRecording()) {
            render.removeAudio();
            if(path!=null){
                render.addAudio(path,looping);
            }
        }
    }

    /**
     * 删除声音图层
     */
    public void removeAudioLayer(){
        if (render!=null && !render.isRecording()) {
            render.removeAudio();
        }
    }


    /**
     * 增加一个纹理图层;
     * @param width 纹理的宽度
     * @param height 纹理的高度
     * @return 返回一个图层对象;
     */
    public LSOCamLayer addSurfaceLayer(int width, int height){
        if(render!=null && render.isRunning()){
            return render.addSurfaceLayer(width,height);
        }else {
            return null;
        }
    }

    /**
     * 在摄像机上层增加
     * 用在多机位场合;
     * @param bmp
     * @return
     */
    public LSOCamLayer addBitmapLayer(Bitmap bmp) {
        if(render!=null && render.isRunning()){
            return render.addBitmapLayer(bmp);
        }else {
            return null;
        }
    }

    /**
     * 在背景层上增加一层画面
     * 用在多机位场合
     * @param bmp 图片对象
     * @return 返回的是图层对象;
     */
    public LSOCamLayer addBitmapLayerAboveBackGround(Bitmap bmp) {
        if(render!=null && render.isRunning()){
            return render.addBitmapLayerAboveBackGround(bmp);
        }else {
            return null;
        }
    }

    /**
     * 删除一个图层
     * @param layer
     */
    public void removeLayer(LSOCamLayer layer){
        if(render!=null && render.isRunning()){
            render.removeLayer(layer);
        }
    }


    /**
     * 是否所有的图层都可以触摸事件;
     * @param is
     */
    public void setAllLayerTouchEnable(boolean is){
        if(render!=null){
            render.setAllLayerTouchEnable(is);
        }
    }


    private static String getFileSuffix(String path) {
        if (path == null)
            return "";
        int index = path.lastIndexOf('.');
        if (index > -1)
            return path.substring(index + 1);
        else
            return "";
    }


    private boolean isBitmapSuffix(String suffix) {

        return "jpg".equalsIgnoreCase(suffix)
                || "JPEG".equalsIgnoreCase(suffix)
                || "png".equalsIgnoreCase(suffix)
                || "heic".equalsIgnoreCase(suffix);
    }

    private boolean isVideoSuffix(String suffix) {
        return "mp4".equalsIgnoreCase(suffix)
                || "mov".equalsIgnoreCase(suffix);
    }

    /**
     * 录制一个view的图像
     * @param layout 录制控件的类, 无论是否enable, 这里都要设置对象;
     * @param enable 是否使能
     */
    public void setRelativeLayout(LSOCamRelativeLayout layout, boolean enable) {
        if(render!=null){
            render.setRelativeLayout(layout,enable);
        }
    }

    private float spacing(MotionEvent event) {
        if (event == null) {
            return 0;
        }
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


    private boolean isEnableTouch = true;
    public void setTouchEnable(boolean enable) {
        isEnableTouch = enable;
    }


    public void setCameraFocusListener(OnFocusEventListener listener) {
        this.onFocusListener = listener;
    }

    @Override
    public ILayerInterface getTouchPointLayer(float x, float y) {
        if(render!=null){
            return render.getTouchPointLayer(x,y);
        }else {
            return null;
        }
    }


    public interface OnFocusEventListener {
        void onFocus(int x, int y);
    }

    float x1 = 0;
    float x2 = 0;
    float y1 = 0;
    float y2 = 0;
    private long downTimeMs;
    private boolean isClickEvent = false;
    private boolean isSlideEvent = false;
    private boolean isZoomEvent = false;
    private float touching;
    private boolean disableZoom=false;
    public void setDisableZoom(boolean is){
        disableZoom=is;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int touchSlop =  ViewConfiguration.get(getContext()).getScaledTouchSlop();
        if (render == null || !isEnableTouch) { // 如果禁止了touch事件,则直接返回false;
            return false;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // 手指压下屏幕
            case MotionEvent.ACTION_DOWN:
                isZoomEvent = false;
                isClickEvent = true;
                isSlideEvent = true;
                x1 = event.getX();
                y1 = event.getY();
                downTimeMs = System.currentTimeMillis();

                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 计算两个手指间的距离
                if (isRunning()) {
                    touching = spacing(event);
                    isZoomEvent = true;
                    isClickEvent = false;
                    isSlideEvent = false;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isRunning()) {
                    if (isZoomEvent) {
//                        if (event.getPointerCount() >= 2 && !disableZoom) {// 触屏两个点时才执行
//                            float endDis = spacing(event);// 结束距离
//                            int scale = (int) ((endDis - touching) / 10f); // 每变化10f
//                            // zoom变1, 拉近拉远;
//                            if (scale != 0) {
//                                int zoom = render.getZoom() + scale;
//                                render.setZoom(zoom);
//                                touching = endDis;
//                            }
//                        }
                    }
                    if (isClickEvent && (Math.abs(x1 - event.getX()) > touchSlop||
                            Math.abs(y1 - event.getY()) > touchSlop) ){
                        isClickEvent = false;
                        isSlideEvent = true;
                    }
                }
                break;
            // 手指离开屏幕
            case MotionEvent.ACTION_UP:
                if (isRunning()) {
                    if (isClickEvent && System.currentTimeMillis() - downTimeMs < 200 ) {
                        float x = event.getX();
                        float y = event.getY();
                        render.doFocus((int) x, (int) y);

                        if (onFocusListener != null) {
                            onFocusListener.onFocus((int) x, (int) y);
                        }

                        isClickEvent = false;
                    }

                    if (!isZoomEvent && !isClickEvent && isSlideEvent){
                        float offsetX = x1 - event.getX();
                        float offsetY = y1 - event.getY();
                        if (Math.abs(offsetX) < touchSlop && Math.abs(offsetY) < touchSlop){
                            break;
                        }

                        if (Math.abs(Math.abs(offsetX) - Math.abs(offsetY)) < touchSlop){
                            break;
                        }

                        if (Math.abs(offsetX) > Math.abs(offsetY)){
                            if (offsetX > 0){
                                if (onSlideListener != null ){
                                    onSlideListener.onHorizontalSlide(true);
                                }
                            }else {
                                if (onSlideListener != null ){
                                    onSlideListener.onHorizontalSlide(false);
                                }
                            }

                        }else {
                            if (offsetY > 0){
                                if (onSlideListener != null ){
                                    onSlideListener.onVerticalSlide(true);
                                }
                            }else {
                                if (onSlideListener != null ){
                                    onSlideListener.onVerticalSlide(false);
                                }
                            }
                        }
                    }

                }
                isZoomEvent = false;
                break;
            case MotionEvent.ACTION_CANCEL:
                isZoomEvent = false;
                isClickEvent = false;
                break;
            default:
                break;
        }
        return true;
    }

    private void setup() {
        if (render == null) {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            if (dm.widthPixels * dm.heightPixels < 1080 * 1920) {
                compWidth = 720;
                compHeight = 1280;
            }
            render = new LSOCameraRunnable(getContext(), compWidth, compHeight);
        }
    }


    public void cancel() {
        isCameraOpened = false;
        bgPath=null;
        fgBitmapPath=null;
        fgColorPath=null;
        if (render != null) {
            render.cancel();
            render = null;
        }
    }

    OnSlideListener onSlideListener;


    public interface OnSlideListener{

        void onHorizontalSlide(boolean slideLeft);

        void onVerticalSlide(boolean slideUp);
    }

}
