/*
 * License information at https://github.com/Caltech-IPAC/firefly/blob/master/License.txt
 */
package edu.caltech.ipac.firefly.core;


import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import edu.caltech.ipac.firefly.core.layout.BaseRegion;
import edu.caltech.ipac.firefly.core.layout.Region;
import edu.caltech.ipac.firefly.data.Status;
import edu.caltech.ipac.firefly.data.userdata.UserInfo;
import edu.caltech.ipac.firefly.rpc.UserServices;
import edu.caltech.ipac.firefly.ui.BaseDialog;
import edu.caltech.ipac.firefly.ui.GwtUtil;
import edu.caltech.ipac.firefly.ui.PopupUtil;
import edu.caltech.ipac.firefly.ui.ServerTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * @author tatianag
 * @version $Id: LoginManagerImpl.java,v 1.39 2012/04/05 23:08:24 tatianag Exp $
 */
public class LoginManagerImpl implements LoginManager {

    public static String LOGIN_INFO_REGION = "loginInfo";
    public static String COOKIE_USER_KEY = "usrkey";
//    public static int SYNC_INTERVAL_MILLISEC = 2*60*1000;  // 2 mins

    private List<SignInListener> listeners;
//    Map<String, String> userPrefs;
//    Timer syncTimer;
    private BaseDialog warningDialog;
    private LoginToolbar toolbar;


    public LoginManagerImpl() {
        listeners = new ArrayList<SignInListener>();
//        userPrefs = new HashMap<String, String>();
    }

    public void addSignInListener(SignInListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeSignInListener(SignInListener listener) {
        if (listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    /**************************************************************/
    /*****            Login Manager Interface                ******/
    /**************************************************************/

    public Region makeLoginRegion() {
        toolbar = makeToolbar();
        BaseRegion r = new BaseRegion(LOGIN_INFO_REGION){
            @Override
            public Widget getDisplay() {
                GwtUtil.setStyle(toolbar, "rightPadding", "5px");
                return toolbar;
            }
        };
//        r.getDisplay().setWidth("100%");
        r.setAlign(BaseRegion.ALIGN_RIGHT);
        return r;
    }

    public UserInfo getLoginInfo() {
        return toolbar == null ? UserInfo.newGuestUser() : toolbar.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return toolbar != null && (!toolbar.getCurrentUser().isGuestUser());
    }

    public LoginToolbar getToolbar() {
        return toolbar;
    }

    public void refreshUserInfo(boolean inclPreferences) {
        if(toolbar != null) {
            toolbar.refreshUserInfo(inclPreferences);
        }
    }

    public String getPreference(String prefname) {
        Map<String, String> prefs = getPreferences();
        return  (prefs != null && prefs.containsKey(prefname)) ? prefs.get(prefname) : null;
    }

    public void setPreference(String prefname, String prefvalue) {
        Map<String,String> prefs = new HashMap<String,String>(1);
        prefs.put(prefname, prefvalue);
        setPreferences(prefs, new AsyncCallback<Status>(){
                        public void onFailure(Throwable caught) {}
                        public void onSuccess(Status result) {}
                    });
    }

    public void setPreferences(Map<String, String> prefmap, AsyncCallback<Status> callback) {
        boolean needUpdate = false;
        for (String prefname : prefmap.keySet()) {
            needUpdate = needUpdate | setUserPref(prefname, prefmap.get(prefname));
        }
        if (needUpdate) {
            updatePreferences(prefmap, callback);
        }
    }

    private Map<String,String> getPreferences() {
        UserInfo uinfo = toolbar.getCurrentUser();
        if (uinfo != null) {
            return uinfo.getPreferences();
        }
        return null;
    }

    public Set<String> getPrefNames() {
        Map<String, String> prefs = getPreferences();
        return prefs == null ? null : prefs.keySet();
    }

//====================================================================
//
//====================================================================

    private void onUserChanged(UserInfo info) {
        String msg;
        if (info.isGuestUser()) {
            msg = "You've been logged out.  You are now a Guest user.";
        } else {
            msg = "You've been logged in as " + info.getLoginName() + " from another window.";
        }
        if (warningDialog != null) {
            warningDialog.setVisible(false);
        }
        warningDialog = PopupUtil.showInfo(msg);
    }

    private void updatePreferences(final Map<String, String> prefmap, final AsyncCallback<Status> callback) {

        ServerTask task = new ServerTask<Status>() {
            public void onSuccess(Status result) {
                if (result.getStatus() != 0) {
                    if (callback != null) {
                        callback.onFailure(new Exception(result.getMessage()));
                    } else {
                        PopupUtil.showInfo("Failed to update preferences <br>" + result.getMessage());
                    }
                } else {
                    callback.onSuccess(result);
                    // no need to fire events on preference update
                    //WebEventManager.getAppEvManager().fireEvent(new WebEvent(this, Name.PREFERENCE_UPDATE, prefmap));
                }
            }

            public void doTask(AsyncCallback<Status> passAlong) {
                UserServices.App.getInstance().updatePreferences(prefmap, passAlong);
            }
        };
        task.start();
    }

    private boolean setUserPref(String prefname, String prefvalue) {

        Map<String, String> prefs = getPreferences();
        if (prefs != null) {
            String oldvalue = prefs.get(prefname);
            if (prefvalue == null || !prefvalue.equals(oldvalue)) {
                prefs.put(prefname, prefvalue);
                return true;
            }
        }

        return false;
    }

    protected LoginToolbar makeToolbar() { return new LoginToolbar(true); }

}
