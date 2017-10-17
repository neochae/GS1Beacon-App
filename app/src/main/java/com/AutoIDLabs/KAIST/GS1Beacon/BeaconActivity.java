package com.AutoIDLabs.KAIST.GS1Beacon;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class BeaconActivity extends AppCompatActivity  {

    private boolean mScanning = true;
    private BluetoothLeScanner mLeScanner;
    BluetoothAdapter mBluetoothAdapter;
    private ListView mLeDeviceListView;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Handler mHandler;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    //qr code scanner object
    private IntentIntegrator qrScan;

    //floating button
    private FloatingActionButton fab;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 300000;

    public static final int PERMISSION_ALL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.beacon_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.beacon_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("  GS1Beacon");
        getSupportActionBar().setLogo(R.drawable.ic_launcher);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();

        // carry on the normal flow, as the case of  permissions  granted.
        mHandler = new Handler();
        qrScan = new IntentIntegrator(this);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //scan option
                qrScan.setPrompt("Scanning...");
                qrScan.setOrientationLocked(false);
                qrScan.setCaptureActivity(BarcodeActivity.class);
                qrScan.initiateScan();
            }
        });
    }

    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            Toast.makeText(BeaconActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
        }
        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {
            Toast.makeText(BeaconActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        //Get scanner from adaptor
        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        filters = new ArrayList<ScanFilter>();

        // Initializes list view adapter.
        mLeDeviceListView = (ListView) findViewById(R.id.mBeaconList);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mLeDeviceListView.setAdapter(mLeDeviceListAdapter);
        mLeDeviceListView.setOnItemClickListener(mItemClickListener);

        scanLeDevice(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.beacon_main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        ArrayList<String> Advs = mLeDeviceListAdapter.getAllAdvData();
        Bundle args1 = new Bundle();
        args1.putSerializable("ARRAYLIST1", (Serializable) Advs);
        ArrayList<String> Beacon = mLeDeviceListAdapter.getAllName();
        Bundle args2 = new Bundle();
        args2.putSerializable("ARRAYLIST2",(Serializable) Beacon);

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                mLeDeviceListView.setAdapter(mLeDeviceListAdapter);
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case R.id.action_barcode:
                //scan option
                qrScan.setPrompt("Scanning...");
                qrScan.setOrientationLocked(false);
                qrScan.setCaptureActivity(BarcodeActivity.class);
                qrScan.initiateScan();
                break;
            case R.id.action_filter:
                Intent intent = new Intent(BeaconActivity.this, FilterActivity.class);
                intent.putExtra("BUNDLE1", args1);
                intent.putExtra("BUNDLE2", args2);
                if (mScanning) {
                    mScanning = false;
                    mLeScanner.stopScan(mLeScanCallback);
                }
                startActivity(intent);
                break;
            case R.id.action_settings:
                Intent intent2 = new Intent(BeaconActivity.this, SettingActivity.class);
                intent2.putExtra("BUNDLE1", args1);
                intent2.putExtra("BUNDLE2", args2);
                if (mScanning) {
                    mScanning = false;
                    mLeScanner.stopScan(mLeScanCallback);
                }
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
                Toast.makeText(BeaconActivity.this, "Cancelled!", Toast.LENGTH_SHORT).show();
            } else {
                //qrcode not exist
                if (Pattern.matches("[0-9]+", content) || content.substring(1,3).equals("01") || content.substring(1,4).equals("414"))  {
                    Toast.makeText(BeaconActivity.this, "Completed!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(BeaconActivity.this, ServiceActivity.class);
                    intent.putExtra(ServiceActivity.EXTRAS_DEVICE_NAME, result.getFormatName());
                    if (content.substring(1,3).equals("01")) {
                        intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(01)" + content.substring(4,17) + "\n");
                    } else if (content.substring(1,4).equals("414")) {
                        intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(414)" + content.substring(5,18) + "\n");
                    } else {
                        if (result.getFormatName().equals("EAN_13")) {
                            intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(01)" + content.substring(0, 13) + "\n");
                        } else if (result.getFormatName().equals("EAN_13")){
                            intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(01)" + content.substring(0, 8) + "\n");
                        } else {
                            intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, "(01)" + content + "\n");
                        }
                    }

                    ArrayList<String> Advs = mLeDeviceListAdapter.getAllAdvData();
                    Bundle args1 = new Bundle();
                    args1.putSerializable("ARRAYLIST1", (Serializable) Advs);
                    ArrayList<String> Beacon = mLeDeviceListAdapter.getAllName();
                    Bundle args2 = new Bundle();
                    args2.putSerializable("ARRAYLIST2", (Serializable) Beacon);

                    intent.putExtra("BUNDLE1", args1);
                    intent.putExtra("BUNDLE2", args2);

                    if (mScanning) {
                        mScanning = false;
                        mLeScanner.stopScan(mLeScanCallback);
                    }
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

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    protected AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
            final String advdata = mLeDeviceListAdapter.getAdvData(position);
            if (device == null) return;
            Intent intent = new Intent(BeaconActivity.this, ServiceActivity.class);
            intent.putExtra(ServiceActivity.EXTRAS_DEVICE_NAME, device.getName());
            intent.putExtra(ServiceActivity.EXTRAS_DEVICE_ADVDATA, advdata);

            ArrayList<String> Advs = mLeDeviceListAdapter.getAllAdvData();
            Bundle args1 = new Bundle();
            args1.putSerializable("ARRAYLIST1", (Serializable) Advs);
            ArrayList<String> Beacon = mLeDeviceListAdapter.getAllName();
            Bundle args2 = new Bundle();
            args2.putSerializable("ARRAYLIST2",(Serializable) Beacon);

            intent.putExtra("BUNDLE1", args1);
            intent.putExtra("BUNDLE2", args2);

            if (mScanning) {
                mScanning = false;
                mLeScanner.stopScan(mLeScanCallback);
            }
            startActivity(intent);
        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mLeScanner.stopScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mLeScanner.startScan(filters, settings, mLeScanCallback);
        } else {
            mScanning = false;
            mLeScanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback.
    private ScanCallback mLeScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            //Log.d("onScanResult | ", result.getDevice().getName() + " | " + result.getDevice().getAddress());

            byte[] manufacturerData = result.getScanRecord().getManufacturerSpecificData(52651);

            if (manufacturerData != null) {
                String temp = byteArrayToHex(manufacturerData);
                int len = temp.length();
                int count = 0;
                String gS1ES = "";

                while (count < len) {
                    String aI = "";
                    String data = "";
                    aI = new BigInteger(temp.substring(count,count+4), 16).toString(10);
                    count += 4;

                    switch (aI) {
                        case "1":     //GTIN
                            aI = "0" + aI;
                            data = new BigInteger(temp.substring(count, count+12), 16).toString(10);
                            count += 12;
                            break;
                        //case "21":    //GTIN serial
                            //data = new BigInteger(temp.substring(count, count+10), 16).toString(10);
                            //count += 10;
                            //break;
                        case "414":   //GLN
                            data = new BigInteger(temp.substring(count, count+12), 16).toString(10);
                            count += 12;
                            break;
                        //case "254":   //GLN extension
                            //break;
                        case "255":   //Coupon
                            data = new BigInteger(temp.substring(count, count+12), 16).toString(10);
                            count += 12;
                            break;
                        case "8017":   //GSRN
                            data = new BigInteger(temp.substring(count, count+16), 16).toString(10);
                            count += 16;
                            break;
                        default:
                            break;
                    }

                    gS1ES += "(" + aI + ")" + data + "\n";
                }

                //Log.d("onScanResult | gS1ES | ", gS1ES);

                //RSSI to Distance
                int distance = (int) calculateAccuracy(-63, result.getRssi());

                //String epcCode = "2.3.1.0.0.0.0.0.0.0.0.0.0.gtin.gs1.id.onsepc.kr";

                mLeDeviceListAdapter.addDevice(result.getDevice(), gS1ES, distance);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            //Log.d("onScanFailed ", "| " + errorCode);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                //Log.d("onBatchScanResults | ", result.getDevice().getName() + " | " + result.getDevice().getAddress());
            }
        }
    };

    public static String byteArrayToHex(byte[] ba) {
        if (ba == null || ba.length == 0) {
            return null;
        }

        StringBuffer sb = new StringBuffer(ba.length * 2);
        String hexNumber;
        for (int x = 0; x < ba.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);

            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }

    public double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi*1.0/txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio,10);
        }
        else {
            double accuracy =  (0.89976)*Math.pow(ratio,7.7095) + 0.111;
            return accuracy;
        }
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<Drawable> mIcons;
        private ArrayList<BluetoothDevice> mLeDevices;
        private ArrayList<String> mAdvData;
        private ArrayList<Double> mRSSI;
        private Context mContext = null;

        public class mDeviceViewHolder {
            ImageView beaconIcon;
            TextView beaconName;
            TextView beaconGS1;
            TextView beaconRSSI;
        }

        public LeDeviceListAdapter(Context mContext) {
            super();
            mIcons = new ArrayList<>();
            mLeDevices = new ArrayList<>();
            mAdvData = new ArrayList<>();
            mRSSI = new ArrayList<>();
            this.mContext = mContext;
        }

        public void addDevice(BluetoothDevice device, String data, double rssi) {
            if (!mLeDevices.contains(device)) {
                //TODO:Add icon.
                mLeDevices.add(device);
                mAdvData.add(data);
                mRSSI.add(rssi);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public String getAdvData(int position) {
            return mAdvData.get(position);
        }

        public double getRSSI(int position) {
            return mRSSI.get(position);
        }

        public ArrayList<String> getAllAdvData() {
            return mAdvData;
        }

        public ArrayList<String> getAllName() {
            ArrayList<String> ret = new ArrayList<>();
            for (int i = 0; i < mLeDevices.size(); i++) {
                ret.add(mLeDevices.get(i).getName());
            }
            return ret;
        }

        public void clear() {
            mIcons.clear();
            mLeDevices.clear();
            mAdvData.clear();
            mRSSI.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            mDeviceViewHolder viewHolder;

            // General ListView optimization code.
            if (view == null) {
                viewHolder = new mDeviceViewHolder();

                LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = mInflater.inflate(R.layout.beacon_item, null);

                viewHolder.beaconIcon = (ImageView) view.findViewById(R.id.beacon_icon);
                viewHolder.beaconName = (TextView) view.findViewById(R.id.beacon_name);
                viewHolder.beaconGS1 = (TextView) view.findViewById(R.id.beacon_GS1);
                viewHolder.beaconRSSI = (TextView) view.findViewById(R.id.beacon_rssi);

                view.setTag(viewHolder);
            } else {
                viewHolder = (mDeviceViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            String advdata = mAdvData.get(i);
            String rssi = mRSSI.get(i) + " m";

            String[] temp = advdata.split("[\\(\\)]");

            switch (temp[1]) {
                case "01":     //GTIN
                    viewHolder.beaconIcon.setImageResource(R.drawable.gtin);
                    break;
                case "414":   //GLN
                    viewHolder.beaconIcon.setImageResource(R.drawable.gln);
                    break;
                case "255":   //Coupon
                    viewHolder.beaconIcon.setImageResource(R.drawable.gcn);
                    break;
                case "8017":  //GSRN
                    viewHolder.beaconIcon.setImageResource(R.drawable.gsrn);
                    break;
                default:
                    break;
            }

            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.beaconName.setText(deviceName);
            else
                viewHolder.beaconName.setText(R.string.unknown_device);

            viewHolder.beaconGS1.setText(advdata);
            viewHolder.beaconRSSI.setText(rssi);

            return view;
        }
    }
}

