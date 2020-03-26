package com.android.systemui.statusbar.policy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.EthernetManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.sen5.NetLedNative;


/**
 * @author JesseYao 
 * @version 2017 2017年11月1日 上午9:39:55
 * ClassName：IndicatorLightController.java 
 * Description： Set indicator light
*/
public class IndicatorLightController {

	private IntentFilter mIntentFilter = null;
	private TelephonyManager mTelephonyManager = null;
    private PhoneStateListener mPhoneStateListener = null;
    private Context mContext = null;
    private WifiManager mWifiManager = null;
    private ConnectivityManager mConnectivityManager = null;
    private EthernetManager mEthernetManager = null;
    
    private BroadcastReceiver mNetworkStateChangeReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    || action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				if (SEN5_DEBUG_FLAG) {
					Log.d(TAG, "onReceiver::" + action);
				}
				//set WiFi indicator light
				NetLedNative.setWifiEnableNative(mWifiManager.isWifiEnabled());
				//set Ethernet indicator light
				if (isEthernetAvailable()) {
					NetLedNative.setEthernetEnableNative(isEthernetConnected());
				}
			} else if (WifiManager.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra("wifi_state",  0);
                if (SEN5_DEBUG_FLAG) Log.d(TAG, "onReceive: " + action + " :: state= " + state);
                switch (state) {
                    case 11:
                    	if (SEN5_DEBUG_FLAG) {
                    		Log.d(TAG, "Wi-Fi HotSpot is closed!");
                    	}
                    	break;
                    case 13:
                    	if (SEN5_DEBUG_FLAG) { 
                    		Log.d(TAG, "Wi-Fi HotSpot is opened!");
                    	}
                        break;
                    case 12:
                        if (SEN5_DEBUG_FLAG) {
                        	Log.d(TAG, "Wi-Fi HotSpot is opening...");
                        }
                        break;
                    case 10:
                        if (SEN5_DEBUG_FLAG) {
                        	Log.d(TAG, "Wi-Fi HotSpot is closing...");
                        }
                        break;
                }
            } else {
            	
            }
		}
	};
	
	public IndicatorLightController(Context context) {
		if (SEN5_DEBUG_FLAG) {
			Log.d(TAG, "IndicatorLightController");			
		}
		mContext = context;
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mIntentFilter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
		
		mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		mConnectivityManager = (ConnectivityManager) mContext.getSystemService(
                Context.CONNECTIVITY_SERVICE);
		mEthernetManager = (EthernetManager) mContext.getSystemService(Context.ETHERNET_SERVICE);
	}
	
	public void register() {
		if (SEN5_DEBUG_FLAG) {
			Log.d(TAG, "IndicatorLightController register");
		}
		registerNetReceiver();
		registerPhoneStateListener(mContext);
	}
	
	public void unregister() {
		if (SEN5_DEBUG_FLAG) {
			Log.d(TAG, "IndicatorLightController unregister");
		}
		unregisterNetReceiver();
		unregisterPhoneStateListener();
	}
	
	private void registerNetReceiver () {
		mContext.registerReceiver(mNetworkStateChangeReceiver, mIntentFilter);
	}
	
	private void unregisterNetReceiver () {
		mContext.unregisterReceiver(mNetworkStateChangeReceiver);
	}

	/**
	 * Network class(type): 
	 * 		1 -> 2G
	 * 		2 -> 3G
	 * 		3 -> 4G
	 * 
	 * Strength: 
	 * 		0 -> None or Unknown 
	 * 		1 -> Poor
	 * 		2 -> Moderate
	 * 		3 -> Good
	 * 		4 -> Great
	 * 		5 -> No signal
	 * 		
	 * @param context
	 */
	private void registerPhoneStateListener (Context context) {
    	try {
            if (null == mTelephonyManager) {
                mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            }

            mPhoneStateListener = new PhoneStateListener() {

                @Override
                public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                    super.onSignalStrengthsChanged(signalStrength);
                    if (SEN5_DEBUG_FLAG) {
                    	Log.d(TAG, "onSignalStrengthsChanged: network class = " + TelephonyManager.getNetworkClass(mTelephonyManager.getNetworkType()) + 
                    			" signal strength = " + getSignalStrengthLevel(signalStrength));
                    }
                    switch (getSignalStrengthLevel(signalStrength)) {
					case 0:
						NetLedNative.setModemSignalLevelNative(0);
						break;
					case 1:
						NetLedNative.setModemSignalLevelNative(1);
						break;
					case 2:
					case 3:
						NetLedNative.setModemSignalLevelNative(2);
						break;
					case 4:
						NetLedNative.setModemSignalLevelNative(3);
						break;
					case 5:
					default:
						NetLedNative.setModemSignalLevelNative(0);
						break;
					}
                }
            };

            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            if (SEN5_DEBUG_FLAG) {
            	Log.d(TAG, "register phonestate listener");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "setMobileNetworkListener error!");
        }
            
    }

	private void unregisterPhoneStateListener () {
        if (null != mTelephonyManager && null != mPhoneStateListener) {
        	if (SEN5_DEBUG_FLAG) {
        		Log.d(TAG, "unregister phonestate listener");
        	}
            mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
    }

    /**
     * dBm和asu它们之间的关系是：dBm =-113+2*asu，这是google给android手机定义的特有信号单位。
     * 简单的说dBm值肯定是负数的，越接近0信号就越好，但是不可能为0的 ASU的值则相反，是正数，也是值越大越好
     * 按规定，只要城市里大于-90，农村里大于-94就是正常的，记住负数是-号后面的值越小就越大
     * 具体情况就是：-81dBm的信号比-90dBm的强，-67dBm的信号比-71dBm的强 低于-113那就是没信号了
     * @param signalStrength
     * @return
     */
	private int getSignalStrengthLevel(SignalStrength signalStrength) {
        int phoneType = mTelephonyManager.getPhoneType();
        int dbm = signalStrength.getDbm();
        int asuLevel = signalStrength.getAsuLevel();
        int level = signalStrength.getLevel();
        switch (phoneType) {
            case TelephonyManager.PHONE_TYPE_NONE:
            	if (SEN5_DEBUG_FLAG) {
            		Log.d(TAG, "getSignalStrengthLevel:PHONE_TYPE_NONE level = " + level);
                }
                break;
            case TelephonyManager.PHONE_TYPE_GSM:
                dbm = signalStrength.getGsmDbm();
                asuLevel = signalStrength.getGsmAsuLevel();
                level = signalStrength.getGsmLevel();
                if (SEN5_DEBUG_FLAG) {
                	Log.d(TAG, "getSignalStrengthLevel:PHONE_TYPE_GSM level = " + level +
                			"\n dbm = " + dbm + "\n asuLevel = " + asuLevel);
                }
                break;
            case TelephonyManager.PHONE_TYPE_CDMA:
                dbm = signalStrength.getCdmaDbm();
                asuLevel = signalStrength.getCdmaAsuLevel();
                level = signalStrength.getCdmaLevel();
                if (SEN5_DEBUG_FLAG) {
                	Log.d(TAG, "getSignalStrengthLevel:PHONE_TYPE_CDMA level = " + level +
                        "\n dbm = " + dbm + "\n asuLevel = " + asuLevel);
                }
                break;
            case TelephonyManager.PHONE_TYPE_SIP:
            	if (SEN5_DEBUG_FLAG) {
            		Log.d(TAG, "getSignalStrengthLevel:PHONE_TYPE_SIP level = " + level +
                        "\n dbm = " + dbm + "\n asuLevel = " + asuLevel);
            	}
                break;
            default:
            	if (SEN5_DEBUG_FLAG) {
            		Log.d(TAG, "getSignalStrengthLevel:default level = " + level +
                        "\n dbm = " + dbm + "\n asuLevel = " + asuLevel);
            	}
                break;
        }
        return level;
    }
	
	private boolean isEthernetConnected() {
		if (null != getEthernetIpAddress()) {
			return true;
		} 
		return false;
	}
	
	/**
     * Return whether Ethernet port is available.
     */
    private boolean isEthernetAvailable() {
        return mConnectivityManager.isNetworkSupported(ConnectivityManager.TYPE_ETHERNET)
                && mEthernetManager.isAvailable();
    }

    private Network getFirstEthernet() {
        final Network[] networks = mConnectivityManager.getAllNetworks();
        for (final Network network : networks) {
            NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(network);
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                return network;
            }
        }
        return null;
    }
	
	public String getEthernetIpAddress() {
        final Network network = getFirstEthernet();
        if (network == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        boolean gotAddress = false;
        final LinkProperties linkProperties = mConnectivityManager.getLinkProperties(network);
        for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
            if (gotAddress) {
                sb.append("\n");
            }
            sb.append(linkAddress.getAddress().getHostAddress());
            gotAddress = true;
        }
        if (gotAddress) {
            return sb.toString();
        } else {
            return null;
        }
    }
	
	private static final String TAG = IndicatorLightController.class.getSimpleName();
	private static final boolean SEN5_DEBUG_FLAG = false;
}
