//
//   Copyright 2013 jordi domenech <http://iamyellow.net, jordi@iamyellow.net>
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

import org.appcelerator.kroll.KrollDict;
import org.appcelerator.titanium.TiApplication;
import org.appcelerator.titanium.util.TiRHelper;
import org.appcelerator.titanium.util.TiRHelper.ResourceNotFoundException;

import android.app.ActivityManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GCMIntentService extends IntentService {
	private static final String TAG = "GCMIntentService";

	public static final int NOTIFICATION_ID = 1;
	NotificationCompat.Builder builder;

	public GCMIntentService() {
		super(GCMIntentService.class.getSimpleName());
	}
	
	/**
	 * Converts a extra in the bundle by key. Support for String and Integers.
	 * @param key the key which must be in the Bundle extras
	 * @param extras the Bundle
	 * @return the key as String or null if not successful
	 */
	private String convert(String key, Bundle extras) {
		String eventKey = getEventKey(key);
		// extra can be of any type
		Object objData = extras.get(key);
		String stringData = null;
		// support for integers and strings
		if(objData instanceof Integer) {
			stringData = ((Integer)objData).toString();
		} else if (objData instanceof String) {
			stringData = (String) objData;
		} else if(objData != null){
			@SuppressWarnings("rawtypes")
			Class cls = objData.getClass();
			GcmjsModule.logw(TAG + ": No support for type of key: "+ key +" eventKey: " + eventKey + " class type " +cls.getName()  );
		} else {
			GcmjsModule.logw(TAG + ": No data for key: "+ key +" eventKey: " + eventKey + " objData " + objData );
		}
		if (stringData != null) {
			GcmjsModule.logd(TAG + ": key: "+ key +" eventKey: " + eventKey + " stringData:" + stringData );
		}
		return stringData;
	}
	
	private String getEventKey(String key) {
		return key.startsWith("data.") ? key.substring(5) : key;
	}
	
	private KrollDict getMessageData(Bundle extras) {
		KrollDict messageData = new KrollDict();
		for (String key : extras.keySet()) {
			String eventKey = getEventKey(key);
			String data = convert(key,extras);
			if (data != null && !"".equals(data)) {
				messageData.put(eventKey, data);
			}
		}
		return messageData;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {
			if (messageType == null) {
				GcmjsModule.logd(TAG + ": messageType is null");
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				GcmjsModule.logd(TAG + ": deleted");
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				int appIconId = 0;
				try {
					appIconId = TiRHelper.getApplicationResource("drawable.appicon");
				} catch (ResourceNotFoundException e) {
					GcmjsModule.logd(TAG + ": ResourceNotFoundException: " + e.getMessage());
				}

				if (!isInForeground()) {
					TiApplication tiapp = TiApplication.getInstance();
					Intent launcherIntent = new Intent(tiapp, GcmjsService.class);
					for (String key : extras.keySet()) {
						String eventKey = getEventKey(key);
						String data = convert(key,extras);
						if (data != null && !"".equals(data)) {
							launcherIntent.putExtra(eventKey, data);
						}
					}
					tiapp.startService(launcherIntent);
				} else {
					KrollDict messageData = getMessageData(extras);
					fireMessage(messageData);
				}
			}
		}
		GCMBroadcastReceiver.completeWakefulIntent(intent);
	}

	public static void fireMessage(KrollDict messageData) {
		GcmjsModule module = GcmjsModule.getInstance();
		if (module != null) {
			module.fireMessage(messageData);
		} else {
			GcmjsModule.logd(TAG + ": fireMessage module instance not found.");
		}
	}

	public static boolean isInForeground() {
		Context context = TiApplication.getInstance().getApplicationContext();
		ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		String packageName = context.getPackageName();
		if (am.getRunningTasks(1).get(0).topActivity.getPackageName().equals(packageName)) {
			return true;
		}
		return false;
	}

}
