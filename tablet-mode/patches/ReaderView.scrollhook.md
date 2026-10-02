# Optional: pixel-accurate scroll sync for the two-page spread

The default spread (see `SpreadController`) syncs the two columns **per page** using
only the reader's public API — no edit to the vendored `ru.mobigroup.bookreader.ReaderView`
is required. That is enough for a prayer book: the right column advances to page N+1
whenever the left column lands on page N.

If you want the two columns to track each other **while the user is mid-scroll**
(pixel for pixel), add this tiny hook to `ReaderView`. It is a notification only — it
changes no existing behaviour.

### 1. Add a listener field + setter to `ReaderView`

```java
public interface OnScrollChangeCallback { void onReaderScroll(int scrollY); }

private OnScrollChangeCallback onScrollChangeCallback;

public void setOnScrollChangeCallback(OnScrollChangeCallback cb) {
    this.onScrollChangeCallback = cb;
}
```

### 2. Fire it wherever `mScrollYPos` is updated

`mScrollYPos` is assigned inside `onLayout3()` (the continuous-scroll layout pass).
At the end of that method — after `mScrollYPos` has its final value for the frame — add:

```java
if (onScrollChangeCallback != null) {
    onScrollChangeCallback.onReaderScroll(mScrollYPos);
}
```

(`onLayout3()` is present in full in your real source; it is the method that walks the
page list top-to-bottom and positions each child. There is exactly one place where
`mScrollYPos` gets its new value per layout — fire the callback right after it.)

### 3. Wire it in `PrayerFragment` (SPREAD branch)

```kotlin
left.setOnScrollChangeCallback { _ -> spread?.syncScrollFromLeft() }
```

`SpreadController.syncScrollFromLeft()` is already written for this. Keep it throttled
if you see jank (e.g. only sync when `Math.abs(scrollY - lastSynced) > 8`).
