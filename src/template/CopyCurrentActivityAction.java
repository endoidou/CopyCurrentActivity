/*
 * Copyright (C) 2017 Endo Jun.
 *
 * Foobar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package template;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CopyCurrentActivityAction extends AnAction {

    private static final String ERROR_MESSAGE = "Error";

    private static final String ADB_SUBPATH = "/platform-tools/";
    private static final String ANDROID_SDK_TYPE_NAME = "Android SDK";
    private static final String ADB_WINDOWS = "adb.exe";
    private static final String ADB_UNIX = "adb";

    @Override
    public void actionPerformed(AnActionEvent e) {
        String content;

        String androidSdkPath = getAndroidSdkPath();
        String adbPath = getAdbPath(androidSdkPath);

        ProcessBuilder processBuilder;
        processBuilder = new ProcessBuilder(adbPath, "shell", "dumpsys", "activity", "|", "grep", "mFocusedActivity", "|", "cut", "-d", "' '", "-f6");

        try {
            Process mProcess = processBuilder.start();
            InputStream is = mProcess.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            br.close();

            content = getActivityName(sb.toString());

            if (content.equals(ERROR_MESSAGE)) {
                Notifications.Bus.notify(
                        new Notification("check Activity", "Copy Current Activity", ERROR_MESSAGE, NotificationType.ERROR));
                return;
            }

            setClipboard(content);

        } catch (IOException e1) {
            e1.printStackTrace();
            content = e1.getMessage();
        }

        Notifications.Bus.notify(
                new Notification("check Activity", "Activity Name", content, NotificationType.INFORMATION));
    }

    @Nullable
    private String getAndroidSdkPath() {
        Sdk[] allSdks = ProjectJdkTable.getInstance().getAllJdks();
        for (Sdk sdk : allSdks) {
            String sdkTypeName = sdk.getSdkType().getName();
            if (ANDROID_SDK_TYPE_NAME.equals(sdkTypeName)) {
                return sdk.getHomePath();
            }
        }
        return null;
    }

    @NotNull
    private String getActivityName(String activityName) {
        if (activityName.isEmpty()) {
            return ERROR_MESSAGE;
        }

        // Keep only the class name
        int dotIndex = activityName.lastIndexOf('.');
        if (dotIndex != -1) {
            activityName = activityName.substring(dotIndex + 1);
        }
        return activityName;
    }

    @NotNull
    private String getAdbPath(String androidSdkPath) {
        String adb;
        if (SystemInfo.isWindows) {
            adb = ADB_WINDOWS;
        } else {
            adb = ADB_UNIX;
        }

        return androidSdkPath + ADB_SUBPATH + adb;
    }

    private void setClipboard(String activityName) {
        Toolkit kit = Toolkit.getDefaultToolkit();
        Clipboard clip = kit.getSystemClipboard();
        StringSelection ss = new StringSelection(activityName);
        clip.setContents(ss, ss);
    }
}
