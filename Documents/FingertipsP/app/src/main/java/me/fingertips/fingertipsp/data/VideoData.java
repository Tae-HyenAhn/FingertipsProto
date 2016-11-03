package me.fingertips.fingertipsp.data;

/**
 * Created by sb on 16. 10. 24..
 */
public class VideoData {
    private String videoPath, videoResolution;
    private String videoDuration, thumbPath;
    private String videoRotation;
    private int videoOrientation;


    public VideoData(String videoPath, String videoDuration, String videoResolution, String thumbPath, String videoRotation, int videoOrientation) {
        this.videoPath = videoPath;
        this.videoDuration = videoDuration;
        this.videoResolution = videoResolution;
        this.thumbPath = thumbPath;
        this.videoRotation = videoRotation;
        this.videoOrientation = videoOrientation;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public String getVideoDuration() {
        return videoDuration;
    }

    public String getVideoResolution() {
        return videoResolution;
    }

    public String getThumbPath() { return thumbPath; }

    public String getVideoRotation()    {   return videoRotation;    }

    public int getVideoOrientation()    { return videoOrientation; }
}
