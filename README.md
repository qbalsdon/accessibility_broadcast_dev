# Accessibility Broadcast Dev

This is a "simple" tool for allowing developers to script some of the more mundane actions when dealing with accessibility.

## Introduction

The goal is to make it easier for advanced users, developers and automation engineers to navigate the different options with which accessibility users may engage.

### Installation

1. Download the repo, run the code. Alternately install the app from the artifacts or the Google Play store (link TBA)
2. Add the service to your accessibility shortcut. If you are unsure how to do this, just run the app and click the shortcuts button
3. Turn on accessibility
4. Press and hold the VOLUME_UP and VOLUME_DOWN keys for 3 seconds

**NOTE** Disabling Talkback will mean that the service will **NOT** be enabled the next time you enable Talkback. You'll have to enable it again. I don't make the rules, I just try to hack my way past them with frivolous scripts and keyboard shortcuts

### Project structure

This app has 3 main components:

#### 1. The Accessibility Service

The service is an addition service to standard accessibility options. Once connected, it will ensure the broadcast receiver is running, which in turn can invoke methods on the service. The service is also capable of performing actions on behalf of the user.

#### 2. The Broadcast Receiver

This element listens for specific intents coming in from the system, notably the Android Debug Bridge. It will parse arguments, interpret them and run commands from the Accessibility service. This enables the user to deploy accessibility actions from the adb commands below, enabling the scripting of actions.

#### 3. The Demo Activity

A screen for demonstrating the ability of the system, and provide some helpful setup shortcuts. It also allows users to see the structure of the intents should they want to write Android apps to use the service. This could be done in testing.

### Scripting

I have written a series of scripts on my other repo, [Talos][4], but to hold down the VOLUME_DOWN and VOLUME_UP keys for 3 seconds:

```
  device="/dev/input/event1" # VOLUME KEYS EVENT FILE
  VOLUME_DOWN=114 #0x0072
  VOLUME_UP=115   #0x0073
  BLANK_EVENT="sendevent $device 0 0 0"

  INST_DN="sendevent $device 1 $VOLUME_DOWN 1 && $BLANK_EVENT && sendevent $device 1 $VOLUME_UP 1 && $BLANK_EVENT"
  INST_UP="sendevent $device 1 $VOLUME_DOWN 0 && $BLANK_EVENT && sendevent $device 1 $VOLUME_UP 0 && $BLANK_EVENT"

  adb -s "$DEVICE" shell "$INST_DN"
  sleep 3
  adb -s "$DEVICE" shell "$INST_UP"
```

I have added it as my accessibility shortcut on my device. You can set it up by going here:

```
adb shell am start -n com.android.settings/.Settings$AccessibilitySettingsActivity
```

#### Side note: recording key presses for later scriptable playback

I have found the best way to reverse engineer button presses is to
1. Identify the device - `/dev/input/device[n]`
2. Record the usage

```
adb shell
    cat /dev/input/event[n] > /mnt/sdcard/some_awesome_action
    # do your action
    # ctrl + c
    # if the file is empty, you have the wrong device
    exit

adb pull /mnt/sdcard/some_awesome_action
```

3. Open up a [hex editor][5] and identify the ups and downs
e.g. this is a VOLUME_UP, KEY_DOWN:
> -----------------------------------------------
> 00 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f
> -----------------------------------------------
> 03 88 61 60 9e 5e 0a 00 01 00 73 00 01 00 00 00
> -----------------------------------------------
> 03 88 61 60 9e 5e 0a 00 00 00 00 00 00 00 00 00
> -----------------------------------------------

if you run `adb shell getevent -l` and `adb shell getevent -lp` you'll find that `73` is the key (position 0a, specifically, the VOLUME_UP key) and position 0c is the event type (down). Every press has a reset (all 00's).

4. Create a script with the timing involved as well as the key presses. **Timing is not recorded with the device buffer**

### Usage

You should now be able to run the following adb commands:
```
adb shell am broadcast -a com.balsdon.talkback.accessibility
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_NEXT"
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_PREV"
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_HEADING_NEXT"
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_HEADING_PREV"
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_VOLUME_UP"
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_VOLUME_DOWN"
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_VOLUME_SET" --ei PARAMETER_VOLUME 20

//TODO: [BUG]
adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_MENU"
```

The first two are the same, they fire an intent that will tell the screen reader to focus on the next available element. Sending `-e ACTION "[PARAMETER]"`
  - `ACTION_PREV` will inform the service to focus on the previous element
  - `ACTION_MENU` will open the accessibility menu so users can focus on headings, paragraphs, etc. This is currently not working
  - `ACTION_HEADING_[NEXT | PREV]` focuses on the next / previous heading element
  - `ACTION_VOLUME_[UP | DOWN]` allows developers to incrementally the accessibility volume
  - `ACTION_VOLUME_SET` allows developers to directly set the accessibility volume.

## References:
 - [Help with custom events, mainly in making the script to hold the volume keys down for 3 seconds][1]
 - [Writing an accessibility service][2]
 - [Codelab for accessibility service][3]

## Further reading:

 - [Android Accessibility Development Doesn't Have to Be Scary][10]
 - [Testing gestures][6]
 - [Accessibility scanner][7]
 - [Google I/O on Accessibility][8]
 - [Android Accessibility — Resolving common Talkback issues][9]

## TODO

 - [FEATURE] Add a "perform click" action
 - [BUG] 01 Open the accessibility menu. Currently the code is there but something is not happening

[1]: https://stackoverflow.com/questions/37460463/how-to-send-key-down-and-key-up-events-separately-on-android-using-adb
[2]: https://developer.android.com/guide/topics/ui/accessibility/service
[3]: https://codelabs.developers.google.com/codelabs/developing-android-a11y-service
[4]: https://github.com/qbalsdon/talos/blob/main/scripts/talkback
[5]: https://www.onlinehexeditor.com/
[6]: https://developer.android.com/guide/topics/ui/accessibility/testing#test-gestures
[7]: https://support.google.com/accessibility/android/answer/6376570
[8]: https://www.youtube.com/results?search_query=Whats+new+in+accessibility+google+io
[9]: https://medium.com/microsoft-mobile-engineering/android-accessibility-resolving-common-talkback-issues-3c45076bcdf6
[10]: https://wire.engineering/engineering/accessibility/android/2020/07/10/android-accessibility-development-doesnt-have-to-be-scary.html