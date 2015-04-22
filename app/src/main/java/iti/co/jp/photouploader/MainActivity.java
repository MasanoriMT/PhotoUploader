package iti.co.jp.photouploader;

import android.annotation.TargetApi;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

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
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {

    private static final int REQUEST_PICK_CONTENT = 1;
    private static final int REQUEST_KITKAT_PICK_CONTENT = 2;

    private RequestQueue mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // アップロード処理に用いるVolleyのQueue
        mQueue = Volley.newRequestQueue(this, new MultiPartStack(null, getAllAllowsSocketFactory()));

        // 画像選択方法その１
        // 画像選択アプリをIntent起動して結果を受け取る
        // この方法だと１画像ずつしか選択できない
        Button selectImages = (Button)findViewById(R.id.select_images);
        selectImages.setOnClickListener(this);

        // 画像選択方法その２
        // 外部アプリ（ギャラリーアプリ）で複数選択した画像をIntentで渡してもらう
        // この方法だとギャラリーアプリから当アプリを起動するので少々わかりづらい
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
    public void onClick(View v) {
        if (v.getId() == R.id.select_images) {

            // 画像選択アプリをIntent起動
            //
            // (Storage Access Framework に関する情報)
            //   https://developer.android.com/guide/topics/providers/document-provider.html
            //   http://www.gaprot.jp/pickup/android-4-4/vol3/
            //

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT){
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_PICK_CONTENT);
            } else {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, REQUEST_KITKAT_PICK_CONTENT);
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //
        // Intent起動した画像選択アプリから結果を受け取る
        //

        if (data == null) {
            return;
        }

        Uri uri = data.getData();

        String path = ContentProviderUtil.getPath(this, uri);
        if (path != null) {
            doUpload(new File(path));
        }


        /*
        if(requestCode == REQUEST_PICK_CONTENT) {
            String path = getPath(this, uri);
            if (path != null) {
                doUpload(new File(path));
            }
        } else if (requestCode == REQUEST_KITKAT_PICK_CONTENT) {

            String path = getPath(this, getResultChoosePictureUri(uri));
            if (path != null) {
                doUpload(new File(path));
            }

        }
        */
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

        //
        // ACTION_SEND_MULTIPLEで渡されたコンテンツを処理する
        //

        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        if (imageUris != null) {
            for(Uri uri : imageUris) {
                String path = ContentProviderUtil.getPath(this, uri);
                if (path != null) {
                    // アップロードを実行
                    doUpload(new File(path));
                }
            }


        }
    }

    private void doUpload(File file) {

        //
        // 指定されたファイルをサーバにアップロードする
        // TODO:接続先サーバは要調整
        //

        Map<String,String> stringMap = new HashMap<>();
        stringMap.put("text", "hogege"); // ここは必要に応じて追加すればよい

        Map<String,File> fileMap = new HashMap<>();
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


    //
    // KITKATからは永続的パーミッションを得る必要がある
    //
    //     http://blog.kotemaru.org/2014/11/23/android-choose-picture.html
    //
    // ただし、今回のサンプルアプリでは、毎回画像選択アプリからUriを受け取るので、永続的パーミッションを取得する必要はない
    // 一度受け取ったUriのコンテンツを、後になって利用したい場合は、永続的パーミッションを取得しておかないと、利用できなくなるようだ
    //
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private Uri getResultChoosePictureUri(Uri uri) {
        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        getContentResolver().takePersistableUriPermission(uri, takeFlags);

        return uri;
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
