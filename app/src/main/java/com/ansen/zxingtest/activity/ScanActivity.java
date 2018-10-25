package com.ansen.zxingtest.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ansen.zxingtest.R;
import com.ansen.zxingtest.camera.CameraManager;
import com.ansen.zxingtest.view.ScannerView;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.EnumMap;
import java.util.Map;

/**
 * 扫描二维码
 * @author apple
 * @create time 2017-10-16
 */
public class ScanActivity extends AppCompatActivity implements SurfaceHolder.Callback,
        CompoundButton.OnCheckedChangeListener {
    public static final String INTENT_EXTRA_RESULT = "result";
    public static final int FromGalleryRequestCode = 1606;

    private static final long VIBRATE_DURATION = 50L;
    private static final long AUTO_FOCUS_INTERVAL_MS = 2500L;

    private final CameraManager cameraManager = new CameraManager();
    protected ScannerView scannerView;
    private SurfaceHolder surfaceHolder;
    protected FrameLayout flOverlayContainer;
    private Vibrator vibrator;
    private HandlerThread cameraThread;
    private Handler cameraHandler;

    private boolean fromGallery;

    private static boolean DISABLE_CONTINUOUS_AUTOFOCUS = Build.MODEL.equals("GT-I9100") //
            // Galaxy S2
            || Build.MODEL.equals("SGH-T989") // Galaxy S2
            || Build.MODEL.equals("SGH-T989D") // Galaxy S2 X
            || Build.MODEL.equals("SAMSUNG-SGH-I727") // Galaxy S2 Skyrocket
            || Build.MODEL.equals("GT-I9300") // Galaxy S3
            || Build.MODEL.equals("GT-N7000"); // Galaxy Note

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_scan);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        flOverlayContainer = findViewById(R.id.fl_overlay_container);
        scannerView = findViewById(R.id.scan_activity_mask);
        ((CheckBox) findViewById(R.id.cbx_torch)).setOnCheckedChangeListener(this);
        fromGallery = false;

        ImageView ivTopLeft=findViewById(R.id.iv_top_left);
        ivTopLeft.setImageResource(R.mipmap.icon_back_black);
        findViewById(R.id.view_top_left).setOnClickListener(onClickListener);

        TextView txtTopCenter=findViewById(R.id.txt_top_center);
        txtTopCenter.setText(R.string.qr_code_text);

        TextView tvTopRight=findViewById(R.id.tv_top_right);
        tvTopRight.setText(R.string.album_text);
        findViewById(R.id.view_top_right).setOnClickListener(onClickListener);
    }

    public void setOverlay(View v) {
        flOverlayContainer.removeAllViews();
        flOverlayContainer.addView(v, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    public void setOverlay(int resource) {
        setOverlay(LayoutInflater.from(this).inflate(resource, null));
    }

    @Override
    protected void onResume() {
        super.onResume();

        cameraThread = new HandlerThread("cameraThread", Process.THREAD_PRIORITY_BACKGROUND);
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());

        final SurfaceView surfaceView = (SurfaceView) findViewById(R.id.scan_activity_preview);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(final SurfaceHolder holder) {
        cameraHandler.post(openRunnable);
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(final SurfaceHolder holder, final int format,final int width, final int height) {

    }

    @Override
    protected void onPause() {
        cameraHandler.post(closeRunnable);

        surfaceHolder.removeCallback(this);

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.view_top_left:
                    finish();
                    break;
                case R.id.view_top_right:
                    fromGallery = true;
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, FromGalleryRequestCode);
                    break;
            }
        }
    };
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
                // don't launch camera app
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                cameraHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        cameraManager.setTorch(keyCode == KeyEvent.KEYCODE_VOLUME_UP);
                    }
                });
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void handleResult(final Result scanResult, Bitmap thumbnailImage,
                             final float thumbnailScaleFactor) {
        vibrate();
        // superimpose dots to highlight the key features of the qr code
        final ResultPoint[] points = scanResult.getResultPoints();
        if (points != null && points.length > 0) {
            final Paint paint = new Paint();
            paint.setColor(getResources().getColor(R.color.scan_result_dots));
            paint.setStrokeWidth(10.0f);

            final Canvas canvas = new Canvas(thumbnailImage);
            canvas.scale(thumbnailScaleFactor, thumbnailScaleFactor);
            for (final ResultPoint point : points)
                canvas.drawPoint(point.getX(), point.getY(), paint);
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        thumbnailImage = Bitmap.createBitmap(thumbnailImage, 0, 0,
                thumbnailImage.getWidth(), thumbnailImage.getHeight(), matrix,
                false);
        scannerView.drawResultBitmap(thumbnailImage);

        final Intent result = getIntent();
        Log.i("ansen","扫描结果:"+scanResult.getText());
        result.putExtra(INTENT_EXTRA_RESULT,scanResult.getText());
        setResult(RESULT_OK, result);

        // delayed finish
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    public void vibrate() {
        vibrator.vibrate(VIBRATE_DURATION);
    }

    private final Runnable openRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                final Camera camera = cameraManager.open(surfaceHolder,
                        !DISABLE_CONTINUOUS_AUTOFOCUS);

                final Rect framingRect = cameraManager.getFrame();
                final Rect framingRectInPreview = cameraManager
                        .getFramePreview();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        scannerView.setFraming(framingRect,
                                framingRectInPreview);
                    }
                });

                final String focusMode = camera.getParameters().getFocusMode();
                final boolean nonContinuousAutoFocus = Camera.Parameters.FOCUS_MODE_AUTO
                        .equals(focusMode)
                        || Camera.Parameters.FOCUS_MODE_MACRO.equals(focusMode);

                if (nonContinuousAutoFocus)
                    cameraHandler.post(new AutoFocusRunnable(camera));

                cameraHandler.post(fetchAndDecodeRunnable);
            } catch (final IOException x) {
                Log.i("problem opening camera", x.toString());
                finish();
            } catch (final RuntimeException x) {
                Log.i("problem opening camera", x.toString());
                finish();
            }
        }
    };

    private final Runnable closeRunnable = new Runnable() {
        @Override
        public void run() {
            cameraManager.close();

            // cancel background thread
            cameraHandler.removeCallbacksAndMessages(null);
            cameraThread.quit();
        }
    };

    @Override
    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
        if (buttonView.getId() == R.id.cbx_torch) {
            if (cameraHandler == null) {
                return;
            }
            cameraHandler.post(new Runnable() {
                @Override
                public void run() {
                    cameraManager
                            .setTorch(isChecked);
                }
            });
        }
    }

    private final class AutoFocusRunnable implements Runnable {
        private final Camera camera;

        public AutoFocusRunnable(final Camera camera) {
            this.camera = camera;
        }

        @Override
        public void run() {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(final boolean success,
                                        final Camera camera) {
                    // schedule again
                    cameraHandler.postDelayed(AutoFocusRunnable.this,
                            AUTO_FOCUS_INTERVAL_MS);
                }
            });
        }
    }

    private View.OnClickListener galleryClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            fromGallery = true;
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, FromGalleryRequestCode);
//            overridePendingTransition(0,R.anim.scanner_in_exit);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        /*int foregroundCount = getForegroundCount();
        Log.i("requestCode","onActivityResult:"+foregroundCount);
        setForegroundCount(1);*/
        if (requestCode == FromGalleryRequestCode) {
//            overridePendingTransition(R.anim.scanner_out_enter, 0);
            if (resultCode == RESULT_OK) {
                fromGallery = true;
                /*final DialogProgress dp = new DialogProgress(this, R.string.please_wait);
                dp.show();*/
                new Thread() {
                    @Override
                    public void run() {
                        String text = null;
                        Uri uri = data.getData();
                        if (uri != null) {
                            File fromFile = convertUriToFile(ScanActivity.this, uri);
                            if (fromFile != null && fromFile.exists()) {
                                Bitmap bmp = getBitmapNearestSize(fromFile,612);
                                if (bmp != null) {
                                    text = decodeQrCodeFromBitmap(bmp);
                                }
                            }
                        }
//                        Log.i("ansen","扫描结果:"+text);
                        final String r = text;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                /*dp.dismiss();*/
                                if (r == null) {
                                    fromGallery = false;
                                    Toast.makeText(ScanActivity.this, R.string.scan_qr_code_from_photo_wrong, Toast.LENGTH_SHORT).show();
                                } else {
                                    final Intent result = getIntent();
                                    Log.i("ansen","111 扫描结果:"+r);
                                    result.putExtra(INTENT_EXTRA_RESULT, r);
                                    setResult(RESULT_OK, result);
                                    finish();
                                }
                            }
                        });
                    }
                }.start();
            } else {
                fromGallery = false;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private final Runnable fetchAndDecodeRunnable = new Runnable() {
        private final QRCodeReader reader = new QRCodeReader();
        private final Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType,
                Object>(DecodeHintType.class);

        @Override
        public void run() {
            if (fromGallery) {
                cameraHandler.postDelayed(fetchAndDecodeRunnable, 500);
                return;
            }
            cameraManager.requestPreviewFrame(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(final byte[] data, final Camera camera) {
                    decode(data);
                }
            });
        }

        private void decode(final byte[] data) {
            final PlanarYUVLuminanceSource source = cameraManager.buildLuminanceSource(data);
            final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            try {
                hints.put(DecodeHintType.NEED_RESULT_POINT_CALLBACK,
                        new ResultPointCallback() {
                            @Override
                            public void foundPossibleResultPoint(
                                    final ResultPoint dot) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        scannerView.addDot(dot);
                                    }
                                });
                            }
                        });
                final Result scanResult = reader.decode(bitmap, hints);
                if (!resultValid(scanResult.getText())) {
                    cameraHandler.post(fetchAndDecodeRunnable);
                    return;
                }
                final int thumbnailWidth = source.getThumbnailWidth();
                final int thumbnailHeight = source.getThumbnailHeight();
                final float thumbnailScaleFactor = (float) thumbnailWidth
                        / source.getWidth();

                final Bitmap thumbnailImage = Bitmap.createBitmap(
                        thumbnailWidth, thumbnailHeight,
                        Bitmap.Config.ARGB_8888);
                thumbnailImage.setPixels(source.renderThumbnail(), 0,
                        thumbnailWidth, 0, 0, thumbnailWidth, thumbnailHeight);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleResult(scanResult, thumbnailImage,
                                thumbnailScaleFactor);
                    }
                });
            } catch (final Exception x) {
                cameraHandler.post(fetchAndDecodeRunnable);
            } finally {
                reader.reset();
            }
        }
    };

    public boolean resultValid(String result) {
        return true;
    }

    public final void startScan() {
        cameraHandler.post(fetchAndDecodeRunnable);
    }

    public void finish() {
        super.finish();
//        overridePendingTransition(R.anim.scanner_out_enter, 0);
    }

    private String decodeQrCodeFromBitmap(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int[] pixels = new int[width * height];
        bmp.getPixels(pixels, 0, width, 0, 0, width, height);
        bmp.recycle();
        bmp = null;
        QRCodeReader reader = new QRCodeReader();
        Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        try {
            Result result = reader.decode(new BinaryBitmap(new HybridBinarizer(new
                    RGBLuminanceSource(width, height, pixels))), hints);
            return result.getText();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap getBitmapNearestSize(File file, int size) {
        try {
            if (file == null || !file.exists()) {
                return null;
            } else if (file.length() == 0) {
                file.delete();
                return null;
            }
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            int sampleSize = getSampleSize(
                    Math.min(opts.outHeight, opts.outWidth), size);
            opts.inSampleSize = sampleSize;
            opts.inJustDecodeBounds = false;
            opts.inPurgeable = true;
            opts.inInputShareable = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bit = BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
            return bit;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static int getSampleSize(int fileSize, int targetSize) {
        int sampleSize = 1;
        if (fileSize > targetSize * 2) {
            int sampleLessThanSize = 0;
            do {
                sampleLessThanSize++;
            } while (fileSize / sampleLessThanSize > targetSize);

            for (int i = 1;
                 i <= sampleLessThanSize;
                 i++) {
                if (Math.abs(fileSize / i - targetSize) <= Math.abs(fileSize
                        / sampleSize - targetSize)) {
                    sampleSize = i;
                }
            }
        } else {
            if (fileSize <= targetSize) {
                sampleSize = 1;
            } else {
                sampleSize = 2;
            }
        }
        return sampleSize;
    }

    public static File convertUriToFile(Activity activity, Uri uri) {
        File file = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            @SuppressWarnings("deprecation")
            Cursor actualimagecursor = activity.managedQuery(uri, proj, null,
                    null, null);
            if (actualimagecursor != null) {
                int actual_image_column_index = actualimagecursor
                        .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                actualimagecursor.moveToFirst();
                String img_path = actualimagecursor
                        .getString(actual_image_column_index);
                if (!isEmpty(img_path)) {
                    file = new File(img_path);
                }
            } else {
                file = new File(new URI(uri.toString()));
                if (file.exists()) {
                    return file;
                }

            }
        } catch (Exception e) {
        }
        return file;

    }

    public static boolean isEmpty(String str) {
        return str == null || str.equals("");
    }
}
