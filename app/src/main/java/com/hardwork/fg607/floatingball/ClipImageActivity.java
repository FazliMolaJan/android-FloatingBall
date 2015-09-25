package com.hardwork.fg607.floatingball;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.hardwork.fg607.floatingball.utils.ClipImageView;
import com.hardwork.fg607.floatingball.utils.FloatingBallUtils;

public class ClipImageActivity extends ActionBarActivity {

    private ClipImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clip_image);

        mImageView = (ClipImageView) findViewById(R.id.src_pic);
        // 设置需要裁剪的图片
        if(FloatingBallUtils.bitmap != null) {
            mImageView.setImageBitmap(FloatingBallUtils.bitmap);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_clip_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clip) {
            // 此处获取剪裁后的bitmap,进行缩放后暂存
            FloatingBallUtils.bitmap = FloatingBallUtils.scaleBitmap(mImageView.clip(),100);
            //当前时间为图标名字
            String fileName = System.currentTimeMillis()+ ".png";
            Intent intent = new Intent();
            intent.putExtra("filename",fileName);
            setResult(FuncKeySetupActivity.REQUEST_CLIP,intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
