/*
 *  Copyright (C) 2013 - 2014 Alexander "Evisceration" Martinz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.namelessrom.devicecontrol.preferences;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.execution.ShellWriter;

import alexander.martinz.libs.hardware.utils.IoUtils;
import alexander.martinz.libs.materialpreferences.MaterialListPreference;

/**
 * Automatically handles reading to files to automatically set the value,
 * writing to files on preference change, even with multiple files,
 * handling bootup restoration.
 */
public class AutoListPreference extends MaterialListPreference {
    private boolean mStartup = true;
    private boolean mMultiFile = false;

    private String mPath;
    private String[] mPaths;

    private String mCategory; // = BootupConfig.CATEGORY_EXTRAS;

    private boolean mShouldReinit;

    public AutoListPreference(Context context) {
        super(context);
    }

    public AutoListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoListPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override public boolean init(final Context context, final AttributeSet attrs) {
        if (!super.init(context, attrs)) {
            return false;
        }

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AwesomePreference);

        int filePath = -1, filePathList = -1;
        if (a != null) {
            filePath = a.getResourceId(R.styleable.AwesomePreference_filePath, -1);
            filePathList = a.getResourceId(R.styleable.AwesomePreference_filePathList, -1);
            mStartup = a.getBoolean(R.styleable.AwesomePreference_startup, mStartup);
            mMultiFile = a.getBoolean(R.styleable.AwesomePreference_multifile, mMultiFile);
            a.recycle();
        }

        final Resources res = context.getResources();
        if (filePath != -1) {
            mPath = IoUtils.checkPath(res.getString(filePath));
            mPaths = null;
        } else if (filePathList != -1) {
            mPaths = res.getStringArray(filePathList);
            mPath = IoUtils.checkPaths(mPaths);
            if (TextUtils.isEmpty(mPath) || !mMultiFile) {
                mPaths = null;
            }
        } else {
            mPath = "";
            mPaths = null;
        }

        handleSelf(true);

        return true;
    }

    public void initValue() {
        if (isSupported()) {
            // TODO: automated parsing and adapter creation
            final String value = IoUtils.readOneLine(mPath);
            if (!TextUtils.isEmpty(value)) {
                setValue(value);
            }
        }
    }

    public void setCategory(String category) {
        mCategory = category;
    }

    public String getCategory() {
        return mCategory;
    }

    public String getPath() { return mPath; }

    public boolean isSupported() {
        return ((mPath != null && !mPath.isEmpty()) || (mPaths != null && mPaths.length != 0));
    }

    public void setPath(String path) {
        path = IoUtils.checkPath(path);
        if (!TextUtils.isEmpty(path)) {
            mPath = path;
            mPaths = null;
        }
    }

    public void setPaths(String[] paths) {
        String path = IoUtils.checkPaths(paths);
        if (!TextUtils.isEmpty(path)) {
            mPath = path;
            if (mPath.isEmpty() || !mMultiFile) {
                mPaths = null;
            } else {
                mPaths = paths;
            }
        }
    }

    public void setMultiFile(boolean isMultiFile) {
        mMultiFile = isMultiFile;
    }

    public void setStartup(boolean isStartup) {
        mStartup = isStartup;
    }

    public void setShouldReinint(boolean shouldReinint) {
        mShouldReinit = shouldReinint;
    }

    public void writeValue(final String value) {
        if (!isSupported()) {
            return;
        }

        if (mPaths != null && mMultiFile) {
            final int length = mPaths.length;
            for (int i = 0; i < length; i++) {
                ShellWriter.with().enableRoot().write(value).into(mPaths[i]).start();
                /*if (mStartup) {
                    BootupConfig.setBootup(new BootupItem(
                            mCategory, getKey() + String.valueOf(i), mPaths[i], value, true));
                }*/
            }
        } else {
            ShellWriter.with().enableRoot().write(value).into(mPath).start();
            /*if (mStartup) {
                BootupConfig.setBootup(
                        new BootupItem(mCategory, getKey(), mPath, value, true));
            }*/
        }

        if (mShouldReinit) {
            postDelayed(this::initValue, 200);
        }
    }

    public void handleSelf(boolean handleSelf) {
        MaterialPreferenceChangeListener listener = null;
        if (handleSelf) {
            listener = (pref, o) -> {
                writeValue(String.valueOf(o));
                return true;
            };
        }
        setOnPreferenceChangeListener(listener);
    }

}
