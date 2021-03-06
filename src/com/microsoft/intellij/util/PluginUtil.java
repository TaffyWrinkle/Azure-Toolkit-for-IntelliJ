/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.intellij.util;

import com.intellij.ide.DataManager;
import com.intellij.ide.projectView.impl.ProjectRootsUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.interopbridges.tools.windowsazure.WindowsAzureInvalidProjectOperationException;
import com.interopbridges.tools.windowsazure.WindowsAzureProjectManager;
import com.interopbridges.tools.windowsazure.WindowsAzureRole;
import com.interopbridges.tools.windowsazure.WindowsAzureRoleComponentImportMethod;
import com.microsoft.intellij.AzurePlugin;
import com.microsoft.wacommon.utils.WACommonException;

import java.io.File;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class PluginUtil {
    private static final Logger LOG = Logger.getInstance("#com.microsoft.intellij.util.PluginUtil");
    public static final String BASE_PATH = "${basedir}" + File.separator + "..";

    public static boolean isModuleRoot(VirtualFile moduleFolder, Module module) {
        return moduleFolder != null && ProjectRootsUtil.isModuleContentRoot(moduleFolder, module.getProject());
    }

    public enum ProjExportType {WAR, EAR, JAR}

    /**
     * This method returns currently selected project in workspace.
     *
     * @return Project
     */
    public static Project getSelectedProject() {
        DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
        return DataKeys.PROJECT.getData(dataContext);
    }

    public static String getModulePath(Module module) {
        return new File(module.getModuleFilePath()).getParent();
    }

    /**
     * This method will display the error message box when any error occurs.It takes two parameters
     *
     * @param title   the text or title of the window.
     * @param message the message which is to be displayed
     */
    public static void displayErrorDialog(String title, String message) {
        Messages.showErrorDialog(message, title);
    }

    public static void displayErrorDialogAndLog(String title, String message, Exception e) {
        LOG.error(message, e);
        displayErrorDialog(title, message);
    }

    public static void displayErrorDialogInAWTAndLog(final String title, final String message, Exception e) {
        LOG.error(message, e);
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                PluginUtil.displayErrorDialog(title, message);
            }
        });
    }

    public static void displayInfoDialog(String title, String message) {
        Messages.showInfoMessage(message, title);
    }

    public static void displayWarningDialog(String title, String message) {
        Messages.showWarningDialog(message, title);
    }


    public static void displayWarningDialogInAWT(final String title, final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                displayWarningDialog(title, message);
            }
        });
    }

    /**
     * Gets location of Azure Libraries
     *
     * @throws WACommonException
     */
    public static String getAzureLibLocation() throws WACommonException {
        String libLocation;
        try {
            String pluginInstLoc = String.format("%s%s%s", PathManager.getPluginsPath(), File.separator, AzurePlugin.COMMON_LIB_PLUGIN_ID);
            libLocation = String.format(pluginInstLoc + "%s%s", File.separator, "lib");
            File file = new File(String.format(libLocation + "%s%s", File.separator, message("sdkLibBaseJar")));
            if (!file.exists()) {
                throw new WACommonException(message("SDKLocErrMsg"));
            }
        } catch (WACommonException e) {
            e.printStackTrace();
            throw e;
        }
        return libLocation;
    }

    /**
     * Determines whether a folder is a role folder or not.
     *
     * @return true if the folder is a role folder else false.
     */
    public static boolean isRoleFolder(VirtualFile vFile, Module module) {
        boolean retVal = false;
        try {
            WindowsAzureProjectManager projMngr = WindowsAzureProjectManager.load(new File(getModulePath(module)));
            WindowsAzureRole role = projMngr.roleFromPath(new File(vFile.getPath()));
            if (role != null) {
                retVal = true;
            }
        } catch (WindowsAzureInvalidProjectOperationException e) {
//            not a role folder - silently ignore
        }
        return retVal;
    }

    /**
     * This method find the absolute path from
     * relative path.
     *
     * @param path : relative path
     * @return absolute path
     */
    public static String convertPath(Project project, String path) {
        String newPath = "";
        if (path.startsWith(BASE_PATH)) {
            String projectPath = project.getBasePath();
            String rplStr = path.substring(path.indexOf('}') + 4, path.length());
            newPath = String.format("%s%s", projectPath, rplStr);
        } else {
            newPath = path;
        }
        return newPath;
    }

    /**
     * This method returns the deployment name of any component
     * it also prepares the name if name is not specified.
     *
     * @param path
     * @param method
     * @param asName
     * @return
     */
    public static String getAsName(Project project, String path,
                                   WindowsAzureRoleComponentImportMethod method,
                                   String asName) {
        String name;
        if (asName.isEmpty()) {
            name = new File(path).getName();
            if (method == WindowsAzureRoleComponentImportMethod.auto) {
//                ProjExportType type = ProjectNatureHelper.getProjectNature(PluginUtil.findModule(project, convertPath(project, path)));
                // todo!!!
                ProjExportType type = ProjExportType.WAR;
                name = String.format("%s%s%s", name, ".", type.name().toLowerCase());
            } else if (method == WindowsAzureRoleComponentImportMethod.zip) {
                name = String.format("%s%s", name, ".zip");
            }
        } else {
            name = asName;
        }
        return name;
    }

    public static Module findModule(Project project, String path) {
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (PluginUtil.getModulePath(module).equals(path)) {
                return module;
            }
        }
        return null;
    }
}
