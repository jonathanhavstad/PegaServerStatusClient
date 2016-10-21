package com.ciscozensarpegateam.pegaserverstatusclient.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.widget.LinearLayout;

import com.ciscozensarpegateam.pegaserverstatusclient.R;
import com.ciscozensarpegateam.pegaserverstatusclient.adapters.PegaServerNetworkAdapter;
import com.ciscozensarpegateam.pegaserverstatusclient.binders.PegaServerNetworkBinder;
import com.ciscozensarpegateam.pegaserverstatusclient.data.PegaServerNetworkData;

import butterknife.BindView;
import butterknife.ButterKnife;

public class PegaServerDataActivity extends AppCompatActivity {
    @BindView(R.id.activity_rest_data)
    LinearLayout restDataView;

    @BindView(R.id.rest_data_list)
    RecyclerView restDataList;

    private PegaServerNetworkAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pega_server_data);

        ButterKnife.bind(this);

        Intent intent = getIntent();
        PegaServerNetworkData pegaData =
                intent.getParcelableExtra(getString(R.string.app_binder_data_bundle_key));
        PegaServerNetworkBinder binder = pegaData.getBinder();
        Object appData = binder.getAppData();
        PegaServerNetworkAdapter.OnItemSelectedListener onItemSelectedListener =
                new PegaServerNetworkAdapter.OnItemSelectedListener() {
            @Override
            public void sendData(Object data) {
                PegaServerNetworkBinder childBinder = new PegaServerNetworkBinder();
                childBinder.setAppData(data);
                launchPegaServerDataActivity(childBinder);
            }
        };
        adapter = new PegaServerNetworkAdapter(onItemSelectedListener, appData);
        restDataList.setAdapter(adapter);
    }

    private void launchPegaServerDataActivity(PegaServerNetworkBinder restBinder) {
        Intent intent = new Intent(
                this,
                PegaServerDataActivity.class);
        PegaServerNetworkData restData = new PegaServerNetworkData();
        restData.setBinder(restBinder);
        intent.putExtra(getString(R.string.app_binder_data_bundle_key), restData);
        startActivity(intent);
    }
}
