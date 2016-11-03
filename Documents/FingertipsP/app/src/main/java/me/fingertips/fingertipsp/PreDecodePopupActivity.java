package me.fingertips.fingertipsp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.Window;

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import me.fingertips.fingertipsp.decoder.PreDecoder;
import me.fingertips.fingertipsp.encoder.TextureMovieEncoder;
import me.fingertips.fingertipsp.gles.Drawable2d;
import me.fingertips.fingertipsp.gles.ScaledDrawable2d;
import me.fingertips.fingertipsp.gles.Sprite2d;
import me.fingertips.fingertipsp.gles.Texture2dProgram;
import me.fingertips.fingertipsp.utils.FileUtil;
import me.fingertips.fingertipsp.utils.MediaDataUtil;

public class PreDecodePopupActivity extends Activity implements PreDecoder.OnDecodeListener, TextureMovieEncoder.OnEncodeListener{
    private static final String TAG = "PreDecodePopupActivity";

    private GLSurfaceView preSurface;
    private PreSurfaceRenderer preRenderer;

    private PreDecoder decoder;

    private int vWidth, vHeight;
    private int orientation;

    private String targetFilePath;
    private int frameRate;

    private TextureMovieEncoder encoder;
    private boolean mRecordingEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_pre_decode_popup);
        init();
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener(){
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            preSurface.requestRender();
        }
    };

    private void init(){
        Intent intent = getIntent();
        targetFilePath = intent.getExtras().getString("PATH");
        frameRate = intent.getExtras().getInt("FRAME_RATE");
        orientation = intent.getExtras().getInt("ORIENTATION");

        encoder = new TextureMovieEncoder();
        mRecordingEnabled = encoder.isRecording();
        encoder.setOnEncodeListener(this);

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        vWidth = dm.widthPixels;
        vHeight = dm.heightPixels;

        decoder = new PreDecoder();
        decoder.setOnDecodeListner(this);

        preSurface = (GLSurfaceView)findViewById(R.id.pre_glsurface);
        preSurface.setEGLContextClientVersion(2);
        preSurface.setZOrderOnTop(false);
        preRenderer = new PreSurfaceRenderer(this, onFrameAvailableListener, encoder, targetFilePath, frameRate, orientation);
        preSurface.setRenderer(preRenderer);

    }

    @Override
    protected void onPause() {
        super.onPause();
        decoder.close();
        finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
        decoder.close();
        finish();
    }

    @Override
    public void onBackPressed() {

    }

    @Override
    public void onStopDecoder() {
        switchEncoding();
        //finish();
    }

    @Override
    public void onStartDecoder() {
        switchEncoding();
    }

    @Override
    public void onStartEncoder() {

    }

    @Override
    public void onStopEncoder() {


        Intent intent = new Intent(this, EditActivity.class);
        intent.putExtra("TEMP_PATH", preRenderer.getTempFilePath());
        intent.putExtra("TEMP_RATE", preRenderer.getOutputFrameRate());
        intent.putExtra("TEMP_ORIENTATION", preRenderer.gettOrientation());

        startActivity(intent);

        finish();
    }

    @Override
    public void onGetDecodedData(ByteBuffer buffer) {

    }

    private void switchEncoding(){
        mRecordingEnabled = !mRecordingEnabled;
        preSurface.queueEvent(new Runnable() {
            @Override public void run() {
                // notify the renderer that we want to change the encoder's state
                preRenderer.changeRecordingState(mRecordingEnabled);
            }
        });
    }

    class PreSurfaceRenderer implements GLSurfaceView.Renderer{
        private static final int RECORDING_OFF = 0;
        private static final int RECORDING_ON = 1;
        private static final int RECORDING_RESUMED = 2;

        private int mRecordingStatus;
        private boolean mRecordingEnabled;

        private TextureMovieEncoder mVideoEncoder;

        private SurfaceTexture mSurfaceTexture;
        private Context context;
        private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener;

        private final float[] mSTMatrix = new float[16];

        private int mTextureId;

        private Texture2dProgram mTexProgram;
        private final ScaledDrawable2d mRectDrawable =
                new ScaledDrawable2d(Drawable2d.Prefab.RECTANGLE);
        private final Sprite2d mRect = new Sprite2d(mRectDrawable);

        private float mPosX, mPosY;

        private String tPath;
        private String tempFilePath;
        private int tFrameRate;
        private int tOrientation;

        private int outputFrameRate;
        private int outputWidth;
        private int outputHeight;
        private int outputOrientation;
        private int outputBitrate;


        public PreSurfaceRenderer(Context context,
                                  SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener,
                                  TextureMovieEncoder encoder, String tPath, int tFrameRate, int tOrientation) {
            this.context = context;
            this.onFrameAvailableListener = onFrameAvailableListener;
            this.mVideoEncoder = encoder;
            this.tPath = tPath;
            this.tempFilePath = FileUtil.generateTempOutputPath();
            this.tFrameRate = tFrameRate;
            this.tOrientation = tOrientation;
        }

        public void clearEncoder(){
            mVideoEncoder = null;

        }

        public int gettOrientation(){
            return this.tOrientation;
        }

        public String getTempFilePath(){
            return this.tempFilePath;
        }

        /**
         * Notifies the renderer that we want to stop or start recording.
         */
        public void changeRecordingState(boolean isRecording) {
            Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
            mRecordingEnabled = isRecording;
        }


        public int getOutputFrameRate() {
            return outputFrameRate;
        }


        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            mRecordingEnabled = mVideoEncoder.isRecording();
            if (mRecordingEnabled) {
                mRecordingStatus = RECORDING_RESUMED;
            } else {
                mRecordingStatus = RECORDING_OFF;
            }


            mTexProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
            mTextureId = mTexProgram.createTextureObject();

            mSurfaceTexture = new SurfaceTexture(mTextureId);
            mRect.setTexture(mTextureId);

            GLES20.glViewport(0, 0, vWidth, vHeight);


            if(tOrientation == MediaDataUtil.ORIENTATION_0_AND_SMALL_WIDTH_VERTICAL){
                mPosX = vWidth/2;
                mPosY = vHeight/2;
                Matrix.orthoM(mSTMatrix, 0, 0, vWidth, 0, vHeight, -1, 1);
                //mRect.setRotation(90);
                mRect.setScale(vWidth, vHeight);
                mRect.setPosition(mPosX, mPosY);
                Log.d("PRE_ENCODE", "ORIENTATION_0_AND_SMALL_WIDTH_VERTICAL");
                //set encode meta data
                outputFrameRate = tFrameRate;
                outputWidth = 720;
                outputHeight = 1280;
                outputOrientation = 0;
                outputBitrate = 12000000;


            }else if(tOrientation == MediaDataUtil.ORIENTATION_90_AND_SMALL_HEIGHT_VERTICAL){
                mPosX = vWidth/2;
                mPosY = vHeight/2;
                Matrix.orthoM(mSTMatrix, 0, 0, vWidth, 0, vHeight, -1, 1);
                mRect.setRotation(-90);
                mRect.setScale(vHeight, vWidth);
                mRect.setPosition(mPosX, mPosY);
                Log.d("PRE_ENCODE", "ORIENTATION_90_AND_SMALL_HEIGHT_VERTICAL");

                //set encode meta data
                outputFrameRate = tFrameRate;
                outputWidth = 1280;
                outputHeight = 720;
                outputOrientation = 90;
                outputBitrate = 10000000;

            }else if(tOrientation == MediaDataUtil.ORIENTATION_0_AND_SMALL_HEIGHT_HORIZENTAL){
                Log.d("PRE_ENCODE", "ORIENTATION_0_AND_SMALL_HEIGHT_HORIZENTAL");
                mPosX = vWidth/2;
                mPosY = vHeight/2;
                Matrix.orthoM(mSTMatrix, 0, 0, vWidth, 0, vHeight, -1, 1);
                mRect.setRotation(0);
                mRect.setScale(vWidth, ((9f/16f)*vWidth));
                mRect.setPosition(mPosX, mPosY);

                //set encode meta data
                outputFrameRate = tFrameRate;
                outputWidth = 1280;
                outputHeight = 720;
                outputOrientation = 0;
                outputBitrate = 12000000;

            }else if(tOrientation == MediaDataUtil.ORIENTATION_90_AND_SMALL_WIDTH_HORIZENTAL){

            }

            startDecoding(mSurfaceTexture);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {

        }

        @Override
        public void onDrawFrame(GL10 gl) {
            mSurfaceTexture.updateTexImage();

            if(mRecordingEnabled){
                switch(mRecordingStatus){
                    case RECORDING_OFF:
                        mVideoEncoder.startRecording(new TextureMovieEncoder.EncoderConfig(tempFilePath, outputWidth, outputHeight,
                                outputBitrate, outputFrameRate, outputOrientation, tOrientation, EGL14.eglGetCurrentContext()));
                        mRecordingStatus = RECORDING_ON;
                        break;
                    case RECORDING_RESUMED:
                        mVideoEncoder.updateSharedContext(EGL14.eglGetCurrentContext());
                        mRecordingStatus = RECORDING_ON;
                        break;
                    case RECORDING_ON:
                        break;
                    default:
                        throw new RuntimeException("unknown status " + mRecordingStatus);
                }
            }else{
                switch(mRecordingStatus){
                    case RECORDING_ON:
                    case RECORDING_RESUMED:
                        Log.d(TAG, "STOP recording");
                        mVideoEncoder.stopRecording();
                        mRecordingStatus = RECORDING_OFF;
                        break;
                    case RECORDING_OFF:
                        break;
                    default:
                        throw new RuntimeException("unknown status " + mRecordingStatus);
                }
            }

            if(mRecordingEnabled){
                mVideoEncoder.setTextureId(mTextureId);
                mVideoEncoder.frameAvailable(mSurfaceTexture);

                GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                mRect.draw(mTexProgram, mSTMatrix);
            }else{
                //TODO: stop Encode
            }

        }


        private void startDecoding(SurfaceTexture surfaceTexture){

            surfaceTexture.setOnFrameAvailableListener(this.onFrameAvailableListener);
            Surface surface = new Surface(surfaceTexture);


            if(decoder.init(surface, tPath, tFrameRate)){
                decoder.start();
            }else{
                Log.d(TAG, "Init Error");
                surface.release();
                decoder.close();
            }
        }
    }
}
