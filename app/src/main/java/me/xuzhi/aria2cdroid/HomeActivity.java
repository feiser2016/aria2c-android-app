package me.xuzhi.aria2cdroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.NetworkUtils;
import com.google.common.base.Strings;
import com.jkb.fragment.rigger.annotation.Puppet;
import com.jkb.fragment.rigger.rigger.Rigger;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.nio.file.Files;
import java.util.Map;

import me.xuzhi.aria2cdroid.views.AdvanceFragment;
import me.xuzhi.aria2cdroid.views.LicenceFragment;
import me.xuzhi.aria2cdroid.views.LogsFragment;
import me.xuzhi.aria2cdroid.views.OnFragmentInteractionListener;
import me.xuzhi.aria2cdroid.views.ServFragment;
import me.xuzhi.aria2cdroid.views.SourceFragment;

@Puppet(containerViewId = R.id.mainFragment)
public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnFragmentInteractionListener {

    private Fragment[] fragments;
    private LogsFragment logsFragment;
    private AdvanceFragment advanceFragment;
    private Typeface typeface;
    private TextView tvHeadTitle, tvHeadDesc;
    private FloatingActionButton fab;
    private PermissionRequest permissionRequest;
    private Aria2Service aria2Service;
    private Intent intentService;
    private Map<String, String> aria2config;

    private ServiceConnection conn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Aria2Service.MyBinder binder = (Aria2Service.MyBinder) service;
            aria2Service = binder.getService();
            if (!aria2Service.isRunning()) {
                startService(intentService);
                try {
                    String ip = NetworkUtils.getIPAddress(true);
                    String port = aria2config.get("rpc-listen-port") + "";
                    String secret = aria2config.get("rpc-secret") + "";
                    Utils.writeConsoleLog(logsFragment, 0, "ip:" + ip);
                    Utils.writeConsoleLog(logsFragment, 0, "rpc-port:" + port);
                    Utils.writeConsoleLog(logsFragment, 0, "rpc-secret:" + secret);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Utils.writeConsoleLog(logsFragment, 0, getString(R.string.service_is_running));
            }
            resetFabStatus();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private void resetFabStatus() {
        if (fabMode == 0) {
            if (aria2Service == null || !aria2Service.isRunning()) {
                fab.setTag(null);
                fab.setImageResource(R.drawable.ic_action_start_arrow);
            } else {
                fab.setTag("true");
                fab.setImageResource(R.drawable.ic_action_stop);
            }
        } else if (fabMode == 1) {
            fab.setImageResource(R.drawable.ic_save);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        fragments = new Fragment[4];
        logsFragment = new LogsFragment();
        advanceFragment = new AdvanceFragment();
        fragments[0] = logsFragment;
        fragments[1] = advanceFragment;
        fragments[2] = new SourceFragment();
        fragments[3] = new LicenceFragment();

        Rigger.getRigger(this).addFragment(R.id.mainFragment, fragments);


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setItemIconTintList(null);

        EventBus.getDefault().register(this);

        permissionRequest = new PermissionRequest(this, 3100);
        permissionRequest.request(permissionRequestCallback, Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private void onAllPermissionsGranted() {

        try {
            if (!Utils.getConfigFile(getApplicationContext()).exists()) {
                String cfg = Utils.createDefaultConfig(getApplicationContext());
                com.google.common.io.Files.write(cfg.getBytes(), Utils.getConfigFile(getApplicationContext()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        aria2config = Utils.readAria2Config(getApplicationContext());
        Rigger.getRigger(this).showFragment(Rigger.getRigger(fragments[0]).getFragmentTAG());

        fab.setOnClickListener(onFabClickListener);

        intentService = new Intent(this, Aria2Service.class);
        bindService(intentService, conn, BIND_AUTO_CREATE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionRequest.processPermissionRequestResult(requestCode, permissions, grantResults);
    }

    private PermissionRequest.PermissionRequestCallback permissionRequestCallback = new PermissionRequest.PermissionRequestCallback() {
        @Override
        public void onPermissionGranted() {
            onAllPermissionsGranted();
        }

        @Override
        public void onPermissionDenied() {
            finish();
        }
    };

    private void onSwitchChanged(boolean ifStart) {
        if (ifStart) {
            Utils.writeConsoleLog(logsFragment, 0, getString(R.string.starting));
            aria2Service.start();
        } else {
            Utils.writeConsoleLog(logsFragment, 0, getString(R.string.stopping));
            aria2Service.stop();
        }
    }

    private int fabMode = 0;

    private View.OnClickListener onFabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (fabMode == 0) {
                Object oTag = fab.getTag();
                if (oTag == null) {
                    onSwitchChanged(true);
                } else {
                    onSwitchChanged(false);
                }
            } else if (fabMode == 1) {
                advanceFragment.saveSettings();
            }
            resetFabStatus();
        }
    };

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.home, menu);
        //return true;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("RestrictedApi")
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_logs:
                try {
                    fabMode = 0;
                    getSupportActionBar().setTitle(getString(R.string.app_menu_service));
                    fab.setVisibility(View.VISIBLE);
                    resetFabStatus();
                    Rigger.getRigger(this).showFragment(Rigger.getRigger(fragments[0]).getFragmentTAG());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_advance:
                try {
                    fabMode = 1;
                    getSupportActionBar().setTitle(getString(R.string.app_menu_advance));
                    fab.setVisibility(View.VISIBLE);
                    resetFabStatus();
                    fab.setImageResource(R.drawable.ic_save);
                    Rigger.getRigger(this).showFragment(Rigger.getRigger(fragments[1]).getFragmentTAG());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_source:
                try {
                    fabMode = 2;
                    getSupportActionBar().setTitle(getString(R.string.app_menu_source));
                    fab.setVisibility(View.INVISIBLE);
                    Rigger.getRigger(this).showFragment(Rigger.getRigger(fragments[2]).getFragmentTAG());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.nav_rate:
                try {
                    fabMode = 3;
                    getSupportActionBar().setTitle(getString(R.string.app_menu_rate));
                    fab.setVisibility(View.INVISIBLE);
                    Rigger.getRigger(this).showFragment(Rigger.getRigger(fragments[3]).getFragmentTAG());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onFragmentInteraction(Message msg) {

        if (msg.what == 0x6000) {

            if (logsFragment != null && logsFragment.getLogView3() != null) {
                int type = msg.getData().getInt("type");
                String log = msg.getData().getString("msg");
                Utils.writeConsoleLog(logsFragment, type, log);
            }
        } else if (msg.what == AdvanceFragment.SAVE_CONFIG_SUCCESS) {
            if (fabMode == 1 && fab != null) {
                Snackbar.make(fab, getString(R.string.app_string_save_config_success), Snackbar.LENGTH_LONG)
                        .setAction(getString(R.string.app_string_advance_config), null).show();
                Utils.writeConsoleLog(logsFragment, 0, getString(R.string.app_string_save_config_success));
            }
        }

    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        switch (event.getEventName()) {
            case Aria2Service.ARIA2_SERVICE_START_SUCCESS:
                Utils.writeConsoleLog(logsFragment, 0, getString(R.string.service_start_alrerady) + event.getEventData().toString());
                break;
            case Aria2Service.ARIA2_SERVICE_START_FAIL:
                Utils.writeConsoleLog(logsFragment, 0, getString(R.string.listen_service_fail));
                break;
            case Aria2Service.ARIA2_SERVICE_START_STOPPED:
                Utils.writeConsoleLog(logsFragment, 0, getString(R.string.service_stop_alrerady));
                break;
            case Aria2Service.ARIA2_SERVICE_BIN_CONSOLE:
                Utils.writeConsoleLog(logsFragment, 0, event.getEventData().toString());
                break;
            case Aria2Service.ARIA2_CONFIG_MISS:
                Utils.writeConsoleLog(logsFragment, 0, getString(R.string.app_string_config_miss));
                break;

        }
    }
}
