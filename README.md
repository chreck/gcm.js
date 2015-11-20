# gcm.js

chreck/gcm.js if forked from tdtsh/gcm.js and original from [https://github.com/iamyellow/gcm.js](https://github.com/iamyellow/gcm.js)
Thank you iamyellow!

**gcm.js** is an open source module for Titanium Android SDK that lets developers receive GCM push notifications in their Android apps.

Using Google Play Service instead of the fork original gcm.jar 

It supports the Appcelerator Platform (4.1.1.SDK) and SDK 3.5.1.

It has a very simple API -almost identical to iOS!- yet flexible and powerful, as it executes Javascript whenever a notification is received, no matter if the app is in foreground or background. More info at:
[Titanium の Android で GCM(Push通知) Google Play Service版 - Qiita 変更する](http://qiita.com/hntn/items/a83072e441ccf4fc715c)


## build module

> install before android-ndk

```
ant clean

ant

./cp_zip.sh
```


## run client example

> install Appcelerator Platform before

> and connect your Android device to PC

```
cd ./example

ti build -p android -T device
```


## run server example

> install node.js before

```
cd ./example_server

npm install

vi demoserver.js
```

> set your API key and Device's RegistrationId

>>>	var GCM = new _GCM('SET YOU API KEY HERE');

>>>	var registration_id = 'SET YOUR DEVICES REGISTRATIONID HERE';


```
node demoserver.js
```



## known issue

- unregister is not implemented

- It does not receive a notification if the application is not started


## Forking the module

If you want to fork and compile this module, first rename manifest_template file to manifest.
Then fill the guid (line #16) with a new module uuid with the **uuidgen** command as [this post](http://developer.appcelerator.com/blog/2011/09/module-verification.html) explains.

## Author

jordi domenech, [iamyellow.net](http://iamyellow.net)

## License

Apache License, Version 2.0
