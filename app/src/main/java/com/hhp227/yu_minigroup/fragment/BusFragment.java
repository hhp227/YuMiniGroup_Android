package com.hhp227.yu_minigroup.fragment;

import android.os.Bundle;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.hhp227.yu_minigroup.R;

public class BusFragment extends Fragment {
    private AppCompatActivity mActivity;
    private DrawerLayout mDrawerLayout;
    private Toolbar mToolbar;

    public BusFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_bus, container, false);
        mActivity = (AppCompatActivity) getActivity();
        mDrawerLayout = mActivity.findViewById(R.id.drawer_layout);
        mToolbar = rootView.findViewById(R.id.toolbar);

        mActivity.setTitle("Fragment04");
        mActivity.setSupportActionBar(mToolbar);
        setDrawerToggle();

        return rootView;
    }

    private void setDrawerToggle() {
        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(mActivity, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();
    }
}
