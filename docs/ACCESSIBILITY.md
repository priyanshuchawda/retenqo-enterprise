# Accessibility verification

Accessibility of Retenqo's own UI is distinct from its optional Android Accessibility capture service.

Target inclusive Android usability: meaningful Compose semantics, descriptive labels, at least 48dp touch targets, focus order, accessible state changes, non-color-only cues, text scaling and sufficient contrast. Test TalkBack and Switch Access on supported devices; verify permissions, capture, task review, Ask, backup and errors without relying on gestures alone.

The implementation report documents labeled microphone and expandable settings controls. The current evidence covers implemented semantics; comprehensive manual TalkBack and Switch Access verification is a separate evaluation activity. Record device, OS, scenario and outcome for each run.

## Evidence navigation

Existing control semantics can be inspected in [MnemeHomeScreen.kt](../android/mneme/app/src/main/java/com/mneme/app/ui/timeline/MnemeHomeScreen.kt) and [SettingsPane.kt](../android/mneme/app/src/main/java/com/mneme/app/ui/timeline/SettingsPane.kt). Website automation and manual verification boundaries are recorded in [website accessibility](../web/docs/ACCESSIBILITY.md) and [testing](TESTING.md). This documentation pass did not change application presentation or execute new assistive-technology tests.
