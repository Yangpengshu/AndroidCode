package com.example.administrator.dispose_photo;


import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;

import static android.media.ExifInterface.TAG_SUBJECT_DISTANCE;


public class AnalyzeActivity extends AppCompatActivity {
    private static final String IMAGE_UNSPECIFIED = "image/*";
    private Button mbtn_pic;
    private ImageView miv_pic;
    private TextView mtv_content;
    private TextView mtv_pic;
    private TextView mtv_con;
    private TextView mtv_flag1;
    private TextView mtv_flag2;
    private  TextView mtvaltflag;
    private TextView mtvalt;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_analyze);
        mbtn_pic=(Button)findViewById(R.id.btn_pic);
        miv_pic=(ImageView)findViewById(R.id.iv_pic);
        mtv_pic=(TextView)findViewById(R.id.tv_pic);
        mtv_content=(TextView)findViewById(R.id.tv_content);
        mtv_con=(TextView)findViewById(R.id.tv_con);
        mtv_flag1=(TextView)findViewById(R.id.tv_flag1);
        mtv_flag2=(TextView)findViewById(R.id.tv_flag2);
        mtvaltflag=(TextView)findViewById(R.id.tv_altflag);
        mtvalt=(TextView)findViewById(R.id.tv_alt);

        mbtn_pic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
               startActivityForResult(intent, 1);

            }
        });
    }


        public static String getImageAbsolutePath(AnalyzeActivity context, Uri imageUri)
        {
            if (context == null || imageUri == null)
                return null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT
                    && DocumentsContract.isDocumentUri(context, imageUri))
            {
                if (isExternalStorageDocument(imageUri))
                {
                    String docId = DocumentsContract.getDocumentId(imageUri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type))
                    {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(imageUri))
                {
                    String id = DocumentsContract.getDocumentId(imageUri);
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                            Long.valueOf(id));
                    return getDataColumn(context, contentUri, null, null);
                } else if (isMediaDocument(imageUri))
                {
                    String docId = DocumentsContract.getDocumentId(imageUri);
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type))
                    {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type))
                    {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type))
                    {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    String selection = MediaStore.Images.Media._ID + "=?";
                    String[] selectionArgs = new String[]
                            {
                                    split[1]
                            };
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            } // MediaStore (and general)
            else if ("content".equalsIgnoreCase(imageUri.getScheme()))
            {
                // Return the remote address
                if (isGooglePhotosUri(imageUri))
                    return imageUri.getLastPathSegment();
                return getDataColumn(context, imageUri, null, null);
            }
            // File
            else if ("file".equalsIgnoreCase(imageUri.getScheme()))
            {
                return imageUri.getPath();
            }
            return null;
        }

        public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs)
        {
            Cursor cursor = null;
            String column = MediaStore.Images.Media.DATA;
            String[] projection =
                    {
                            column
                    };
            try
            {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                if (cursor != null && cursor.moveToFirst())
                {
                    int index = cursor.getColumnIndexOrThrow(column);
                    return cursor.getString(index);
                }
            } finally
            {
                if (cursor != null)
                    cursor.close();
            }
            return null;
        }

        /**
         * @param uri
         *            The Uri to check.
         * @return Whether the Uri authority is ExternalStorageProvider.
         */
        public static boolean isExternalStorageDocument(Uri uri)
        {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri
         *            The Uri to check.
         * @return Whether the Uri authority is DownloadsProvider.
         */
        public static boolean isDownloadsDocument(Uri uri)
        {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri
         *            The Uri to check.
         * @return Whether the Uri authority is MediaProvider.
         */
        public static boolean isMediaDocument(Uri uri)
        {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

        /**
         * @param uri
         *            The Uri to check.
         * @return Whether the Uri authority is Google Photos.
         */
        public static boolean isGooglePhotosUri(Uri uri)
        {
            return "com.google.android.apps.photos.content".equals(uri.getAuthority());
        }



    /**
     * 动态获取图片的缩放值
     * @param options  BitmapFactory.Options
     * @param reqWidth 设定的Img控件宽度
     * @param reqHeight 设定的Img控件高度
     * @return inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
    {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth)
        {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth)
            {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
    /**
     * 获取手机屏幕密度，将dp值转换为px

     * @return px
     */
    private int dpTopx(float dpValue)
    {
        // 获取手机屏幕密度
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //用户操作完成，结果码返回是-1，即RESULT_OK
        double output1=0;
        double output2=0;
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK)
        {
            String path = null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            if (requestCode == 1)
            {
                // 如果是相机拍照，那么得到的data会为空，所以添加判断
                if (data != null)
                {
                    Uri address = data.getData();
                    // 将得到的Uri转换为真实的path路径
                    path = AnalyzeActivity.getImageAbsolutePath(this, address);
                } else
                {
                    path = Environment.getExternalStorageDirectory().getAbsolutePath()
                            + "/DJGXpic/applycourier_true.png";
                }
                // 为了内存考虑，需要将得到的图片根据ImageView的控件大小来进行缩放，然后再显示到ImageView上
                // inJustDecodeBounds方法为true时，得到bitmap时不分配内存
               opts.inJustDecodeBounds = true;
               BitmapFactory.decodeFile(path, opts);
                // 根据options来获得inSampleSize缩放值
               int inSampleSize = calculateInSampleSize(opts, dpTopx(300), dpTopx(300));
                opts.inSampleSize = inSampleSize;
                // 然后再根据options的缩放值将显示出来的图片进行缩放，并进行内存分配
                opts.inJustDecodeBounds = false;
                Bitmap bitmap = BitmapFactory.decodeFile(path, opts);
                miv_pic.setImageBitmap(bitmap);
                try {
                    mtv_pic.setText("经纬度：");
                    ExifInterface exif = new ExifInterface(path);
                    String latValue = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
                    String lngValue = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
                    String latRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
                    String lngRef = exif.getAttribute
                            (ExifInterface.TAG_GPS_LONGITUDE_REF);


                    if (latValue != null && latRef != null && lngValue != null && lngRef != null) {
                        try {
                            output1 = convertRationalLatLonToFloat(latValue, latRef);
                            output2 = convertRationalLatLonToFloat(lngValue, lngRef);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                    }
                    mtv_content.setText(String.valueOf(output1));
                    mtv_flag1.setText(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF));
                    mtv_con.setText(String.valueOf(output2));
                    mtv_flag2.setText(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
                    mtvaltflag.setText("高程：");
                    String x=exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE);
                    mtvalt.setText(x);



                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }
}
    private static double convertRationalLatLonToFloat(String rationalString, String ref) {
        String[] parts = rationalString.split(",");
        String[] pair;
        pair = parts[0].split("/");
        double degrees = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());   //parseDouble方法获取指定String值的double表示形式
        pair = parts[1].split("/");
        double minutes = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());
        pair = parts[2].split("/");
        double seconds = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());
        double result = degrees + (minutes / 60.0) + (seconds / 3600.0);
        if ((ref.equals("S") || ref.equals("W"))) {
            return  -result;
        }
        return  result;
    }
}