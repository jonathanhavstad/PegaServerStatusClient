package com.cisco.pegaserverstatusclient.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by jonathanhavstad on 10/25/16.
 */

public class TabViewAdapter extends FragmentStatePagerAdapter {
    List<PegaBaseFragment> fragments = new ArrayList<>();

    public TabViewAdapter(FragmentManager fm) {
        super(fm);

    }

    public void addFragment(PegaBaseFragment fragment) {
        fragments.add(fragment);
    }

    @Override
    public Fragment getItem(int position) {
        if (position < fragments.size()) {
            return fragments.get(position);
        }
        return null;
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}
