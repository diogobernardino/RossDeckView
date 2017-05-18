package com.db.rossdeckview;

import android.view.View;

/**
 * Interface used on fling callbacks.
 */
public interface FlingChiefListener {

	/**
	 * Callbacks following fling actions.
	 */
	interface Actions {

		/**
		 * Once flinged view is about to be dismissed.
		 *
		 * @param direction Direction which view will be dismissed
		 * @param view View where action will be applied
		 * @return True if action should happen, False otherwise
		 */
		boolean onDismiss(FlingChief.Direction direction, View view);

		/**
		 * Once done with dismiss action.
		 *
		 * @param view View where action will be applied
		 * @return True if action should happen, False otherwise
		 */
		boolean onDismissed(View view);

		/**
		 * Once flinged view is about to return to its initial position/state.
		 *
		 * @param view View where action will be applied
		 * @return True if action should happen, False otherwise
		 */
		boolean onReturn(View view);

		/**
		 * Once done with return action.
		 *
		 * @param view View where action will be applied
		 * @return True if action should happen, False otherwise
		 */
		boolean onReturned(View view);

        /**
         * Once the top card is tapped
         *
         * @return True if action should happen, False otherwise
         */
        boolean onTapped();

		/**
		 * Once the top card is double tapped
		 *
		 * @return True if action should happen, False otherwise
		 */
		boolean onDoubleTapped();
	}

	/**
	 * Callback following scroll actions.
	 */
	interface Proximity {

		/**
		 * Report on how close is view to parent borders.
		 *
		 * @param proximities Array of integers [left, top, right, bottom], with values from 0 to 1,
		 *                          representing the proximity of the view to the parent border.
		 * @param view View
		 */
		void onProximityUpdate(float[] proximities, View view);
	}

}
