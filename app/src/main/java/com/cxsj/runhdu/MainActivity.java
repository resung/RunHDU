package com.cxsj.runhdu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.cxsj.runhdu.adapters.MyFragmentPagerAdapter;
import com.cxsj.runhdu.constant.Types;
import com.cxsj.runhdu.sport.RunningInfo;
import com.cxsj.runhdu.utils.QueryUtil;
import com.cxsj.runhdu.utils.ScreenShot;
import com.cxsj.runhdu.utils.SyncUtil;
import com.cxsj.runhdu.utils.Utility;
import com.cxsj.runhdu.view.GradeProgressView;

import org.litepal.LitePal;
import org.litepal.LitePalDB;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import lecho.lib.hellocharts.gesture.ContainerScrollType;
import lecho.lib.hellocharts.listener.ColumnChartOnValueSelectListener;
import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.AxisValue;
import lecho.lib.hellocharts.model.Column;
import lecho.lib.hellocharts.model.ColumnChartData;
import lecho.lib.hellocharts.model.SubcolumnValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.ColumnChartView;

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    //private CollapsingToolbarLayoutState state;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private GradeProgressView circleProgress;
    private ImageView menuButton;
    private ImageView menuBgImg;
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private TextView dataDescription;
    private TextView progressRunTimesText;
    private TextView progressStepsText;
    private TextView progressDisEnergyText;
    private TextView welcomeText;
    private CircleImageView profileImage;
    private DrawerLayout drawerLayout;
    private FloatingActionButton fab;
    private ColumnChartView columnChart;
    private TodayFragment todayFragment;
    private HistoryFragment historyFragment;
    private FrameLayout progressView;
    private LinearLayout chartView;
    private ProgressDialog progressDialog;

    private List<String> mTitle = new ArrayList<>();
    private List<Fragment> mFragment = new ArrayList<>();
    private List<RunningInfo> runningInfoList = new ArrayList<>();
    private List<String> chartLabels = new ArrayList<>();
    private List<Float> chartValues = new ArrayList<>();

    private int chartColumnNum;//图表显示的列数
    private float targetSteps;

