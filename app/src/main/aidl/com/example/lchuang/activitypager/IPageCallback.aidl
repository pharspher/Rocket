// IPageCallback.aidl
package com.example.lchuang.activitypager;

// Declare any non-default types here with import statements

interface IPageCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    void scrollTo(int left);
}
