package com.andy.colorpicker.view;

import com.andy.colorpicker.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Paint.FontMetrics;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {

	private static final int RADIUS_OFFSET1 = 130; // 余辉半径差
	private static final int RADIUS_OFFSET2 = 80;// 选色圆环半径差

	private static final int COLOR_WIDTH = 40;// 选色圆环宽度

	private static final int ANGLE_CHANGE_BY = 5;// 当变化角度大于此数值 ，做出操作（发送命令）

	private Paint mPaint;
	private Paint mBgPaint;
	private Paint mCirclePaint;//
	private Paint mGrayPaint;// 点击时 遮住
	private Paint mGradientPaint;// 余辉画笔
	private Paint mColorPaint;// 选色 画笔
	private Paint mSelectPaint;
	private Paint mWhitePaint;

	private RadialGradient mGradient; // 余辉效果 渐变

	private SweepGradient mSweepGradient; // 选色渐变

	private int[] colors;// 选色 渐变颜色数组
	private int curColor;

	private boolean isEdit = true;// 是否修改颜色

	private float circle_radius;

	private Point curPoint;// 当前触摸选色点
	private Point curCirclePoint;// 圆环圆心

	float radius;

	int angle = 270;

	float mSat = -1;
	int mAlpha = 0xff;

	boolean isMoving;// 滑动选色圆形
	boolean isPressed;// 是否按下中间区域
	private Point midPoint;// 圆心

	OnColorChangeListener colorListener;
	OnCircleClickListener clickListener;

	// 颜色变化监听器
	public interface OnColorChangeListener {
		public void onColorChange(int color);
	};

	// 中间区域点击监听器
	public interface OnCircleClickListener {
		public void onClick();
	};

	public void setOnColorChangeListener(OnColorChangeListener listener) {
		this.colorListener = listener;
	}

	public void setOnCircleClickListener(OnCircleClickListener listener) {
		this.clickListener = listener;
	}

	public void setEditMode(boolean isEdit) {
		this.isEdit = isEdit;
		this.invalidate();
	}

	public void setSat(float sat, boolean isInit) {

		float[] hsv = new float[3];
		Color.colorToHSV(curColor, hsv);
		if (sat < 0) {
			sat = 0;
		}
		if (sat > 1) {
			sat = 1;
		}
		mSat = sat;
		hsv[1] = sat;

		if (isInit == false) {
			Log.e("andy1","sat refresh,sat="+mSat);
			this.invalidate();
		}
	}

	public void setAlpha(int alpha, boolean isInit) {

		if (alpha < 0 || alpha > 0xff) {
			return;
		}

		mAlpha = alpha;

		if (isInit == false) {
			Log.e("andy1","alpha refresh,alpha="+mAlpha);
			this.invalidate();
		}
	}

	public ColorPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);

		TypedArray array = context.obtainStyledAttributes(attrs,
				R.styleable.ColorPickerView);
		int color = array.getColor(R.styleable.ColorPickerView_textColor,
				Color.BLACK);
		float size = array.getDimension(R.styleable.ColorPickerView_textSize,
				30);

		mPaint = new Paint();
		mPaint.setColor(color);
		mPaint.setTextSize(size);
		mPaint.setAntiAlias(true);

		mBgPaint = new Paint();
		mBgPaint.setColor(Color.GRAY);
		mBgPaint.setAntiAlias(true);

		mCirclePaint = new Paint();
		mCirclePaint.setColor(curColor);
		mCirclePaint.setAntiAlias(true);

		mGrayPaint = new Paint();
		mGrayPaint.setColor(0x88888888);
		mGrayPaint.setAntiAlias(true);

		mGradientPaint = new Paint();
		mGradientPaint.setAntiAlias(true);

		mColorPaint = new Paint();
		mColorPaint.setStyle(Paint.Style.STROKE);
		mColorPaint.setStrokeWidth(COLOR_WIDTH);
		mColorPaint.setAntiAlias(true);

		mSelectPaint = new Paint();

		mWhitePaint = new Paint();
		mWhitePaint.setColor(Color.WHITE);

		radius = COLOR_WIDTH;

		colors = buildHueColorArray();

		array.recycle();
		this.setClickable(true);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		midPoint = new Point();
		midPoint.x = this.getWidth() / 2;
		midPoint.y = this.getHeight() / 2;
	}
	
	private int getColor(int angle){
		int color = Color.HSVToColor(new float[] { 361 - angle,
				mSat > 0 ? mSat : 1f, 1f });
		int r = Color.red(color);
		int g = Color.green(color);
		int b = Color.blue(color);
		int newColor = Color.argb(mAlpha, r, g, b);
		Log.e("andy1","refresh colr="+Integer.toHexString(newColor));
		return newColor;
	}

	@Override
	protected void onDraw(Canvas canvas) {

		int w = this.getWidth();
		int h = this.getHeight();

		canvas.drawRect(new Rect(0, 0, w, h), mBgPaint);

		circle_radius = Math.min(h / 8, w / 8);

		float cx = w / 2;
		float cy = h / 2;

		// 选色
		if (isEdit) {
			if (mSweepGradient == null) {
				mSweepGradient = new SweepGradient(cx, cy, colors, null);
			}
			mColorPaint.setShader(mSweepGradient);
			float r = circle_radius + RADIUS_OFFSET2;
			// 选色圆环
			canvas.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), 0, 360,
					false, mColorPaint);

			// 选中颜色圆圈
			int color ;
			if (curPoint == null) {
				curPoint = new Point();
				curPoint.x = (int) cx;
				curPoint.y = (int) (cy - r);

				curCirclePoint = new Point();
				curCirclePoint.x = (int) cx;
				curCirclePoint.y = (int) (cy - r);
				angle = 270;
				color=curColor =Color.HSVToColor(new float[] { 361 - angle,
						 1f, 1f });

			} else {
				curCirclePoint = getPoint1(midPoint, curPoint, r);
				color = getColor(angle);
			}
			
			canvas.drawCircle(curCirclePoint.x, curCirclePoint.y, radius + 5,
					mWhitePaint);

			// angle = getAngle(m, curPoint);
			// curColor = Color.HSVToColor(new float[] { 361 - angle, 1f, 1f });
			Log.e("andy1", "angle=" + angle);

			// 可移动的圆形
			mSelectPaint.setColor(curColor);
			canvas.drawCircle(curCirclePoint.x, curCirclePoint.y, radius,
					mSelectPaint);

			// 中间大圆形
			mCirclePaint.setColor(color);
			canvas.drawCircle(cx, cy, circle_radius, mCirclePaint);

			// 余辉
			mGradient = new RadialGradient(cx, cy, circle_radius
					+ RADIUS_OFFSET1,
					new int[] { color, Color.TRANSPARENT }, null,
					Shader.TileMode.CLAMP);
			mGradientPaint.setShader(mGradient);
			canvas.drawCircle(cx, cy, circle_radius + RADIUS_OFFSET1,
					mGradientPaint); // 产生余辉效果

			if (isPressed) {
				canvas.drawCircle(cx, cy, circle_radius, mGrayPaint);
			}

		} else {
			int color = getColor(angle);
			mGradient = new RadialGradient(cx, cy, circle_radius
					+ RADIUS_OFFSET1,
					new int[] { color, Color.TRANSPARENT }, null,
					Shader.TileMode.CLAMP);
			mGradientPaint.setShader(mGradient);

			canvas.drawCircle(cx, cy, circle_radius + RADIUS_OFFSET1,
					mGradientPaint); // 产生余辉效果
			
			mCirclePaint.setColor(color);
			canvas.drawCircle(cx, cy, circle_radius, mCirclePaint);
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			Point p = new Point();
			p.x = (int) event.getX();
			p.y = (int) event.getY();

			if (isInCircle(midPoint, circle_radius, p)) { // 是否在中间圆形区域里
				isPressed = true;
				isMoving = false;
				this.invalidate();
				break;
			} else {
				isPressed = false;
			}

			if (!isInCircle(curCirclePoint, radius, p)) {// 是否在选色圆形区域里
				isMoving = false;

				return true;
			} else {
				isMoving = true;

				curPoint.x = (int) event.getX();
				curPoint.y = (int) event.getY();
			}

			this.invalidate();

			break;
		case MotionEvent.ACTION_MOVE:
			if (isPressed || !isMoving) {
				break;
			}
			curPoint.x = (int) event.getX();
			curPoint.y = (int) event.getY();

			int oldAngle = angle;
			angle = getAngle(midPoint, curPoint);
			curColor = getColor(angle);

			this.invalidate();
			break;
		case MotionEvent.ACTION_UP:
			if (isMoving) {
				isMoving = false;
				isPressed = false;
				if (colorListener != null) {
					colorListener.onColorChange(curColor);

				}
				break;
			}

			if (isPressed) {
				isPressed = false;
				isMoving = false;
				this.invalidate();

				if (clickListener != null) {
					clickListener.onClick();
				}
				break;
			}

			break;
		}

		return true;
	}

	// 获取所有颜色数组
	private int[] buildHueColorArray() {

		int[] hue = new int[361];

		int count = 0;
		for (int i = hue.length - 1; i >= 0; i--, count++) {
			hue[count] = Color.HSVToColor(new float[] { i, 1f, 1f });
		}

		return hue;
	}

	// 获取字体高度
	private float getTxtHeight(Paint p) {
		FontMetrics fontMetris = p.getFontMetrics();
		return fontMetris.bottom - fontMetris.top;
	}

	// 居中打印字符串
	private void drawTextAlignCenter(Canvas canvas, Paint paint, String str,
			Rect rect) {
		float txtWidth = paint.measureText(str);
		float txtHeight = getTxtHeight(paint);

		Rect mBound = new Rect();
		paint.getTextBounds(str, 0, str.length(), mBound);

		float centerX = (rect.left + rect.right) / 2;
		float centerY = (rect.top + rect.bottom) / 2;

		canvas.drawText(str, 0, str.length(), centerX
				- (mBound.right + mBound.left) / 2, centerY
				- (mBound.bottom + mBound.top) / 2, paint);
	}

	// 触摸点是否在圆环内
	private boolean isInCircle(Point m, float radius, Point p) {

		return (p.x - m.x) * (p.x - m.x) + (p.y - m.y) * (p.y - m.y) <= (radius + 10)
				* (radius + 10);

	}

	private int getAngle(Point m, Point pa) {
		int x = pa.x;
		int y = pa.y;
		int cx = m.x;
		int cy = m.y;
		float degrees = (float) ((float) ((Math.toDegrees(Math.atan2(x - cx, cy
				- y)) + 360.0)) % 360.0) - 90;
		// and to make it count 0-360
		if (degrees < 0) {
			degrees += 360;
		}
		Log.e("andy1", "degree=" + degrees);
		return (int) degrees;
	}

	// 已知一点和圆心，求圆弧上的点的坐标
	private Point getPoint1(Point m, Point pa, float r) {
		Point p = new Point();
		p.x = (int) (m.x + r
				* Math.cos(Math.atan2(pa.x - m.x, -pa.y + m.y) - (Math.PI / 2)));
		p.y = (int) (m.y + r
				* Math.sin(Math.atan2(pa.x - m.x, -pa.y + m.y) - (Math.PI / 2)));
		return p;
	}
}
