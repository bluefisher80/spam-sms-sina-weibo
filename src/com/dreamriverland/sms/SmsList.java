package com.dreamriverland.sms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.weibo.net.AccessToken;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;

public class SmsList extends Activity {

	Weibo mWeibo = Weibo.getInstance();

	WeiboMessageFormatInterface msgFormater = new SinaWeiboMessageFormater();

	WeiBoGenericInterface weiboEngine = new SinaWeiboImpl();

	public static final String LOGTAG = SmsList.class.getSimpleName();

	Weibo weibo = Weibo.getInstance();

	public static final String WEIBO_ACCESS_TOKEN_STR = "Weibo_access_token_key";
	public static final String WEIBO_ACCESS_TOKEN_SECRECT = "Weibo_access_token_secret";

	String smsID;

	/**
	 * The current only-in-view message
	 */
	String origMsg;

	/**
	 * The current only-in-view message number
	 */
	String origNum;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		SharedPreferences setting = getPreferences(MODE_PRIVATE);

		String accesstoken = setting.getString(WEIBO_ACCESS_TOKEN_STR, null);
		if (accesstoken != null) {

			weibo.setAccessToken(new AccessToken(accesstoken, setting
					.getString(WEIBO_ACCESS_TOKEN_SECRECT, null)));

			Log.e(LOGTAG, "Reuse the access token");

		}

		Uri uri = this.getIntent().getData();

		if (uri == null && weibo.getAccessToken() == null) {
			// 1. new launch, so no URI
			// 2. 没有access token,所以去oauth　
			startActivity(new Intent(this, AuthorizeActivity.class));
			return;
		} else if (uri != null && weibo.getAccessToken() == null) {

			String oauth_verifier = uri.getQueryParameter("oauth_verifier");
			weibo.addOauthverifier(oauth_verifier);

			try {
				weibo.generateAccessToken(this, null);

				SharedPreferences.Editor editor = setting.edit();
				editor.putString(WEIBO_ACCESS_TOKEN_STR, weibo.getAccessToken()
						.getToken());
				editor.putString(WEIBO_ACCESS_TOKEN_SECRECT, weibo
						.getAccessToken().getSecret());
				editor.commit();

			} catch (Exception e) {
				Log.v(LOGTAG, "No access key here, so oauth now!");
				startActivity(new Intent(this, AuthorizeActivity.class));

				return;

			}

		} else {
			// We have the access token,
		}

		db = new SmsDatabase(this);

		Cursor cursor = db.getLatestOne();

		StringBuffer all = new StringBuffer();

		cursor.moveToFirst();
		while (cursor.isAfterLast() == false) {
			all.append(cursor.getString(1)).append(cursor.getString(2))
					.append("\n");

			// There is only one record, so ...
			smsID = cursor.getString(0);
			origMsg = cursor.getString(1);
			origNum = cursor.getString(2);

			cursor.moveToNext();
		}

		db.close();
		cursor.close();

		super.onCreate(savedInstanceState);

		setContentView(R.layout.smslist);

		TextView allmsg = (TextView) findViewById(R.id.allmsg);

		allmsg.setText(all.toString());

		Button delbutton = (Button) findViewById(R.id.backbutton);

		// Weibo the sms and delete it from DB
		Button weiboItButton = (Button) findViewById(R.id.publishDelete);

		weiboItButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				try {
					weiboEngine.update(v.getContext(), weibo, "Myapp",
							msgFormater.format(origMsg, origNum), null, null);

					db.deleteByID(smsID);
				} catch (WeiboException e) {

					e.printStackTrace();

					Log.e(LOGTAG, e.toString());

				}

				Intent intent = getIntent();
				finish();
				startActivity(intent);

			}
		});

		// Delete it from DB
		Button DeleteNotSpam = (Button) findViewById(R.id.onlyDelete);

		DeleteNotSpam.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				db.deleteByID(smsID);

				Intent intent = getIntent();
				finish();
				startActivity(intent);
			}
		});

		// startService.setOnClickListener(new OnClickListener() {
		//
		// @Override
		// public void onClick(View v) {
		//
		// v.getContext().startService(
		// new Intent(v.getContext(), WeiboService.class));
		//
		// }
		// });

		delbutton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				db.deleteAll();
				Toast.makeText(SmsList.this, "删除完成了吧", Toast.LENGTH_LONG);
			}
		});

		if (all.toString() == null || all.toString().equals("")) {

			delbutton.setVisibility(Button.INVISIBLE);
			weiboItButton.setVisibility(Button.INVISIBLE);
			DeleteNotSpam.setVisibility(Button.INVISIBLE);

			allmsg.setText("这会儿还没有垃圾收到，等它一会儿。");

		}

	}

	@SuppressWarnings("unused")
	private Boolean isOnLine() {
		ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

		if (conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.CONNECTED
				|| conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.CONNECTING) {

			return Boolean.TRUE;
		} else if (conMgr.getNetworkInfo(0).getState() == NetworkInfo.State.DISCONNECTED
				|| conMgr.getNetworkInfo(1).getState() == NetworkInfo.State.DISCONNECTED) {
			return Boolean.FALSE;

		}

		return Boolean.FALSE;

	}

	SmsDatabase db;

}