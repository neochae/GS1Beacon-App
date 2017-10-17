package com.AutoIDLabs.KAIST.GS1Beacon;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.NAPTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.Type;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Created by SNAIL on 2016-02-15.
 */
public class FilterActivity extends AppCompatActivity {

    private ListView mServiceList;
    private ServiceListAdapter mServiceAdapter;
    //private ArrayList<ServiceItem> mServiceItemList;
    private ApplicationClass applicationClass;

    private ArrayList<String> Advs;
    private ArrayList<String> Names;

    //qr code scanner object
    private IntentIntegrator qrScan;

    //private static final String RESOLVER_ADDRESS = "143.248.56.100";
    private static final String RESOLVER_ADDRESS = "143.248.53.213";
    //private static final String RESOLVER_ADDRESS = "8.8.8.8";
    private static final int RESOLVER_PORT = 53;
    private static final String[] LOCAL_SEARCH_PATH = { "onsepc.kr." };
    private String FQDN = "";
    //private static final String DN = "2.3.1.0.0.0.0.0.0.0.0.0.0.gtin.gs1.id.onsepc.kr";
    //private static final String DN = "google.com";
    //private OnsQueryThread mOnsQueryThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.filter_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.filter_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("  Favorite");
        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        final Intent intent = getIntent();
        Bundle args1 = intent.getBundleExtra("BUNDLE1");
        Advs = (ArrayList<String>)  args1.getSerializable("ARRAYLIST1");
        Bundle args2 = intent.getBundleExtra("BUNDLE2");
        Names = (ArrayList<String>)  args2.getSerializable("ARRAYLIST2");

        qrScan = new IntentIntegrator(this);

        applicationClass = (ApplicationClass) getApplicationContext();

