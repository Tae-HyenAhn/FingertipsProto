package me.fingertips.fingertipsp.adapter;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.List;

import me.fingertips.fingertipsp.EditActivity;
import me.fingertips.fingertipsp.PreDecodePopupActivity;
import me.fingertips.fingertipsp.R;
import me.fingertips.fingertipsp.data.VideoData;
import me.fingertips.fingertipsp.decoder.PreDecoder;
import me.fingertips.fingertipsp.holder.PickerItemHolder;
import me.fingertips.fingertipsp.utils.MediaDataUtil;

/**
 * Created by sb on 16. 10. 24..
 */
public class PickerItemAdapter extends RecyclerView.Adapter<PickerItemHolder> {
    private List<VideoData> itemList;
    private Context context;
    private PreDecoder decoder;

    public PickerItemAdapter(List<VideoData> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
        decoder = new PreDecoder();
    }

    @Override
    public PickerItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View layoutPickerItem = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_picker_layout, parent, false);
        return new PickerItemHolder(layoutPickerItem, context);
    }

    @Override
    public void onBindViewHolder(PickerItemHolder holder, final int position) {
        holder.setThumbnail(itemList.get(position).getThumbPath());
        holder.getItemView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        decoder.init(itemList.get(position).getVideoPath(), null);
                        decoder.start();

                    }
                }).start();
                */


                int frameRate = (int) MediaDataUtil.getVideoFrameRate(itemList.get(position).getVideoPath());

                Intent intent = new Intent(context, PreDecodePopupActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("PATH", itemList.get(position).getVideoPath());
                intent.putExtra("FRAME_RATE", frameRate);
                intent.putExtra("ORIENTATION", itemList.get(position).getVideoOrientation());
                Log.d("FRAME_RATE", frameRate+"-frame_rate");
                context.startActivity(intent);


                //Toast.makeText(context, "ORIENTATION: "+itemList.get(position).getVideoOrientation(), Toast.LENGTH_SHORT).show();

            }
        });
    }

    @Override
    public int getItemCount() {
        return (null == itemList) ? 0 : itemList.size();
    }
}
