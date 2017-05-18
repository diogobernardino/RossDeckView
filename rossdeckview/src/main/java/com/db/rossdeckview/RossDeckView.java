package com.db.rossdeckview;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.support.annotation.FloatRange;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.FrameLayout;


/**
 * View representing a deck of view on top of each other, where each can be moved around
 * and flinged to each parent's side.
 */
public class RossDeckView extends BaseAdapterView {
    
    private static int sMaxVisible = 4;

    private static float sStackPadding = .0f;

    private static float sStackScale = .0f;

    private static int sLastObjectOnStack = 0;

    private Adapter mAdapter;

    private DataSetObserver mDataSetObserver;

    private boolean mInLayout = false;

    private View mActiveCard = null;

    private FlingChief mFlingChief;


    public RossDeckView(Context context) {
        super(context);
        init(null);
    }

    public RossDeckView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context.obtainStyledAttributes(attrs, R.styleable.RossDeckView));
    }

    public RossDeckView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context.obtainStyledAttributes(attrs, R.styleable.RossDeckView, defStyle, 0));
    }

    /**
     * Init method.
     * @param tArray Array containing layout attributes.
     */
    private void init(TypedArray tArray) {

        if (tArray != null) {
            sMaxVisible = tArray.getInt(R.styleable.RossDeckView_max_visible, sMaxVisible);
            sStackPadding = tArray.getDimension(R.styleable.RossDeckView_stack_padding, sStackPadding);
            sStackScale = tArray.getFloat(R.styleable.RossDeckView_stack_scale, sStackScale);
            tArray.recycle();
        }
        mFlingChief = new FlingChief(getContext());
        mFlingChief.setProximityListener(new FlingChiefListener.Proximity() {
            @Override
            public void onProximityUpdate(float[] proximities, View view) {
                moveBackgroundViews(calculateBackgroundFactor(proximities), false);
            }
        });
    }

    @Override
    public View getSelectedView() {
        return mActiveCard;
    }

    @Override
    public void requestLayout() {

        if (!mInLayout)
            super.requestLayout();
    }

    @Override
    public Adapter getAdapter() {
        return mAdapter;
    }

    @Override
    public void setAdapter(@NonNull Adapter adapter) {

        if (mDataSetObserver != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
            mDataSetObserver = null;
        }

        mAdapter = adapter;
        mDataSetObserver = new DataSetObserver() {
            @Override public void onChanged() { requestLayout(); }

            @Override public void onInvalidated() { requestLayout(); }
        };
        mAdapter.registerDataSetObserver(mDataSetObserver);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FrameLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        super.onLayout(changed, left, top, right, bottom);

        if (mAdapter == null)
            return;

        mInLayout = true;

        int nChilds = Math.min(mAdapter.getCount(), sMaxVisible);

        if (nChilds == 0) { // No cards
            removeAllViewsInLayout();
            mActiveCard = null;
        } else {
            if (mActiveCard == null) { // Front card discarded
                removeAllViewsInLayout();
                positionViews(0, nChilds);
                if (getChildCount() > 0) {
                    mActiveCard = getChildAt(sLastObjectOnStack);
                    mFlingChief.injectView(mActiveCard);
                }
            } else { // Add cards on the background
                removeViewsInLayout(0, sLastObjectOnStack);
                positionViews(1, nChilds);
            }
        }

        mInLayout = false;
    }


    /**
     * Place child views on parent.
     *
     * @param startIndex First view index to iterate from
     * @param nViews Number of views to be added
     */
    private void positionViews(int startIndex, int nViews) {

        View view;
        while (startIndex < nViews) {
            view = mAdapter.getView(startIndex, null, this);
            addChildToLayout(view, startIndex);
            startIndex++;
        }
        sLastObjectOnStack = startIndex - 1;
    }


    /**
     * Place view in center parent.
     *
     * @param child Child {@link View} to be placed
     * @param index child's index in stack
     */
    private void addChildToLayout(View child, int index) {

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) child.getLayoutParams();
        addViewInLayout(child, 0, lp, true);

        if (child.isLayoutRequested()) {
            int childWidthSpec = getChildMeasureSpec(getWidthMeasureSpec(),
                    getPaddingLeft() + getPaddingRight() + lp.leftMargin + lp.rightMargin,
                    lp.width);
            int childHeightSpec = getChildMeasureSpec(getHeightMeasureSpec(),
                    getPaddingTop() + getPaddingBottom() + lp.topMargin + lp.bottomMargin,
                    lp.height);
            child.measure(childWidthSpec, childHeightSpec);
        } else {
            cleanupLayoutState(child);
        }

        int w = child.getMeasuredWidth();
        int h = child.getMeasuredHeight();

        // Find left and top center coordinates
        int childLeft = (getWidth() + getPaddingLeft() - getPaddingRight() - w) / 2
                + lp.leftMargin - lp.rightMargin;
        int childTop = (getHeight() + getPaddingTop() - getPaddingBottom() - h) / 2
                + lp.topMargin - lp.bottomMargin;

        child.layout(childLeft,
                childTop + (int) (index * sStackPadding),
                childLeft + w,
                childTop + h + (int) (index * sStackPadding));

        child.setScaleX(1.f - index * sStackScale);
    }


    /**
     * Calc factor to apply to the translation and scale of a background card based on the
     * distance between the active card and view sides.
     *
     * @param proximities Proximity values (from 0 to 1) from the
     *                    initial active card position and each parent view side
     * @return factor used to move and scale background cards
     */
    private float calculateBackgroundFactor(float[] proximities) {

        float max = 0;
        for (float proximity : proximities)
            max = Math.max(max, 1 - proximity);
        return Math.min(1, max);
    }


    /**
     * Translate Y up based on a factor from 0 to 1.
     *
     * @param factor Value from 0 to 1
     */
    private void moveBackgroundViews(@FloatRange(from = 0.f, to = 1.f) float factor, boolean animate) {

        View view;
        for (int i = 0; i < sLastObjectOnStack; i++) {
            view = getChildAt(i);
            if (animate) {
                view.animate()
                        .translationY(-factor * sStackPadding)
                        .scaleX(1.f - (sLastObjectOnStack - i) * sStackScale + factor * sStackScale);
            } else {
                view.setTranslationY(-factor * sStackPadding);
                view.setScaleX(1.f - (sLastObjectOnStack - i) * sStackScale + factor * sStackScale);
            }
        }
    }

    /**
     * Set number of childs to be displayed on the stack.
     * @param max Maximum number of views to be displayed
     */
    public void setMaxVisible(int max) {
        sMaxVisible = max;
    }


    /**
     * Set padding used between background views.
     *
     * @param padding Padding used between background views
     */
    public void setStackPadding(float padding) {
        sStackPadding = padding;
    }


    /**
     * Set scale used between background views.
     *
     * @param factor Factor to be used between background views
     */
    public void setStackScale(float factor) {
        sStackPadding = factor;
    }


    /**
     * Defined allowed directions, of type {@link FlingChief.Direction}, to where view can be dismissed.
     *
     * @param directions Allowed directions to where view should be dismissed
     */
    public void setDirections(FlingChief.Direction[] directions) {
        mFlingChief.setDirections(directions);
    }


    /**
     * Set listener to be notified once an action is about to be performed or performed.
     *
     * @param actionListener Listener to be called
     */
    public void setActionsListener(final FlingChiefListener.Actions actionListener) {

        mFlingChief.setActionListener(new FlingChiefListener.Actions() {
            @Override public boolean onDismiss(FlingChief.Direction direction, View view) {
                moveBackgroundViews(1.f, true);
                return actionListener.onDismiss(direction, view);
            }

            @Override public boolean onDismissed(View view) {
                mActiveCard = null;
                return actionListener.onDismissed(view);
            }

            @Override public boolean onReturn(View view) {
                moveBackgroundViews(0.f, true);
                return actionListener.onReturn(view);
            }

            @Override public boolean onReturned(View view) {
                return actionListener.onReturned(view);
            }

            @Override
            public boolean onTapped() {
                return actionListener.onTapped();
            }

            @Override
            public boolean onDoubleTapped() {
                return actionListener.onDoubleTapped();
            }
        });
    }


    /**
     * Set listener to be used to notifiy about parent's sides proximity in comparison to the view.
     *
     * @param proximityListener Listener to be used
     */
    public void setProximityListener(final FlingChiefListener.Proximity proximityListener) {

        mFlingChief.setProximityListener(new FlingChiefListener.Proximity() {
            @Override
            public void onProximityUpdate(float[] proximities, View view) {
                moveBackgroundViews(calculateBackgroundFactor(proximities), false);
                proximityListener.onProximityUpdate(proximities, view);
            }
        });
    }

}
