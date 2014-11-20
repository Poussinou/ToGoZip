/*
 * Copyright (C) 2014 k3b
 * 
 * This file is part of de.k3b.android.toGoZip (https://github.com/k3b/ToGoZip/) .
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package de.k3b.android.toGoZip;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

import de.k3b.android.AndroidCompressJob;

/** show settings/config activity. On Start and Exit checks if data is valid.  */
public class SettingsActivity extends PreferenceActivity {

    /** if not null: try to execute add2zip on finish */
    private static File[] fileToBeAdded;

    /** public api to start settings-activity */
    public static void show(Context context, File[] fileToBeAdded) {
        final Intent i = new Intent(context,SettingsActivity.class);

        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, "SettingsActivity.show(startActivity='" + i
                    + "')");
        }

        SettingsActivity.fileToBeAdded = fileToBeAdded;
        context.startActivity(i);

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SettingsImpl.init(this);

        this.addPreferencesFromResource(R.xml.preferences);

        showAlertOnError();
    }

    /** return false if no error */
    private boolean showAlertOnError() {
        boolean canWriteCurrent = SettingsImpl.init(this);

        if (!canWriteCurrent) {
            String currentZipPath = SettingsImpl.getZipfile();
            String defaultZipPath = SettingsImpl.getDefaultZipPath(this);
            boolean canWriteDefault = SettingsImpl.canWrite(defaultZipPath);

            String format = (canWriteDefault)
                    ? getString(R.string.ERR_NO_WRITE_PERMISSIONS_CHANGE_TO_DEFAULT)
                    : getString(R.string.ERR_NO_WRITE_PERMISSIONS);

            if (defaultZipPath.compareTo(currentZipPath) == 0) {
                currentZipPath = ""; // display name only once
            }
            String msg = String.format(
                    format,
                    currentZipPath,
                    defaultZipPath);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(msg);
            builder.setTitle(R.string.title_activity_add2zip);
            builder.setIcon(R.drawable.ic_launcher);
            //builder.setPositiveButton(R.string.delete, this);
            //builder.setNegativeButton(R.string.cancel, this);

            if (canWriteDefault) {
                builder.setNeutralButton(R.string.cmd_use_default, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDefault();
                    }
                });
            }

            builder.setNegativeButton(R.string.cmd_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    cancel();
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    cancel();
                }
            });

            builder.setPositiveButton(R.string.cmd_edit, null);

            builder.show();
        }
        return !canWriteCurrent;
    }

    /** android os function to end this activity. Hooked to verify that data is valid. */
    @Override
    public void finish() {
        if (Global.debugEnabled) {
            Log.i(Global.LOG_CONTEXT, "SettingsActivity.finish");
        }

        if (!showAlertOnError()) {
            finishWithoutCheck();
        }
    }

    private void finishWithoutCheck() {
        if (SettingsActivity.fileToBeAdded != null) {
            SettingsImpl.init(this);
            AndroidCompressJob.addToZip(this, new File(SettingsImpl.getZipfile()), SettingsActivity.fileToBeAdded);
            SettingsActivity.fileToBeAdded = null;
        }
        super.finish();
    }

    /** resets zip to default and restart settings activity. */
    private void setDefault() {
        String defaultZipPath = SettingsImpl.getDefaultZipPath(this);
        SettingsImpl.setZipfile(this, defaultZipPath);
        File[] fileToBeAdded = SettingsActivity.fileToBeAdded;
        SettingsActivity.fileToBeAdded = null; // do not start add2zip
        finishWithoutCheck();
        // restart with new settings
        show(this,fileToBeAdded);
    }

    private void cancel() {
        if (SettingsActivity.fileToBeAdded != null) {
            Toast.makeText(this, getString(R.string.WARN_ADD_CANCELED), Toast.LENGTH_LONG).show();
            SettingsActivity.fileToBeAdded = null;
        }

        finishWithoutCheck();
    }


}