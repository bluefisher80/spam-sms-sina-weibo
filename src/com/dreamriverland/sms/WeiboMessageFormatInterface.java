package com.dreamriverland.sms;

public interface WeiboMessageFormatInterface {

	/**
	 * 将原始短信格式化并输出
	 * 
	 * @param origMsg
	 * @param origNum
	 * @return
	 */
	String format(String origMsg, String origNum);

}
