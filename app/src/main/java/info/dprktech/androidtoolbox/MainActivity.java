package info.dprktech.androidtoolbox;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Research toolbox for DPRK Android tablets (Ryongaksan 4.4, Taeyang 8321 6.0/MTK).
 * Most actions need the app installed in /system/priv-app (or platform-signed);
 * "Diagnose" shows which privileged permissions were actually granted.
 */
public class MainActivity extends Activity {
    private static final String USB_STATE = "android.hardware.usb.action.USB_STATE";
    private static final String NO_DEBUG = "no_debugging_features";
    private static final String NO_USB_FILES = "no_usb_file_transfer";

    private static final String[] PROPS = {
            "ro.product.manufacturer", "ro.product.model", "ro.product.device", "ro.hardware",
            "ro.build.display.id", "ro.build.fingerprint", "ro.build.version.release", "ro.build.type",
            "ro.build.tags", "ro.secure", "ro.debuggable", "ro.adb.secure", "ro.boot.mode", "ro.bootmode",
            "ro.serialno", "persist.sys.usb.config", "sys.usb.config", "sys.usb.state", "init.svc.adbd",
            "service.adb.tcp.port", "persist.adb.tcp.port", "persist.service.adb.enable",
            "ro.sys.usb.storage.type", "ro.sys.usb.bicr", "ro.sys.usb.charging.only", "sys.usb.vid",
    };
    private static final String[] PERMS = {
            "android.permission.WRITE_SECURE_SETTINGS", "android.permission.MANAGE_USB",
            "android.permission.MANAGE_USERS", "android.permission.READ_LOGS", "android.permission.DUMP",
            "android.permission.TETHER_PRIVILEGED", "android.permission.WRITE_SETTINGS",
            "android.permission.WRITE_EXTERNAL_STORAGE",
    };
    private static final String[] GADGET = {"enable", "functions", "state", "idVendor", "idProduct",
            "iManufacturer", "iProduct", "iSerial"};
    // Shell output appended to exported reports. Runs as the app uid; dumpsys needs DUMP (priv-app).
    private static final String[] REPORT_CMDS = {
            "id", "cat /proc/version", "getprop", "ls -la /", "ls -la /sbin /system/bin/adbd /system/xbin",
            "cat /init.usb.rc /init.*usb*.rc", "ls -l /dev/android_adb* /dev/usb-ffs/adb /dev/mtp_usb",
            "ls -la /sys/class/android_usb/android0/", "mount", "ps", "dumpsys usb", "dumpsys user",
            "dumpsys device_policy", "pm list packages -f -i", "logcat -d -v time",
    };

