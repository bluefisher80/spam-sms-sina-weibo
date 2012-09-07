package com.dreamriverland.sms;

import android.content.Context;
import android.text.TextUtils;

import com.weibo.net.Utility;
import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;
import com.weibo.net.WeiboParameters;

public class SinaWeiboImpl implements WeiBoGenericInterface {

	@Override
	public String update(Context context, Weibo weibo, String source,
			String status, String lon, String lat) throws WeiboException {
		WeiboParameters bundle = new WeiboParameters();
		bundle.add("source", source);
		bundle.add("status", status);
		if (!TextUtils.isEmpty(lon)) {
			bundle.add("lon", lon);
		}
		if (!TextUtils.isEmpty(lat)) {
			bundle.add("lat", lat);
		}
		String rlt = "";
		String url = Weibo.SERVER + "statuses/update.json";
		rlt = weibo.request(context, url, bundle, Utility.HTTPMETHOD_POST,
				weibo.getAccessToken());
		return rlt;
	}

}
