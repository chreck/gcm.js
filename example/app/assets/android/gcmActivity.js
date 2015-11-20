/*global Ti: true, require: true */

(function(activity, gcm) {

	Ti.API.info('gcm_activity.js');
	var intent = activity.intent;
	var ntfId = 0;
	var title = '';
	var message = '';
	var appdata = {};

	// HERE we catch the intent extras of our notifications
	if (intent.hasExtra('title')) {
		title =  intent.getStringExtra('title', '');
	}

	if (intent.hasExtra('message')) {
		message =  intent.getStringExtra('message', '');
	}

	if (intent.hasExtra('appdata')) {
		appdata =  intent.getStringExtra('appdata', '');
	}

	if (intent.hasExtra('ntfId')) {
		ntfId = intent.getIntExtra('ntfId', 0);
	}

	gcm.data = {
		ntfId: ntfId,
		title: title,
		message: message,
		appdata: appdata
	};
	Ti.API.info(String.format('gcm.data:', gcm.data || {}));

	// 'isLauncherActivity' is a module property which tell us 
	// if the app is not running
	if (gcm.isLauncherActivity) {
		// if the app is not running, we need to start our app 
		// launcher activity
		// (launcher activity shows the splash screen and setup 
		// your app environment, so we need this)
		var mainActivityIntent = Ti.Android.createIntent({
			// 'mainActivityClassName' is another module property 
			// with name of our app launcher activity
			className: gcm.mainActivityClassName,
			packageName: Ti.App.id,
			flags: Ti.Android.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Ti.Android.FLAG_ACTIVITY_SINGLE_TOP
		});
		mainActivityIntent.addCategory(Ti.Android.CATEGORY_LAUNCHER);
		activity.startActivity(mainActivityIntent);
	} else {
		// if the app is running (is being resumed), just finish 
		// this activity!
		activity.finish();
	}

})(Ti.Android.currentActivity, require('net.iamyellow.gcmjs'));
