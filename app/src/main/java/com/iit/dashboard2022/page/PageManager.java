package com.iit.dashboard2022.page;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PageManager extends FragmentStateAdapter {
    public static final int DASHBOARD = 0;
    public static final int LIVEDATA = 1;
    public static final int LOGS = 2;
    public static final int COMMANDER = 3;
    public static final int ABOUT = 4;
    private final Page[] pages = new Page[5];

    public PageManager(@NonNull FragmentManager fragmentManager) {
        super(fragmentManager, new Lifecycle() {
            @Override
            public void addObserver(@NonNull LifecycleObserver observer) {
            }

            @Override
            public void removeObserver(@NonNull LifecycleObserver observer) {
            }

            @NonNull
            @Override
            public State getCurrentState() {
                return State.STARTED;
            }
        });

        /* ADD NEW PAGES HERE */
        pages[DASHBOARD] = new CarDashboard();
        pages[LIVEDATA] = new LiveData();
        pages[LOGS] = new Logs();
        pages[COMMANDER] = new Commander();
        pages[ABOUT] = new About();
    }

    @NonNull
    public Page[] getPages() {
        return pages;
    }

    @NonNull
    public String getPageTitle(@PageIndex int page) {
        return pages[page].getTitle();
    }

    @NonNull
    @Override
    public Fragment createFragment(@PageIndex int page) {
        return pages[page];
    }

    public Page getPage(@PageIndex int page) {
        return pages[page];
    }

    @Override
    public int getItemCount() {
        return pages.length;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({
            DASHBOARD,
            LIVEDATA,
            LOGS,
            COMMANDER,
            ABOUT
    })
    @interface PageIndex {
    }
}
