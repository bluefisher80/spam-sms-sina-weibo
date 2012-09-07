package com.dreamriverland.sms;

import java.util.Calendar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.telephony.gsm.SmsMessage;
import android.util.Log;

import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;

public class SMSFilter extends BroadcastReceiver {

	public static final String LOGTAG = SMSFilter.class.getSimpleName();

	public interface SmsModemListener {
		public void onSMSSent(String token);

		public void onSMSSendError(String token, String errorDetails);

		public void onNewSMS(String address, String message);
	}

	// WeiboService weiboService;

	// public SMSFilter(Context applicationContext, WeiboService weiboService) {
	//
	// this.weiboService = weiboService;
	//
	// final IntentFilter receivedFilter = new IntentFilter();
	// receivedFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
	// applicationContext.registerReceiver(this, receivedFilter);
	// }

	SmsDatabase db;

	// Weibo mWeibo = Weibo.getInstance();

	/**
	 * Is WIFI, GPRS, 3G available ? Weibo mWeibo = Weibo.getInstance();
	 * 
	 * @param context
	 * @return
	 */
	private Boolean isOnLine(Context context) {
		ConnectivityManager conMgr = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		if (conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
				|| conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING) {

			return Boolean.TRUE;
		} else if (conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED
				|| conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED) {
			return Boolean.FALSE;

		}

		return Boolean.FALSE;

	}

	/**
	 * Receive a sms or something
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		Log.d(LOGTAG, "A no use log");

		context.startService(new Intent(context, Weibo.class));

		Log.d(LOGTAG, "Is it on line " + isOnLine(context));

		Bundle bundle = intent.getExtras();
		Object[] pdus = (Object[]) bundle.get("pdus");
		SmsMessage[] messages = new SmsMessage[pdus.length];

		String msg = null;

		String num;

		for (int i = 0; i < messages.length; i++) {

			messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
			Log.v(LOGTAG, "Body:" + messages[i].getDisplayMessageBody());
			Log.v(LOGTAG,
					"Address:" + messages[i].getDisplayOriginatingAddress());

			String originatingAddress = messages[i]
					.getDisplayOriginatingAddress();

			if (SmsPopupUtils.getPersonIdFromPhoneNumber(context,
					originatingAddress) != null) {
				// normal message, do nothing
			} else {

				// this.weiboService.onNewSMS(
				// messages[i].getDisplayOriginatingAddress(),
				// messages[i].getDisplayMessageBody());

				msg = msg + messages[i].getDisplayMessageBody();
				num = messages[i].getDisplayOriginatingAddress();

				// Weibo weibo = Weibo.getInstance();

				// disabled, the invocation do nothing now.
				// update(context,
				// weibo,
				// "Myapp",
				// "#垃圾短信#"
				// + messages[i].getDisplayMessageBody()
				// + "来自#"
				// + messages[i]
				// .getDisplayOriginatingAddress()
				// + "#", null, null);

				// abortBroadcast();

			}

			db = SpamKiller.getHelper(context);

			db.addSms(messages[i].getDisplayMessageBody(),
					messages[i].getDisplayOriginatingAddress());

			// For the appspot web site version
			// try {
			// SmsList.sendtoServer(messages[i].getDisplayMessageBody(),
			// messages[i].getDisplayOriginatingAddress());
			// abortBroadcast();
			// } catch (Exception e) {
			// Log.v(e.toString());
			// e.printStackTrace();
			// }

		}
	}
	/**
	 * Put the message to SINA Weibo.
	 * 
	 * @param context
	 * @param weibo
	 * @param source
	 * @param status
	 * @param lon
	 * @param lat
	 * @return
	 * @throws WeiboException
	 */
	// private String update(Context context, Weibo weibo, String source,
	// String status, String lon, String lat) throws WeiboException {
	//
	// if (2 > 1)
	// return null;
	// WeiboParameters bundle = new WeiboParameters();
	// bundle.add("source", source);
	// bundle.add("status", status);
	// if (!TextUtils.isEmpty(lon)) {
	// bundle.add("lon", lon);
	// }
	// if (!TextUtils.isEmpty(lat)) {
	// bundle.add("lat", lat);
	// }
	// String rlt = "";
	// String url = Weibo.SERVER + "statuses/update.json";
	// rlt = weibo.request(context, url, bundle, Utility.HTTPMETHOD_POST,
	// mWeibo.getAccessToken());
	// return rlt;
	// }

}
