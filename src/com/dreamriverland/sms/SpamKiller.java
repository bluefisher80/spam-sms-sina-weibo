package com.dreamriverland.sms;

import android.app.Application;
import android.content.Context;

public class SpamKiller extends Application {

	private SmsDatabase m_helper;

	@Override
	public void onCreate() {
		super.onCreate();
		m_helper = new SmsDatabase(this);
	}

	public static void clearMessages(Context context) {
		getHelper(context).deleteAll();

	}

	public static SmsDatabase getHelper(Context context) {
		return ((SpamKiller) context.getApplicationContext()).m_helper;
	}
}