    private final ExecutorService bg = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView out, status;
    private ScrollView outScroll;
    private EditText cmd;
    private CheckBox useSu;
    private boolean tethering;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            log("USB_STATE " + extras(i));
            refreshStatus();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        boolean wide = getResources().getConfiguration().screenWidthDp >= 600;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(8), dp(8), dp(8), dp(8));

        status = new TextView(this);
        status.setTypeface(Typeface.MONOSPACE);
        status.setTextColor(Color.WHITE);
        status.setPadding(dp(8), dp(8), dp(8), dp(8));
        panel.addView(status);

        section(panel, "ADB over USB",
                "Diagnose", (Runnable) () -> log(diagnosis()),
                "Enable ADB", (Runnable) () -> setAdb(1),
                "Disable ADB", (Runnable) () -> setAdb(0),
                "Clear ADB restrictions", (Runnable) this::clearRestrictions,
                "USB: mtp,adb", (Runnable) () -> usbFunction("mtp,adb"),
                "USB: adb", (Runnable) () -> usbFunction("adb"),
                "USB: rndis,adb", (Runnable) () -> usbFunction("rndis,adb"),
                "USB tethering on/off", (Runnable) this::toggleTethering,
                "USB: mtp,acm", (Runnable) () -> usbFunction("mtp,acm"),
                "Serial shell (ttyGS0)", (Runnable) () -> new Thread(() -> serialShell("/dev/ttyGS0")).start());
        section(panel, "Open screen",
                "Developer options", (Runnable) () -> open(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)),
                "USB mode", (Runnable) () -> open(component("com.android.settings", "com.android.settings.deviceinfo.UsbModeChooserActivity")),
                "Device admins", (Runnable) () -> open(component("com.android.settings", "com.android.settings.DeviceAdminSettings")),
                "Device info", (Runnable) () -> open(new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)),
                "MTK EngineerMode", (Runnable) () -> open(component("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode")),
                "Security", (Runnable) () -> open(new Intent(Settings.ACTION_SECURITY_SETTINGS)));
        section(panel, "Collect",
                "Save report to sdcard", (Runnable) this::saveReport,
                "Logcat (usb/adb)", (Runnable) this::usbLogcat,
                "dumpsys usb", (Runnable) () -> log(sh("dumpsys usb", false)),
                "Clear output", (Runnable) () -> ui.post(() -> out.setText("")));

        header(panel, "Shell");
        cmd = new EditText(this);
        cmd.setSingleLine();
        cmd.setTypeface(Typeface.MONOSPACE);
        cmd.setHint("getprop | grep usb");
        cmd.setImeOptions(EditorInfo.IME_ACTION_GO);
        cmd.setOnEditorActionListener((v, a, e) -> { runShell(); return true; });
        panel.addView(cmd);
        LinearLayout row = new LinearLayout(this);
        useSu = new CheckBox(this);
        useSu.setText("run via su");
        row.addView(useSu, new LinearLayout.LayoutParams(0, -2, 1));
        Button run = new Button(this);
        run.setText("Run");
        run.setOnClickListener(v -> runShell());
        row.addView(run, new LinearLayout.LayoutParams(0, -2, 1));
        panel.addView(row);

        ScrollView left = new ScrollView(this);
        left.addView(panel);
        out = new TextView(this);
        out.setTypeface(Typeface.MONOSPACE);
        out.setTextSize(12);
        out.setTextIsSelectable(true);
        out.setPadding(dp(8), dp(8), dp(8), dp(8));
        outScroll = new ScrollView(this);
        outScroll.setBackgroundColor(0xFF101418);
        outScroll.addView(out);
        root.addView(left, wide ? new LinearLayout.LayoutParams(0, -1, 2) : new LinearLayout.LayoutParams(-1, 0, 3));
        root.addView(outScroll, wide ? new LinearLayout.LayoutParams(0, -1, 3) : new LinearLayout.LayoutParams(-1, 0, 2));
        setContentView(root);
        log("AndroidToolbox " + BuildConfig.VERSION_NAME + " uid=" + Process.myUid()
                + "  Tap Diagnose first; plug/unplug USB to see live USB_STATE broadcasts.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(usbReceiver, new IntentFilter(USB_STATE));
        refreshStatus();
    }

    @Override
    protected void onPause() {
        unregisterReceiver(usbReceiver);
        super.onPause();
    }

    // ---- actions ----

    private void setAdb(int v) {
        try {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, v);
            Settings.Global.putInt(getContentResolver(), Settings.Global.ADB_ENABLED, v);
        } catch (SecurityException e) {
            log("DENIED: " + e.getMessage() + "\n  -> needs WRITE_SECURE_SETTINGS: install to /system/priv-app or platform-sign.");
            return;
        }
        // SettingsProvider (6.0) silently drops the write when no_debugging_features is set, so read back.
        String now = global(Settings.Global.ADB_ENABLED);
        log("adb_enabled requested " + v + ", now " + now
                + (String.valueOf(v).equals(now) ? "" : restricted(NO_DEBUG)
                ? "  <- REJECTED: user restriction " + NO_DEBUG + " is set (try 'Clear ADB restrictions')"
                : "  <- REJECTED by SettingsProvider"));
        SystemClock.sleep(1500);
        log("sys.usb.config=" + prop("sys.usb.config") + " sys.usb.state=" + prop("sys.usb.state")
                + " init.svc.adbd=" + prop("init.svc.adbd"));
    }

    @SuppressWarnings("deprecation")
    private void clearRestrictions() {
        UserManager um = (UserManager) getSystemService(USER_SERVICE);
        for (String k : new String[]{NO_DEBUG, NO_USB_FILES}) {
            try {
                um.setUserRestriction(k, false);
            } catch (SecurityException e) {
                log("DENIED clearing " + k + ": " + e.getMessage() + " (needs MANAGE_USERS)");
            }
            log(k + " = " + um.getUserRestrictions().getBoolean(k));
        }
        log("A device admin may re-apply restrictions; check 'Device admins' / Diagnose.");
    }

    private void usbFunction(String f) {
        Object um = getSystemService(USB_SERVICE);
        try {
            try { // 6.0
                um.getClass().getMethod("setCurrentFunction", String.class).invoke(um, f);
            } catch (NoSuchMethodException e) { // 4.4 / 5.x
                um.getClass().getMethod("setCurrentFunction", String.class, boolean.class).invoke(um, f, false);
            }
            try {
                um.getClass().getMethod("setUsbDataUnlocked", boolean.class).invoke(um, true);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Exception e) {
            log("setCurrentFunction(" + f + ") failed: " + cause(e) + " (needs MANAGE_USB)");
            return;
        }
        SystemClock.sleep(2000);
        log("requested " + f + " -> sys.usb.config=" + prop("sys.usb.config") + " sys.usb.state=" + prop("sys.usb.state")
                + "\n  (6.0 strips 'adb' unless adb_enabled=1)");
    }

    /**
     * adb substitute for Taeyang, whose boot image has no /sbin/adbd: bridges `sh` onto the USB ACM
     * gadget port. Needs USB config mtp,acm and the system uid (ttyGS0 is system:0660), i.e. the
     * "system" build flavor signed with the AOSP platform key. Host: screen /dev/cu.usbmodem* 115200
     */
    private void serialShell(String dev) {
        log("serial shell on " + dev + ": connect from the host now; type 'exit' to end");
        try (InputStream in = new FileInputStream(dev); OutputStream o = new FileOutputStream(dev)) {
            java.lang.Process p = new ProcessBuilder("sh", "-i").redirectErrorStream(true).start();
            new Thread(() -> pump(p.getInputStream(), o)).start();
            pump(in, p.getOutputStream());
            p.destroy();
        } catch (Exception e) {
            log("serial shell failed: " + e + "\n  (needs system uid; SELinux may also deny system_app on ttyGS_device)");
            return;
        }
        log("serial shell on " + dev + " ended");
    }

    private static void pump(InputStream in, OutputStream o) {
        byte[] buf = new byte[4096];
        try {
            for (int n; (n = in.read(buf)) > 0; ) {
                o.write(buf, 0, n);
                o.flush();
            }
        } catch (Exception ignored) { // either side closed
        }
    }

    private void toggleTethering() {
        tethering = !tethering;
        Object cm = getSystemService(CONNECTIVITY_SERVICE);
        try {
            Object r = cm.getClass().getMethod("setUsbTethering", boolean.class).invoke(cm, tethering);
            log("setUsbTethering(" + tethering + ") = " + r + " (0 = ok)");
        } catch (Exception e) {
            log("setUsbTethering failed: " + cause(e));
        }
    }

    private void open(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            log("cannot open " + i + ": " + e);
        }
    }

    private void usbLogcat() {
        String[] lines = sh("logcat -d -v time", false).split("\n");
        StringBuilder b = new StringBuilder();
        for (String l : lines) {
            String low = l.toLowerCase(Locale.US);
            if (low.contains("usb") || low.contains("adb")) b.append(l).append('\n');
        }
        log(b.length() == 0 ? "no usb/adb lines (" + lines.length + " lines total; without READ_LOGS only own logs are visible)" : b.toString());
    }

    private void saveReport() {
        StringBuilder r = new StringBuilder(diagnosis());
        for (String c : REPORT_CMDS) r.append("\n== $ ").append(c).append(" ==\n").append(sh(c, false));
        File dir = new File(Environment.getExternalStorageDirectory(), "AndroidToolbox");
        dir.mkdirs();
        // .log, not .txt: the 2018/19 framework deletes unsigned files of listed types (txt included) on MTP read (SF-35).
        File f = new File(dir, "report-" + prop("ro.product.model").replace(' ', '_') + "-"
                + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".log");
        try (OutputStream o = new FileOutputStream(f)) {
            o.write(r.toString().getBytes("UTF-8"));
        } catch (Exception e) {
            log("write failed: " + e);
            return;
        }
        // Without a media scan the file is invisible over MTP until reboot.
        MediaScannerConnection.scanFile(this, new String[]{f.getPath()}, null, null);
        log("Saved " + f + " (" + f.length() + " bytes). Copy it off via USB file transfer.");
    }

    private void runShell() {
        final String c = cmd.getText().toString();
        final boolean su = useSu.isChecked();
        log("$ " + c);
        // ponytail: own thread, no timeout; a non-terminating command (e.g. plain logcat) leaks a thread.
        new Thread(() -> log(sh(c, su))).start();
    }

    // ---- diagnosis ----

    private String diagnosis() {
        StringBuilder r = new StringBuilder();
        r.append("== Device ==\n");
        for (String p : PROPS) r.append(p).append(" = ").append(prop(p)).append('\n');

        r.append("\n== Toolbox privileges ==\n");
        ApplicationInfo ai = getApplicationInfo();
        r.append("uid=").append(Process.myUid()).append(" apk=").append(ai.sourceDir)
                .append(" system=").append((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                .append(" platform-signed=").append(getPackageManager().checkSignatures("android", getPackageName())
                        == PackageManager.SIGNATURE_MATCH).append('\n');
        for (String p : PERMS) r.append(granted(p) ? "  granted " : "  MISSING ").append(p).append('\n');

        r.append("\n== Settings ==\n");
        for (String k : new String[]{Settings.Global.ADB_ENABLED, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                Settings.Global.DEVICE_PROVISIONED, Settings.Global.USB_MASS_STORAGE_ENABLED})
            r.append("global ").append(k).append(" = ").append(global(k)).append('\n');

        r.append("\n== User restrictions (set) ==\n");
        Bundle rs = ((UserManager) getSystemService(USER_SERVICE)).getUserRestrictions();
        for (String k : rs.keySet()) if (rs.getBoolean(k)) r.append("  ").append(k).append('\n');

        r.append("\n== Device admins ==\n");
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
        List<ComponentName> admins = dpm.getActiveAdmins();
        if (admins != null) for (ComponentName c : admins) r.append("  active ").append(c.flattenToShortString()).append('\n');
        try {
            r.append("  device owner = ").append(dpm.getClass().getMethod("getDeviceOwner").invoke(dpm)).append('\n');
        } catch (Exception e) {
            r.append("  device owner = ? ").append(cause(e)).append('\n');
        }

        r.append("\n== USB gadget (sysfs) ==\n");
        for (String g : GADGET) r.append(g).append(" = ").append(read("/sys/class/android_usb/android0/" + g)).append('\n');
        String[] fs = new File("/sys/class/android_usb/android0/").list();
        r.append("android0/ = ").append(fs == null ? "<not listable>" : TextUtils.join(" ", fs)).append('\n');

        r.append("\n== adbd processes ==\n");
        File[] procs = new File("/proc").listFiles();
        if (procs != null) for (File p : procs) {
            if (!p.getName().matches("\\d+")) continue;
            String c = read(p + "/cmdline").replace('\0', ' ').trim();
            if (c.contains("adbd")) r.append("  pid ").append(p.getName()).append(' ').append(c).append('\n');
        }

        r.append("\n== Last USB_STATE ==\n").append(extras(registerReceiver(null, new IntentFilter(USB_STATE)))).append('\n');

        r.append("\n== Verdict ==\n");
        for (String v : verdict()) r.append("* ").append(v).append('\n');
        return r.toString();
    }

    private List<String> verdict() {
        List<String> v = new ArrayList<>();
        if (!granted("android.permission.WRITE_SECURE_SETTINGS"))
            v.add("Toolbox lacks WRITE_SECURE_SETTINGS: 'Enable ADB' cannot work. Install to /system/priv-app or platform-sign.");
        if (restricted(NO_DEBUG))
            v.add("BLOCKER: user restriction " + NO_DEBUG + " is set. SettingsProvider silently drops adb_enabled writes "
                    + "and Developer options opens empty. Likely set by a device admin (see above). Try 'Clear ADB restrictions'.");
        if (restricted(NO_USB_FILES)) v.add(NO_USB_FILES + " is set.");
        String adb = global(Settings.Global.ADB_ENABLED), cfg = prop("sys.usb.config"),
                st = prop("sys.usb.state"), svc = prop("init.svc.adbd");
        if (!"1".equals(adb))
            v.add("adb_enabled = " + adb + ": the framework will not add adb to the USB configuration.");
        else if (!cfg.contains("adb"))
            v.add("adb_enabled=1 but sys.usb.config='" + cfg + "' lacks adb: UsbDeviceManager ignored it (patched services.jar?). "
                    + "Try 'USB: mtp,adb'; check 'dumpsys usb'.");
        else if (!st.contains("adb"))
            v.add("sys.usb.config='" + cfg + "' but sys.usb.state='" + st + "': init did not apply it; the vendor usb .rc "
                    + "probably has no entry for this combo. The report lists / and init*usb*.rc.");
        if (svc.isEmpty() && st.contains("adb"))
            v.add("USB is in an adb configuration but init.svc.adbd was never set: init could not exec adbd. On Taeyang "
                    + "DY8321 /sbin/adbd is absent from both boot and recovery ramdisks (and its SELinux exec type from the "
                    + "policy), so USB adb needs a repacked boot image. Without reflashing use 'Serial shell (ttyGS0)'.");
        else if (!"running".equals(svc))
            v.add("init.svc.adbd='" + svc + "': adbd is not running.");
        if (v.isEmpty())
            v.add("Device side looks correct. Check the host with tools/usb-probe.sh (is there a ff/42/01 ADB interface?) "
                    + "and watch the tablet for the RSA prompt (ro.adb.secure=" + prop("ro.adb.secure") + ").");
        return v;
    }

    private void refreshStatus() {
        ui.post(() -> {
            String st = prop("sys.usb.state"), svc = prop("init.svc.adbd");
            boolean ok = st.contains("adb") && "running".equals(svc);
            status.setBackgroundColor(ok ? 0xFF2E7D32 : restricted(NO_DEBUG) ? 0xFFC62828 : 0xFFEF6C00);
            status.setText(prop("ro.product.model") + " / Android " + prop("ro.build.version.release")
                    + "\nadb_enabled=" + global(Settings.Global.ADB_ENABLED) + "  " + NO_DEBUG + "=" + restricted(NO_DEBUG)
                    + "\nsys.usb.config=" + prop("sys.usb.config") + "  state=" + st
                    + "\nadbd=" + (svc.isEmpty() ? "<none>" : svc) + "  ro.adb.secure=" + prop("ro.adb.secure"));
        });
    }

    // ---- helpers ----

    private void section(LinearLayout p, String title, Object... items) {
        header(p, title);
        LinearLayout row = null;
        for (int i = 0; i < items.length; i += 2) {
            if (i % 4 == 0) {
                row = new LinearLayout(this);
                p.addView(row);
            }
            final String label = (String) items[i];
            final Runnable r = (Runnable) items[i + 1];
            Button b = new Button(this);
            b.setText(label);
            b.setAllCaps(false);
            b.setOnClickListener(v -> bg.execute(() -> {
                log("> " + label);
                try {
                    r.run();
                } catch (Throwable e) {
                    log("ERROR " + cause(e));
                }
                refreshStatus();
            }));
            row.addView(b, new LinearLayout.LayoutParams(0, -2, 1));
        }
    }

    private void header(LinearLayout p, String title) {
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(12), 0, dp(4));
        p.addView(t);
    }

    private void log(final String s) {
        ui.post(() -> {
            if (out.length() > 300_000) out.setText("[trimmed]\n");
            out.append(s + "\n");
            outScroll.post(() -> outScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private static Intent component(String pkg, String cls) {
        return new Intent().setComponent(new ComponentName(pkg, cls));
    }

    private boolean granted(String perm) {
        return checkCallingOrSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean restricted(String key) {
        return ((UserManager) getSystemService(USER_SERVICE)).getUserRestrictions().getBoolean(key);
    }

    private String global(String key) {
        return String.valueOf(Settings.Global.getString(getContentResolver(), key));
    }

    private static String prop(String key) {
        try {
            return (String) Class.forName("android.os.SystemProperties").getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }

    private static String extras(Intent i) {
        if (i == null || i.getExtras() == null) return "<none>";
        StringBuilder b = new StringBuilder();
        for (String k : i.getExtras().keySet()) b.append(k).append('=').append(i.getExtras().get(k)).append(' ');
        return b.toString().trim();
    }

    private static String cause(Throwable e) {
        return String.valueOf(e instanceof InvocationTargetException ? e.getCause() : e);
    }

    private static String read(String path) {
        try (InputStream in = new FileInputStream(path)) {
            return drain(in).trim();
        } catch (Exception e) {
            return "<" + e.getMessage() + ">";
        }
    }

    private static String drain(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        for (int n; (n = in.read(buf)) > 0; ) b.write(buf, 0, n);
        return b.toString("UTF-8");
    }

    private static String sh(String c, boolean su) {
        try {
            java.lang.Process p = new ProcessBuilder(su ? new String[]{"su"} : new String[]{"sh", "-c", c})
                    .redirectErrorStream(true).start();
            OutputStream o = p.getOutputStream();
            if (su) o.write((c + "\nexit\n").getBytes());
            o.close();
            String s = drain(p.getInputStream());
            return s + "[exit " + p.waitFor() + "]";
        } catch (Exception e) {
            return "<" + e + ">";
        }
    }
}
