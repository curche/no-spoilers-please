| Download | Check out the Parent App |
|----------|------------|
| [![fork release](https://img.shields.io/github/release/curche/no-spoilers-please.svg?maxAge=3600&label=fork-release)](https://github.com/curche/no-spoilers-please/releases) | [![parent release](https://img.shields.io/gitlab/pipeline/juanitobananas/scrambled-exif/master?maxAge=3600&label=parent-release)](https://gitlab.com/juanitobananas/scrambled-exif/-/tree/master/) |

# No Spoilers Please

No Spoilers Please is a nifty Android app to easily share Spoiler-marked Images to Discord on devices running Android. Discord Android App still doesn't allow for app users to share spoiler-marked images in the same experience as on Desktop versions.

# THIS APP IS NO LONGER MAINTAINED

From the recent Discord Android App onwards (`Discord App 102.17 - Stable Update` or above), Discord has finally added the `SPOILER_` feature to its mobile apps! As a result, this app will no longer be required and the repo has been archived. Thanks to everyone who used the app till now! :D

While you're reading this or were interested in this app, check out the Parent App [ScrambledEggsIF](https://gitlab.com/juanitobananas/scrambled-exif)

<details>
    <summary><b>Show old readme</b></summary>


## What does it do?

* Share images to Discord while marking them as Spoilers. This is possible by renaming the file by appending SPOILER_.
* Works for PNG/JPG images
* Optional: Removes any metadata from the photos you share. This removes the normal EXIF data stored with an image including location data, device details etc
* Auto Dark theme based on System Dark theme setting

## Instructions to use

* Get the latest version of the apk from the Releases section (or [here](https://github.com/curche/no-spoilers-please/releases/latest))
* After installation, open the app to ensure all permissions (Also Note: MIUI users will have to grant additional permissions)
* To see the app in action:
  - choose any image and use the Share option. 
  - In the Share Menu that pops up, choose `No Spoilers Please` from the app list.
  - Wait for a moment and the Menu shows up again, now click on Discord and share it to any channel
  - The file uploaded will be marked as Spoiler!

## Download

Get the app from the [releases page](https://github.com/curche/no-spoilers-please/releases/latest).

## Contributing

This app is a fork on the [Scrambled-Exif](https://gitlab.com/juanitobananas/scrambled-exif/) - a FOSS app for sharing Pictures after Removing Exif data - available from Play Store, F-Droid and GitLab

If you wish to contribute, please have a look at the list of Open issues. Note that the app uses an additional `lib-common` library as a git sub-module is being used and you will need to fetch it to be able to build the app in Android Studio. More on that [here](https://gitlab.com/juanitobananas/libcommon)

Modifications have been made for personal use while hopefully adhering to the Licenses of the parent app. I do not intend to release this to Play Store or F-Droid. In case you notice any problems, please file an [Issue](https://github.com/curche/no-spoilers-please/issues/new)

## Thanks

* [juanitobanas](https://gitlab.com/juanitobananas) for making the original app. Do check it out.
* Folks at the Tachiyomi Discord for testing out (and actually using it XD)
* Thanks to you for visiting this Repo!
</details>
