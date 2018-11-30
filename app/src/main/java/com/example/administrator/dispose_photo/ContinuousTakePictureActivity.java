package com.example.administrator.dispose_photo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ContinuousTakePictureActivity extends AppCompatActivity implements SurfaceHolder.Callback,SensorEventListener{

    private ArrayList<String> mPicturePaths;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceViewHolder;
    private Camera mcamera;
    private int mPictureCount=3;//默认自动拍3张
    private final int mPictureCountMax=30;
    private String provider;
    private LocationManager locationManager;
    double lat=0;
    double lng=0;
    double alt=0;
    private SensorManager mSensorManager=null;
    private Sensor accelerometer; // 加速度传感器
    private Sensor magnetic; // 地磁场传感器
    int pic_count=0;
    String  filename=" ";
    String fileName1="";
    String message="";
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_continuous_take_picture);
        //StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        //StrictMode.setVmPolicy(builder.build());
        //builder.detectFileUriExposure();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//实例化传感器管理者

        accelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//初始化加速度传感器
        magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);//初始化地磁场传感器

        init();
    }
    private void init()
    {
        mPicturePaths=new ArrayList<String>();
        mSurfaceView=(SurfaceView)findViewById(R.id.surface_view);
        //获取SurfaceHolder
        mSurfaceViewHolder=mSurfaceView.getHolder();
        mSurfaceViewHolder.addCallback(this);
        //触摸屏幕完成对焦
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mcamera.autoFocus(null);
                //平滑的缩放：取值在0~mCamera.getParameters().getMaxZoom()
//                 mCamera.startSmoothZoom(2);
            }
        });
    }
    private void startCapture(){
        if(mcamera!=null)
        {
            Camera.Parameters parameters=mcamera.getParameters();

            List<String> list = parameters.getSupportedFocusModes();
            if (list.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            Point point=getBestCameraResolution(parameters,getScreenMetrics(ContinuousTakePictureActivity.this));

            parameters.setPictureSize(point.x,point.y);
            parameters.setPreviewSize(point.x,point.y);
            parameters.setPictureFormat(ImageFormat.JPEG);
            //调整保存的照片的旋转
            parameters.set("orientation", "portrait");
            parameters.setRotation(90);

            mcamera.setParameters(parameters);
            mcamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        camera.takePicture(null,null ,mPictureCallback );
                    }
                }
            });

        }
    }
    /**
     * 获取屏幕宽度和高度，单位为px
     * @param context
     * @return
     */
    public static Point getScreenMetrics(Context context){
        DisplayMetrics dm =context.getResources().getDisplayMetrics();
        int w_screen = dm.widthPixels;
        int h_screen = dm.heightPixels;
        return new Point(w_screen, h_screen);

    }
    /**
     * 获取最佳预览大小
     * @param parameters 相机参数
     * @param screenResolution 屏幕宽高
     * @return
     */
    private Point getBestCameraResolution(Camera.Parameters parameters, Point screenResolution) {
        float tmp = 0f;
        float mindiff = 100f;
        float x_d_y = (float) screenResolution.x / (float) screenResolution.y;
        Camera.Size best = null;
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        for (Camera.Size s : supportedPreviewSizes) {
            tmp = Math.abs(((float) s.height / (float) s.width) - x_d_y);
            if (tmp < mindiff) {
                mindiff = tmp;
                best = s;
            }
        }
        return new Point(best.width, best.height);
    }
    //拍摄成功后对图片的处理
    private Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
        @SuppressLint("LongLogTag")
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            FileOutputStream fileOutputStream = null;
            File file=null;
            try {
                String fileName=System.currentTimeMillis()+ ".jpg";
                File appDir = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath(), "image");
                if (!appDir.exists()) {
                    // 目录不存在 则创建
                    appDir.mkdirs();
                }
                 file = new File(appDir, fileName);

               fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(data);

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(ContinuousTakePictureActivity.this, "图片保存失败", Toast.LENGTH_SHORT).show();
            } finally {
                if (fileOutputStream != null) {
                    try {
                        if (mPictureCount>mPictureCountMax){
                            Toast.makeText(ContinuousTakePictureActivity.this,"为了节约内存，连拍张数不要超过"+mPictureCountMax+"张", Toast.LENGTH_SHORT).show();
                        }else {
                            if (++pic_count<mPictureCount){
                                //连拍三张
                                mPicturePaths.add(Environment.getExternalStorageDirectory()
                                        .getAbsolutePath());
                                try {
                                    TimeUnit.SECONDS.sleep(2);
                                }catch(InterruptedException e){
                                    e.printStackTrace();
                                }
                                setStartPreview(mcamera, mSurfaceViewHolder);
                            }else {

                                mPicturePaths.add(Environment.getExternalStorageDirectory()
                                        .getAbsolutePath());//最后一张图片加入集合
                                Intent intent = new Intent();
                                 setResult(RESULT_OK,intent);
                                fileOutputStream.close();
                                //保证最后一张图片加入集合并优化用户体验
                                //SystemClock.sleep(2000);
                                finish();
                            }
                            Toast.makeText(ContinuousTakePictureActivity.this, "图片保存成功"+pic_count+"张", Toast.LENGTH_SHORT).show();
                        }


                    } catch (IOException e) {
                        e.printStackTrace();
                    }
             filename=file.getAbsolutePath();  //照片的存储位置
            int dot=file.getName().lastIndexOf('.');
            if ((dot >-1) && (dot < (file.getName().length()))) {
                fileName1=file.getName().substring(0, dot)+".txt";   //与所拍照片同名的存储手机姿态的txt文件
            }
                    }
                }
            getlocationGps();
            writeLatLonOritentionIntoJpeg(filename,lat,lng,alt);
            ScannerByReceiver(ContinuousTakePictureActivity.this, filename);

            }
    };
