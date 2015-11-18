//
//   Copyright 2013 jordi domenech <http://iamyellow.net, jordi@iamyellow.net>
//	 Copyright 2015	Christoph Eck <http://shuubz.com, christoph.eck@movento.com>
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.
//

package net.iamyellow.gcmjs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.kroll.KrollFunction;
import org.appcelerator.kroll.KrollModule;
import org.appcelerator.kroll.KrollRuntime;
import org.appcelerator.kroll.annotations.Kroll;
import org.appcelerator.kroll.common.Log;
import org.appcelerator.titanium.TiApplication;

import android.content.Context;
import android.os.AsyncTask;

import com.google.android.gms.gcm.GoogleCloudMessaging;

@Kroll.module(name = "Gcmjs", id = "net.iamyellow.gcmjs")
public class GcmjsModule extends KrollModule {

	private GoogleCloudMessaging gcm;

	// *************************************************************
	// constants

	private static final String PROPERTY_ON_SUCCESS = "success";
	private static final String PROPERTY_ON_ERROR = "error";
	private static final String PROPERTY_ON_MESSAGE = "callback";
	private static final String PROPERTY_ON_UNREGISTER = "unregister";
	private static final String PROPERTY_ON_DATA = "data";
	private static final String PROPERTY_SENDER_IDS = "senderid";

	private static final String EVENT_PROPERTY_DEVICE_TOKEN = "deviceToken";
	private static final String EVENT_PROPERTY_ERROR = "error";

	public static String PROPERTY_TI_APP_SENDER_ID = "GCM_sender_id";
	public static final boolean DBG = org.appcelerator.kroll.common.TiConfig.LOGD;

	// *************************************************************
	// logging

	private static final String LCAT = "gcmjs";

	public static void logd(String msg) {
		if (DBG) {
			Log.d(LCAT, msg);
		}
	}

	public static void logw(String msg) {
		Log.e(LCAT, msg);
	}
	
	// *************************************************************
	// members
	private List<String> senderId;

	// *************************************************************
	// callbacks

	private static KrollFunction onSuccessCallback;
	private static KrollFunction onErrorCallback;
	private static KrollFunction onMessageCallback;
	private static KrollFunction onUnregisterCallback;
	private static KrollFunction onDataCallback;

	// *************************************************************
	// singleton

	private static GcmjsModule instance = null;

	public static GcmjsModule getInstance() {
		return instance;
	}

	// *************************************************************
	// constructor

	private static AppStateListener appStateListener = null;

	public GcmjsModule() {
		super();

		onSuccessCallback = null;
		onErrorCallback = null;
		onMessageCallback = null;
		onUnregisterCallback = null;
		onDataCallback = null;
		senderId = new ArrayList<String>();

		instance = this;
		if (appStateListener == null) {
			appStateListener = new AppStateListener();
			TiApplication.addActivityTransitionListener(appStateListener);
		}
	}

	// *************************************************************
	// related to activities lifecycle

	public boolean isInFg() {
		if (!KrollRuntime.isInitialized()) {
			return false;
		}

		if (AppStateListener.oneActivityIsResumed) {
			return true;
		}

		return false;
	}

	@Kroll.getProperty
	@Kroll.method
	public boolean getIsLauncherActivity() {
		return AppStateListener.appWasNotRunning;
	}

	// *************************************************************
	// registration

