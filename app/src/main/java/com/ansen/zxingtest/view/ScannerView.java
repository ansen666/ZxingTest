/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansen.zxingtest.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.ansen.zxingtest.R;
import com.ansen.zxingtest.utils.ScreenUtil;
import com.google.zxing.ResultPoint;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class ScannerView extends View {
	private static final long LASER_ANIMATION_DELAY_MS = 100l;
	private static final int DOT_OPACITY = 0xa0;

	private static final int DOT_TTL_MS = 500;

	private final Paint maskPaint;
	private final Paint laserPaint;
	private final Paint dotPaint;
	private Bitmap resultBitmap;
	private final int maskColor;
	private final int resultColor;
	private final Map<ResultPoint, Long> dots = new HashMap<ResultPoint, Long>(
			16);
	private Rect frame, framePreview;
	private final Paint textPaint;

	public ScannerView(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		final Resources res = getResources();
		maskColor = res.getColor(R.color.scan_mask);
		resultColor = res.getColor(R.color.scan_result_view);
		final int laserColor = res.getColor(R.color.scan_laser);
		final int dotColor = res.getColor(R.color.scan_dot);

		maskPaint = new Paint();
		maskPaint.setStyle(Style.FILL);

		textPaint = new Paint();
		textPaint.setColor(Color.parseColor("#FFFFFF"));
		textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(ScreenUtil.spToPx(context,16));

		int DOT_SIZE = ScreenUtil.dip2pix(context,3);

		laserPaint = new Paint();
		laserPaint.setColor(laserColor);
		laserPaint.setStrokeWidth(DOT_SIZE);
		laserPaint.setStyle(Style.STROKE);

		dotPaint = new Paint();
		dotPaint.setColor(dotColor);
		dotPaint.setAlpha(DOT_OPACITY);
		dotPaint.setStyle(Style.STROKE);
		dotPaint.setStrokeWidth(DOT_SIZE);
		dotPaint.setAntiAlias(true);
	}

	public void setFraming(final Rect frame,
			final Rect framePreview) {
		this.frame = frame;
		this.framePreview = framePreview;

		invalidate();
	}

	public void drawResultBitmap(final Bitmap bitmap) {
		resultBitmap = bitmap;

		invalidate();
	}

	public void addDot(final ResultPoint dot) {
		dots.put(dot, System.currentTimeMillis());

		invalidate();
	}

	@Override
	public void onDraw(final Canvas canvas) {
		if (frame == null)
			return;

		final long now = System.currentTimeMillis();

		final int width = canvas.getWidth();
		final int height = canvas.getHeight();

		// draw mask darkened
		maskPaint.setColor(resultBitmap != null ? resultColor : maskColor);
		canvas.drawRect(0, 0, width, frame.top, maskPaint);
		canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, maskPaint);
		canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
				maskPaint);
		canvas.drawRect(0, frame.bottom + 1, width, height, maskPaint);

		Rect rect = new Rect();
		textPaint.getTextBounds(getResources().getString(R.string.scan_qr_code_warn), 0, getResources().getString(R.string.scan_qr_code_warn).length(), rect);
		canvas.drawText(getResources().getString(R.string.scan_qr_code_warn), ScreenUtil.getWidthPixels()/2,frame.bottom+rect.height()+ScreenUtil.dpToPx(getContext(),7),textPaint);

		if (resultBitmap != null) {
			canvas.drawBitmap(resultBitmap, null, frame, maskPaint);
		} else {
			// draw red "laser scanner" to show decoding is active
			final boolean laserPhase = (now / 600) % 2 == 0;
			laserPaint.setAlpha(laserPhase ? 160 : 255);
			canvas.drawRect(frame, laserPaint);

			// draw points
			final int frameLeft = frame.left;
			final int frameTop = frame.top;
			final float scaleX = frame.width() / (float) framePreview.width();
			final float scaleY = frame.height() / (float) framePreview.height();

			for (final Iterator<Map.Entry<ResultPoint, Long>> i = dots
					.entrySet().iterator(); i.hasNext();) {
				final Map.Entry<ResultPoint, Long> entry = i.next();
				final long age = now - entry.getValue();
				if (age < DOT_TTL_MS) {
					dotPaint.setAlpha((int) ((DOT_TTL_MS - age) * 256 / DOT_TTL_MS));

					final ResultPoint point = entry.getKey();
					canvas.drawPoint(frameLeft + (int) (point.getX() * scaleX),
							frameTop + (int) (point.getY() * scaleY), dotPaint);
				} else {
					i.remove();
				}
			}

			// schedule redraw
			postInvalidateDelayed(LASER_ANIMATION_DELAY_MS);
		}
	}
}
