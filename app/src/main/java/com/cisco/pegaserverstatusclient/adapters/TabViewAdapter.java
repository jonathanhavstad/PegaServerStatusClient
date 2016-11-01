package com.cisco.pegaserverstatusclient.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import com.cisco.pegaserverstatusclient.data.LifecycleLayoutInfo;
import com.cisco.pegaserverstatusclient.fragments.PegaBaseFragment;

import java.util.ArrayList;
import java.util.List;

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

    public void replaceFragment(int position, PegaBaseFragment fragment) {
        if (position >= 0 && position < fragments.size()) {
            fragments.remove(position);
            fragments.add(position, fragment);
        }
    }

    public int getFragmentPosition(String key) {
        for (int i = 0; i < LifecycleLayoutInfo.LC_KEY_ORDER.length; i++) {
            if (LifecycleLayoutInfo.LC_KEY_ORDER[i].equalsIgnoreCase(key)) {
                return i;
            }
        }

        return -1;
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
