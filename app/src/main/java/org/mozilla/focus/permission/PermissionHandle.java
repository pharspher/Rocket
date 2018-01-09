package org.mozilla.focus.permission;

import android.os.Parcelable;
import android.support.design.widget.Snackbar;

import java.io.Serializable;

public interface PermissionHandle {

    int TRIGGER_DIRECT = 0;
    int TRIGGER_GRANTED = 1;
    int TRIGGER_SETTING = 2;

    /**
     * action implementations that requires a permission that is used in this
     * Activity/Fragment. When permission is already granted, this is called.
     *
     * @param  permission the required permission
     * @param  actionId the designated action
     * @param  params the optional params that is used in this action
     */
    void doActionDirect(String permission, int actionId, Parcelable params);

    /**
     * action implementations that requires a permission that is used in this
     * Activity/Fragment. When permission is not granted but requested, this is called.
     *
     * @param  permission the required permission
     * @param  actionId the designated action
     * @param  params the optional params that is used in this action
     */
    void doActionGranted(String permission, int actionId, Parcelable params);

    /**
     * action implementations that requires a permission that is used in this
     * Activity/Fragment. When permission is not granted but user later tried to visit Settings,
     * this is called. Note that Activity does my be destroyed so be aware to test
     * with ALWAYS_CLOSE_ACTIVITY enabled in developer options.
     *
     * @param  permission the required permission
     * @param  actionId the designated action
     * @param  params the optional params that is used in this action
     */
    void doActionSetting(String permission, int actionId, Parcelable params);

    /**
     * A mapping of used string that is used in the DoNotAskAgainDialog
     *
     * @param  actionId the designated action
     * @return      the string id of the message
     */
    int getDoNotAskAgainDialogString(int actionId);

    /**
     * The generation of the AskAgainSnackBar a Bar is returned instead of String since we need to
     * know which view to insert and we want to add callbacks in {@link PermissionHandler}
     *
     * @param  actionId the designated action
     * @return      a {@link Snackbar} instance which can be showed directly
     */
    Snackbar makeAskAgainSnackBar(int actionId);

    /**
     * Try to Request the corresponding permission to this actionId
     *
     * @param  actionId the designated action
     */
    void requestPermissions(int actionId);
}