//    private enum CollapsingToolbarLayoutState {
//        EXPANDED,
//        COLLAPSED,
//        INTERMEDIATE
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.getMenu().getItem(0).setChecked(true);

        collapsingToolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        viewPager = (ViewPager) findViewById(R.id.view_pager);
        progressRunTimesText = (TextView) findViewById(R.id.progress_times);
        progressStepsText = (TextView) findViewById(R.id.progress_step_num);
        progressDisEnergyText = (TextView) findViewById(R.id.progress_dis_energy);
        circleProgress = (GradeProgressView) findViewById(R.id.circle_progress);
        menuButton = (ImageView) findViewById(R.id.menu_button);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        menuBgImg = (ImageView) navigationView.getHeaderView(0).findViewById(R.id.menu_bg_img);
        columnChart = (ColumnChartView) findViewById(R.id.column_chart);
        welcomeText = (TextView) navigationView.getHeaderView(0).findViewById(R.id.welcome_text);
        profileImage = (CircleImageView) navigationView.getHeaderView(0).findViewById(R.id.profile_image);
        dataDescription = (TextView) findViewById(R.id.data_description);
        progressView = (FrameLayout) findViewById(R.id.progress_view_layout);
        chartView = (LinearLayout) findViewById(R.id.chart_view_layout);
        addToolbar(R.id.toolbar_main, false);

        menuButton.setOnClickListener(this);
        fab.setOnClickListener(this);
        profileImage.setOnClickListener(this);
        menuBgImg.setOnClickListener(this);
        navigationView.setNavigationItemSelectedListener(this);

        if (TextUtils.isEmpty(username)) exitLogin();

        //初始化数据库
        LitePalDB litePalDB = LitePalDB.fromDefault(username);
        LitePal.use(litePalDB);

        initSettings();
        initView();
        checkUpdate(this);
        checkServerData();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    chartView.setVisibility(View.INVISIBLE);
                    progressView.setVisibility(View.VISIBLE);
                } else {
                    chartView.setVisibility(View.VISIBLE);
                    progressView.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });


        columnChart.setOnValueTouchListener(new ColumnChartOnValueSelectListener() {
            @SuppressLint({"SetTextI18n", "WrongConstant"})
            @Override
            public void onValueSelected(int columnIndex, int subcolumnIndex, SubcolumnValue value) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(new Date());
                calendar.add(Calendar.DATE, -1 * (chartColumnNum - columnIndex - 1));
                String year = String.valueOf(calendar.get(Calendar.YEAR));
                String month = String.valueOf(calendar.get(Calendar.MONTH) + 1);
                String day = String.valueOf(calendar.get(Calendar.DATE));

                int dis = 0;
                runningInfoList = QueryUtil.findOrder(
                        "year = ? and month = ? and date = ?",
                        year, month, day);
                for (RunningInfo info : runningInfoList) {
                    dis += info.getDistance();
                }
                dataDescription.setText(
                        year + "年" + month + "月" + day + "日\n"
                                + "跑步" + runningInfoList.size()
                                + "次 | "
                                + (int) value.getValue()
                                + "步 | "
                                + dis + "米");
            }

            @Override
            public void onValueDeselected() {

            }
        });
    }

    @Override
    protected void onDestroy() {
        closeProgressDialog();
        super.onDestroy();
    }

    //检查网络数据
    private void checkServerData() {
        if (!isSyncOn) return;
        showProgressDialog("正在获取服务器信息...");
        //获取服务器端的跑步次数
        Log.d("次数", username);

        SyncUtil.checkServerData(username, new SyncUtil.checkDataCallback() {
            @Override
            public void onCheckFailure(String msg) {
                closeProgressDialog();
                showSnackBar(msg);
            }

            @Override
            public void onCheckSuccess(int serverTimes, int localTimes) {
                closeProgressDialog();
                if (serverTimes != localTimes) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("同步数据")
                            .setMessage(String.format("本地数据与服务器不一致：\n" +
                                    "本地数据：%d条；\n" +
                                    "服务器数据：%d条。\n" +
                                    "请选择操作：", localTimes, serverTimes))
                            .setPositiveButton("服务器数据同步至本地", (dialog, which) -> {
                                syncFromServer();
                            })
                            .setNegativeButton("本地数据上传至服务器", (dialog, which) -> {
                                uploadToServer();
                            })
                            .setNeutralButton("以后再说", (dialog, which) -> {
                            }).create().show();
                }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void initView() {
        //初始化进度条数值
        circleProgress.setProgressWidthAnimation(0);
        //初始化Title
        collapsingToolbarLayout.setTitle(getResources().getString(R.string.title_activity_main));
        //初始化TabLayout
        mTitle.add("跑步详情");
        mTitle.add("数据统计");
        mFragment.add(new TodayFragment());
        mFragment.add(new HistoryFragment());
        MyFragmentPagerAdapter adapter = new MyFragmentPagerAdapter(getSupportFragmentManager(), mTitle, mFragment);
        todayFragment = (TodayFragment) adapter.getItem(0);
        historyFragment = (HistoryFragment) adapter.getItem(1);
        viewPager.setAdapter(adapter);
        tabLayout.setupWithViewPager(viewPager);
        //初始化ColumnChart
        columnChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        columnChart.setInteractive(true);
        columnChart.setZoomEnabled(false);
        columnChart.setContainerScrollEnabled(true, ContainerScrollType.HORIZONTAL);
        chartView.setVisibility(View.INVISIBLE);
        //初始化welcome文字
        welcomeText.setText(Utility.getTime(Types.TYPE_AM_PM) + "好，" + username + "！");
        setMainData();
    }

    private void initSettings() {
        //设置头像
        String fileName = (String) prefs.get("profile_path", null);
        if (!TextUtils.isEmpty(fileName)) {
            Uri uri = Uri.fromFile(new File(fileName));
            profileImage.setImageURI(uri);
        }
//
//        Glide.with(this).load(URLs.PROFILE_URL + username + "_1.JPEG")
//                .bitmapTransform(new CropCircleTransformation(this))
//                .placeholder(R.drawable.photo).error(R.drawable.photo)
//                .crossFade(0).into(profileImage);

        //设置菜单栏背景图
        String menuBgUri = (String) prefs.get("menu_bg_uri", null);
        if (!TextUtils.isEmpty(menuBgUri)) {
            Glide.with(this).load(Uri.parse(menuBgUri)).into(menuBgImg);
        }

        chartColumnNum = Integer.parseInt(
                (String) prefs.get("chart_column_num", "30"));
        targetSteps = Float.parseFloat(
                (String) prefs.get("target_steps", "5000")
        );
        isSyncOn = (boolean) prefs.get("sync_data", true);
    }

    private void setMainData() {
        setProgressView();
        setChartData();
    }


    private void setAllData() {
        setMainData();
        try {
            todayFragment.setListData();
            historyFragment.updateData();
        } catch (NullPointerException e) {
            Toast.makeText(this, "APP异常，现已恢复。如果出现此提示，请联系开发者！", Toast.LENGTH_LONG).show();
            recreate();
        }
    }

    @SuppressLint("SetTextI18n")
    private void setProgressView() {
        circleProgress.setProgress(0);
        //从数据库里读出数据。
        runningInfoList = QueryUtil.findOrder(
                "year = ? and month = ? and date = ?",
                Utility.getTime(Calendar.YEAR),
                Utility.getTime(Types.TYPE_MONTH),
                Utility.getTime(Calendar.DATE));
        int steps = 0;
        int times = 0;
        int dis = 0;
        int energy = 0;
        for (RunningInfo info : runningInfoList) {
            steps += info.getSteps();
            times++;
            dis += info.getDistance();
            energy += info.getEnergy();
        }
        progressRunTimesText.setText("今日跑步" + times + "次");
        progressStepsText.setText(String.valueOf(steps));
        progressDisEnergyText.setText(Utility.formatDecimal(dis / 1000.0, 2)
                + "KM" + " | " + energy + "千卡");
        circleProgress.setProgressWidthAnimation((int) (steps / targetSteps * 100));
    }

    @SuppressLint("WrongConstant")
    private void setChartData() {
        chartLabels.clear();
        chartValues.clear();
        //初始化ColumnChart
        Calendar c = Calendar.getInstance();
        c.setTime(new Date());
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.CHINA);
        for (int i = 0; i < chartColumnNum; i++) {
            chartLabels.add(sdf.format(c.getTime()));
            runningInfoList = QueryUtil.findOrder("year = ? and month = ? and date = ?",
                    String.valueOf(c.get(Calendar.YEAR)),
                    String.valueOf(c.get(Calendar.MONTH) + 1),
                    String.valueOf(c.get(Calendar.DATE)));
            int steps = 0;
            for (RunningInfo info : runningInfoList) {
                steps += info.getSteps();
            }
            chartValues.add((float) steps);
            c.add(Calendar.DATE, -1);
        }
        Collections.reverse(chartLabels);
        Collections.reverse(chartValues);
        columnChart.setColumnChartData(getChartData(chartLabels, chartValues));

        Viewport v1 = new Viewport(columnChart.getMaximumViewport());
        v1.bottom = 0;
        if (!chartValues.isEmpty()) {
            v1.top = Collections.max(chartValues);
        } else {
            v1.top = 5000;
        }
        columnChart.setMaximumViewport(v1);
        //设置当前的窗口显示多少个坐标数据
        Viewport v2 = new Viewport(columnChart.getMaximumViewport());
        v2.right = chartLabels.size();
        v2.left = chartLabels.size() - 7;
        columnChart.setCurrentViewport(v2);

        dataDescription.setText("此处显示30天内步数统计图\n点击柱形图，查看详细信息");

    }


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
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initSettings();
        checkServerData();
        setAllData();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.sport_status:
                break;
            case R.id.quit:
                finish();
                break;
            case R.id.sunlight_long_run:
                toActivity(MainActivity.this, SunnyRunActivity.class);
                break;
            case R.id.share:
                ScreenShot.takeAndShare(this);
                break;
            case R.id.about:
                toActivity(MainActivity.this, AboutActivity.class);
                break;
            case R.id.settings:
                toActivity(this, SettingsActivity.class);
                break;
            default:
                showComingSoonDialog();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.menu_button:
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            case R.id.fab:
                checkRunPermission();
                break;
            case R.id.menu_bg_img:
            case R.id.profile_image:
                String[] items = {"修改头像", "更改背景图", "退出登录"};
                new AlertDialog.Builder(this)
                        .setItems(items, (dialog, which) -> {
                            switch (which) {
                                case 0:
                                    choosePhoto(Types.TYPE_CHANGE_PROFILE);
                                    break;
                                case 1:
                                    choosePhoto(Types.TYPE_CHANGE_MENU_BG);
                                    break;
                                case 2:
                                    exitLogin();
                                    break;
                                default:
                            }
                        }).show();
                break;
            default:
                break;
        }
    }

    //开启跑步Activity，此方法包括一些跑步条件检查
    public void startRunActivity() {
        if (isSyncOn) {
            if (!Utility.isNetworkAvailable(getApplicationContext())) {
                Snackbar.make(collapsingToolbarLayout, R.string.internet_not_connect, Snackbar.LENGTH_LONG)
                        .setAction("知道了", v -> {
                        }).show();
                return;
            }
        }
        toActivity(MainActivity.this, RunningActivity.class);
    }

    private void checkRunPermission() {
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission
                (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!permissionList.isEmpty()) {
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this, permissions, 1);
        } else {
            startRunActivity();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Snackbar.make(collapsingToolbarLayout, "必须同意所有权限，才可以开始跑步。", Snackbar.LENGTH_LONG)
                                    .setAction("知道了", v -> {
                                    }).show();
                            return;
                        }
                    }
                    startRunActivity();
                } else {
                    Toast.makeText(this, "发生未知错误", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
        }
    }

    private ColumnChartData getChartData(List<String> labels, List<Float> nums) {
        if (labels.size() != nums.size()
                || labels.isEmpty()
                || nums.isEmpty()) return null;

        ColumnChartData data;
        // 列，每列1个柱状图。
        int numSubcolumns = 1;
        int numColumns = labels.size();
        //圆柱对象集合
        List<Column> columns = new ArrayList<>();
        //子列数据集合
        List<SubcolumnValue> values;
        List<AxisValue> axisValues = new ArrayList<>();
        //遍历列数numColumns
        for (int i = 0; i < numColumns; ++i) {
            values = new ArrayList<>();
            //遍历每一列的每一个子列
            for (int j = 0; j < numSubcolumns; ++j) {
                //为每一柱图添加颜色和数值
                values.add(new SubcolumnValue(nums.get(i), Color.parseColor("#03A9F4")));
            }
            //创建Column对象
            Column column = new Column(values);
            //是否有数据标注
            column.setHasLabels(true);
            //是否是点击圆柱才显示数据标注
            column.setHasLabelsOnlyForSelected(false);
            columns.add(column);
            //给x轴坐标设置描述
            axisValues.add(new AxisValue(i).setLabel(labels.get(i)));
        }
        //创建一个带有之前圆柱对象column集合的ColumnChartData
        data = new ColumnChartData(columns);

        //定义x轴y轴相应参数
        Axis axisX = new Axis();
        axisX.setTextColor(Color.parseColor("#ffffff"));
        axisX.setValues(axisValues);

        //把X轴Y轴数据设置到ColumnChartData 对象中
        data.setAxisXBottom(axisX);
        //data.setAxisYLeft(axisY);
        return data;
    }

    private void choosePhoto(int type) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");//相片类型
        startActivityForResult(intent, type);
    }

    public void startPhotoZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 设置裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 196);
        intent.putExtra("outputY", 196);
        intent.putExtra("return-data", true);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        startActivityForResult(intent, Types.TYPE_SAVE_PROFILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK) {
            return;
        }
        switch (requestCode) {
            case Types.TYPE_CHANGE_PROFILE:
                Uri profileUri = data.getData();
                startPhotoZoom(profileUri);
                break;
            case Types.TYPE_CHANGE_MENU_BG:
                Uri bgUri = data.getData();
                prefs.put("menu_bg_uri", bgUri.toString());
                Glide.with(MainActivity.this).load(bgUri).into(menuBgImg);
                break;
            case Types.TYPE_SAVE_PROFILE:
                if (data != null) {
                    Bitmap bitmap = data.getParcelableExtra("data");
                    profileImage.setImageBitmap(bitmap);
                    saveProfile(bitmap);
                }
                break;
            default:
                break;
        }
    }

    private void showSnackBar(String text) {
        Snackbar.make(collapsingToolbarLayout,
                text, Snackbar.LENGTH_LONG)
                .setAction("知道了", v -> {
                }).show();
    }

    //同步从服务器得到的数据到本地并显示
    private void syncFromServer() {
        showProgressDialog("正在从服务器同步...");

        SyncUtil.syncFromServer(username, new SyncUtil.SyncDataCallback() {
            @Override
            public void onSyncFailure(String msg) {
                closeProgressDialog();
                showSnackBar(msg);
            }

            @Override
            public void onSyncSuccess() {
                closeProgressDialog();
                showSnackBar("同步成功。");
                setAllData();
            }
        });
    }

    //把本地的数据同步至服务器
    private void uploadToServer() {
        showProgressDialog("正在上传至服务器...");

        SyncUtil.uploadAllToServer(username, new SyncUtil.SyncDataCallback() {
            @Override
            public void onSyncFailure(String msg) {
                closeProgressDialog();
                showSnackBar(msg);
            }

            @Override
            public void onSyncSuccess() {
                closeProgressDialog();
                showSnackBar("上传成功。");
            }
        });
    }

    private void saveProfile(Bitmap bitmap) {
        //保存到SD卡
        FileOutputStream fos = null;
        String fileName = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                + "profile.jpg";
        try {
            fos = new FileOutputStream(fileName);
            if (null != fos) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.flush();
                fos.close();
                prefs.put("profile_path", fileName);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
