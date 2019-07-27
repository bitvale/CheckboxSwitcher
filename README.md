# CheckboxSwitcher

<img src="/art/preview.gif" alt="sample" title="sample" width="320" height="600" align="right"/>

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/platform-android-green.svg)](http://developer.android.com/index.html)
[![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=19)

Created this [switch animation](https://dribbble.com/shots/6266357-Checkboxswitcher) from [Oleg Frolov](https://dribbble.com/Volorf) as an android library. 

USAGE
-----

Just add CheckboxSwitcher view in your layout XML and CheckboxSwitcher library in your project via Gradle:

```gradle
dependencies {
  implementation 'com.bitvale:checkboxswitcher:1.0.0'
}
```

XML
-----

```xml
<com.bitvale.checkboxswitcher.CheckboxSwitcher
    android:id="@+id/switcher"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:switcher_bg_color="@color/switcher_color"
    app:thumb_off_color="@color/off_color"
    app:thumb_on_color="@color/icon_color"
    app:thumb_icon="@drawable/thumb_icon" />
```

You must use the following properties in your XML to change your Switcher.


##### Properties:

* `android:checked`                 (boolean)   -> default  false
* `app:switcher_height`             (dimension) -> default  26dp // width is calculated automatically
* `app:switcher_bg_color`           (color)     -> default  white
* `app:thumb_on_color`              (color)     -> default  #95cd75
* `app:thumb_off_color`             (color)     -> default  #e5e5e5
* `app:thumb_icon`                  (reference) -> default  check icon
* `app:elevation`                   (dimension) -> default  4dp

Kotlin
-----

```kotlin
switcher.setOnCheckedChangeListener { checked ->
    if (checked) action()
}
```

LICENCE
-----

CheckboxSwitcher by [Alexander Kolpakov](https://play.google.com/store/apps/dev?id=7044571013168957413) is licensed under an [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0).