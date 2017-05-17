package com.db.rossdeckviewdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.db.rossdeckview.FlingChief;
import com.db.rossdeckview.FlingChiefListener;
import com.db.rossdeckview.RossDeckView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements FlingChiefListener.Actions, FlingChiefListener.Proximity{

    private List<String> mItems;
    private ArrayAdapter<String> mAdapter;
    private View mLeftView;
    private View mUpView;
    private View mRightView;
    private View mDownView;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mItems = new ArrayList<>();
        mItems.add("hey");
        mItems.add("there");
        mAdapter = new ArrayAdapter<>(this, R.layout.item, R.id.item_text, mItems);

        RossDeckView mDeckLayout = (RossDeckView) findViewById(R.id.decklayout);
        mDeckLayout.setAdapter(mAdapter);
        mDeckLayout.setActionsListener(this);
        mDeckLayout.setProximityListener(this);

        mLeftView = findViewById(R.id.left);
        mUpView = findViewById(R.id.up);
        mRightView = findViewById(R.id.right);
        mDownView = findViewById(R.id.down);
    }

    @Override
    public boolean onDismiss(FlingChief.Direction direction, View view) {

        Toast.makeText(this, "Dismiss to " + direction, Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public boolean onDismissed(View view) {

        mItems.remove(0);
        mItems.add("card" + Integer.toString(count++));
        mAdapter.notifyDataSetChanged();
        return true;
    }

    @Override
    public boolean onReturn(View view) {
        return true;
    }

    @Override
    public boolean onReturned(View view) {
        return true;
    }

    @Override
    public boolean onTopCardTapped() {
        System.out.println(mItems.get(0));
        return true;
    }

    @Override
    public void onProximityUpdate(float[] proximities, View view) {

        mLeftView.setScaleY((1 - proximities[0] >= 0) ? 1 - proximities[0] : 0);
        mUpView.setScaleX((1 - proximities[1] >= 0) ? 1 - proximities[1] : 0);
        mRightView.setScaleY((1 - proximities[2] >= 0) ? 1 - proximities[2] : 0);
        mDownView.setScaleX((1 - proximities[3] >= 0) ? 1 - proximities[3] : 0);
    }
}
