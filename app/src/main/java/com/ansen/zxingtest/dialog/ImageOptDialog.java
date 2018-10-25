package com.ansen.zxingtest.dialog;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.ansen.zxingtest.R;

/**
 * @author apple
 * @create time 2018/4/27
 */

public class ImageOptDialog extends Dialog {
    private Context context;
    private ImageOptCallback callback;

    public ImageOptDialog(@NonNull Context context) {
        super(context, R.style.bottom_dialog);
        setContentView(R.layout.dialog_image_opt);
        this.context = context;
        Window window = getWindow();
        window.setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams params = window.getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(params);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        findViewById(R.id.tv_cancel).setOnClickListener(clickListener);
        findViewById(R.id.tv_identify_qr).setOnClickListener(clickListener);
        findViewById(R.id.tv_save_image).setOnClickListener(clickListener);

    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            dismiss();
            switch (view.getId()) {
                case R.id.tv_identify_qr:
                    if (callback!=null){
                        callback.onIdentifyQrClick();
                    }
                    dismiss();
                    break;
                case R.id.tv_cancel:
                    dismiss();
                    break;
                case R.id.tv_save_image:
                    if (callback!=null){
                        callback.onSaveImageClick();
                    }
                    dismiss();
                    break;
            }
        }
    };

    public void setCallback(ImageOptCallback callback) {
        this.callback = callback;
    }

    public interface  ImageOptCallback{
        void onIdentifyQrClick();//识别二维码

        void onSaveImageClick();//保存图片
    }
}
