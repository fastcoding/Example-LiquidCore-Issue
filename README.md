# LiquidCoreAndroid Example

This example is prepared for demonstrating memory leak in [LiquidCoreAndroid](https://github.com/LiquidPlayer/LiquidCore).

## About This Example

It firstly starts an Express WebServer listening on :3000. Once there is a web client connected, it will first upgrade to a WebSocket channel, then keep sending pictures in public/images to each client @ 10HZ.

After app started on device, Open a browser from PC, navigate to the address as shown in app's title. You should see an animation.

If the javascript detected it's running on android device, it will load picture through Java codes. This is where the memory leak occurs. If large resolution picture is prepared, the android application will quickly run out of memory.

The main function involved is at MainActivity.java#38.

    webServer.getJSContext().property("android_fetch_pic",new JSFunction(webServer.getJSContext(),"fetch_pic") {
                   public String fetch_pic() {
                     ....
                   }

## Build Steps
- install node modules. on the project root directory, issue the command.

        npm install


- open Android Studio, open TestLiquidCore subdirectory, click run and notice the Logcat report about current heap usage.

##Screen Shots

- Testing on an old Pad

  ![SS1](screenshot/s1.jpg)

- Testing on HUWEI P9

  ![SS2](screenshot/s2.jpg)
