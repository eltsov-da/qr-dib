package com.appindesign.dib;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

import static android.webkit.URLUtil.isHttpsUrl;

public class CallAPI extends AsyncTask<String, String, String> {

    private static final String TAG = "";
    private Context mContext;
    public CallAPI(Context context){
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        Toast.makeText(mContext,
                result,
                Toast.LENGTH_LONG
        ).show();
    }
    @Override
    protected String doInBackground(String... params) {
        HttpURLConnection conn = null;
        OutputStream out = null;
        int rc=0;
        String  ansver="";
        try {
            Log.i("chat",
                    "+ FoneService --------------- ОТКРОЕМ СОЕДИНЕНИЕ");
            String rUrl;
            rUrl=params[0]+"?dibstr="+URLEncoder.encode(params[1], "UTF-8");
            if(isHttpsUrl(rUrl)) {
                conn = (HttpsURLConnection) new URL(rUrl)
                        .openConnection();
            }
            else
            {
                conn = (HttpURLConnection) new URL(rUrl)
                        .openConnection();

            }
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setDoInput(true);
            conn.connect();
            rc=conn.getResponseCode();
        } catch (Exception e) {
            Log.i("chat", "+ FoneService ошибка: " + e.getMessage());
        }
        // получаем ответ ---------------------------------->
        try {
            InputStream is = conn.getInputStream();
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, "UTF-8"));
            StringBuilder sb = new StringBuilder();
            String bfr_st = null;
            while ((bfr_st = br.readLine()) != null) {
                sb.append(bfr_st);
            }

            Log.i("chat", "+ FoneService - полный ответ сервера:\n"
                    + sb.toString());
            // сформируем ответ сервера в string
            // обрежем в полученном ответе все, что находится за "]"
            // это необходимо, т.к. json ответ приходит с мусором
            // и если этот мусор не убрать - будет невалидным

            ansver = sb.toString();
 //           ansver = ansver.substring(0, ansver.indexOf("]") + 1);

            is.close(); // закроем поток
            br.close(); // закроем буфер

        } catch (Exception e) {
            Log.i("chat", "+ FoneService ошибка: " + e.getMessage());
        } finally {
            conn.disconnect();
            Log.i("chat",
                    "+ FoneService --------------- ЗАКРОЕМ СОЕДИНЕНИЕ");
        }
        if(rc==200)
        {

            return (ansver);
        }
        else
        {

            return("Ошибка при отправке");
        }
    }
}
