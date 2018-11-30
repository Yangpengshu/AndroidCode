package com.example.administrator.dispose_photo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.location.LocationListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private static final int NONE = 0;
    private static final int PHOTO_GRAPH = 1;// 拍照
    private Button btnPhone = null;
    private Button btnTakePicture = null;
    private Button continuetakepicture=null;
    private String provider;
    private LocationManager locationManager;
    private SensorManager mSensorManager = null;
    private Sensor accelerometer; // 加速度传感器
    private Sensor magnetic; // 地磁场传感器
    double lat=0;
    double lng=0;
    double alt=0;
    PowerManager.WakeLock wakeLock;
    private  SensorManager sensorManager=null;
    String  filename=" ";
    String fileName1="";
    String message="";
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "myapp:MyWakelockTag");

        acquireWakeLock();
        btnPhone = (Button) findViewById(R.id.btnAnalyze);
        btnPhone.setOnClickListener(onClickListener);//绑定监听器
        btnTakePicture = (Button) findViewById(R.id.btnTakePicture);
        btnTakePicture.setOnClickListener(onClickListener);
        continuetakepicture=(Button)findViewById(R.id.ContinueTakePicture) ;
        continuetakepicture.setOnClickListener(onClickListener);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);//实例化传感器管理者

        accelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);//初始化加速度传感器
        magnetic = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);//初始化地磁场传感器
        //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        //动态获取权限
        List<String> permissionList = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);

        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.WAKE_LOCK) != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WAKE_LOCK);

        }

        if (!permissionList.isEmpty()){  //申请的集合不为空时，表示有需要申请的权限
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[permissionList.size()]),1);
        }else { //所有的权限都已经授权过了

        }

    }
    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    private void acquireWakeLock()
    {
        if (null == wakeLock)
        {
            PowerManager pm = (PowerManager)this.getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK|PowerManager.ON_AFTER_RELEASE, "myapp:PostLocationService");
            if (null != wakeLock)
            {
                wakeLock.acquire();
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0){ //安全写法，如果小于0，肯定会出错了
                    for (int i = 0; i < grantResults.length; i++) {

                        int grantResult = grantResults[i];
                        if (grantResult == PackageManager.PERMISSION_DENIED){ //这个是权限拒绝
                            String s = permissions[i];
                            Toast.makeText(this,s+"权限被拒绝了",Toast.LENGTH_SHORT).show();
                        }else{ //授权成功了
                            //do Something
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
    private final View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v==btnPhone){ //分析相册照片
                Intent intent=new Intent(MainActivity.this,AnalyzeActivity.class);
                startActivity(intent);
            }
            else if (v==continuetakepicture) {
                Intent intent=new Intent(MainActivity.this,ContinuousTakePictureActivity.class);
                startActivity(intent);
            }
            else if(v==btnTakePicture){ //从拍照获取图片
                //开相机
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                String fileName=System.currentTimeMillis()+ ".jpg";
                File appDir = new File(Environment.getExternalStorageDirectory()
                        .getAbsolutePath(), "image");
                if (!appDir.exists()) {
                    // 目录不存在 则创建
                    appDir.mkdirs();
                }
                File file = new File(appDir, fileName);

                Uri imageUri = Uri.fromFile(file );//取出图片uri

                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);//指定拍照后的图片输出为imageUri
                filename=file.getAbsolutePath();  //照片的存储位置
                int dot=file.getName().lastIndexOf('.');
                if ((dot >-1) && (dot < (file.getName().length()))) {
                    fileName1=file.getName().substring(0, dot)+".txt";   //与所拍照片同名的存储手机姿态的txt文件
                }
                startActivityForResult(intent, PHOTO_GRAPH);
            }
        }
    };

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == NONE)
            return;
        // 拍照
        if (requestCode == PHOTO_GRAPH) {

            getlocationGps();
            writeLatLonOritentionIntoJpeg(filename,lat,lng,alt);
            ScannerByReceiver(MainActivity.this, filename);
        }
            if (data == null)
                return;
            Toast.makeText(this,"保存成功",Toast.LENGTH_SHORT).show();
            super.onActivityResult(requestCode, resultCode, data);

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
                Toast.makeText(MainActivity.this,"请检查网络或GPS是否打开",Toast.LENGTH_SHORT).show();

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
        mSensorManager.registerListener(this,
                accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, magnetic,
                SensorManager.SENSOR_DELAY_NORMAL); //为传感器注册监听器
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
        //mSensorManager.unregisterListener(this);
        super.onPause();

    }
    /** Receiver扫描更新图库图片 **/
    private static void ScannerByReceiver(Context context, String path) {
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + path)));
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
        mSensorManager.unregisterListener(this); // 解除监听器注册
        releaseWakeLock();
    }
    //释放设备电源锁
    private void releaseWakeLock()
    {
        if (null != wakeLock)
        {
            wakeLock.release();
            wakeLock = null;
        }
    }
}

