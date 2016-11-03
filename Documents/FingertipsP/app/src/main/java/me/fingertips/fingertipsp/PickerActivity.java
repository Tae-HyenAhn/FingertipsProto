package me.fingertips.fingertipsp;

import android.Manifest;
import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Toast;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.util.ArrayList;

import me.fingertips.fingertipsp.adapter.PickerItemAdapter;
import me.fingertips.fingertipsp.data.VideoData;
import me.fingertips.fingertipsp.utils.MediaDataUtil;

public class PickerActivity extends AppCompatActivity {

    private RecyclerView picker;
    private ArrayList<VideoData> dataList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new TedPermission(this).setPermissionListener(permissionlistener).
                setDeniedMessage("If you reject permission,you can not use this service\\n\\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .check();

        init();
    }

    private void init(){
        picker = (RecyclerView)findViewById(R.id.picker);
        picker.setLayoutManager(new GridLayoutManager(this, 3));

        dataList = new ArrayList<VideoData>();
        //final ProgressDialog pd = ProgressDialog.show(this, "Loading", "Get Video Data...", true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                dataList = MediaDataUtil.getVideoData(getApplicationContext());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        PickerItemAdapter adapter = new PickerItemAdapter(dataList, getApplicationContext());
                        picker.setAdapter(adapter);
                        //pd.dismiss();

                    }
                });
            }
        }).start();

    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Toast.makeText(PickerActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(PickerActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
        }


    };


}
