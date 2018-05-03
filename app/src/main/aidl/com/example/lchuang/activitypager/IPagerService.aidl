// IPagerService.aidl
package com.example.lchuang.activitypager;

// Declare any non-default types here with import statements
import com.example.lchuang.activitypager.IPageCallback;
import android.view.WindowManager.LayoutParams;

interface IPagerService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    void onPageInstantiated(IPageCallback callback);
    void scrollDone();
    void attachWindow(in LayoutParams params);
    void scrollTo(float position);
    void startScroll();
    void stopScroll();
}
