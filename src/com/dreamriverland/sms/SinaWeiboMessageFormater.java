package com.dreamriverland.sms;

public class SinaWeiboMessageFormater implements WeiboMessageFormatInterface {

	@Override
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.dreamriverland.sms.WeiboMessageFormatInterface#format(java.lang.String
	 * , java.lang.String)
	 */
	public String format(String origMsg, String origNum) {
		return "#一人曝光一条垃圾短信# " + origMsg + " 来自#" + origNum

		+ "#";
	}
}
