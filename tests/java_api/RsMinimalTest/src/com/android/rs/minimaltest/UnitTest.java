/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.rs.minimaltest;

import android.content.Context;
import android.renderscript.RenderScript;
import android.renderscript.RenderScript.RSMessageHandler;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import dalvik.system.DexFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public abstract class UnitTest {
    public enum UnitTestResult {
        UT_NOT_STARTED,
        UT_RUNNING,
        UT_SUCCESS,
        UT_FAIL;

        @Override
        public String toString() {
            switch (this) {
                case UT_NOT_STARTED:
                    return "NOT STARTED";
                case UT_RUNNING:
                    return "RUNNING";
                case UT_SUCCESS:
                    return "PASS";
                case UT_FAIL:
                    return "FAIL";
                default:
                    throw new RuntimeException(
                        "missing enum case in UnitTestResult#toString()");
            }
        }
    }

    private final static String TAG = "RSUnitTest";

    private String mName;
    private UnitTestResult mResult;
    private Context mCtx;
    /* Necessary to avoid race condition on pass/fail message. */
    private CountDownLatch mCountDownLatch;

    /* These constants must match those in shared.rsh */
    public static final int RS_MSG_TEST_PASSED = 100;
    public static final int RS_MSG_TEST_FAILED = 101;

    public UnitTest(String n, Context ctx) {
        mName = n;
        mCtx = ctx;
        mResult = UnitTestResult.UT_NOT_STARTED;
        mCountDownLatch = null;
    }

    protected void _RS_ASSERT(String message, boolean b) {
        if (!b) {
            Log.e(TAG, message + " FAILED");
            failTest();
        }
    }

    /**
     * Returns a RenderScript instance created from mCtx.
     *
     * @param enableMessages
     * true if expecting exactly one pass/fail message from the RenderScript instance.
     * false if no messages expected.
     * Any other messages are not supported.
     */
    protected RenderScript createRenderScript(boolean enableMessages) {
        RenderScript rs = RenderScript.create(mCtx);
        if (enableMessages) {
            RSMessageHandler handler = new RSMessageHandler() {
                public void run() {
                    switch (mID) {
                        case RS_MSG_TEST_PASSED:
                            passTest();
                            break;
                        case RS_MSG_TEST_FAILED:
                            failTest();
                            break;
                        default:
                            Log.w(TAG, String.format("Unit test %s got unexpected message %d",
                                    UnitTest.this.toString(), mID));
                            break;
                    }
                    mCountDownLatch.countDown();
                }
            };
            rs.setMessageHandler(handler);
            mCountDownLatch = new CountDownLatch(1);
        }
        return rs;
    }

    protected synchronized void failTest() {
        mResult = UnitTestResult.UT_FAIL;
    }

    protected synchronized void passTest() {
        if (mResult != UnitTestResult.UT_FAIL) {
            mResult = UnitTestResult.UT_SUCCESS;
        }
    }

    public void logStart(String tag, String testSuite) {
        String thisDeviceName = android.os.Build.DEVICE;
        int thisApiVersion = android.os.Build.VERSION.SDK_INT;
        Log.i(tag, String.format("%s: starting '%s' "
                + "on device %s, API version %d",
                testSuite, toString(), thisDeviceName, thisApiVersion));
    }

    public void logEnd(String tag) {
        Log.i(tag, String.format("RenderScript test '%s': %s",
                toString(), getResultString()));
    }

    public UnitTestResult getResult() {
        return mResult;
    }

    public String getResultString() {
        return mResult.toString();
    }

    public boolean getSuccess() {
        return mResult == UnitTestResult.UT_SUCCESS;
    }

    public void runTest() {
        mResult = UnitTestResult.UT_RUNNING;
        run();
        if (mCountDownLatch != null) {
            try {
                boolean success = mCountDownLatch.await(5 * 60, TimeUnit.SECONDS);
                if (!success) {
                    failTest();
                    Log.e(TAG, String.format("Unit test %s waited too long for pass/fail message",
                          toString()));
                }
            } catch (InterruptedException e) {
                failTest();
                Log.e(TAG, String.format("Unit test %s raised InterruptedException when " +
                        "listening for pass/fail message", toString()));
            }
        }
        switch (mResult) {
            case UT_NOT_STARTED:
            case UT_RUNNING:
                Log.w(TAG, String.format("unexpected unit test result for test %s: %s",
                        this.toString(), mResult.toString()));
                break;
        }
    }

    abstract protected void run();

    @Override
    public String toString() {
        return mName;
    }


    /**
     * Throws RuntimeException if any tests have the same name.
     */
    public static void checkDuplicateNames(Iterable<UnitTest> tests) {
        Set<String> names = new HashSet<>();
        List<String> duplicates = new ArrayList<>();
        for (UnitTest test : tests) {
            String name = test.toString();
            if (names.contains(name)) {
                duplicates.add(name);
            }
            names.add(name);
        }
        if (!duplicates.isEmpty()) {
            throw new RuntimeException("duplicate name(s): " + duplicates);
        }
    }

    public static Iterable<Class<? extends UnitTest>> getProperSubclasses(Context ctx)
            throws ClassNotFoundException, IOException {
        return getProperSubclasses(UnitTest.class, ctx);
    }

    /** Returns a list of all proper subclasses of the input class */
    private static <T> Iterable<Class<? extends T>> getProperSubclasses(Class<T> klass, Context ctx)
            throws ClassNotFoundException, IOException {
        ArrayList<Class<? extends T>> ret = new ArrayList<>();
        DexFile df = new DexFile(ctx.getPackageCodePath());
        Enumeration<String> iter = df.entries();
        while (iter.hasMoreElements()) {
            String s = iter.nextElement();
            Class<?> cur = Class.forName(s);
            while (cur != null) {
                if (cur.getSuperclass() == klass) {
                    break;
                }
                cur = cur.getSuperclass();
            }
            if (cur != null) {
                ret.add((Class<? extends T>) cur);
            }
        }
        return ret;
    }
}

