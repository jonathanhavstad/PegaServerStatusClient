package com.cisco.pegaserverstatusclient.binders;

import android.os.Binder;

import com.cisco.pegaserverstatusclient.layouts.BaseLayoutInfo;

import java.util.Stack;

/**
 * Created by jonathanhavstad on 11/10/16.
 */

public class LayoutFilterBinder extends Binder {
    private Stack<BaseLayoutInfo> baseLayoutInfoStack;

    public Stack<BaseLayoutInfo> getBaseLayoutInfoStack() {
        return baseLayoutInfoStack;
    }

    public void setBaseLayoutInfoStack(Stack<BaseLayoutInfo> baseLayoutInfoStack) {
        this.baseLayoutInfoStack = baseLayoutInfoStack;
    }
}
