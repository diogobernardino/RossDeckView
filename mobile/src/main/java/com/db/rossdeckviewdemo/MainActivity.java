package com.db.rossdeckviewdemo;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.db.rossdeckview.FlingChief;
import com.db.rossdeckview.FlingChiefListener;
import com.db.rossdeckview.RossDeckView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FlingChiefListener.Actions, FlingChiefListener.Proximity {

    private final static int DELAY = 1000;

    private List<Pair<String, Integer>> mItems;

    private DeckAdapter mAdapter;

    private RossDeckView mDeckLayout;

    private View mLeftView;

    private View mUpView;

    private View mRightView;

    private View mDownView;

    private int[] mColors;

    private int mCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mColors  = getResources().getIntArray(R.array.cardsBackgroundColors);
        mItems = new ArrayList<>();

        mItems.add(newItem());
        mItems.add(newItem());
        mItems.add(newItem());
        mAdapter = new DeckAdapter(this, mItems);

        mDeckLayout = (RossDeckView) findViewById(R.id.decklayout);
        mDeckLayout.setAdapter(mAdapter);
        mDeckLayout.setActionsListener(this);
        mDeckLayout.setProximityListener(this);

        mLeftView = findViewById(R.id.left);
        mUpView = findViewById(R.id.up);
        mRightView = findViewById(R.id.right);
        mDownView = findViewById(R.id.down);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_fling:
                mDeckLayout.fling(FlingChief.Direction.TOP);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onDismiss(@NonNull FlingChief.Direction direction, @NonNull View view) {

        Toast.makeText(this, "Dismiss to " + direction, Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onDismissed(@NonNull View view) {

        mItems.remove(0);
        mAdapter.notifyDataSetChanged();
        newItemWithDelay(DELAY);
        return true;
    }

    @Override
    public boolean onReturn(@NonNull View view) {
        return true;
    }

    @Override
    public boolean onReturned(@NonNull View view) {
        return true;
    }

    @Override
    public boolean onTapped() {
        Toast.makeText(this, "Tapped", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onDoubleTapped() {
        Toast.makeText(this, "Double tapped", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onProximityUpdate(@NonNull float[] proximities, @NonNull View view) {

        mLeftView.setScaleY((1 - proximities[0] >= 0) ? 1 - proximities[0] : 0);
        mUpView.setScaleX((1 - proximities[1] >= 0) ? 1 - proximities[1] : 0);
        mRightView.setScaleY((1 - proximities[2] >= 0) ? 1 - proximities[2] : 0);
        mDownView.setScaleX((1 - proximities[3] >= 0) ? 1 - proximities[3] : 0);
    }


    private Pair<String, Integer> newItem(){

        Pair<String, Integer> res = new Pair<>("Card" + Integer.toString(mCount), mColors[mCount]);
        mCount = (mCount >= mColors.length - 1) ? 0 : mCount + 1;
        return res;
    }


    private void newItemWithDelay(int delay){

        final Pair<String, Integer> res = newItem();
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mItems.add(res);
                mAdapter.notifyDataSetChanged();
            }
        }, delay);
    }

}
