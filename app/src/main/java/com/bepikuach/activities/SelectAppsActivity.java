package com.bepikuach.activities;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bepikuach.R;
import com.bepikuach.utils.AdminAppsAdapter;
import com.bepikuach.utils.AppInfo;
import com.bepikuach.utils.LockTaskManager;
import com.bepikuach.utils.PrefManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SelectAppsActivity extends AppCompatActivity {

    private PrefManager prefManager;
    private LockTaskManager lockTaskManager;
    private AdminAppsAdapter adapter;

    // טאבים
    private View tabApps, tabBlocked;
    private View panelApps, panelBlocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_apps);

        prefManager = new PrefManager(this);
        lockTaskManager = new LockTaskManager(this);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("ניהול אפליקציות");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        setupTabs();
        setupAppsList();
        setupBlockedLog();
    }

    private void setupTabs() {
        tabApps = findViewById(R.id.tabApps);
        tabBlocked = findViewById(R.id.tabBlocked);
        panelApps = findViewById(R.id.panelApps);
        panelBlocked = findViewById(R.id.panelBlocked);

        tabApps.setOnClickListener(v -> switchTab(true));
        tabBlocked.setOnClickListener(v -> switchTab(false));
        switchTab(true);
    }

    private void switchTab(boolean showApps) {
        panelApps.setVisibility(showApps ? View.VISIBLE : View.GONE);
        panelBlocked.setVisibility(showApps ? View.GONE : View.VISIBLE);

        tabApps.setBackgroundColor(showApps ? 0xFF3F51B5 : 0xFFE8EAF6);
        tabBlocked.setBackgroundColor(showApps ? 0xFFE8EAF6 : 0xFF3F51B5);

        ((TextView) tabApps).setTextColor(showApps ? 0xFFFFFFFF : 0xFF3F51B5);
        ((TextView) tabBlocked).setTextColor(showApps ? 0xFF3F51B5 : 0xFFFFFFFF);

        if (!showApps) refreshBlockedLog();
    }

    private void setupAppsList() {
        List<AppInfo> apps = getAllInstalledApps();
        adapter = new AdminAppsAdapter(apps);

        RecyclerView list = findViewById(R.id.appsList);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        SearchView search = findViewById(R.id.searchApps);
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { return false; }
            @Override public boolean onQueryTextChange(String q) { adapter.filter(q); return true; }
        });

        Button saveBtn = findViewById(R.id.btnSave);
        saveBtn.setOnClickListener(v -> {
            Set<String> approved = new HashSet<>();
            for (AppInfo app : adapter.getAllApps()) {
                if (app.isApproved) approved.add(app.packageName);
            }
            prefManager.setApprovedApps(approved);
            lockTaskManager.updateApprovedPackages(approved);
            Toast.makeText(this, "נשמר בהצלחה ✓", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupBlockedLog() {
        refreshBlockedLog();

        Button clearBtn = findViewById(R.id.btnClearLog);
        clearBtn.setOnClickListener(v -> {
            prefManager.clearBlockedLog();
            refreshBlockedLog();
            Toast.makeText(this, "הלוג נוקה ✓", Toast.LENGTH_SHORT).show();
        });
    }

    private void refreshBlockedLog() {
        LinearLayout logContainer = findViewById(R.id.blockedLogContainer);
        TextView emptyMsg = findViewById(R.id.blockedLogEmpty);
        logContainer.removeAllViews();

        List<String> log = prefManager.getBlockedLog();
        if (log.isEmpty()) {
            emptyMsg.setVisibility(View.VISIBLE);
            return;
        }
        emptyMsg.setVisibility(View.GONE);

        // מיון לפי זמן — חדש קודם
        Collections.sort(log, Collections.reverseOrder());

        PackageManager pm = getPackageManager();
        Set<String> approved = prefManager.getApprovedApps();

        for (String entry : log) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length < 2) continue;
            String pkg = parts[1];
            long timestamp = 0;
            try { timestamp = Long.parseLong(parts[0]); } catch (Exception ignored) {}

            // בנה שורה
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, 12, 0, 12);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // אייקון + שם
            LinearLayout info = new LinearLayout(this);
            info.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            info.setLayoutParams(infoParams);

            TextView nameView = new TextView(this);
            String appName = pkg;
            try {
                appName = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
            } catch (Exception ignored) {}
            nameView.setText(appName);
            nameView.setTextColor(0xFF1A1A2E);
            nameView.setTextSize(14);

            TextView timeView = new TextView(this);
            if (timestamp > 0) {
                timeView.setText(new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(new Date(timestamp)));
            }
            timeView.setTextColor(0xFF9E9E9E);
            timeView.setTextSize(11);

            info.addView(nameView);
            info.addView(timeView);

            // כפתור "התר"
            boolean isAlreadyApproved = approved.contains(pkg);
            Button allowBtn = new Button(this);
            allowBtn.setText(isAlreadyApproved ? "מותר ✓" : "התר");
            allowBtn.setTextSize(12);
            allowBtn.setEnabled(!isAlreadyApproved);
            allowBtn.setBackgroundColor(isAlreadyApproved ? 0xFF9E9E9E : 0xFF43A047);
            allowBtn.setTextColor(0xFFFFFFFF);

            final String finalPkg = pkg;
            final String finalName = appName;
            allowBtn.setOnClickListener(v -> {
                Set<String> newApproved = prefManager.getApprovedApps();
                newApproved.add(finalPkg);
                prefManager.setApprovedApps(newApproved);
                lockTaskManager.updateApprovedPackages(newApproved);
                allowBtn.setText("מותר ✓");
                allowBtn.setEnabled(false);
                allowBtn.setBackgroundColor(0xFF9E9E9E);
                Toast.makeText(this, finalName + " הותר ✓", Toast.LENGTH_SHORT).show();
            });

            // קו הפרדה
            View divider = new View(this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            divider.setBackgroundColor(0xFFC5CAE9);

            row.addView(info);
            row.addView(allowBtn);

            LinearLayout wrapper = new LinearLayout(this);
            wrapper.setOrientation(LinearLayout.VERTICAL);
            wrapper.setPadding(16, 0, 16, 0);
            wrapper.addView(row);
            wrapper.addView(divider);

            logContainer.addView(wrapper);
        }
    }

    private List<AppInfo> getAllInstalledApps() {
        List<AppInfo> result = new ArrayList<>();
        PackageManager pm = getPackageManager();
        Set<String> approved = prefManager.getApprovedApps();

        List<ApplicationInfo> installed = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo info : installed) {
            if (info.packageName.equals("com.bepikuach")) continue;

            Intent launchIntent = new Intent(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            launchIntent.setPackage(info.packageName);
            if (pm.queryIntentActivities(launchIntent, 0).isEmpty()) continue;

            String name;
            try { name = pm.getApplicationLabel(info).toString(); }
            catch (Exception e) { name = info.packageName; }

            try {
                result.add(new AppInfo(name, info.packageName,
                        pm.getApplicationIcon(info.packageName),
                        approved.contains(info.packageName)));
            } catch (PackageManager.NameNotFoundException ignored) {}
        }
        result.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        return result;
    }

    @Override public boolean onSupportNavigateUp() { finish(); return true; }
}
