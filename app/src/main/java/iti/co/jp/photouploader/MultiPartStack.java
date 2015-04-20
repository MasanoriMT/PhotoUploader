package iti.co.jp.photouploader;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.HurlStack;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.util.Map;

/**
 * Created by matoh on 2015/04/20.
 *
 * 以下の記事を参考に作成
 *
 * 　　http://miblog.guruguruheadslab.com/archives/555
 *
 */
public class MultiPartStack extends HurlStack {

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    public MultiPartStack(HurlStack.UrlRewriter urlRewriter, javax.net.ssl.SSLSocketFactory sslSocketFactory) {
        super(urlRewriter, sslSocketFactory);
    }

    @Override
    public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders)
            throws AuthFailureError, IOException {
        if ( ! (request instanceof MultipartRequest) ) {
            return super.performRequest(request, additionalHeaders);
        }

        HttpPost httpRequest = new HttpPost(request.getUrl());
        httpRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
        setMultipartBody(httpRequest, request);
        addHeaders(httpRequest, additionalHeaders);
        addHeaders(httpRequest, request.getHeaders());
        HttpParams httpParams = httpRequest.getParams();
        int timeoutMs = request.getTimeoutMs();
        HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
        HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);

        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", new PlainSocketFactory(), 80));
        registry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));

        ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(httpParams, registry);
        HttpClient httpClient = new DefaultHttpClient(manager, httpParams);

        return httpClient.execute(httpRequest);
    }

    private void addHeaders(HttpPost httpRequest, Map<String, String> headers) {
        for ( String key : headers.keySet() ) {
            httpRequest.setHeader(key, headers.get(key));
        }
    }

    private static void setMultipartBody(HttpEntityEnclosingRequestBase httpRequest, Request<?> request)
            throws AuthFailureError {
        if ( request instanceof MultipartRequest ) {
            httpRequest.setEntity(((MultipartRequest) request).getEntity());
        }
    }

}
