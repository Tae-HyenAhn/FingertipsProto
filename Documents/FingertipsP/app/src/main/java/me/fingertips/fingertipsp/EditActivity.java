package me.fingertips.fingertipsp;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import me.fingertips.fingertipsp.decoder.EditDecoder;
import me.fingertips.fingertipsp.encoder.EditTextureMovieEncoder;
import me.fingertips.fingertipsp.encoder.TextureMovieEncoder;
import me.fingertips.fingertipsp.gles.Drawable2d;
import me.fingertips.fingertipsp.gles.ScaledDrawable2d;
import me.fingertips.fingertipsp.gles.Sprite2d;
import me.fingertips.fingertipsp.gles.Texture2dProgram;
import me.fingertips.fingertipsp.utils.FileUtil;
import me.fingertips.fingertipsp.utils.MediaDataUtil;

public class EditActivity extends AppCompatActivity implements View.OnClickListener,
        EditDecoder.OnDecodeListener, EditTextureMovieEncoder.OnEncodeListener{

    private static final String TAG = "EditActivity";

    private Button repeatBtn, slowBtn, fastBtn, completeBtn;
    private ImageButton reverseBtn, normalBtn;
    private EditText repeatTimeET, repeatCountET, repeatRSET, repeatFSET;

    private GLSurfaceView editSurface;
    private EditSurfaceRenderer renderer;

    private int vWidth, vHeight;
    private String tempFilePath;

    private EditDecoder editDecoder;

    private int videoFrameRate;

    private int videoOrientation;

    private EditTextureMovieEncoder encoder;
    private boolean mRecordingEnabled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        init();
    }

    private void init(){

        Intent intent = getIntent();
        tempFilePath = intent.getExtras().getString("TEMP_PATH");
        videoFrameRate = intent.getExtras().getInt("TEMP_RATE");
        videoOrientation = intent.getExtras().getInt("TEMP_ORIENTATION");

        DisplayMetrics dm = getApplicationContext().getResources().getDisplayMetrics();
        vWidth = dm.widthPixels;
        vHeight = dm.heightPixels;

        encoder = new EditTextureMovieEncoder();
        mRecordingEnabled = encoder.isRecording();
        encoder.setOnEncodeListener(this);
        editDecoder = new EditDecoder();
        editDecoder.setOnDecoderListener(this);
        editSurface = (GLSurfaceView)findViewById(R.id.edit_surface);
        editSurface.setEGLContextClientVersion(2);
        editSurface.setZOrderOnTop(false);
        renderer = new EditSurfaceRenderer(this, onFrameAvailableListener, encoder, tempFilePath, videoFrameRate, videoOrientation);
        editSurface.setRenderer(renderer);

        repeatBtn = (Button)findViewById(R.id.edit_go_repeat);
        repeatBtn.setOnClickListener(this);
        completeBtn = (Button)findViewById(R.id.edit_complete);
        completeBtn.setOnClickListener(this);
        slowBtn = (Button)findViewById(R.id.edit_slow);
        slowBtn.setOnTouchListener(slowTouchListener);
        fastBtn = (Button)findViewById(R.id.edit_fast);
        fastBtn.setOnTouchListener(fastTouchListener);
        reverseBtn = (ImageButton)findViewById(R.id.edit_reverse);
        reverseBtn.setOnTouchListener(reverseTouchListener);
        normalBtn = (ImageButton)findViewById(R.id.edit_normal);
        normalBtn.setOnTouchListener(normalTouchListener);
        repeatTimeET = (EditText)findViewById(R.id.edit_repeat_time);
        repeatCountET = (EditText)findViewById(R.id.edit_repeat_count);
        repeatRSET = (EditText)findViewById(R.id.edit_repeat_rspeed);
        repeatFSET = (EditText)findViewById(R.id.edit_repeat_fspeed);

    }

    private int getNumEditText(EditText et){
        if(et.getText().toString() != "")
            return Integer.parseInt(et.getText().toString());
        else
            return -1;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id){
            case R.id.edit_go_repeat:
                editDecoder.repeatPlay(Long.parseLong(repeatTimeET.getText().toString()), Integer.parseInt(repeatCountET.getText().toString()),
                            Integer.parseInt(repeatFSET.getText().toString()), Integer.parseInt(repeatRSET.getText().toString()));
                break;
            case R.id.edit_complete:
                switchEncoding();
                break;
        }
    }

    private View.OnTouchListener normalTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            switch (action){
                case MotionEvent.ACTION_DOWN:
                    editDecoder.play();
                    break;
                case MotionEvent.ACTION_UP:
                    editDecoder.pause();
                    break;
            }

            return false;
        }
    };

    private View.OnTouchListener reverseTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            switch (action){
                case MotionEvent.ACTION_DOWN:
                    editDecoder.reversePlay();
                    break;
                case MotionEvent.ACTION_UP:
                    editDecoder.pause();
                    break;
            }

            return false;
        }
    };

    private View.OnTouchListener fastTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            switch (action){
                case MotionEvent.ACTION_DOWN:
                    editDecoder.playFast();
                    break;
                case MotionEvent.ACTION_UP:
                    editDecoder.pause();
                    break;
            }

            return false;
        }
    };

    private View.OnTouchListener slowTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();

            switch (action){
                case MotionEvent.ACTION_DOWN:
                    editDecoder.playSlow();
                    break;
                case MotionEvent.ACTION_UP:
                    editDecoder.pause();
                    break;
            }

            return false;
        }
    };

    private void switchEncoding(){
        mRecordingEnabled = !mRecordingEnabled;
        editSurface.queueEvent(new Runnable() {
            @Override public void run() {
                // notify the renderer that we want to change the encoder's state
                renderer.changeRecordingState(mRecordingEnabled);
            }
        });
    }

    @Override
    public void onStartEncoder() {

    }

    @Override
    public void onStopEncoder() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(renderer.getOutputFilePath()), "video/*");
        startActivity(intent);
        finish();
    }

    @Override
    public void onStopDecoder() {
        //switchEncoding();
    }

    @Override
    public void onStartDecoder() {
        switchEncoding();
    }

    private SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener = new SurfaceTexture.OnFrameAvailableListener() {
        @Override
        public void onFrameAvailable(SurfaceTexture surfaceTexture) {
            editSurface.requestRender();
        }
    };

    private class EditSurfaceRenderer implements GLSurfaceView.Renderer{
        private static final int RECORDING_OFF = 0;
        private static final int RECORDING_ON = 1;
        private static final int RECORDING_RESUMED = 2;

        private int mRecordingStatus;
        private boolean mRecordingEnabled;

        private EditTextureMovieEncoder mVideoEncoder;

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
        private String outputFilePath;
        private int videoFrameRate;
        private int videoOrientation;

        private int outputFrameRate;
        private int outputWidth;
        private int outputHeight;
        private int outputRotation;
        private int outputBitrate;

        public EditSurfaceRenderer(Context context, SurfaceTexture.OnFrameAvailableListener onFrameAvailableListener,
                                   EditTextureMovieEncoder encoder, String tPath, int videoFrameRate, int videoOrientation) {
            this.context = context;
            this.onFrameAvailableListener = onFrameAvailableListener;
            this.mVideoEncoder = encoder;
            this.tPath = tPath;
            this.outputFilePath = FileUtil.generateOutputVideoPath();
            this.videoFrameRate = videoFrameRate;
            this.videoOrientation = videoOrientation;
        }

        public String getOutputFilePath(){
            return this.outputFilePath;
        }

        /**
         * Notifies the renderer that we want to stop or start recording.
         */
        public void changeRecordingState(boolean isRecording) {
            Log.d(TAG, "changeRecordingState: was " + mRecordingEnabled + " now " + isRecording);
            mRecordingEnabled = isRecording;
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

            if(videoOrientation == MediaDataUtil.ORIENTATION_0_AND_SMALL_WIDTH_VERTICAL){
                Log.d("ORIEN", "ORIENTATION_0_AND_SMALL_WIDTH_VERTICAL");
                mPosX = vWidth/2;
                mPosY = vHeight/2;
                Matrix.orthoM(mSTMatrix, 0, 0, vWidth, 0, vHeight, -1, 1);

                mRect.setScale(vWidth, vHeight);
                mRect.setPosition(mPosX, mPosY);

                outputFrameRate = this.videoFrameRate;
                outputWidth = 720;
                outputHeight = 1280;
                outputRotation = 0;
                outputBitrate = 10000000;


            }else if(videoOrientation == MediaDataUtil.ORIENTATION_90_AND_SMALL_HEIGHT_VERTICAL){
                Log.d("ORIEN", "ORIENTATION_90_AND_SMALL_HEIGHT_VERTICAL");
                mPosX = vWidth/2;
                mPosY = vHeight/2;
                Matrix.orthoM(mSTMatrix, 0, 0, vWidth, 0, vHeight, -1, 1);
                mRect.setRotation(-90);
                //mRect.setScale(vHeight, vWidth);
                mRect.setScale(vHeight, vWidth);
                mRect.setPosition(mPosX, mPosY);

                outputFrameRate = this.videoFrameRate;
                outputWidth = 1280;
                outputHeight = 720;
                outputRotation = 90;
                outputBitrate = 10000000;

            }else if(videoOrientation == MediaDataUtil.ORIENTATION_0_AND_SMALL_HEIGHT_HORIZENTAL){
                Log.d("ORIEN", "ORIENTATION_0_AND_SMALL_HEIGHT_HORIZENTAL");
                mPosX = vWidth/2;
                mPosY = vHeight/2;
                Matrix.orthoM(mSTMatrix, 0, 0, vWidth, 0, vHeight, -1, 1);
                mRect.setRotation(0);
                mRect.setScale(vWidth, ((9f/16f)*vWidth));
                mRect.setPosition(mPosX, mPosY);

                outputFrameRate = this.videoFrameRate;
                outputWidth = 1280;
                outputHeight = 720;
                outputRotation = 0;
                outputBitrate = 12000000;

            }else if(videoOrientation == MediaDataUtil.ORIENTATION_90_AND_SMALL_WIDTH_HORIZENTAL){
                Log.d("ORIEN", "ORIENTATION_90_AND_SMALL_WIDTH_HORIZENTAL");
            }

            /*
            mPosX = vWidth/2;
            mPosY = vHeight/2;
            Matrix.orthoM(mSTMatrix, 0, 0, vWidth, 0, vHeight, -1, 1);
            mRect.setRotation(-90);
            mRect.setScale(vHeight, vWidth);
            mRect.setPosition(mPosX, mPosY);*/

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
                        mVideoEncoder.startRecording(new EditTextureMovieEncoder.EncoderConfig(outputFilePath, outputWidth, outputHeight,
                                outputBitrate, outputFrameRate, outputRotation, EGL14.eglGetCurrentContext()));
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

            if(mRecordingEnabled) {
                mVideoEncoder.setTextureId(mTextureId);
                mVideoEncoder.frameAvailable(mSurfaceTexture);
            }
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

            mRect.draw(mTexProgram, mSTMatrix);



        }

        private void startDecoding(SurfaceTexture surfaceTexture){
            surfaceTexture.setOnFrameAvailableListener(this.onFrameAvailableListener);
            Surface surface = new Surface(surfaceTexture);

            if(editDecoder.init(surface, tPath, videoFrameRate)){
                editDecoder.start();
            }else{
                surface.release();
                editDecoder.close();
            }
        }
    }

}
