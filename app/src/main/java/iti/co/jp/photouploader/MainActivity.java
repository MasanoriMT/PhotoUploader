package iti.co.jp.photouploader;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends ActionBarActivity {

    private RequestQueue mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mQueue = Volley.newRequestQueue(this, new MultiPartStack(null, getAllAllowsSocketFactory()));

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                handleSendMultipleImages(intent); // 送られて来た複数の画像を処理します
            }
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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

    private void handleSendMultipleImages(Intent intent) {
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            List<ListItem> list = new ArrayList<>();
            for(Uri uri : imageUris) {
                ListItem item = new ListItem();
                item.setText(uri.getPath());
                list.add(item);

                // Uri → File
                //
                //   http://www.united-bears.co.jp/blog/archives/2336
                //
                String scheme = uri.getScheme();
                String path = null;
                if ("file".equals(scheme)) {
                    path = uri.getPath();
                } else if("content".equals(scheme)) {
                    ContentResolver contentResolver = this.getContentResolver();
                    Cursor cursor = contentResolver.query(uri, new String[] { MediaStore.MediaColumns.DATA }, null, null, null);
                    if (cursor != null) {
                        cursor.moveToFirst();
                        path = cursor.getString(0);
                        cursor.close();
                    }
                }

                doUpload(new File(path));

            }

            MyArrayAdapter adapter = new MyArrayAdapter(this, 0, list);

            ListView lv = (ListView) findViewById(R.id.list_view);
            lv.setAdapter(adapter);

        }
    }

    private void doUpload(File file) {
        Map<String,String> stringMap = new HashMap<String, String>();
        stringMap.put("text", "hogege"); // ここは必要に応じて追加すればよい

        Map<String,File> fileMap = new HashMap<String, File>();
        fileMap.put("file", file);

        MultipartRequest multipartRequest = new MultipartRequest(
                "http://172.16.241.100:80/server.php",
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Upload成功
                        Log.d("MainActivity", "Upload success: " + response);

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //Upload失敗
                        Log.d("MainActivity","Upload error: " + error.getMessage());
                    }
                },
                stringMap,
                fileMap);

        mQueue.add(multipartRequest);
    }


    /*
        オレオレ証明書サイト用
        ちゃんと証明書を取得しているサーバには不要

        http://qiita.com/kojionilk/items/a82cf3797d68f62cbd55
     */
    private static SSLSocketFactory getAllAllowsSocketFactory() {
        try {
            // ホスト名検証をスキップする
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            });

            // 証明書検証スキップする空の TrustManager
            final TrustManager[] manager = {new X509TrustManager() {

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // do nothing
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    // do nothing
                }
            }};
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, manager, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new AndroidRuntimeException(e);
        }
    }
}
