package me.fingertips.fingertipsp.utils;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;

import me.fingertips.fingertipsp.data.VideoData;

/**
 * Created by sb on 16. 10. 24..
 */
public class MediaDataUtil {

    public static final int ORIENTATION_0_AND_SMALL_WIDTH_VERTICAL = 10;
    public static final int ORIENTATION_0_AND_SMALL_HEIGHT_HORIZENTAL = 20;
    public static final int ORIENTATION_90_AND_SMALL_HEIGHT_VERTICAL = 11;
    public static final int ORIENTATION_90_AND_SMALL_WIDTH_HORIZENTAL = 21;

    /**
     *
     * @param rotation
     * @param resolution
     * @return orientation 0=가로, 1=세로, -1=못찾음
     */
    public static int getVideoOrientation(String rotation, String resolution){

        int mRotation = Integer.parseInt(rotation);

        int width=0, height=0;
        if(resolution != null && resolution != ""){
            String[] arrayResolution = resolution.split("x");
            width = Integer.parseInt(arrayResolution[0]);
            height = Integer.parseInt(arrayResolution[1]);
        }


        if(mRotation == 0 && height > width){
            return ORIENTATION_0_AND_SMALL_WIDTH_VERTICAL;
        }else if(mRotation == 0 && height < width){
            return ORIENTATION_0_AND_SMALL_HEIGHT_HORIZENTAL;
        }else if(mRotation == 90 && height < width){
            return ORIENTATION_90_AND_SMALL_HEIGHT_VERTICAL;
        }else if(mRotation == 90 && height > width){
            return ORIENTATION_90_AND_SMALL_WIDTH_HORIZENTAL;
        }else{
            return -1;
        }
    }

    public static String getVideoRotation(String path){
        MediaMetadataRetriever m = new MediaMetadataRetriever();
        String rotation = "0";

        try{
            m.setDataSource(path);
            rotation = m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        }catch(RuntimeException re){
            re.printStackTrace();
        }

        m.release();

        return rotation;
    }

    public static long getVideoFrameRate(String path){

        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(path);
            for( int i = 0; i < extractor.getTrackCount(); i++){
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if(mime.startsWith("video/")){
                    extractor.selectTrack(i);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        extractor.advance();
        extractor.advance();
        extractor.advance();
        long time = (extractor.getSampleTime())/3;
        long frameRate;
        Log.d("UTIL","TIME: "+time);
        if(time != 0){
            frameRate = 1000000/time;
            Log.d("UTIL","RATE: "+frameRate);
        }else{
            Log.d("UTIL","RATE: 0000");
            frameRate = 1000000/30;
        }

        extractor.release();

        if(frameRate == 0){
            return 30;
        }else{
            return frameRate;
        }

    }

    private static String path, duration, resolution, thumbPath, rotation;
    private static int orientation;
    public static ArrayList<VideoData> getVideoData(Context context) {
        ArrayList<VideoData> list = new ArrayList<VideoData>();
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Video.VideoColumns.DATA,
                MediaStore.Video.VideoColumns.DURATION, MediaStore.Video.VideoColumns.RESOLUTION };
        Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
        int vidsCount = 0;
        if (c != null) {
            vidsCount = c.getCount();
            c.moveToLast();
            do {
                path = c.getString(0);
                duration = c.getString(1);
                resolution = c.getString(2);
                thumbPath = getThumbnailPath(context, path);
                rotation = MediaDataUtil.getVideoRotation(path);
                if(resolution != null && resolution != "")
                    orientation = MediaDataUtil.getVideoOrientation(rotation, resolution);
                else
                    orientation = -1;
                //Log.d("ORIENTATION", orientation);
                list.add(new VideoData(path, duration, resolution, thumbPath, rotation, orientation));
            }while(c.moveToPrevious());
            c.close();
            Log.d("VIDEO_FIND_END", "COUNT :"+vidsCount);
        }

        return list;
    }


    private static String getThumbnailPath(Context context, String videoPath)
    {
        Uri video = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{MediaStore.Video.Media._ID};
        String where = MediaStore.Video.Media.DATA+"=?";
        String[] whereArgs = new String[]{videoPath};
        Cursor media = context.getContentResolver().query(video, projection, where, whereArgs, null);

        if(!media.moveToFirst()){

            return null;
        }

        String videoId = media.getString(0);

        Uri thumbnail = MediaStore.Video.Thumbnails.EXTERNAL_CONTENT_URI;
        projection = new String[]{MediaStore.Video.Thumbnails.DATA};
        where = MediaStore.Video.Thumbnails.VIDEO_ID+"=?";
        whereArgs = new String[]{videoId};
        Cursor thumb = context.getContentResolver().query(thumbnail, projection, where, whereArgs, null);

        if(!thumb.moveToFirst()){
            return null;
        }

        String thumnailPath = thumb.getString(0);

        return thumnailPath;
    }
}
