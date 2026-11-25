# Accessibility verification

Accessibility of Retenqo's own UI is distinct from its optional Android Accessibility capture service.

Target inclusive Android usability: meaningful Compose semantics, descriptive labels, at least 48dp touch targets, focus order, accessible state changes, non-color-only cues, text scaling and sufficient contrast. Test TalkBack and Switch Access on supported devices; verify permissions, capture, task review, Ask, backup and errors without relying on gestures alone.

The implementation report documents labeled microphone and expandable settings controls. Comprehensive manual assistive-technology verification remains pending. Record device, OS, test scenario, result and any defect for each future evaluation.
