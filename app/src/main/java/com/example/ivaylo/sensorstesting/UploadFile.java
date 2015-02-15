package com.example.ivaylo.sensorstesting;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ivaylo on 2/13/2015.
 */
public class UploadFile extends AsyncTask<String, Void, String> {


    @Override
    protected String doInBackground(String... params) {
        try {
            URL url= new URL(params[0]);
            return uploadGpsData(url);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        return "";
    }

    protected void onPostExecute(String message) {
        // TODO: check this.exception
        // TODO: do something with the feed
    }


    private String uploadGpsData(URL url) {
        //check if external storage is writable
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // the file to be posted
            String textFile = Constants.externalStorageDirectory + "/" + Constants.outFile;
            Log.v(Constants.TAG, "File: " + textFile);

            // the URL where the file will be posted
            String postReceiverUrl = url.toString();
            Log.v(Constants.TAG, "postURL: " + postReceiverUrl);

            // new HttpClient
            HttpClient httpClient = new DefaultHttpClient();

            // post header
            HttpPost httpPost = new HttpPost(postReceiverUrl);

            File file = new File(textFile);
            FileBody fileBody = new FileBody(file);

            MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
            reqEntity.addPart("file", fileBody);



            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
            nameValuePairs.add(new BasicNameValuePair("upload_file", "1"));
            nameValuePairs.add(new BasicNameValuePair("android_app", "1"));
            nameValuePairs.add(new BasicNameValuePair("filename", "gps_data_" + new Date().getTime() + ".csv"));

            for(int index=0; index < nameValuePairs.size(); index++) {
                // Normal string data
                try {
                    reqEntity.addPart(nameValuePairs.get(index).getName(), new StringBody(nameValuePairs.get(index).getValue()));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            httpPost.setEntity(reqEntity);


            // execute HTTP post request
            HttpResponse response = null;
            try {
                response = httpClient.execute(httpPost);
                Log.v(Constants.TAG, "Response: " +  response.toString());

                HttpEntity resEntity = response.getEntity();

                if (resEntity != null) {

                    String responseStr = null;
                    try {
                        responseStr = EntityUtils.toString(resEntity).trim();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.v(Constants.TAG, "Response string: " +  responseStr);

                    return responseStr;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(Constants.TAG, "Storage not writable....");
        }

        return "";
    }

}
