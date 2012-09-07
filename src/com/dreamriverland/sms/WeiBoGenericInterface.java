package com.dreamriverland.sms;

import android.content.Context;

import com.weibo.net.Weibo;
import com.weibo.net.WeiboException;

public interface WeiBoGenericInterface {

	String update(Context context, Weibo weibo, String source, String status,
			String lon, String lat) throws WeiboException;

}
