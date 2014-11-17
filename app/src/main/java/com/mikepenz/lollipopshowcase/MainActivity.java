package com.mikepenz.lollipopshowcase;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.util.Pair;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.ui.LibsActivity;
import com.mikepenz.lollipopshowcase.adapter.ApplicationAdapter;
import com.mikepenz.lollipopshowcase.entity.AppInfo;
import com.mikepenz.lollipopshowcase.itemanimator.RebountItemAnimator;
import com.mikepenz.lollipopshowcase.util.UploadHelper;
import com.mikpenz.iconics.IconicsDrawable;
import com.mikpenz.iconics.typeface.FontAwesome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends ActionBarActivity {
    private List<AppInfo> applicationList = new ArrayList<AppInfo>();

    private ApplicationAdapter mAdapter;
    private ImageButton fabButton;
    private SwipeRefreshLayout mSwipeRefreshLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set explode animation when enter and exit the activity
        //Utils.configureWindowEnterExitTransition(getWindow());

        // Handle Toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Handle DrawerLayout
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer);

        // Handle ActionBarDrawerToggle
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        actionBarDrawerToggle.syncState();

        // Handle different Drawer States :D
        mDrawerLayout.setDrawerListener(actionBarDrawerToggle);

        // Handle DrawerList
        LinearLayout mDrawerList = (LinearLayout) findViewById(R.id.drawerList);

        // Init DrawerElems NOTE Just don't do this in a live app :D
        final SharedPreferences pref = getSharedPreferences("com.mikepenz.applicationreader", 0);
        ((Switch) mDrawerList.findViewById(R.id.drawer_autoupload)).setChecked(pref.getBoolean("autouploadenabled", false));
        ((Switch) mDrawerList.findViewById(R.id.drawer_autoupload)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean("autouploadenabled", isChecked);
                editor.apply();
            }
        });

        mDrawerList.findViewById(R.id.drawer_opensource).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Create an intent with context and the Activity class
                Intent i = new Intent(getApplicationContext(), LibsActivity.class);
                //Pass the fields of your application to the lib so it can find all external lib information
                i.putExtra(Libs.BUNDLE_FIELDS, Libs.toStringArray(R.string.class.getFields()));

                //Display the library version (OPTIONAL)
                i.putExtra(Libs.BUNDLE_VERSION, false);
                //Display the library license (OPTIONAL
                i.putExtra(Libs.BUNDLE_LICENSE, true);

                //Set a title (OPTIONAL)
                i.putExtra(Libs.BUNDLE_TITLE, getString(R.string.drawer_opensource));

                //Pass your theme (OPTIONAL)
                i.putExtra(Libs.BUNDLE_THEME, R.style.AboutTheme);

                //start the activity
                startActivity(i);
            }
        });
        ((ImageView) mDrawerList.findViewById(R.id.drawer_opensource_icon)).setImageDrawable(new IconicsDrawable(this, FontAwesome.Icon.faw_github).colorRes(R.color.secondary).actionBarSize());

        // Fab Button
        fabButton = (ImageButton) findViewById(R.id.fab_button);
        fabButton.setImageDrawable(new IconicsDrawable(this, FontAwesome.Icon.faw_upload).color(Color.WHITE).actionBarSize());
        fabButton.setOnClickListener(fabClickListener);
        Utils.configureFab(fabButton);

        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.list);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new RebountItemAnimator());
        mAdapter = new ApplicationAdapter(new ArrayList<AppInfo>(), R.layout.row_application, MainActivity.this);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new InitializeApplicationsTask().execute();
            }
        });

        new InitializeApplicationsTask().execute();
    }


    View.OnClickListener fabClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            UploadHelper.getInstance(MainActivity.this, applicationList).uploadAll();
        }
    };


    public void animateActivity(AppInfo appInfo, View appIcon) {
        Intent i = new Intent(this, DetailActivity.class);
        i.putExtra("appInfo", appInfo.getComponentName());

        ActivityOptionsCompat transitionActivityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(this, Pair.create((View) fabButton, "fab"), Pair.create(appIcon, "appIcon"));
        startActivity(i, transitionActivityOptions.toBundle());
    }


    private class InitializeApplicationsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            mSwipeRefreshLayout.setRefreshing(true);

            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            //Query the applications
            final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            applicationList.clear();

            List<ResolveInfo> ril = getPackageManager().queryIntentActivities(mainIntent, 0);
            for (ResolveInfo ri : ril) {
                applicationList.add(new AppInfo(MainActivity.this, ri));
            }
            Collections.sort(applicationList);

            for (AppInfo appInfo : applicationList) {
                //load icons before shown. so the list is smoother
                appInfo.getIcon();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mAdapter.setApplications(applicationList);

            mSwipeRefreshLayout.setRefreshing(false);

            super.onPostExecute(result);
        }
    }
}
