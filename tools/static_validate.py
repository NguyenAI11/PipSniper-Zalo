from pathlib import Path
import re, sys, json
root=Path(__file__).resolve().parents[1]
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
service=(root/'app/src/main/java/com/pipsniper/zaloprobe/ZaloNotificationListener.java').read_text(encoding='utf-8')
main=(root/'app/src/main/java/com/pipsniper/zaloprobe/MainActivity.java').read_text(encoding='utf-8')
exp=(root/'app/src/main/java/com/pipsniper/zaloprobe/ReportExporter.java').read_text(encoding='utf-8')
san=(root/'app/src/main/java/com/pipsniper/zaloprobe/ZaloEventSanitizer.java').read_text(encoding='utf-8')
checks={
 'no_internet_permission':'android.permission.INTERNET' not in manifest.replace('<!-- Intentionally NO INTERNET permission. Probe data cannot be uploaded by this app. -->',''),
 'listener_permission':'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE' in manifest,
 'zalo_filter':'ProbeStore.ZALO_PACKAGE.equals(sbn.getPackageName())' in service,
 'manual_export':'Intent.ACTION_CREATE_DOCUMENT' in main,
 'no_accessibility':'AccessibilityService' not in manifest and 'AccessibilityService' not in service,
 'salted_hash':'ProbeStore.stableHash' in san,
 'unknown_objects_not_stringified':'Unknown Parcelable/objects: class name only' in san,
 'report_internet_false':'internet_permission_declared' in exp,
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'),k)
if not all(checks.values()): sys.exit(1)
