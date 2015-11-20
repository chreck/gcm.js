
var gcm = require('net.iamyellow.gcmjs');
var deviceToken = '';

function doClick(e) {
	if (deviceToken) {
		alert('do unregister');
		// unregister not implemented yet
		//require('net.iamyellow.gcmjs').fireUnregister(deviceToken);
	}
}

$.container.open();


var pendingData = gcm.data;
if (pendingData && pendingData !== null) {
	// if we're here is because user has clicked on the notification
	// and we set extras for the intent 
	// and the app WAS NOT running
	// (don't worry, we'll see more of this later)
	Ti.API.info('******* data (started) ' + JSON.stringify(pendingData));
}

var receivePush = function(ev) {
	Ti.API.info(String.format('receivePush %s', ev.appdata || {}));
	alert(String.format('receivePush' , ev.appdata || {}));
};

var deviceTokenSuccess = function(ev) {
	Ti.API.info('deviceTokenSuccess:' + ev.deviceToken);
	deviceToken = ev.deviceToken;
};

var deviceTokenError = function(ev) {
	Ti.API.info('deviceTokenError:' + ev.error);
};

var dataWhenResume = function(ev) {
	Ti.API.info(String.format('ev.data %s' , ev.data));
};

var afterUnregister = function(ev) {
	Ti.API.info(String.format('afterUnregister %s' , ev));
	Ti.API.info('******* unregister ' + ev.deviceToken);
	deviceToken = '';
};

var isCallbackMethod = false;

if (isCallbackMethod) {

	// Method 1 call-back (Original)
	gcm.registerForPushNotifications({
		callback: receivePush,
		success: deviceTokenSuccess,
		error: deviceTokenError,
		unregister: afterUnregister,
		data: dataWhenResume,
		senderid: 123123123 // an array is also possible like [123,124,125]
	});
} else {

	// Method 2 eventlistener
	gcm.addEventListener('callback', receivePush);
	gcm.addEventListener('success', deviceTokenSuccess);
	gcm.addEventListener('error', deviceTokenError);
	gcm.addEventListener('data', dataWhenResume);
	gcm.addEventListener('unregister', afterUnregister);
	gcm.registerForPushNotifications({senderid: 123123123});
}


