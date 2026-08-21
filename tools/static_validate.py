from pathlib import Path
import re, sys
root=Path(__file__).resolve().parents[1]
base=root/'app/src/main/java/com/pipsniper/zaloprobe'
manifest=(root/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8')
service=(base/'ZaloNotificationListener.java').read_text(encoding='utf-8')
engine=(base/'BridgeEngine.java').read_text(encoding='utf-8')
telegram=(base/'TelegramApi.java').read_text(encoding='utf-8')
secure=(base/'SecureStore.java').read_text(encoding='utf-8')
queue=(base/'QueueStore.java').read_text(encoding='utf-8')
routes=(base/'RouteStore.java').read_text(encoding='utf-8')
main=(base/'MainActivity.java').read_text(encoding='utf-8')
bot_runtime=(base/'TelegramBotRuntime.java').read_text(encoding='utf-8')
bot_engine=(base/'SmartBotEngine.java').read_text(encoding='utf-8')
bot_security=(base/'BotSecurity.java').read_text(encoding='utf-8')
bot_client=(base/'TelegramBotClient.java').read_text(encoding='utf-8')
discovery=(base/'TelegramDiscovery.java').read_text(encoding='utf-8')
all_java='\n'.join(p.read_text(encoding='utf-8', errors='ignore') for p in base.glob('*.java'))
checks={
 'internet_required':'android.permission.INTERNET' in manifest,
 'https_only':'android:usesCleartextTraffic="false"' in manifest and 'https://api.telegram.org/' in telegram,
 'listener_permission':'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE' in manifest,
 'zalo_package_filter':'BridgePrefs.ZALO_PACKAGE.equals(sbn.getPackageName())' in service,
 'no_accessibility':'AccessibilityService' not in manifest and 'AccessibilityService' not in all_java,
 'keystore_token':'AndroidKeyStore' in secure and 'AES/GCM/NoPadding' in secure,
 'durable_queue':'AtomicFile' in queue and 'MAX_ITEMS' in queue,
 'telegram_retry':'TelegramDispatcher.kick' in service and 'RetryReceiver' in manifest,
 'dynamic_routes':'findAllMatching' in routes and 'RecentSourceStore.load' in main,
 'secondary_fingerprint':'keyHash' in routes and 'Hashing.shortHash(sbn.getKey())' in engine,
 'smart_bot_runtime':'TelegramBotRuntime.start(this)' in service and 'getUpdates(token, offset, 20)' in bot_runtime,
 'smart_bot_owner_gate':'BotSecurity.isOwner' in bot_engine and '"creator".equals(status)' in bot_engine,
 'smart_bot_commands':'setMyCommands' in bot_client and 'command("status"' in bot_client and 'command("pause"' in bot_client,
 'serialized_polling':'static synchronized ApiResult getUpdates' in bot_client and 'TelegramBotClient.getUpdates' in discovery,
 'no_hardcoded_bot_token':re.search(r'(?<![A-Za-z0-9])[0-9]{5,}:[A-Za-z0-9_-]{20,}', all_java) is None,
 'no_ai_provider':'openai.com' not in all_java.lower() and 'shopaikey' not in all_java.lower(),
}
for k,v in checks.items(): print(('PASS' if v else 'FAIL'),k)
if not all(checks.values()): sys.exit(1)
