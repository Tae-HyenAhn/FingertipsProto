package me.fingertips.fingertipsp.decoder;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

import me.fingertips.fingertipsp.encoder.EditTextureMovieEncoder;
import me.fingertips.fingertipsp.encoder.TextureMovieEncoder;

/**
 * Created by sb on 16. 10. 28..
 */
public class EditDecoder extends Thread {
    private static final String VIDEO = "video/";
    private static final String TAG = "VideoDecoder";
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;

    private boolean eosReceived;
    private int frameRate;

    private boolean isSpeedMode = false;

    private boolean isPause;
    private boolean isReverse;
    private boolean isSlow;
    private boolean isFast;

    private boolean isRepeat = false;
    private boolean repeatFirst = false;
    private long repeatFirstTime;
    private long repeatTime;
    private boolean isRepeatReverse = false;
    private boolean isRepeatFoward = false;

    private int slow_rate = 3;
    private int fast_rate = 3;
    private int repeatCount = 1;
    private int repeat_foward_speed = 3;
    private int repeat_reverse_speed = 3;
    private boolean isRFSlow;
    private boolean isRRSlow;

    private OnDecodeListener listener;

    public boolean init(Surface surface, String filePath, int frameRate){

        eosReceived = false;
        this.frameRate = frameRate;

        isPause = false;
        isReverse = false;
        isSlow = false;
        isFast = false;
        isRepeat = false;

        try{
            mExtractor = new MediaExtractor();
            mExtractor.setDataSource(filePath);

            for(int i=0; i< mExtractor.getTrackCount(); i++){
                MediaFormat format = mExtractor.getTrackFormat(i);

                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith(VIDEO)){
                    mExtractor.selectTrack(i);
                    mDecoder = MediaCodec.createDecoderByType(mime);
                    try{
                        mDecoder.configure(format, surface, null, 0);

                    }catch (IllegalStateException e){
                        e.printStackTrace();
                        return false;
                    }

                    if(listener != null){
                        listener.onStartDecoder();
                    }
                    mDecoder.start();
                    break;
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        return true;
    }

    public void setOnDecoderListener(OnDecodeListener listener){
        this.listener = listener;
    }


    public void pause(){
        isPause = true;
    }

    public void play(){
        slowCount = 0;
        isSpeedMode = false;
        isFast = false;
        isSlow = false;
        isReverse = false;
        isPause = false;

    }

    public void reversePlay(){
        slowCount = 0;
        isSpeedMode = false;
        isFast = false;
        isSlow = false;
        isReverse = true;
        isPause = false;

    }

    public void playSlow(){
        slowCount = 0;
        isSpeedMode = true;
        isFast = false;
        isSlow = true;
        isReverse = false;
        isPause = false;

    }

    public void playFast(){
        slowCount = 0;
        isSpeedMode = true;
        isFast = true;
        isSlow = false;
        isReverse = false;
        isPause = false;

    }

    public void repeatPlay(long mRepeatTime, int mRepeatCount, int mFowardSpeed, int mReverseSpeed){
        repeatTime = mRepeatTime * 1000;
        repeatCount = mRepeatCount;
        slowCount = 0;
        if(mFowardSpeed < 0){
            repeat_foward_speed = Math.abs(mFowardSpeed);
            isRFSlow = true;
        }else if(mFowardSpeed > 0){
            repeat_foward_speed = mFowardSpeed;
            isRFSlow = false;
        }else{
            mFowardSpeed = 1;
            repeat_foward_speed = mFowardSpeed;
            isRFSlow = false;
        }

        if(mReverseSpeed < 0){
            repeat_reverse_speed = Math.abs(mReverseSpeed);
            isRRSlow = true;
        }else if(mReverseSpeed > 0){
            repeat_reverse_speed = mReverseSpeed;
            isRRSlow = false;
        }else{
            mReverseSpeed = 1;
            repeat_reverse_speed = mReverseSpeed;
            isRRSlow = false;
        }

        isRepeat = true;
        isRepeatReverse = true;
        isRepeatFoward = false;
        repeatFirst = true;
        isPause = false;
        isSpeedMode = false;

    }

    private long slowCount = 0;
    @Override
    public void run() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();

        boolean first = false;
        long startWhen = 0;
        boolean isInput = true;
        long nowTime = 0;

        while(!eosReceived){


            while(true){
                EditTextureMovieEncoder.stopSign();
                if(!isPause){
                    EditTextureMovieEncoder.resumeSign();
                    break;
                }
            }

            if(isInput){
                int inputIndex = mDecoder.dequeueInputBuffer(10000);
                if(inputIndex >= 0){
                    ByteBuffer inputBuffer = inputBuffers[inputIndex];

                    int sampleSize = mExtractor.readSampleData(inputBuffer, 0);

                    if(sampleSize > 0){

                        if(!isRepeat){
                            if(!isReverse){
                                long presentationTimeUs = mExtractor.getSampleTime();

                                nowTime = presentationTimeUs;
                                mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                                if(isSpeedMode){
                                    if(isSlow){
                                        slowCount++;
                                        if(slowCount % slow_rate == 0){
                                            mExtractor.advance();
                                        }
                                    }

                                    if(isFast){
                                        if(fast_rate > 0){
                                            for(int i=1; i<fast_rate; i++){
                                                mExtractor.advance();
                                            }
                                        }
                                    }
                                }else if(!isSpeedMode){
                                    mExtractor.advance();

                                }

                            }else{
                                mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, nowTime, 0);
                                mExtractor.seekTo(nowTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                                nowTime = nowTime - (1000000/frameRate);

                                if(nowTime<0){
                                    mExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
                                    isReverse = false;
                                }
                            }
                        }else if(isRepeat){
                            if(repeatFirst){
                                repeatFirstTime = nowTime;
                                repeatFirst = false;
                            }
                            if(isRepeatReverse){
                                if(Math.abs(repeatTime) <= Math.abs(repeatFirstTime-nowTime)){
                                    isRepeatReverse = false;
                                    isRepeatFoward = true;
                                    Log.d("PEAK", "PEAK");
                                }else{
                                    //nowTime = mExtractor.getSampleTime();
                                    if(isRRSlow){
                                        nowTime = nowTime - ((1000000/frameRate)/repeat_reverse_speed);
                                    }else{
                                        nowTime = nowTime - ((1000000/frameRate)*repeat_reverse_speed);
                                    }
                                    mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, nowTime, 0);
                                    mExtractor.seekTo(nowTime, MediaExtractor.SEEK_TO_CLOSEST_SYNC);



                                }
                            }

                            if(isRepeatFoward){

                                long presentationTimeUs = mExtractor.getSampleTime();

                                nowTime = presentationTimeUs;


                                mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, presentationTimeUs, 0);
                                if(isRFSlow){
                                    slowCount++;
                                    if(slowCount%repeat_foward_speed == 0){
                                        mExtractor.advance();
                                    }
                                }else{
                                    for(int i=0; i<repeat_foward_speed; i++){
                                        mExtractor.advance();
                                    }
                                }



                                if(repeatFirstTime < mExtractor.getSampleTime()){

                                    if(repeatCount > 1){
                                        isRepeatReverse = true;
                                        repeatFirst = true;
                                        repeatCount--;
                                    }else{
                                        isRepeat = false;
                                        isPause = true;
                                    }
                                    isRepeatFoward = false;
                                    repeatFirstTime = 0;

                                }
                            }

                        }

                        //mDecoder.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);

                    } else {
                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");

                        mDecoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isInput = false;
                    }
                }
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    mDecoder.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    break;

                default:
                    if(!first){
                        startWhen = System.currentTimeMillis();
                    }

                    try {
                        //long sleepTime = (info.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                        //if(sleepTime < 0){
                        long sleepTime = 1000/frameRate;
                        //}
                        //long sleepTime = 1000/30;
                        Log.d("FRATE", frameRate+"-fRATE");
                        Log.d(TAG, "info.presentationTimeUs : " + (info.presentationTimeUs / 1000) + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime);

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    mDecoder.releaseOutputBuffer(outIndex, true);

                    if(!first){
                        isPause = true;
                        first = true;
                    }
                    break;
            }

            if((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0){
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                if(listener != null){
                    listener.onStopDecoder();
                }
                break;
            }
        }

        mDecoder.stop();
        mDecoder.release();
        mExtractor.release();
    }

    public void close() {   eosReceived = false;   }

    public interface OnDecodeListener{
        void onStopDecoder();
        void onStartDecoder();
    }

}
