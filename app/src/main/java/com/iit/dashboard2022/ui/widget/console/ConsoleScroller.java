package com.iit.dashboard2022.ui.widget.console;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class ConsoleScroller extends ScrollView {

    private boolean enabled = true;
    private ScrollerStatusListener scrollerStatusListener;

    public interface ScrollerStatusListener {
        void run(boolean enabled);
    }

    Runnable scroller = () -> fullScroll(ScrollView.FOCUS_DOWN);

    public void scroll() { // TODO: Is there a better way to scroll?
        if (enabled)
            post(scroller);
    }

    public void enable(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (scrollerStatusListener != null)
                scrollerStatusListener.run(enabled);
        }
    }

    public void toggle() {
        enable(!enabled);
    }

    public void setScrollerStatusListener(ScrollerStatusListener listener) {
        scrollerStatusListener = listener;
    }

    public ConsoleScroller(Context context) {
        this(context, null);
    }

    public ConsoleScroller(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConsoleScroller(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN)
            enable(false);
        return super.onTouchEvent(ev);
    }
}