	@Kroll.method
	public void registerForPushNotifications(KrollDict arg) {

		// collect parameters
		if(arg.containsKeyAndNotNull(PROPERTY_ON_SUCCESS) && arg.get(PROPERTY_ON_SUCCESS) instanceof KrollFunction) {
			logd("Setting onSuccessCallback.");
			onSuccessCallback = (KrollFunction) arg.get(PROPERTY_ON_SUCCESS);
		}
		
		if(arg.containsKeyAndNotNull(PROPERTY_ON_ERROR) && arg.get(PROPERTY_ON_ERROR) instanceof KrollFunction) {
			logd("Setting onErrorCallback.");
			onErrorCallback = (KrollFunction) arg.get(PROPERTY_ON_ERROR);
		}
		
		if(arg.containsKeyAndNotNull(PROPERTY_ON_MESSAGE) && arg.get(PROPERTY_ON_MESSAGE) instanceof KrollFunction) {
			logd("Setting onMessageCallback.");
			onMessageCallback = (KrollFunction) arg.get(PROPERTY_ON_MESSAGE);
		}
		
		if(arg.containsKeyAndNotNull(PROPERTY_ON_UNREGISTER) && arg.get(PROPERTY_ON_UNREGISTER) instanceof KrollFunction) {
			logd("Setting onUnregisterCallback.");
			onUnregisterCallback = (KrollFunction) arg.get(PROPERTY_ON_UNREGISTER);
		}
		
		if(arg.containsKeyAndNotNull(PROPERTY_ON_DATA) && arg.get(PROPERTY_ON_DATA) instanceof KrollFunction) {
			logd("Setting onDataCallback.");
			onDataCallback = (KrollFunction) arg.get(PROPERTY_ON_DATA);
		}
		
		if(arg.containsKeyAndNotNull(PROPERTY_SENDER_IDS) ) {			
			if (arg.get(PROPERTY_SENDER_IDS) instanceof Object[]) {
				senderId.addAll(Arrays.asList(arg.getStringArray(PROPERTY_SENDER_IDS)));
				logd("Setting senderIds.");
			} else if(arg.get(PROPERTY_SENDER_IDS) instanceof Object) {
				senderId.add(arg.getString(PROPERTY_SENDER_IDS));
				logd("Setting senderId.");
			}
		}

		// if we're executing this, we **SHOULD BE** in fg
		AppStateListener.oneActivityIsResumed = true;

		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				try {
					if (gcm == null) {
						Context context = TiApplication.getInstance().getApplicationContext();
						gcm = GoogleCloudMessaging.getInstance(context);
					}
					String tiAppSenderId =  TiApplication.getInstance().getAppProperties().getString(GcmjsModule.PROPERTY_TI_APP_SENDER_ID, "");
					// a Set prevents adding duplicate sender id
					Set<String> ids = new LinkedHashSet<String>();
					if(tiAppSenderId.isEmpty() == false) {
						ids.add(tiAppSenderId);
					}
					if(senderId.size() > 0) {
						ids.addAll(senderId);
					}
					if(ids.size() == 0) {
						throw new Exception("Could not found any sender id in tiapp.xml or in object");
					}
					String [] idArray = new String[ids.size()];
					String registrationId = gcm.register(ids.toArray(idArray));
					msg = "Device registered: registrationId = " + registrationId;
					fireSuccess(registrationId);
				} catch (Exception e) {
					msg = "Error: " + e.getMessage();
					fireError(msg);
				}
				return msg;
			}

			@Override
			protected void onPostExecute(String msg) {
			}
		}.execute(null, null, null);
	}

	// *************************************************************
	// main activity class name helper

	@Kroll.getProperty
	@Kroll.method
	public String getMainActivityClassName() {
		return TiApplication.getInstance().getPackageManager()
				.getLaunchIntentForPackage(TiApplication.getInstance().getPackageName()).getComponent().getClassName();
	}

	// *************************************************************
	// data

	private static HashMap<String, Object> data = null;
	private static boolean pendingData = false;

	@SuppressWarnings("rawtypes")
	@Kroll.getProperty
	@Kroll.method
	public HashMap getData() {
		return data;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Kroll.setProperty
	@Kroll.method
	public void setData(HashMap pData) {
		data = pData;

		if (AppStateListener.appWasNotRunning) {
			logd("Setting data while we're in bg.");
			return;
		}

		if (data != null) {
			logd("Mark pending data.");
			pendingData = true;
		} else {
			logd("No pending data to mark.");
		}
	}

	public void executeActionsWhileIfForeground() {
		if (pendingData) {
			logd("Found pending data.");
			pendingData = false;
			fireData();
		} else {
			logd("No pending data found.");
		}
	}

	// *************************************************************
	// events

	public void fireSuccess(String registrationId) {
		if (onSuccessCallback != null) {
			HashMap<String, String> result = new HashMap<String, String>();
			result.put(EVENT_PROPERTY_DEVICE_TOKEN, registrationId);
			onSuccessCallback.call(getKrollObject(), result);
		}
		KrollDict event = new KrollDict();
		event.put("deviceToken", registrationId);
		fireEvent("success", event);
	}

	public void fireError(String error) {
		if (onErrorCallback != null) {
			HashMap<String, String> result = new HashMap<String, String>();
			result.put(EVENT_PROPERTY_ERROR, error);
			onErrorCallback.call(getKrollObject(), result);
		}
		KrollDict event = new KrollDict();
		event.put("error", error);
		fireEvent("error", event);
	}

	@Kroll.method
	public void fireUnregister(String registrationId) {
		if (onUnregisterCallback != null) {
			HashMap<String, String> result = new HashMap<String, String>();
			result.put(EVENT_PROPERTY_DEVICE_TOKEN, registrationId);
			onUnregisterCallback.call(getKrollObject(), result);
		}
		KrollDict event = new KrollDict();
		event.put("deviceToken", registrationId);
		fireEvent("unregister", event);
	}

	public void fireMessage(KrollDict event) {
		if (onMessageCallback != null) {
			onMessageCallback.call(getKrollObject(), event);
		}
		fireEvent("callback", event);
	}

	public void fireData() {
		if (onDataCallback != null) {
			onDataCallback.call(getKrollObject(), data);
		}
		KrollDict event = new KrollDict();
		event.put("data", data);
		fireEvent("data", event);
	}
}
