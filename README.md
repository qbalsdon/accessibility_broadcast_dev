# Accessibility Broadcast Dev

Allow developers and automation testers to interact with Android's Accessibility features using the Android Debug Bridge.

## Introduction

The goal is to make it easier for advanced users, developers and automation engineers to navigate the different options with which accessibility users may engage.

### Installation

1. Download the repo, run the code. Alternately install the app from the artifacts or the Google Play store (link TBA)
2. Turn on accessibility ensuring the "Android Broadcast Developer" Accessibility service is enabled. This can be done
   - via [scripting][15] or
   - in Accessibility Settings under services. **NOTE** Disabling TalkBack will mean that the service will **NOT** be enabled the next time you enable TalkBack. You'll have to enable it again.

### Project structure

This app has 3 main components:

#### 1. The Accessibility Service

The service is an addition service to standard accessibility options. Once connected, it will ensure the broadcast receiver is running, which in turn can invoke methods on the service. The service is also capable of performing actions on behalf of the user.

#### 2. The Broadcast Receiver

This element listens for specific intents coming in from the system, notably the Android Debug Bridge. It will parse arguments, interpret them and run commands from the Accessibility service. This enables the user to deploy accessibility actions from the adb commands below, enabling the scripting of actions.

#### 3. The Demo Activity

A screen for demonstrating the ability of the system, and provide some helpful setup shortcuts. It also allows users to see the structure of the intents should they want to write Android apps to use the service. This could be done in testing.

### Scripting

I have written a series of scripts on my other repo, [Talos][4], but to start the TalkBack and this service:

```
  VALUE_OFF="com.android.talkback/com.google.android.marvin.talkback.TalkBackService"
  TALKBACK="com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
  ALLYSERVICE="com.balsdon.accessibilityDeveloperService/.AccessibilityDeveloperService"
  VALUE_ON="$TALKBACK:$ALLYSERVICE"
  if [ "$ENABLE" = true ]; then
    adb shell settings put secure enabled_accessibility_services $VALUE_ON
  else
    adb shell settings put secure enabled_accessibility_services $VALUE_OFF
  fi
```

### Usage

You should now be able to run the following adb commands:
```
adb shell am broadcast -a com.balsdon.talkback.accessibility -e
    ACTION "ACTION_SWIPE_LEFT"
    ACTION "ACTION_SWIPE_RIGHT"
    ACTION "ACTION_SWIPE_UP"
    ACTION "ACTION_SWIPE_DOWN"
    ACTION "ACTION_FOCUS_ELEMENT"
        -e PARAMETER_ID "resourceId"
        -e PARAMETER_TEXT "some text"
        -e PARAMETER_TYPE "element type" --e DIRECTION "[DIRECTION_FORWARD | DIRECTION_BACK]"
        -e PARAMETER_HEADING "[DIRECTION_FORWARD | DIRECTION_BACK]"
    ACTION "ACTION_SWIPE_DOWN"
    ACTION "ACTION_CLICK"
    ACTION "ACTION_LONG_CLICK"
    ACTION "ACTION_CURTAIN"
    ACTION "ACTION_VOLUME_UP"
    ACTION "ACTION_VOLUME_DOWN"
    ACTION "ACTION_VOLUME_MUTE"
    ACTION "ACTION_VOLUME_SET" --ei PARAMETER_VOLUME 20
    ACTION "ACTION_SAY"
        -e PARAMETER_TEXT "some\ text\ escape\ the\ spaces"

adb shell am broadcast -a com.balsdon.talkback.accessibility -e ACTION "ACTION_MENU"
```

The first two are the same, they fire an intent that will tell the screen reader to focus on the next available element. Sending `-e ACTION "[PARAMETER]"`
  - `ACTION_PREV` will inform the service to focus on the previous element
  - `ACTION_MENU` will open the accessibility menu so users can focus on headings, paragraphs, etc.
  - `ACTION_HEADING_[NEXT | PREV]` focuses on the next / previous heading element
  - `ACTION_VOLUME_[UP | DOWN]` allows developers to incrementally the accessibility volume
  - `ACTION_VOLUME_SET` allows developers to directly set the accessibility volume.