//照片保存方向问题


    //照片预览方向问题
    public int getPreviewDegree(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =    new android.hardware.Camera.CameraInfo();

    android.hardware.Camera.getCameraInfo(cameraId, info);
    int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
    int degrees = 0;
    switch (rotation) {
        case Surface.ROTATION_0: degrees = 90; break;
        case Surface.ROTATION_90: degrees = 0; break;
        case Surface.ROTATION_180: degrees = 270; break;
        case Surface.ROTATION_270: degrees = 180; break;
    }


    return degrees;
}
    /** Receiver扫描更新图库图片 **/
    private static void ScannerByReceiver(Context context, String path) {
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + path)));
    }
    /**
     * 获取Camera对象
     *
     * @return
     */
    private Camera getCamera() {
        mcamera = Camera.open();
        mcamera.setDisplayOrientation(getPreviewDegree(ContinuousTakePictureActivity.this,0,mcamera));
        return mcamera;
    }
    /**
     * 设置并且开启相机预览
     */
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            //将Camera与SurfaceView开始绑定
            camera.setPreviewDisplay(holder);
            //开启预览
            camera.startPreview();
            startCapture();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放Camera资源
     */
    private void releaseCamera() {
        if (mcamera != null) {
            mcamera.setPreviewCallback(null);//取消回调
            stopPreview();
            mcamera.release();
            mcamera = null;
        }
    }

    /**
     * 停止取景
     */
    private void stopPreview() {


        mcamera.stopPreview();
    }

    /**
     * SurfaceHolder 的回调处理
     *
     * @param surfaceHolder
     */
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {



        setStartPreview(mcamera, mSurfaceViewHolder);
    }
    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        stopPreview();//先停止取景，再重新打开
        setStartPreview(mcamera, mSurfaceViewHolder);
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseCamera();
    }
    private  void writeLatLonOritentionIntoJpeg(String picPath, double dLat, double dLon, double dalt) {
        File file = new File(picPath);
        if (file.exists()) {
            try {
                ExifInterface exif = new ExifInterface(picPath);
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                        decimalToDMS(dLat));
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,
                        dLat > 0 ? "N" : "S"); // 区分南北半球

                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
                        decimalToDMS(dLon));
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,
                        dLon > 0 ? "E" : "W"); // 区分东经西经
                exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,
                        dalt+"");
                exif.saveAttributes();
            } catch (Exception e) {
            }
        }
        File appDIR1 = new File(Environment.getExternalStorageDirectory()
                .getAbsolutePath(), "pos");
        if (!appDIR1.exists()) {
            // 目录不存在 则创建
            appDIR1.mkdirs();
        }
        File file1 = new File(appDIR1, fileName1);
        try {
            RandomAccessFile raf = new RandomAccessFile(file1, "rw");
            // 将文件记录指针移动到最后
            raf.seek(file1.length());
            // 输出文件内容
            raf.write(message.getBytes());
            // 关闭RandomAccessFile
            raf.close();
            //第二个参数意义是说是否以append方式添加内容
            //BufferedWriter bw = new BufferedWriter(new FileWriter(file1, true));
            //bw.write( curDate+"  "+vX+"  "+vY+"  "+vZ+"\n");
            //bw.flush();
            //bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String decimalToDMS(double coord) {   //将经纬度转换成分秒格式
        String output, degrees, minutes, seconds;
        // gets the modulus the coordinate divided by one (MOD1).
        // in other words gets all the numbers after the decimal point.
        // e.g. mod := -79.982195 % 1 == 0.982195
        // next get the integer part of the coord. On other words the whole number
        // e.g. intPart := -79
        double mod = coord % 1;
        int intPart = (int) coord;
        // set degrees to the value of intPart
        // e.g. degrees := "-79"
        degrees = String.valueOf(intPart);
        // next times the MOD1 of degrees by 60 so we can find the integer part for
        // minutes.
        // get the MOD1 of the new coord to find the numbers after the decimal
        // point.
        // e.g. coord := 0.982195 * 60 == 58.9317
        // mod := 58.9317 % 1 == 0.9317
        // next get the value of the integer part of the coord.
        // e.g. intPart := 58
        coord = mod * 60;
        mod = coord % 1;
        intPart = (int) coord;
        if (intPart < 0) {
            // Convert number to positive if it's negative.
            intPart *= -1;
        }
        // set minutes to the value of intPart.
        // e.g. minutes = "58"
        minutes = String.valueOf(intPart);
        // do the same again for minutes
        // e.g. coord := 0.9317 * 60 == 55.902
        // e.g. intPart := 55
        coord = mod * 60;
        intPart = (int) coord;
        if (intPart < 0) {
            // Convert number to positive if it's negative.
            intPart *= -1;
        }
        // set seconds to the value of intPart.
        // e.g. seconds = "55"
        seconds = String.valueOf(intPart);
        // I used this format for android but you can change it
        // to return in whatever format you like
        // e.g. output = "-79/1,58/1,56/1"
        output = degrees + "/1," + minutes + "/1," + seconds + "/1";
        // Standard output of D°M′S″
        // output = degrees + "°" + minutes + "'" + seconds + "\"";
        return output;
    }
    private void getlocationGps(){
//此处的判定是主要问题，API23之后需要先判断之后才能调用locationManager中的方法


//包括这里的getLastKnewnLocation方法和requestLocationUpdates方法
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            //获取定位服务

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //获取当前可用的位置控制器
            List<String> list = locationManager.getProviders(true);

            if (list.contains(LocationManager.GPS_PROVIDER)) {
                //是否为GPS位置控制器
                provider = LocationManager.GPS_PROVIDER;
            }else if (list.contains(LocationManager.NETWORK_PROVIDER)) {
                //是否为网络位置控制器

                provider = LocationManager.NETWORK_PROVIDER;

            } else {
                Toast.makeText(ContinuousTakePictureActivity.this,"请检查网络或GPS是否打开",Toast.LENGTH_SHORT).show();

                return;
            }
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                //获取当前位置
                lat = location.getLatitude();
                lng = location.getLongitude();
                alt=location.getAltitude();
            }
