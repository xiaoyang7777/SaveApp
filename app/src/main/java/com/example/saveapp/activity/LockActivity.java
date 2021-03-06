package com.example.saveapp.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.utils.DistanceUtil;
import com.example.saveapp.PositionService;
import com.example.saveapp.R;
import com.example.saveapp.SharePreferenceKey;
import com.example.saveapp.bean.Position;
import com.example.saveapp.bean.User;
import com.example.saveapp.face.RealManFaceCheck.FaceVerify;
import com.example.saveapp.face.faceBase.FaceAdd;
import com.example.saveapp.util.Base64Util;
import com.example.saveapp.util.SharePreferenceUtil;
import com.google.android.cameraview.CameraView;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobGeoPoint;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;

public class LockActivity extends Activity implements SensorEventListener {
    private EditText password;
    private CameraView mCameraView;
    private static final String TAG = "LockActivity";
    private LocationClient mLocationClient = null;
    private BDLocationListener myListener = new LockActivity.MyLocationListener();
    private MediaPlayer mediaPlayer;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    public static int CURRENT_STEP = 0;
    // 加速计的三个维度数值
    public static float[] gravity = new float[3];
    //用三个维度算出的平均值
    public static float average = 0;
    public boolean callPolice = false;
    private ImageView police;


    public static void start(Context context) {
        Intent intent = new Intent(context, LockActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("LockActivity", "onCreate: ");
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.activity_lock);
        initCameraView();
        initLocation();
        password = findViewById(R.id.activity_lock_password);
        police=findViewById(R.id.activity_lock_police);
        TextView cancel = findViewById(R.id.activity_lock_cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String passwordString = password.getText().toString();
                if (passwordString.equals(BmobUser.getCurrentUser(User.class).getLockPassword())) {
                    SharePreferenceUtil.write(SharePreferenceKey.CALL_POLICE, false);
                    finish();
                } else {
                    if (mCameraView != null) {
                        mCameraView.takePicture();
                        callPolice();
                    }
                    Toast.makeText(LockActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
    }
    /**
     * 配置定位参数
     */
    private void initLocation() {
        mLocationClient = new LocationClient(this);
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        //配置定位参数
        //开始定位
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当gps有效时按照1S1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤gps仿真结果，默认需要
        mLocationClient.setLocOption(option);
        mLocationClient.start();
    }

    private void maxVoice() {
        // Audio管理器，用了控制音量
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        assert audioManager != null;
        final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);//获取最大音量值
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0); //tempVolume:音量绝对值
    }

    private void upLoadPosition(Position uploadPosition) {
        uploadPosition.save(new SaveListener<String>() {
            @Override
            public void done(String objectId, BmobException e) {
                if (e == null) {
                    Log.i(TAG, "upLoadPositionSucceed: ");
                } else {
                    Log.i(TAG, "upLoadPositionFailed: ");
                }
            }
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                // 用低通滤波器分离出重力加速度
                // alpha 由 t / (t + dT)计算得来，其中 t 是低通滤波器的时间常数，dT 是事件报送频率
                float alpha = 0.8f;
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
                average = (float) Math.sqrt(Math.pow(gravity[0], 2)
                        + Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));
                float maxValue = 10.0f;
                if (average >= maxValue) {
                    CURRENT_STEP++;
//                    Toast.makeText(LockActivity.this, "移动次数" + CURRENT_STEP, Toast.LENGTH_LONG).show();
                    if (CURRENT_STEP >= 50 && !callPolice) {
                        Toast.makeText(LockActivity.this, "开始报警", Toast.LENGTH_LONG).show();
                        callPolice();
                        mSensorManager.unregisterListener(this, sensor);
                    }
                    if (CURRENT_STEP >= 30) {
                        Toast.makeText(LockActivity.this, "请解除安全模式", Toast.LENGTH_LONG).show();
                    }

                }
            }
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i(TAG, "onKeyDown: ");
        if (mCameraView != null && callPolice) {
            mCameraView.takePicture();
        }
        return true;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class MyLocationListener implements BDLocationListener {
        private LatLng oldPosition = new LatLng(0, 0);

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceiveLocation(BDLocation location) {
            Log.i(TAG, "onReceiveLocation: "+location.getLatitude());
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            if (callPolice && DistanceUtil.getDistance(oldPosition, position) > 10) {
                oldPosition = new LatLng(location.getLatitude(), location.getLongitude());
                Position uploadPosition = new Position();
                uploadPosition.setUser_id(BmobUser.getCurrentUser(User.class).getObjectId());
                uploadPosition.setLocation(new BmobGeoPoint(location.getLongitude(), location.getLatitude()));
                upLoadPosition(uploadPosition);
            }
        }
    }

    private void autoTakePhoto() {
       new CountDownTimer(Integer.MAX_VALUE, 10000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (mCameraView != null) {
                    mCameraView.takePicture();
                }
            }

            @Override
            public void onFinish() {

            }
        }.start();
    }

    private void callPolice() {
        police.setImageResource(R.drawable.police);
        SharePreferenceUtil.write(SharePreferenceKey.CALL_POLICE, true);
        callPolice = true;
        autoTakePhoto();
        maxVoice();
        mediaPlayer = MediaPlayer.create(LockActivity.this, R.raw.police);
        mediaPlayer.setLooping(true);
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                mediaPlayer.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        if (mLocationClient != null) {
            mLocationClient.stop();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void initCameraView() {
        mCameraView = findViewById(R.id.activity_lock_camera);
        if (mCameraView != null) {
            mCameraView.addCallback(mCallback);
            mCameraView.setFacing(CameraView.FACING_FRONT);
            mCameraView.start();
        }
    }

    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
            Log.d(TAG, "onCameraOpened");
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            Log.d(TAG, "onCameraClosed");
        }


        @Override
        public void onPictureTaken(CameraView cameraView, final byte[] data) {
            Log.i(TAG, "onPictureTaken");
            checkAlive(data);
        }
    };

    private void checkAlive(byte[] data) {
        final String image = Base64Util.encode(data);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isFace = FaceVerify.isFace(image, "BASE64");
                if (isFace) {
                    Log.i(TAG, "isFace: ");
                    boolean isFaceAdd = FaceAdd.isFaceAdd(image, "sign", BmobUser.getCurrentUser(User.class).getMobilePhoneNumber());
                    if (isFaceAdd) {
                        Log.i(TAG, "AddFaceSucceed: ");
                    } else {
                        Log.i(TAG, "AddFaceFailed: ");
                    }
                } else {
                    Log.i(TAG, "isNotFace: ");
                }
            }
        }).start();

    }

//
//    @Override
//    public void onBackPressed() {
////        super.onBackPressed();
//    }
}