        //Initialize Service Item
        //mServiceItemList = new ArrayList<>();

    }

    @Override
    protected void onResume() {
        super.onResume();

        displayService();

        // Initializes list view adapter.
        mServiceList = (ListView) findViewById(R.id.mFserviceList);
        mServiceAdapter = new ServiceListAdapter(this);
        mServiceList.setAdapter(mServiceAdapter);
        mServiceList.setOnItemClickListener(mItemClickListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.filtered_main, menu);

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
            case R.id.action_beacon:
                Intent intent1 = new Intent(FilterActivity.this, BeaconActivity.class);
                startActivity(intent1);
                break;
            case R.id.action_barcode:
                //scan option
                qrScan.setPrompt("Scanning...");
                qrScan.setOrientationLocked(false);
                qrScan.setCaptureActivity(BarcodeActivity.class);
                qrScan.initiateScan();
                break;
            case R.id.action_settings:
                Intent intent2 = new Intent(FilterActivity.this, SettingActivity.class);
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
                Toast.makeText(FilterActivity.this, "Cancelled!", Toast.LENGTH_SHORT).show();
            } else {
                //qrcode not exist
                if (Pattern.matches("[0-9]+", content) || content.substring(1,3).equals("01")) {
                    Toast.makeText(FilterActivity.this, "Completed!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(FilterActivity.this, ServiceActivity.class);
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

    protected AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final String url = (mServiceAdapter.getURL(position).split("!"))[2];
            if (url == null) return;
            Intent intent = new Intent(view.getContext(), WebActivity.class);
            intent.putExtra(WebActivity.EXTRAS_SERVICE_URL, url);

            Thread.interrupted();

            startActivity(intent);
        }
    };

    private void displayService() {

        for (int i = 0; i < Advs.size();  i++) {
            String[] arr = Advs.get(i).split("[\\(\\)]");

            for (int j = 0; j < arr.length; j++) {
                switch (arr[j]) {
                    case "01":     //GTIN
                        final String FQDN1 = convertGS1EStoFQDN("01", arr[j+1]);
                        final String bName1 = Names.get(i);
                        Thread t1 = new Thread(new Runnable() {
                            private String tFQDN = FQDN1;
                            private String tName = bName1;
                            @Override
                            public void run() {
                                onsQuery(tName, tFQDN);
                            }
                        });
                        t1.start();
                        break;
                    //case "21":    //GTIN serial
                    //    break;
                    case "414":   //GLN
                        final String FQDN2 = convertGS1EStoFQDN("414", arr[j+1]);
                        final String bName2 = Names.get(i);
                        Thread t2 = new Thread(new Runnable() {
                            private String tFQDN = FQDN2;
                            private String tName = bName2;
                            @Override
                            public void run() {
                                onsQuery(tName, tFQDN);
                            }
                        });
                        t2.start();
                        break;
                    //case "254":   //GLN extension
                    //    break;
                    case "255":   //Coupon
                        final String FQDN3 = convertGS1EStoFQDN("255", arr[j+1]);
                        final String bName3 = Names.get(i);
                        Thread t3 = new Thread(new Runnable() {
                            private String tFQDN = FQDN3;
                            private String tName = bName3;
                            @Override
                            public void run() {
                                onsQuery(tName, tFQDN);
                            }
                        });
                        t3.start();
                        break;
                    case "8017":  //GSRN
                        final String FQDN4 = convertGS1EStoFQDN("8017", arr[j+1]);
                        final String bName4 = Names.get(i);
                        Thread t4 = new Thread(new Runnable() {
                            private String tFQDN = FQDN4;
                            private String tName = bName4;
                            @Override
                            public void run() {
                                onsQuery(tName, tFQDN);
                            }
                        });
                        t4.start();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    public String convertGS1EStoFQDN(String ai, String gs1Code) {
        // TODO Auto-generated method stub

        String retFQDN = "";
        String remain;
        char[] remainCA;

        switch (ai) {
            case "01":     //GTIN
                if (gs1Code.length() == 9) {
                    retFQDN = "0.0.0.0.0.0.";
                    remain = gs1Code.substring(0, gs1Code.length()-2);
                    remainCA = remain.toCharArray();
                    for( int i = remainCA.length-1 ; i >= 0 ; i--)
                    {
                        retFQDN += remainCA[i]+".";
                    }
                    retFQDN += "gtin.gs1.id.onsepc.kr";
                } else if(gs1Code.length() == 13) {
                    retFQDN = "0.0.";
                    remain = gs1Code.substring(0, gs1Code.length()-2);
                    remainCA = remain.toCharArray();
                    for( int i = remainCA.length-1 ; i >= 0 ; i--)
                    {
                        retFQDN += remainCA[i]+".";
                    }
                    retFQDN += "gtin.gs1.id.onsepc.kr";
                } else if(gs1Code.length() == 14) {
                    retFQDN = "0.";
                    remain = gs1Code.substring(0, gs1Code.length()-2);
                    remainCA = remain.toCharArray();
                    for( int i = remainCA.length-1 ; i >= 0 ; i--)
                    {
                        retFQDN += remainCA[i]+".";
                    }
                    retFQDN += "gtin.gs1.id.onsepc.kr";
                } else if(gs1Code.length() == 15) {
                    retFQDN = gs1Code.substring(0, 1) + ".";
                    remain = gs1Code.substring(1, gs1Code.length()-2);
                    remainCA = remain.toCharArray();
                    for( int i = remainCA.length-1 ; i >= 0 ; i--)
                    {
                        retFQDN += remainCA[i]+".";
                    }
                    retFQDN += "gtin.gs1.id.onsepc.kr";
                }
                break;
            //case "21":    //GTIN serial
            //    break;
            case "414":   //GLN
                remain = gs1Code.substring(0, gs1Code.length()-2);
                remainCA = remain.toCharArray();
                for( int i = remainCA.length-1 ; i >= 0 ; i--)
                {
                    retFQDN += remainCA[i]+".";
                }
                retFQDN += "gln.gs1.id.onsepc.kr";
                break;
            //case "254":   //GLN extension
            //    break;
            case "255":   //Coupon
                remain = gs1Code.substring(0, gs1Code.length()-2);
                remainCA = remain.toCharArray();
                for( int i = remainCA.length-1 ; i >= 0 ; i--)
                {
                    retFQDN += remainCA[i]+".";
                }
                retFQDN += "gcn.gs1.id.onsepc.kr";
                break;
            case "8017":  //GSRN
                remain = gs1Code.substring(0, gs1Code.length()-2);
                remainCA = remain.toCharArray();
                for( int i = remainCA.length-1 ; i >= 0 ; i--)
                {
                    retFQDN += remainCA[i]+".";
                }
                retFQDN += "gsrn.gs1.id.onsepc.kr";
                break;
            default:
                break;
        }
        return retFQDN;
    }

    private void onsQuery(String bName, String FQDN) {

        ArrayList<String> ret = new ArrayList<>();

        try {
            Resolver resolver = new SimpleResolver(RESOLVER_ADDRESS);
            resolver.setPort(RESOLVER_PORT);

            Lookup.setDefaultResolver(resolver);
            Lookup.setDefaultSearchPath(LOCAL_SEARCH_PATH);
            Lookup.setDefaultCache(new Cache(), DClass.IN);

            Lookup lookup = new Lookup(FQDN, Type.NAPTR);
            Record[] records = lookup.run();

            if (lookup.getResult() == Lookup.SUCCESSFUL) {
                int count = 0;
                Bundle bun = new Bundle();
                Message hMsg = handler.obtainMessage();

                for (Record record : records) {
                    NAPTRRecord naptrRecord = (NAPTRRecord) record;
                    String msg = "";
                    msg += FQDN + "\t";
                    msg += naptrRecord.getRegexp() + "\t";
                    //TODO: get serviceType.xml form SNS.
                    msg += naptrRecord.getService() + "\t";
                    msg += count + "\t";        //temp icon.
                    msg += count + "\t";        //temp abstract.

                    msg += bName + "\t";        // beacon Name of Service.

                    bun.putString(count + "", msg);
                    hMsg.setData(bun);

                    count++;
                    //Log.d("onsQuery | NAPTR | ", naptrRecord.toString());
                }
                //Send message.
                handler.sendMessage(hMsg);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg == null)
                return;

            Bundle bun = msg.getData();
            if (bun == null)
                return;

            for (String service : bun.keySet()) {
                String[] temp = (bun.getString(service)).split("\t");
                //ServiceItem sI = new ServiceItem(temp[0],temp[1],temp[2],temp[3],temp[4]);

                //mServiceItemList.add(sI);
                if (applicationClass.getCheckByType(temp[2])) {
                    mServiceAdapter.addService(temp[5], temp[2], temp[1]);
                    mServiceAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    // Adapter for holding service found through scanning.
    private class ServiceListAdapter extends BaseAdapter {
        private ArrayList<Drawable> serviceIcons;
        private ArrayList<String> beaconName;
        private ArrayList<String> serviceType;
        private ArrayList<String> serviceURL;
        private Context mContext = null;

        public class mServiceViewHolder {
            ImageView serviceIcon;
            TextView beaconName;
            TextView serviceType;
            TextView serviceURL;
        }

        public ServiceListAdapter(Context mContext) {
            super();
            serviceIcons = new ArrayList<>();
            beaconName = new ArrayList<>();
            serviceType = new ArrayList<>();
            serviceURL = new ArrayList<>();
            this.mContext = mContext;
        }

        public void addService(String bName, String sType, String sURL) {
            //TODO:Add icon.
            beaconName.add(bName);
            serviceType.add(sType);
            serviceURL.add(sURL);
        }

        public  String getName(int position) {
            return beaconName.get(position);
        }

        public String getType(int position) {
            return serviceType.get(position);
        }

        public String getURL(int position) {
            return serviceURL.get(position);
        }

        public void clear() {
            serviceIcons.clear();
            beaconName.clear();
            serviceType.clear();
            serviceURL.clear();
        }

        @Override
        public int getCount() {
            return serviceType.size();
        }

        @Override
        public Object getItem(int i) {
            return serviceType.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            mServiceViewHolder viewHolder;

            // General ListView optimization code.
            if (view == null) {
                viewHolder = new mServiceViewHolder();

                LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = mInflater.inflate(R.layout.filter_item, null);

                viewHolder.serviceIcon = (ImageView) view.findViewById(R.id.fservice_icon);
                viewHolder.beaconName = (TextView) view.findViewById(R.id.fservice_beacon);
                viewHolder.serviceType = (TextView) view.findViewById(R.id.fservice_type);
                viewHolder.serviceURL = (TextView) view.findViewById(R.id.fservice_url);

                view.setTag(viewHolder);
            } else {
                viewHolder = (mServiceViewHolder) view.getTag();
            }

            //TODO: Get icon and set.
            switch (serviceType.get(i).substring(20)) {
                case "global:item":     //GTIN
                    viewHolder.serviceIcon.setImageResource(R.drawable.item);
                    break;
                case "global:price":   //GTIN
                    viewHolder.serviceIcon.setImageResource(R.drawable.price);
                    break;
                case "global:coupon":   //GCN
                    viewHolder.serviceIcon.setImageResource(R.drawable.coupon);
                    break;
                case "global:information":  //GLN
                    viewHolder.serviceIcon.setImageResource(R.drawable.info);
                    break;
                case "global:location":  //GLN
                    viewHolder.serviceIcon.setImageResource(R.drawable.location);
                    break;
                case "global:payment":  //GSRN
                    viewHolder.serviceIcon.setImageResource(R.drawable.payment);
                    break;
                case "global:servicerelation":  //GSRN
                    viewHolder.serviceIcon.setImageResource(R.drawable.service);
                    break;
                case "global:service":  //GSRN
                    viewHolder.serviceIcon.setImageResource(R.drawable.service);
                    break;
                case "global:member":  //GSRN
                    viewHolder.serviceIcon.setImageResource(R.drawable.iden);
                    break;
                case "global:koreannet":  //koreannet
                    viewHolder.serviceIcon.setImageResource(R.drawable.koreanet);
                    break;
                case "global:google":  //google
                    viewHolder.serviceIcon.setImageResource(R.drawable.google);
                    break;
                case "private:lotte:item":  //lotte
                    viewHolder.serviceIcon.setImageResource(R.drawable.lotte);
                    break;
                case "global:pedigree":  //pedigree
                    viewHolder.serviceIcon.setImageResource(R.drawable.pedigree);
                    break;
                case "global:car":  //smartcar
                    viewHolder.serviceIcon.setImageResource(R.drawable.car);
                    break;
                case "global:bus":  //bus
                    viewHolder.serviceIcon.setImageResource(R.drawable.bus);
                    break;
                default:
                    break;
            }

            String name = beaconName.get(i);
            String type = serviceType.get(i).substring(20);
            String url = (serviceURL.get(i).split("!"))[2];

            viewHolder.beaconName.setText(name);
            viewHolder.serviceType.setText(type);
            viewHolder.serviceURL.setText(url);

            return view;
        }
    }

}