## References:
 - [Help with custom events, mainly in making the script to hold the volume keys down for 3 seconds][1]
 - [Writing an accessibility service][2]
 - [Codelab for accessibility service][3]

## Further reading:

 - [Android Accessibility Development Doesn't Have to Be Scary][10]
 - [Building with accessibility in mind][11]
 - [Build more accessible apps][12]
 - [Testing gestures][6]
 - [Accessibility scanner][7]
 - [Google I/O on Accessibility][8]
 - [Android Accessibility — Resolving common Talkback issues][9]
 - [Google Accessibility API][17]

## TODO

 - Resolve the disconnect between Gesture and Action
     - Currently the service takes actions for users, but if they are not my expected defaults they may behave differently. For example: Swiping from higher to lower on the screen on some devices might highlight the next heading, while other devices may adjust selection granularity type (i.e. enable you to use NEXT and PREV to navigate headings, paragraphs, links, characters).
        - :white_check_mark: Option 1: Convert actions to gestures (easy, but makes me sad. ~Also, more complex gestures don't work~)
        - :x: Option 2: Find a method of doing actions that doesn't involve gestures. Currently looking at [AccessibilityNodeInfo.performAction][13]
           - I can navigate the tree myself, but I have no way of knowing if I am consistent with TalkBack
 - Need to create a settings activity. See [Accessibility service configuration][16]
 - Sort out code structure
 - Toggle Talkback toast captions?
 - Allow triple tap (interact with links)
 - :white_check_mark: [FEATURE] Add a "perform click" action
 - :white_check_mark: Add a "focus by id" action - might aid [talos][4] scripts
 - [FEATURE] Create a map of the current screen
     - Show a list of headings
     - Filter by control type
     - Show all "Announcement data" (text / content-description) 
 - [FEATURE] Enable developers to show / hide more of the curtain elements
 - [FEATURE] Show TalkBack version
 - [FEATURE] Change OS Language (low priority)
 - !FIXED! :bug: [BUG] 01 Open the accessibility menu. Currently the code is there but something is not happening. [Opened an issue][14]
 - :bug: [BUG] 02 Scroll down to selected element. Currently an accessibility service can only scroll on scrollable nodes. 
 - !FIXED! :bug: [BUG] 03 Do not store an instance in a companion object. It feels wrong, I have not found a better solution yet
 - !FIXED! :bug: [BUG] 04 With new version the distances used for programmatic gestures need to be larger.
 - !FIXED! :bug: [BUG] [05] When the curtain is drawn the data is blank
 - [FEATURE] See what is readable from the API's of Talkback
   - [Get the gesture mappings][18]
   - Get the node structure: next, previous
   - Read the current granularity
 - [FEATURE] Improvements to the curtain
   - Initial loading is empty
   - "Hole" / "Pigeon" mode - only show certain elements (Headings, links, labels, etc)
 - :white_check_mark: [FEATURE] Improve the look of the test activity. It's hideous and inaccessible
   - Add UI tests to the main screen

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
[11]: https://www.android.com/accessibility/
[12]: https://developer.android.com/guide/topics/ui/accessibility
[13]: https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo#performAction(int,%20android.os.Bundle)
[14]: https://issuetracker.google.com/u/2/issues/185631661
[15]: https://github.com/qbalsdon/accessibility_broadcast_dev#scripting
[16]: https://developer.android.com/guide/topics/ui/accessibility/service#service-config
[17]: https://developer.android.com/reference/android/view/accessibility/package-summary.html
[18]: https://github.com/google/talkback/blob/6c0b475b7f52469e309e51bfcc13de58f18176ff/utils/src/main/java/com/google/android/accessibility/utils/AccessibilityServiceCompatUtils.java#L143