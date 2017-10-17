package com.AutoIDLabs.KAIST.GS1Beacon;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by SNAIL on 2016-02-15.
 */
public class SettingActivity extends AppCompatActivity {

    private ListView serviceCheckBoxList;
    private ApplicationClass applicationClass;
    private SettingListAdapter mSettingAdapter;

    private ArrayList<String> Advs;
    private ArrayList<String> Names;

    //qr code scanner object
    private IntentIntegrator qrScan;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setting_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.setting_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("  Setting");
        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        final Intent intent = getIntent();
        Bundle args1 = intent.getBundleExtra("BUNDLE1");
        Advs = (ArrayList<String>)  args1.getSerializable("ARRAYLIST1");
        Bundle args2 = intent.getBundleExtra("BUNDLE2");
        Names = (ArrayList<String>)  args2.getSerializable("ARRAYLIST2");

        qrScan = new IntentIntegrator(this);

        applicationClass = (ApplicationClass) getApplicationContext();

    }

    @Override
    protected void onResume() {
        super.onResume();

        serviceCheckBoxList = (ListView)findViewById(R.id.mSettingList);
        serviceCheckBoxList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mSettingAdapter = new SettingListAdapter(this);
        serviceCheckBoxList.setAdapter(mSettingAdapter);
        serviceCheckBoxList.setOnItemClickListener(mCheckClickListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.setting_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        Bundle args1 = new Bundle();
        args1.putSerializable("ARRAYLIST1", (Serializable) Advs);
        Bundle args2 = new Bundle();
        args2.putSerializable("ARRAYLIST2",(Serializable) Names);

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.menu_check:
                for(int i=0 ; i < mSettingAdapter.getCount(); i++)
                {
                    applicationClass.availableService.get(i).setFavorite(true);
                    mSettingAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.menu_clear:
                for(int i=0 ; i < serviceCheckBoxList.getAdapter().getCount(); i++)
                {
                    applicationClass.availableService.get(i).setFavorite(false);
                    mSettingAdapter.notifyDataSetChanged();
                }
                break;
            case R.id.action_beacon:
                Intent intent1 = new Intent(SettingActivity.this, BeaconActivity.class);
                startActivity(intent1);
                break;
            case R.id.action_barcode:
                //scan option
                qrScan.setPrompt("Scanning...");
                qrScan.setOrientationLocked(false);
                qrScan.setCaptureActivity(BarcodeActivity.class);
                qrScan.initiateScan();
                break;
            case R.id.action_filter:
                Intent intent2 = new Intent(SettingActivity.this, FilterActivity.class);
                intent2.putExtra("BUNDLE1", args1);
                intent2.putExtra("BUNDLE2", args2);
                startActivity(intent2);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    //Getting the scan results
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            //code exist
            String content = result.getContents();
            if (content == null) {
                Toast.makeText(SettingActivity.this, "Cancelled!", Toast.LENGTH_SHORT).show();
            } else {
                //qrcode not exist
                if (Pattern.matches("[0-9]+", content) || content.substring(1,3).equals("01")) {
                    Toast.makeText(SettingActivity.this, "Completed!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(SettingActivity.this, ServiceActivity.class);
                    intent.putExtra(ServiceActivity.EXTRAS_DEVICE_NAME, result.getFormatName());
                    if (content.substring(1,3).equals("01")) {
                        intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(01)" + content.substring(3,17) + "\n");
                    } else {
                        if (result.getFormatName().equals("EAN_13")) {
                            intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(01)" + content.substring(0, 13) + "\n");
                        } else {
                            intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(01)" + content.substring(0, 8) + "\n");
                        }
                    }

                    Bundle args1 = new Bundle();
                    args1.putSerializable("ARRAYLIST1", (Serializable) Advs);
                    Bundle args2 = new Bundle();
                    args2.putSerializable("ARRAYLIST2", (Serializable) Names);

                    intent.putExtra("BUNDLE1", args1);
                    intent.putExtra("BUNDLE2", args2);

                    startActivity(intent);
                } else if (content.contains("http") || content.contains("www")) {
                    final String url = content;
                    if (url == null) return;
                    Intent intent = new Intent(this, WebActivity.class);
                    intent.putExtra(WebActivity.EXTRAS_SERVICE_URL, url);

                    startActivity(intent);
                    //Toast.makeText(BeaconActivity.this, "Invalid Code!", Toast.LENGTH_SHORT).show();
                } else {
                    final String url = "https://www.google.co.kr/search?q=" + content;
                    if (url == null) return;
                    Intent intent = new Intent(this, WebActivity.class);
                    intent.putExtra(WebActivity.EXTRAS_SERVICE_URL, url);

                    startActivity(intent);
                    //Toast.makeText(BeaconActivity.this, "Invalid Code!", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    protected AdapterView.OnItemClickListener mCheckClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            applicationClass.availableService.get(position).setFavorite(!applicationClass.availableService.get(position).getFavorite());
            mSettingAdapter.notifyDataSetChanged();
        }
    };

    // Adapter for holding devices found through scanning.
    private class SettingListAdapter extends BaseAdapter {
        private Context mContext = null;

        public class mCheckViewHolder {
            ImageView sIcon;
            TextView serviceType;
            CheckBox checkBox;
        }

        public SettingListAdapter(Context mContext) {
            super();
            this.mContext = mContext;
        }

        @Override
        public int getCount() {
            return applicationClass.availableService.size();
        }

        @Override
        public Object getItem(int i) {
            return applicationClass.availableService.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            mCheckViewHolder viewHolder;

            // General ListView optimization code.
            if (view == null) {
                viewHolder = new mCheckViewHolder();

                LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = mInflater.inflate(R.layout.setting_item, null);

                viewHolder.sIcon = (ImageView) view.findViewById(R.id.sservice_icon);
                viewHolder.serviceType = (TextView) view.findViewById(R.id.setting_name);
                viewHolder.checkBox = (CheckBox) view.findViewById(R.id.setting_selected);

                view.setTag(viewHolder);
            } else {
                viewHolder = (mCheckViewHolder) view.getTag();
            }

            //TODO: Get icon and set.
            switch (applicationClass.availableService.get(i).getServiceType().substring(20)) {
                case "global:item":     //GTIN
                    viewHolder.sIcon.setImageResource(R.drawable.item);
                    break;
                case "global:price":   //GTIN
                    viewHolder.sIcon.setImageResource(R.drawable.price);
                    break;
                case "global:coupon":   //GCN
                    viewHolder.sIcon.setImageResource(R.drawable.coupon);
                    break;
                case "global:information":  //GLN
                    viewHolder.sIcon.setImageResource(R.drawable.info);
                    break;
                case "global:location":  //GLN
                    viewHolder.sIcon.setImageResource(R.drawable.location);
                    break;
                case "global:payment":  //GSRN
                    viewHolder.sIcon.setImageResource(R.drawable.payment);
                    break;
                case "global:servicerelation":  //GSRN
                    viewHolder.sIcon.setImageResource(R.drawable.service);
                    break;
                case "global:service":  //GSRN
                    viewHolder.sIcon.setImageResource(R.drawable.service);
                    break;
                case "global:authentication":  //GSRN
                    viewHolder.sIcon.setImageResource(R.drawable.auth);
                    break;
                case "global:member":  //GSRN
                    viewHolder.sIcon.setImageResource(R.drawable.iden);
                    break;
                case "global:koreannet":  //koreannet
                    viewHolder.sIcon.setImageResource(R.drawable.koreanet);
                    break;
                case "global:google":  //google
                    viewHolder.sIcon.setImageResource(R.drawable.google);
                    break;
                case "private:lotte:item":  //lotte
                    viewHolder.sIcon.setImageResource(R.drawable.lotte);
                    break;
                case "global:pedigree":  //pedigree
                    viewHolder.sIcon.setImageResource(R.drawable.pedigree);
                    break;
                case "global:car":  //smartcar
                    viewHolder.sIcon.setImageResource(R.drawable.car);
                    break;
                case "global:bus":  //bus
                    viewHolder.sIcon.setImageResource(R.drawable.bus);
                    break;
                default:
                    break;
            }

            viewHolder.serviceType.setText(applicationClass.availableService.get(i).getServiceType());
            viewHolder.checkBox.setChecked(applicationClass.availableService.get(i).getFavorite());

            return view;
        }
    }

}