//绑定定位事件，监听位置是否改变
//第一个参数为控制器类型第二个参数为监听位置变化的时间间隔（单位：毫秒）
//第三个参数为位置变化的间隔（单位：米）第四个参数为位置监听器
            locationManager.requestLocationUpdates(provider, 2000, 2, locationListener);
        }
        else
        {

            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            //从GPS获取最近的定位信息
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                //获取当前位置
                lat = location.getLatitude();
                lng = location.getLongitude();
                alt=location.getAltitude();
            }
            locationManager.requestLocationUpdates(provider, 2000, 2, locationListener);
        }
    }
    private final LocationListener locationListener =new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onProviderEnabled(String provider) {

        }
        @Override
        public void onProviderDisabled(String provider) {

        }
        public void onLocationChanged(Location location) {

        }
    };


    //获得手机姿态
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mSensorManager.registerListener((SensorEventListener)this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener((SensorEventListener) this, magnetic, SensorManager.SENSOR_DELAY_NORMAL); //为传感器注册监听器
        if (mcamera == null) {
            mcamera = getCamera();
            if (mSurfaceViewHolder != null) {
                setStartPreview(mcamera, mSurfaceViewHolder);//将Camera和Activity的生命周期绑定
            }
        }


    }
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
    public void onSensorChanged(SensorEvent event) {
        // TODO Auto-generated method stub
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accelerometerValues = event.values;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticFieldValues = event.values;
        }
        float[] values = new float[3];
        float[] R = new float[9];
        SensorManager.getRotationMatrix(R, null, accelerometerValues,
                magneticFieldValues);
        SensorManager.getOrientation(R, values);
        //提取数据
        double azimuth = Math.toDegrees(values[0]);
        if (azimuth<0) {
            azimuth=azimuth+360;
        }
        values[0] = (float)  azimuth;
        values[1] = (float) Math.toDegrees(values[1]);
        values[2] = (float) Math.toDegrees(values[2]);
            /*x=String.valueOf(values[0]);
            y=String.valueOf(values[1]);
            z=String.valueOf(values[2]);
              xyz=x+"a"+y+"a"+z;*/
        message = " X:" + values[0] + " Y:" + values[1] + " Z:" + values[2] + "\n";
    }
    @Override
    protected void onPause() {
// TODO Auto-generated method stub
        super.onPause();
        releaseCamera();

    }
    //关闭时解除监听器
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        //api23需要这样写
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            if (locationManager != null) {
                locationManager.removeUpdates(locationListener);
            }
        }
        mSensorManager.unregisterListener((SensorEventListener)this); // 解除监听器注册
    }

}
