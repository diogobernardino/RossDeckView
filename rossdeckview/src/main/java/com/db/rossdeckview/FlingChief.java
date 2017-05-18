package com.db.rossdeckview;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.Arrays;


/**
 * Class which empowers a view, giving it drag and fling capabilities.
 */
public class FlingChief implements View.OnTouchListener, GestureDetector.OnGestureListener,
		  GestureDetector.OnDoubleTapListener {

	private static final float ROTATION_COEFFICIENT = 15f;

	private static final float OUTSIDE_THRESHOLD = .9f;

	private static final int ANIMATION_DURATION = 250;

	private static final int FLING_MIN_DP_DISTANCE = 30;

	private final float mMinFlingDistance;

	/** View to be handled */
	private View mView;

	/** Updated View position information */
	private final Rect mViewRect;

	/** Gestures detector */
	private final GestureDetector mGesturesDetector;

	/** Initial specifications */
	private Rect mInitRect;

	private Rect mParenRect;

	/** Callbacks */
	private FlingChiefListener.Proximity mProximityCallback;

	private FlingChiefListener.Actions mActionCallback;

	/** Allowed directions */
	private Direction[] mDirections;

	/** Flag reporting whether view is animating */
	private boolean mIsAnimating;

	/** Listener used while dismissing View **/
	private final AnimatorListener mDismissAnimationListener = new AnimatorListener() {

		@Override public void onAnimationStart(Animator animation) { }

		@Override
		public void onAnimationEnd(Animator animation) {

			restoreState();
			if (mActionCallback != null) mActionCallback.onDismissed(mView);
		}

		@Override public void onAnimationCancel(Animator animation) { }

		@Override public void onAnimationRepeat(Animator animation) { }
	};

	/** Listener used while returning View to initial position **/
	private final AnimatorListener mReturnAnimationListener = new AnimatorListener() {

		@Override public void onAnimationStart(Animator animation) { }

		@Override
		public void onAnimationEnd(Animator animation) {

			restoreState();
			if (mActionCallback != null) mActionCallback.onReturned(mView);
		}

		@Override public void onAnimationCancel(Animator animation) { }

		@Override public void onAnimationRepeat(Animator animation) { }
	};


	/** Possible directions to be used */
	public enum Direction {
		/** Left */
		LEFT,
		/** Top */
		TOP,
		/** Right */
		RIGHT,
		/** Bottom */
		BOTTOM
	}


	public FlingChief(Context context) {

		mGesturesDetector = new GestureDetector(context, this);
		mViewRect = new Rect();
		mMinFlingDistance = FLING_MIN_DP_DISTANCE * Resources.getSystem().getDisplayMetrics().density;
		mDirections = new Direction[] {Direction.LEFT, Direction.TOP, Direction.RIGHT, Direction.BOTTOM};
	}


	public FlingChief(Context context, View view) {

		this(context);
		mView = view;
	}

	@Override
	public boolean onDown(MotionEvent e) {

		// Measure parent
		mParenRect = new Rect(0, 0, ((View) mView.getParent()).getWidth(),
				((View) mView.getParent()).getHeight());

		// Keep snapshot of initial view state
		mInitRect = new Rect(mView.getLeft(), mView.getTop(), mView.getRight(), mView.getBottom());

		return !mIsAnimating;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {

		// Update current view area
		mViewRect.set((int) v.getX(), (int) v.getY(), (int) (v.getX() + v.getWidth()),
				(int) (v.getY() + v.getHeight()));

		if (mGesturesDetector.onTouchEvent(event)) return true;

		if (event.getAction() == MotionEvent.ACTION_UP) {
			// If view dropped outside of target area
			if (calculateIntersection(mParenRect, mViewRect) > OUTSIDE_THRESHOLD
					&& (mActionCallback != null && mActionCallback.onReturn(mView) || mActionCallback == null))
					restore();
			else
				dismissTo(whereTo(mViewRect, mParenRect));
			return true;
		}
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) { }

	@Override
	public boolean onSingleTapUp(MotionEvent e) { return false; }

	@Override public boolean onDoubleTapEvent(MotionEvent e) { return false; }

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {

		mActionCallback.onTapped();
		return true;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {

		mActionCallback.onDoubleTapped();
		return true;
	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {

		// Find delta between initial and current position
		final float deltaX = e2.getRawX() - e1.getRawX();
		final float deltaY = e2.getRawY() - e1.getRawY();

		// Move view
		mView.setX(mInitRect.left + deltaX);
		mView.setY(mInitRect.top + deltaY);

		final float leftProximity = calculateProximity(mInitRect.centerX(), mViewRect.centerX(), mParenRect.left);
		final float topProximity = calculateProximity(mInitRect.centerY(), mViewRect.centerY(), mParenRect.top);
		final float rightProximity = calculateProximity(mInitRect.centerX(), mViewRect.centerX(), mParenRect.right);
		final float bottomProximity = calculateProximity(mInitRect.centerY(), mViewRect.centerY(), mParenRect.bottom);

		// Rotate view
		if (deltaY > 0) mView.setRotation(deltaX / ROTATION_COEFFICIENT * (1 - bottomProximity));
		else if (deltaY < 0) mView.setRotation(-deltaX / ROTATION_COEFFICIENT * (1 - topProximity));

		if (mProximityCallback != null)
			mProximityCallback.onProximityUpdate(
					new float[] {leftProximity, topProximity, rightProximity, bottomProximity},
					mView);

		return true;
	}

	@Override
	public void onLongPress(MotionEvent e) { }

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {

		// Find delta between fling start and end position
		float deltaX = e1.getRawX() - e2.getRawX();
		float deltaY = e1.getRawY() - e2.getRawY();

		if (Math.abs(deltaX) > Math.abs(deltaY)) {
			if (Math.abs(deltaX) < mMinFlingDistance) return false; // Fling is big enough
			if (deltaX > 0) dismissTo(Direction.LEFT);
			else dismissTo(Direction.RIGHT);
		} else {
			if (Math.abs(deltaY) < mMinFlingDistance) return false; // Fling is big enough
			if (deltaY > 0) dismissTo(Direction.TOP);
			else dismissTo(Direction.BOTTOM);
		}
		return true;
	}


	/**
	 * Replace view into its initial state (ex. position, scale)
	 */
	private void restore() {

		mIsAnimating = true;
		mView.animate()
				.x(mInitRect.left)
				.y(mInitRect.top)
				.rotation(0)
				.setListener(mReturnAnimationListener)
				.setDuration(ANIMATION_DURATION);
	}


	/**
	 * Dismiss view to specific position.
	 *
	 * @param x x position to dismiss view
	 * @param y y position to dismiss view
	 */
	private void dismiss(int x, int y) {

		mIsAnimating = true;
		mView.animate()
				.x(x)
				.y(y)
				.setListener(mDismissAnimationListener)
				.setDuration(ANIMATION_DURATION);
	}


	/**
	 * Attempt to dismiss view to a specific direction.
	 *
	 * @param direction Direction where view should be dismissed
	 */
	private void dismissTo(Direction direction) {

		if (Arrays.asList(mDirections).contains(direction)
				&& (mActionCallback != null && mActionCallback.onDismiss(direction, mView) || mActionCallback == null))
			switch (direction) {
				case LEFT:
					dismiss(mViewRect.left - (mViewRect.right - mParenRect.left), mViewRect.top);
					break;
				case TOP:
					dismiss(mViewRect.left, mViewRect.top - (mViewRect.bottom - mParenRect.top));
					break;
				case RIGHT:
					dismiss(mViewRect.left + (mParenRect.right - mViewRect.left), mViewRect.top);
					break;
				case BOTTOM:
					dismiss(mViewRect.left, mViewRect.top + (mParenRect.bottom - mViewRect.top));
					break;
				default:
					restore();
					break;
			}
		else if (mActionCallback != null && mActionCallback.onReturn(mView)
				|| mActionCallback == null)
			restore();
	}


	/**
	 * Restore view intial state.
	 */
	private void restoreState() {

		mIsAnimating = false;
		mView.animate().setListener(null);
		if (mProximityCallback != null)
			mProximityCallback.onProximityUpdate(new float[]{1, 1, 1, 1}, mView);
		mView.setX(mInitRect.left);
		mView.setY(mInitRect.top);
		mView.setRotation(0);
	}


	/**
	 * Find direction to where view should be dismissed based on sides proximities.
	 *
	 * @param view Current view area
	 * @param parent Parent area
	 *
	 * @return Direction to where view should be dismissed
	 */
	private Direction whereTo(Rect view, Rect parent) {

		final float[] proximities
				= new float[] {calculateProximity(mInitRect.centerX(), view.centerX(), parent.left),
				calculateProximity(mInitRect.centerY(), view.centerY(), parent.top),
				calculateProximity(mInitRect.centerX(), view.centerX(), parent.right),
				calculateProximity(mInitRect.centerY(), view.centerY(), parent.bottom)};

		int index = 0;
		float min = proximities[index];
		for (int i = 1; i < proximities.length; i++)
			if (proximities[i] < min) {
				min = proximities[i];
				index = i;
			}

		switch (index) {
			case 0: return Direction.LEFT;
			case 1: return Direction.TOP;
			case 2: return Direction.RIGHT;
			case 3: return Direction.BOTTOM;
			default: return Direction.LEFT;
		}
	}


	/**
	 * Calculate proximity, from 0 to 1, between two points having into account an initial position.
	 *
	 * @param initPos Initial view position
	 * @param viewPos Current view position
	 * @param boundPos Bound position
	 *
	 * @return Proximity between 2 points considering an initial position
	 */
	private float calculateProximity(float initPos, float viewPos, float boundPos) {

		return (initPos >= boundPos) ? (viewPos - boundPos) / (initPos - boundPos)
				: (boundPos - viewPos) / (boundPos - initPos);
	}


	/**
	 * Calculate the percentage of the intersection between two areas.
	 *
	 * @param rectA {@link Rect} A
	 * @param rectB {@link Rect} B
	 *
	 * @return Intersection percentage between the two given areas
	 */
	private float calculateIntersection(Rect rectA, Rect rectB) {

		final float overlapArea = Math.max(0,
				Math.min(rectA.right, rectB.right) - Math.max(rectA.left, rectB.left))
				* Math.max(0, Math.min(rectA.bottom, rectB.bottom) - Math.max(rectA.top, rectB.top));
		final float areaA = rectA.width() * rectA.height();
		final float areaB = rectB.width() * rectB.height();

		return (areaA < areaB) ? overlapArea / areaA : overlapArea / areaB;
	}


	/**
	 * Set callback to be used to report view proximity against parent sides.
	 *
	 * @param callback Callback to report about proximity
	 */
	public void setProximityListener(FlingChiefListener.Proximity callback) {

		this.mProximityCallback = callback;
	}


	/**
	 * Set callback to be used to report actions being performed.
	 *
	 * @param callback Callback to report about actions being performed
	 */
	public void setActionListener(FlingChiefListener.Actions callback) {

		this.mActionCallback = callback;
	}


	/**
	 * Defined allowed directions, of type {@link Direction}, to where view can be dismissed.
	 *
	 * @param directions Allowed directions to where view should be dismissed
	 */
	public void setDirections(Direction[] directions) {

		mDirections = directions;
	}


	/**
	 * Inject new view to be moved. Will override any previous view set.
	 * @param view New view to be handled.
	 */
	public void injectView( View view) {

		mView = view;
		mView.setOnTouchListener(this);
	}

}
