package com.ansen.zxingtest.camera;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

/**
 * Created by Administrator on 2017/3/18.
 */

public class CreateQRBitmp {
    /** 生成二维码图片大小 */
    private static  int QRCODE_SIZE = 300;
    /** 头像图片大小 */
    private static  int PORTRAIT_SIZE = 55;
    /** 头像图片 */
    private Bitmap portrait;
    /**
     *  功能:创建QR二维码图片
     *  可设置图片大小和头像图片大小
     *  @param portrait 头像bitmap
     *  @param content 生成二维码内容数据
     */
    public static Bitmap createQRCodeBitmap(String content, Bitmap portrait,int widthAndHeight,int portraitSize) {
        QRCODE_SIZE = widthAndHeight;
        PORTRAIT_SIZE = portraitSize;
        // 用于设置QR二维码参数
        Hashtable<EncodeHintType, Object> qrParam = new Hashtable<EncodeHintType, Object>();
        // 设置QR二维码的纠错级别——这里选择最高H级别
        qrParam.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        // 设置编码方式
        qrParam.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        // 生成QR二维码数据——这里只是得到一个由true和false组成的数组
        // 参数顺序分别为：编码内容，编码类型，生成图片宽度，生成图片高度，设置参数
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE, qrParam);

            // 开始利用二维码数据创建Bitmap图片，分别设为黑白两色
            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            int[] data = new int[w * h];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (bitMatrix.get(x, y))
                        data[y * w + x] = 0xff000000;// 黑色
                    else
                        data[y * w + x] = 0x00ffffff;// -1 相当于0xffffffff 白色
                }
            }

            // 创建一张bitmap图片，采用最高的图片效果ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            // 将上面的二维码颜色数组传入，生成图片颜色
            bitmap.setPixels(data, 0, w, 0, 0, w, h);
            if(portrait!=null){
                createQRCodeBitmapWithPortrait(bitmap,initProtrait(portrait));
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     *  功能:创建QR二维码图片
     *  头像图片大小默认
     *  @param portrait 头像bitmap
     *  @param content 生成二维码内容数据
     */
    public static Bitmap createQRCodeBitmap(String content,Bitmap portrait) {
        // 用于设置QR二维码参数
        Hashtable<EncodeHintType, Object> qrParam = new Hashtable<>();
        // 设置QR二维码的纠错级别——这里选择最高H级别
        qrParam.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        // 设置编码方式
        qrParam.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        // 生成QR二维码数据——这里只是得到一个由true和false组成的数组
        // 参数顺序分别为：编码内容，编码类型，生成图片宽度，生成图片高度，设置参数
        try {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(content,
                    BarcodeFormat.QR_CODE, QRCODE_SIZE, QRCODE_SIZE, qrParam);

            // 开始利用二维码数据创建Bitmap图片，分别设为黑白两色
            int w = bitMatrix.getWidth();
            int h = bitMatrix.getHeight();
            int[] data = new int[w * h];

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (bitMatrix.get(x, y))
                        data[y * w + x] = 0xff000000;// 黑色
                    else
                        data[y * w + x] = 0x00ffffff;// -1 相当于0xffffffff 白色
                }
            }

            // 创建一张bitmap图片，采用最高的图片效果ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            // 将上面的二维码颜色数组传入，生成图片颜色
            bitmap.setPixels(data, 0, w, 0, 0, w, h);
            if(portrait!=null){//添加最中间的logo
                createQRCodeBitmapWithPortrait(bitmap,initProtrait(portrait));
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 初始化头像图片
     */
    public static Bitmap initProtrait(Bitmap portrait) {
        // 对原有图片压缩显示大小
        Matrix mMatrix = new Matrix();
        float width = portrait.getWidth();
        float height = portrait.getHeight();
        mMatrix.setScale(PORTRAIT_SIZE / width, PORTRAIT_SIZE / height);
        return Bitmap.createBitmap(portrait, 0, 0, (int) width,
                (int) height, mMatrix, true);
    }

    /**
     * 在二维码上绘制头像
     */
    public static void createQRCodeBitmapWithPortrait(Bitmap qr, Bitmap portrait) {
        // 头像图片的大小
        int portrait_W = portrait.getWidth();
        int portrait_H = portrait.getHeight();

        // 设置头像要显示的位置，即居中显示
        int left = (QRCODE_SIZE - portrait_W) / 2;
        int top = (QRCODE_SIZE - portrait_H) / 2;
        int right = left + portrait_W;
        int bottom = top + portrait_H;
        Rect rect1 = new Rect(left, top, right, bottom);

        // 取得qr二维码图片上的画笔，即要在二维码图片上绘制我们的头像
        Canvas canvas = new Canvas(qr);

        // 设置我们要绘制的范围大小，也就是头像的大小范围
        Rect rect2 = new Rect(0, 0, portrait_W, portrait_H);
        // 开始绘制
        canvas.drawBitmap(portrait, rect2, rect1, null);
    }
}