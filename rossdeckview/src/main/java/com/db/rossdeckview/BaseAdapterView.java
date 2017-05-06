package com.db.rossdeckview;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AdapterView;


/**
 * Base View to serve RossDeckView.
 */
abstract class BaseAdapterView extends AdapterView {

    private int heightMeasureSpec;
    private int widthMeasureSpec;

    public BaseAdapterView(Context context) {
        super(context);
    }

    public BaseAdapterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseAdapterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setSelection(int i) {
        throw new UnsupportedOperationException("Not supported");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.widthMeasureSpec = widthMeasureSpec;
        this.heightMeasureSpec = heightMeasureSpec;
    }

    public int getWidthMeasureSpec() {
        return widthMeasureSpec;
    }

    public int getHeightMeasureSpec() {
        return heightMeasureSpec;
    }

}
