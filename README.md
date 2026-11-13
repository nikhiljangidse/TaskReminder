# Task Reminder (Android)

Time aur task set karo — us time pe phone full screen mein reminder dikhata hai, alarm ki tarah ring karta hai, aur "Mark as done" ya Snooze (5 / 10 / 30 min) karne tak bajta rehta hai.

Kotlin + Jetpack Compose · minSdk 26 (Android 8.0) · targetSdk 35

## APK kaise banaye

### Option A — Android Studio (sabse aasaan)
1. Zip extract karo.
2. Android Studio → **Open** → `TaskReminder` folder select karo.
3. Gradle sync hone do (pehli baar internet se dependencies download hongi).
4. Phone USB se connect karo (Developer options → USB debugging ON) aur ▶ **Run** dabao.
   Ya **Build → Build App Bundle(s) / APK(s) → Build APK(s)** — APK yahan milega:
   `app/build/outputs/apk/debug/app-debug.apk`

### Option B — GitHub (bina Android Studio ke)
1. GitHub pe ek naya repo banao aur ye poora folder push karo.
2. Repo → **Actions** tab → "Build APK" workflow apne aap chalega (~4-5 min).
3. Run khatam hone pe neeche **Artifacts** mein `TaskReminder-apk` download karo → zip ke andar `app-debug.apk`.
4. APK phone pe bhejo aur install karo ("Install unknown apps" allow karna padega).

## Pehli baar app kholne pe
- **Notifications** allow karo.
- Home screen pe "Setup needed" card dikhe to **Fix** dabao aur sab allow karo:
  - Alarms & reminders
  - Full-screen alerts (Android 14+)
  - Display over other apps *(recommended — isse phone use karte waqt bhi full screen khulega)*
  - No battery restriction *(Xiaomi / Vivo / Oppo / Realme pe zaroori)*
- Xiaomi/Redmi/POCO pe: Settings → Apps → Task Reminder → **Autostart ON** bhi kar do.

## Jaldi test karna hai?
New reminder → Time card pe **"Test: in 1 min"** dabao → task likho → **Save**.
Phone lock karke 1 minute wait karo — full-screen reminder aana chahiye.

## Kaise kaam karta hai
- `AlarmScheduler` — har reminder ke liye exact alarm (`setAlarmClock`) set karta hai.
- `ReminderReceiver` — time aane pe ringing notification + full-screen intent bhejta hai.
- `AlertActivity` — lock screen ke upar full-screen reminder (Done / Snooze).
- `BootReceiver` — phone restart / time change ke baad alarms dobara set karta hai.
- Reminders phone pe hi save hote hain (SharedPreferences, koi server nahi).

Note: Android rule ke hisaab se, phone unlocked aur use ho raha ho to full-screen ki jagah upar ek banner (heads-up) aata hai — usse tap karke full screen khulta hai. "Display over other apps" allow karne pe seedha full screen khulta hai.
