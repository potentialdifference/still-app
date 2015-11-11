package uk.org.potentialdifference.stillapp.imageservice;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import uk.org.potentialdifference.stillapp.R;
import uk.org.potentialdifference.stillapp.imageservice.webservice.StillAppService;

/**
 * Created by russell on 07/11/2015.
 */
public abstract class BaseImageServiceTask<T> extends AsyncTask<T, Void, Void> {


    protected static final String[] SAFE_NETWORK_SSIDS = {"roomie", "VivianMaier"};







    protected StillAppService getService(Context context){

        Retrofit retrofit = new Retrofit.Builder().baseUrl(String.format("%s://%s:%s", "https", context.getString(R.string.still_server_hostname), context.getString(R.string.still_server_https_port)))
                .addConverterFactory(GsonConverterFactory.create())
                .client(getSslClient(context))
                .build();
        return retrofit.create(StillAppService.class);
    }

    protected static OkHttpClient getSslClient(final Context context){

        OkHttpClient client = new OkHttpClient();

        try{
            //load the certificate containing our trusted CAs
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream certInputStream = context.getResources().openRawResource(R.raw.server_cert);
            Certificate ca;
            try {
                ca = cf.generateCertificate(certInputStream);
            }finally{
                certInputStream.close();
            }

            //create a trust manager that trusts the CAs in our keystore
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keyStore);

            SSLContext sslContext  = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            client.setSslSocketFactory(sslContext.getSocketFactory());
            //client.setHostnameVerifier(new AllowAllHostnameVerifier());
            client.setHostnameVerifier(new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    if(hostname.equals(context.getString(R.string.still_server_hostname))){
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            });


        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | CertificateException | IOException e) {
            e.printStackTrace();
        }
        return client;

    }

    public static boolean isConnectedToSafeWifi(Context context){
        boolean safe = false;
        String wifiId = getWifiSSID(context);
        for (String id : SAFE_NETWORK_SSIDS) {
            if (id.equals(wifiId)) {
                safe = true;
                continue;
            }
        }
        return safe;
    }
    protected static String getWifiSSID(Context context){
        String ssid = "";
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
        if (connectionInfo != null && !(connectionInfo.getSSID().equals(""))) {
            ssid = connectionInfo.getSSID();

        }
        if (ssid.startsWith("\"") && ssid.endsWith("\"")){
            ssid = ssid.substring(1, ssid.length()-1);
        }
        return ssid;
    }
}

