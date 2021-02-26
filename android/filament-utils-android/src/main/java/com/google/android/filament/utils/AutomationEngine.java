/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.google.android.filament.utils;

import androidx.annotation.NonNull;

import com.google.android.filament.View;
import com.google.android.filament.MaterialInstance;
import com.google.android.filament.Renderer;

public class AutomationEngine {
    private long mNativeObject;

    /**
     * Allows users to toggle screenshots, change the sleep duration between tests, etc.
     */
    public static class Options {
        /**
         * Minimum time that the engine waits between applying a settings object and subsequently
         * taking a screenshot. After the screenshot is taken, the engine immediately advances to
         * the next test case. Specified in seconds.
         */
        float sleepDuration = 0.2f;

        /**
         * Similar to sleepDuration, but expressed as a frame count. Both the minimum sleep time
         * and the minimum frame count must be elapsed before the engine advances to the next test.
         */
        int minFrameCount = 2;

        /**
         * If true, test progress is dumped to the utils Log (info priority).
         */
        boolean verbose = true;
    }

    /**
     * Creates an automation engine from a JSON specification.
     *
     * @param jsonSpec Valid JSON string that conforms to the automation schema.
     */
    public AutomationEngine(@NonNull String jsonSpec) {
        mNativeObject = nCreateAutomationEngine(jsonSpec);
        if (mNativeObject == 0) throw new IllegalStateException("Couldn't create AutomationEngine");
    }

    /**
     * Creates an automation engine for the default test sequence.
     *
     * To see how the default test sequence is generated, search for DEFAULT_AUTOMATION.
     */
    public AutomationEngine() {
        mNativeObject = nCreateDefaultAutomationEngine();
        if (mNativeObject == 0) throw new IllegalStateException("Couldn't create AutomationEngine");
    }

    /**
     * Configures the automation engine for users who wish to set up a custom sleep time
     * between tests, etc.
     */
    public void setOptions(@NonNull Options options) {
        nSetOptions(mNativeObject, options.sleepDuration, options.minFrameCount, options.verbose);
    }

    /**
     * Activates automation. During the subsequent call to tick(), the first test is applied
     * and the engine enters the running state.
     */
    public void startRunning() { nStartRunning(mNativeObject); }

    /**
     * Activates automation, but enters a paused state until the user calls signalBatchMode().
     */
    public void startBatchMode() { nStartBatchMode(mNativeObject); }

    /**
     * Notifies the automation engine that time has passed and a new frame has been rendered.
     *
     * This is when settings get applied, screenshots are (optionally) exported, etc.
     *
     * @param view          The Filament View that automation pushes changes to.
     * @param materials     Optional set of of materials that can receive parameter tweaks.
     * @param renderer      The Filament Renderer that can be used to take screenshots.
     * @param deltaTime     The amount of time that has passed since the previous tick in seconds.
     */
    public void tick(@NonNull View view, @Nullable MaterialInstance[] materials,
            @NonNull Renderer renderer, float deltaTime) {
        long[] nativeMaterialInstances = null;
        if (materials != null) {
            nativeMaterialInstances = new long[materials.length];
            for (int i = 0; i < nativeMaterialInstances.length; i++) {
                nativeMaterialInstances[i] = materials[i].getNativeObject();
            }
        }
        long nativeView = view.getNativeObject();
        long nativeRenderer = renderer.getNativeObject();
        nTick(mNativeObject, nativeView, nativeMaterialInstances, nativeRenderer, deltaTime);
    }

    /**
     * Signals that batch mode can begin. Call this after all meshes and textures finish loading.
     */
    public void signalBatchMode() { nSignalBatchMode(mNativeObject); }

    /**
     * Cancels an in-progress automation session.
     */
    public void stopRunning() { nStopRunning(mNativeObject); }

    /**
     * Returns true if automation is in batch mode and all tests have finished.
     */
    public boolean shouldClose() { return nShouldClose(mNativeObject); }

    @Override
    protected void finalize() throws Throwable {
        nDestroyAutomationEngine(mNativeObject);
        super.finalize();
    }

    private static native long nCreateAutomationEngine(String jsonSpec);
    private static native long nCreateDefaultAutomationEngine();
    private static native void nSetOptions(long nativeObject, float sleepDuration, int minFrameCount, boolean verbose);
    private static native void nStartRunning(long nativeObject);
    private static native void nStartBatchMode(long nativeObject);
    private static native void nTick(long nativeObject, long view, long[] materials, long renderer, float deltaTime);
    private static native void nSignalBatchMode(long nativeObject);
    private static native void nStopRunning(long nativeObject);
    private static native boolean nShouldClose(long nativeObject);
    private static native void nDestroy(long nativeObject);
}
