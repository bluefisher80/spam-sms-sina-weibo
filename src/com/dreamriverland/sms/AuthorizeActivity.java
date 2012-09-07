/*
 * Copyright 2011 Sina.
 *
 * Licensed under the Apache License and Weibo License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.open.weibo.com
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dreamriverland.sms;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.weibo.net.RequestToken;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;

/**
 * Sample code for testing weibo APIs.
 * 
 * @author ZhangJie (zhangjie2@staff.sina.com.cn)
 */

public class AuthorizeActivity extends Activity {
	/** Called when the activity is first created. */
	private Button mLogin;
	private TextView mToken;

	private static final String URL_ACTIVITY_CALLBACK = "ljdx://listpage";
	private static final String FROM = "xweibo";

	private static final String CONSUMER_KEY = "2236168743";
	private static final String CONSUMER_SECRET = "47e2e9f5d0941de45722a8c5ffe8f1e6";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		mToken = (TextView) this.findViewById(R.id.tvToken);
		mLogin = (Button) this.findViewById(R.id.btnLogin);
		mLogin.setText("oauth!");
		mLogin.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v == mLogin) {
					Weibo weibo = Weibo.getInstance();
					weibo.setupConsumerConfig(CONSUMER_KEY, CONSUMER_SECRET);
					try {
						/*
						 * AccessToken at =
						 * weibo.getXauthAccessToken(TextActivity.this,
						 * Weibo.APP_KEY, Weibo.APP_SECRET, "", "");
						 * mToken.setText(at.getToken());
						 */

						RequestToken requestToken = weibo.getRequestToken(
								AuthorizeActivity.this, Weibo.APP_KEY,
								Weibo.APP_SECRET,
								AuthorizeActivity.URL_ACTIVITY_CALLBACK);
						mToken.setText(requestToken.getToken());
						Uri uri = Uri.parse(Weibo.URL_AUTHENTICATION
								+ "?display=wap2.0&oauth_token="
								+ requestToken.getToken() + "&from="
								+ AuthorizeActivity.FROM);
						startActivity(new Intent(Intent.ACTION_VIEW, uri));
					} catch (WeiboException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void onResume() {
		super.onResume();
	}

}