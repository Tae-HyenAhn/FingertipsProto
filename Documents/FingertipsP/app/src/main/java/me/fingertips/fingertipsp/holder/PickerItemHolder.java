package me.fingertips.fingertipsp.holder;

import android.content.Context;
import android.graphics.Point;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import me.fingertips.fingertipsp.R;

/**
 * Created by sb on 16. 10. 24..
 */
public class PickerItemHolder extends RecyclerView.ViewHolder {

    private View itemView;
    private ImageView thumbImg;
    private Context con;

    public PickerItemHolder(View itemView, Context con) {
        super(itemView);
        this.itemView = itemView;
        this.thumbImg = (ImageView)itemView.findViewById(R.id.item_picker_thumb);
        this.con = con;
    }

    public View getItemView(){
        return this.itemView;
    }

    public void setThumbnail(String path){
        int gridWidth = getGridWidth(con);

        Glide.with(this.itemView.getContext()).
                load(path).thumbnail(0.1f).
                centerCrop().override(gridWidth, gridWidth).into(this.thumbImg);
    }

    private int getGridWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int displayWidth = dm.widthPixels;

        return displayWidth / 3;
    }


}
