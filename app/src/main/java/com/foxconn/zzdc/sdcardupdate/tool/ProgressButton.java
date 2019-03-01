package com.foxconn.zzdc.sdcardupdate.tool;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.Button;

import com.foxconn.zzdc.sdcardupdate.R;

@SuppressLint("AppCompatCustomView")
public class ProgressButton extends Button {

    private int mProgress;
    private int mMaxProgress = 100;
    private int mMinProgress = 0;
    private GradientDrawable mProgressDrawable;
    private GradientDrawable mProgressDrawableBg;
    private StateListDrawable mNormalDrawable;
    private boolean isShowProgress;
    private boolean isFinish;
    private boolean isStop;
    private boolean isStart ;
    private OnStateListener onStateListener;
    private float cornerRadius;


    public ProgressButton(Context context, AttributeSet attrs) {
        super(context,attrs);
        init(context,attrs);
    }

    public ProgressButton(Context context, AttributeSet attrs, int defStyleAttr){
        super(context,attrs,defStyleAttr);
        init(context,attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProgressButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        super(context,attrs,defStyleAttr,defStyleRes);
        init(context,attrs);
    }

    private void init(Context context, AttributeSet attributeSet) {

        mNormalDrawable = new StateListDrawable();

        mProgressDrawable = (GradientDrawable)getResources().getDrawable(R.drawable.rect_progress).mutate();

        mProgressDrawableBg = (GradientDrawable)getResources().getDrawable(R.drawable.rect_progressbg).mutate();

        TypedArray attr = context.obtainStyledAttributes(attributeSet,R.styleable.progressbutton);

        try {
            float defValue = getResources().getDimension(R.dimen.corner_radius);

            cornerRadius = attr.getDimension(R.styleable.progressbutton_buttonCornerRadius,defValue);

            isShowProgress = attr.getBoolean(R.styleable.progressbutton_showProgressNumber,true);

            mNormalDrawable.addState(new int[]{android.R.attr.state_pressed},
                    getPressedDrawable(attr));

            mNormalDrawable.addState(new int[] { }, getNormalDrawable(attr));

            int defaultProgressColor = getResources().getColor(R.color.colorAccent);
            int progressColor = attr.getColor(R.styleable.progressbutton_progressColor,defaultProgressColor);

            mProgressDrawable.setColor(progressColor);


            int defaultProgressBgColor = getResources().getColor(R.color.deepskyblue);
            int progressBgColor = attr.getColor(R.styleable.progressbutton_progressBgColor,defaultProgressBgColor);

            mProgressDrawableBg.setColor(progressBgColor);



        } finally {
            attr.recycle();
        }

        isFinish = false;
        isStop = true;
        isStart = false;

        mProgressDrawable.setCornerRadius(cornerRadius);
        mProgressDrawableBg.setCornerRadius(cornerRadius);

        setBackgroundCompat(mNormalDrawable);
    }



    @Override
    protected void onDraw(Canvas canvas) {

        if (mProgress > mMinProgress && mProgress <= mMaxProgress && !isFinish) {

            float scale = (float) getProgress() / (float) mMaxProgress;
            float indicatorWidth = (float) getMeasuredWidth() * scale;

            if(indicatorWidth < cornerRadius * 2){
                indicatorWidth = cornerRadius * 2;
            }

            mProgressDrawable.setBounds(0, 0, (int) indicatorWidth, getMeasuredHeight());

            mProgressDrawable.draw(canvas);

            if(mProgress==mMaxProgress) {
                setBackgroundCompat(mProgressDrawable);
                isFinish = true;
                if(onStateListener!=null) {
                    onStateListener.onFinish();
                }

            }

        }

        super.onDraw(canvas);
    }

    public void setProgress(int progress) {

            mProgress = progress;
            if(isShowProgress) setText(mProgress + " %");
            if(mProgress == 100) setText(R.string.install);
            setBackgroundCompat(mProgressDrawableBg);
            invalidate();

    }

    public int getProgress() {
        return mProgress;
    }

    public void setStop(boolean stop) {
        isStop = stop;
        invalidate();
    }

    public boolean isStop() {
        return isStop;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void toggle(){
        if(!isFinish&&isStart){
            if(isStop){
                setStop(false);
                onStateListener.onContinue();
            } else {
                setStop(true);
                onStateListener.onStop();
            }
        }else {
            setStop(false);
            isStart = true;
        }
    }

    private void setBackgroundCompat(Drawable drawable) {
        int pL = getPaddingLeft();
        int pT = getPaddingTop();
        int pR = getPaddingRight();
        int pB = getPaddingBottom();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setBackground(drawable);
        } else {
            setBackgroundDrawable(drawable);
        }
        setPadding(pL, pT, pR, pB);
    }

    public void initState(){
        setBackgroundCompat(mNormalDrawable);
        isFinish = false;
        isStop = true;
        isStart = false;
        mProgress = 0;
    }

    private Drawable getNormalDrawable(TypedArray attr) {

        GradientDrawable drawableNormal =
                (GradientDrawable) getResources().getDrawable(R.drawable.rect_normal).mutate();// 修改时就不会影响其它drawable对象的状态
        drawableNormal.setCornerRadius(cornerRadius); // 设置圆角半径

        int defaultNormal =  getResources().getColor(R.color.deepskyblue);
        int colorNormal =  attr.getColor(R.styleable.progressbutton_buttonNormalColor,defaultNormal);
        drawableNormal.setColor(colorNormal);//设置颜色

        return drawableNormal;
    }

    private Drawable getPressedDrawable(TypedArray attr) {
        GradientDrawable drawablePressed =
                (GradientDrawable) getResources().getDrawable(R.drawable.rect_pressed).mutate();// 修改时就不会影响其它drawable对象的状态
        drawablePressed.setCornerRadius(cornerRadius);// 设置圆角半径

        int defaultPressed = getResources().getColor(R.color.mediumslateblue);
        int colorPressed = attr.getColor(R.styleable.progressbutton_buttonPressedColor,defaultPressed);
        drawablePressed.setColor(colorPressed);//设置颜色

        return drawablePressed;
    }

    public interface OnStateListener{

        abstract void onFinish();
        abstract void onStop();
        abstract void onContinue();

    }

    public void setOnStateListener(OnStateListener onStateListener){
        this.onStateListener = onStateListener;
    }

    public void isShowProgressNum(boolean b){
        this.isShowProgress = b;
    }

}
