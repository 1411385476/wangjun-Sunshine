package com.example.wgj.myapplication;

import android.app.Fragment;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class DetailActivity extends AppCompatActivity {

    private static final String LOG_TAG = "DetailActivity";
    private String data;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null){
            getFragmentManager().beginTransaction().
                    add(R.id.detail_cotainer,new DetailFragment()).commit();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.detail, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_settings){
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class DetailFragment extends Fragment{

        public DetailFragment(){}
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_detail,container,false);

            Intent intent = getActivity().getIntent();
            TextView  detailText = (TextView) rootView.findViewById(R.id.forecast_detail);
            if (intent != null){
                String detail = intent.getDataString();
                detailText.setText(detail);

            }
            Log.e(LOG_TAG, LOG_TAG+"  fdff");
            return rootView;
        }
    }
}
