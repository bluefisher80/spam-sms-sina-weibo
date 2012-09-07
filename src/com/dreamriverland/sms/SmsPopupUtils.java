package com.dreamriverland.sms;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;

public class SmsPopupUtils {

	public static final String LOGTAG = SmsPopupUtils.class.getSimpleName();
	// Content URIs for SMS app, these may change in future SDK
	public static final Uri MMS_SMS_CONTENT_URI = Uri
			.parse("content://mms-sms/");
	public static final Uri THREAD_ID_CONTENT_URI = Uri.withAppendedPath(
			MMS_SMS_CONTENT_URI, "threadID");
	public static final Uri CONVERSATION_CONTENT_URI = Uri.withAppendedPath(
			MMS_SMS_CONTENT_URI, "conversations");
	public static final String SMSTO_URI = "smsto:";
	private static final String UNREAD_CONDITION = "read=0";

	public static final Uri SMS_CONTENT_URI = Uri.parse("content://sms");
	public static final Uri SMS_INBOX_CONTENT_URI = Uri.withAppendedPath(
			SMS_CONTENT_URI, "inbox");

	public static final Uri MMS_CONTENT_URI = Uri.parse("content://mms");
	public static final Uri MMS_INBOX_CONTENT_URI = Uri.withAppendedPath(
			MMS_CONTENT_URI, "inbox");

	public static final String SMSMMS_ID = "_id";
	public static final String SMS_MIME_TYPE = "vnd.android-dir/mms-sms";
	public static final int READ_THREAD = 1;
	public static final int MESSAGE_TYPE_SMS = 1;
	public static final int MESSAGE_TYPE_MMS = 2;

	public static final int CONTACT_PHOTO_PLACEHOLDER = android.R.drawable.ic_dialog_info;

	// The size of the contact photo thumbnail on the popup
	public static final int CONTACT_PHOTO_THUMBSIZE = 96;

	// The max size of either the width or height of the contact photo
	public static final int CONTACT_PHOTO_MAXSIZE = 1024;

	public static final Uri DONATE_PAYPAL_URI = Uri
			.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=8246419");
	public static final Uri DONATE_MARKET_URI = Uri
			.parse("market://search?q=pname:net.everythingandroid.smspopupdonate");

	// Platform detection
	public static final int SDK_VERSION_ECLAIR = 5;
	// public static final int SDK_VERSION_DONUT = 4;

	public static boolean PRE_ECLAIR = SmsPopupUtils.getSDKVersionNumber() < SmsPopupUtils.SDK_VERSION_ECLAIR ? true
			: false;

	/**
	 * Looks up a contacts display name by contact id - if not found, the
	 * address (phone number) will be formatted and returned instead.
	 */
	public static String getPersonName(Context context, String id,
			String address) {

		// Check for id, if null return the formatting phone number as the name
		if (id == null) {
			if (address != null) {
				return PhoneNumberUtils.formatNumber(address);
			} else {
				return null;
			}
		}

		Cursor cursor = context.getContentResolver().query(
				Uri.withAppendedPath(ContactWrapper.getContentUri(), id),
				new String[] { ContactWrapper
						.getColumn(ContactWrapper.COL_DISPLAY_NAME) }, null,
				null, null);

		if (cursor != null) {
			try {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					String name = cursor.getString(0);

					Log.v(LOGTAG, "Contact Display Name: " + name);
					return name;
				}
			} finally {
				cursor.close();
			}
		}

		if (address != null) {
			return PhoneNumberUtils.formatNumber(address);
		}

		return null;
	}

	/*
	 * Class to hold contact lookup info (as of Android 2.0+ we need the id and
	 * lookup key)
	 */
	public static class ContactIdentification {
		String contactId = null;
		String contactLookup = null;
		String contactName = null;

		public ContactIdentification(String _contactId, String _contactLookup,
				String _contactName) {
			contactId = _contactId;
			contactLookup = _contactLookup;
			contactName = _contactName;
		}

		public ContactIdentification(String _contactId, String _contactName) {
			contactId = _contactId;
			contactName = _contactName;
		}
	}

	/**
	 * Looks up a contacts id, given their address (phone number in this case).
	 * Returns null if not found
	 */
	synchronized public static ContactIdentification getPersonIdFromPhoneNumber(
			Context context, String address) {
		if (address == null)
			return null;

		Cursor cursor = context.getContentResolver().query(
				Uri.withAppendedPath(
						ContactWrapper.getPhoneLookupContentFilterUri(),
						Uri.encode(address)),
				ContactWrapper.getPhoneLookupProjection(), null, null, null);

		if (cursor != null) {
			try {
				if (cursor.getCount() > 0) {
					cursor.moveToFirst();
					String contactId = String.valueOf(cursor.getLong(0));
					String contactName = cursor.getString(1);

					String contactLookup = null;

					if (!PRE_ECLAIR) {
						contactLookup = cursor.getString(2);
					}

					Log.v(LOGTAG, "Found person: " + contactId + ", "
							+ contactName + ", " + contactLookup);
					return new ContactIdentification(contactId, contactLookup,
							contactName);
				}
			} finally {
				cursor.close();
			}
		}

		return null;
	}

	/**
	 * Looks up a contacts id, given their email address. Returns null if not
	 * found
	 */
	synchronized public static ContactIdentification getPersonIdFromEmail(
			Context context, String email) {
		if (email == null)
			return null;

		Cursor cursor = context.getContentResolver().query(
				Uri.withAppendedPath(
						ContactWrapper.getEmailLookupContentFilterUri(),
						Uri.encode(extractAddrSpec(email))),
				ContactWrapper.getEmailLookupProjection(), null, null, null);

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {

					String contactId = String.valueOf(cursor.getLong(0));
					String contactName = cursor.getString(1);
					String contactLookup = null;

					if (!PRE_ECLAIR) {
						contactLookup = cursor.getString(2);
					}

					Log.v(LOGTAG, "Found person: " + contactId + ", "
							+ contactName + ", " + contactLookup);
					return new ContactIdentification(contactId, contactLookup,
							contactName);
				}
			} finally {
				cursor.close();
			}
		}
		return null;
	}

	/**
	 * 
	 * Looks up a contats photo by their contact id, returns a Bitmap array that
	 * represents their photo (or null if not found or there was an error.
	 * 
	 * I do my own scaling and validation of sizes - Android OS supports any
	 * size for contact photos and some apps are adding huge photos to contacts.
	 * Doing the scaling myself allows me more control over how things play out
	 * in those cases.
	 * 
	 * @param context
	 * @param id
	 *            contact id
	 * @return Bitmap of the contacts photo (null if none or an error)
	 */
	public static Bitmap getPersonPhoto(Context context, String id) {

		if (id == null)
			return null;
		if ("0".equals(id))
			return null;

		// First let's just check the dimensions of the contact photo
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;

		// The height and width are stored in 'options' but the photo itself is
		// not loaded
		// Contacts.People.loadContactPhoto(
		// context, Uri.withAppendedPath(Contacts.People.CONTENT_URI, id), 0,
		// options);
		loadContactPhoto(context, id, 0, options);

		// Raw height and width of contact photo
		int height = options.outHeight;
		int width = options.outWidth;

		Log.v(LOGTAG, "Contact photo size = " + height + "x" + width);

		// If photo is too large or not found get out
		if (height > CONTACT_PHOTO_MAXSIZE || width > CONTACT_PHOTO_MAXSIZE
				|| width == 0 || height == 0)
			return null;

		// This time we're going to do it for real
		options.inJustDecodeBounds = false;

		// Calculate new thumbnail size based on screen density
		final float scale = context.getResources().getDisplayMetrics().density;
		int thumbsize = CONTACT_PHOTO_THUMBSIZE;
		if (scale != 1.0) {
			Log.v(LOGTAG, "Screen density is not 1.0, adjusting contact photo");
			thumbsize = Math.round(thumbsize * scale);
		}

		int newHeight = thumbsize;
		int newWidth = thumbsize;

		// If we have an abnormal photo size that's larger than thumbsize then
		// sample it down
		boolean sampleDown = false;

		if (height > thumbsize || width > thumbsize) {
			sampleDown = true;
		}

		// If the dimensions are not the same then calculate new scaled
		// dimenions
		if (height < width) {
			if (sampleDown) {
				options.inSampleSize = Math.round(height / thumbsize);
			}
			newHeight = Math.round(thumbsize * height / width);
		} else {
			if (sampleDown) {
				options.inSampleSize = Math.round(width / thumbsize);
			}
			newWidth = Math.round(thumbsize * width / height);
		}

		// Fetch the real contact photo (sampled down if needed)
		Bitmap contactBitmap = null;
		try {
			// contactBitmap = Contacts.People.loadContactPhoto(
			// context, Uri.withAppendedPath(Contacts.People.CONTENT_URI, id),
			// 0, options);
			contactBitmap = loadContactPhoto(context, id, 0, options);
		} catch (OutOfMemoryError e) {
			Log.e(LOGTAG, "Out of memory when loading contact photo");
		}

		// Not found or error, get out
		if (contactBitmap == null)
			return null;

		// Return bitmap scaled to new height and width
		return Bitmap.createScaledBitmap(contactBitmap, newWidth, newHeight,
				true);
	}

	/**
	 * Opens an InputStream for the person's photo and returns the photo as a
	 * Bitmap. If the person's photo isn't present returns the
	 * placeholderImageResource instead.
	 * 
	 * @param context
	 *            the Context
	 * @param id
	 *            the id of the person
	 * @param placeholderImageResource
	 *            the image resource to use if the person doesn't have a photo
	 * @param options
	 *            the decoding options, can be set to null
	 */
	public static Bitmap loadContactPhoto(Context context, String id,
			int placeholderImageResource, BitmapFactory.Options options) {
		if (id == null) {
			return loadPlaceholderPhoto(placeholderImageResource, context,
					options);
		}

		InputStream stream = ContactWrapper.openContactPhotoInputStream(
				context.getContentResolver(), id);

		Bitmap bm = stream != null ? BitmapFactory.decodeStream(stream, null,
				options) : null;
		if (bm == null) {
			bm = loadPlaceholderPhoto(placeholderImageResource, context,
					options);
		}
		return bm;
	}

	private static Bitmap loadPlaceholderPhoto(int placeholderImageResource,
			Context context, BitmapFactory.Options options) {
		if (placeholderImageResource == 0) {
			return null;
		}
		return BitmapFactory.decodeResource(context.getResources(),
				placeholderImageResource, options);
	}

	/**
	 * 
	 * Tries to locate the message thread id given the address (phone or email)
	 * of the message sender.
	 * 
	 * @param context
	 *            a context to use
	 * @param address
	 *            phone number or email address of sender
	 * @return the thread id (or 0 if there was a problem)
	 */
	synchronized public static long findThreadIdFromAddress(Context context,
			String address) {
		if (address == null)
			return 0;

		String THREAD_RECIPIENT_QUERY = "recipient";

		Uri.Builder uriBuilder = THREAD_ID_CONTENT_URI.buildUpon();
		uriBuilder.appendQueryParameter(THREAD_RECIPIENT_QUERY, address);

		long threadId = 0;

		Cursor cursor = context.getContentResolver().query(
				uriBuilder.build(),
				new String[] { ContactWrapper
						.getColumn(ContactWrapper.COL_CONTACT_ID) }, null,
				null, null);

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					threadId = cursor.getLong(0);
				}
			} finally {
				cursor.close();
			}
		}
		return threadId;
	}

	/**
   * 
   */
	public static Intent getSmsInboxIntent() {
		Intent conversations = new Intent(Intent.ACTION_MAIN);
		// conversations.addCategory(Intent.CATEGORY_DEFAULT);
		conversations.setType(SMS_MIME_TYPE);
		// should I be using FLAG_ACTIVITY_RESET_TASK_IF_NEEDED??
		int flags = Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP;
		conversations.setFlags(flags);

		return conversations;
	}

	/**
	 * Get system view sms thread Intent
	 * 
	 * @param context
	 *            context
	 * @param threadId
	 *            the message thread id to view
	 * @return the intent that can be started with startActivity()
	 */
	public static Intent getSmsToIntent(Context context, long threadId) {
		Intent popup = new Intent(Intent.ACTION_VIEW);
		// should I be using FLAG_ACTIVITY_RESET_TASK_IF_NEEDED??
		int flags = Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP;
		popup.setFlags(flags);
		if (threadId > 0) {
			// Log.v("^^Found threadId (" + threadId +
			// "), sending to Sms intent");
			popup.setData(Uri.withAppendedPath(THREAD_ID_CONTENT_URI,
					String.valueOf(threadId)));
		} else {
			return getSmsInboxIntent();
		}
		return popup;
	}

	/**
	 * Get system sms-to Intent (normally "compose message" activity)
	 * 
	 * @param context
	 *            context
	 * @param phoneNumber
	 *            the phone number to compose the message to
	 * @return the intent that can be started with startActivity()
	 */
	public static Intent getSmsToIntent(Context context, String phoneNumber) {
		Intent popup = new Intent(Intent.ACTION_SENDTO);
		// should I be using FLAG_ACTIVITY_RESET_TASK_IF_NEEDED??
		int flags = Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_SINGLE_TOP
				| Intent.FLAG_ACTIVITY_CLEAR_TOP;
		popup.setFlags(flags);
		if (!"".equals(phoneNumber)) {
			// Log.v("^^Found threadId (" + threadId +
			// "), sending to Sms intent");
			popup.setData(Uri.parse(SMSTO_URI + Uri.encode(phoneNumber)));
		} else {
			return getSmsInboxIntent();
		}
		return popup;
	}

	/**
	 * Fetch output from logcat, dump it in a file and return the URI to the
	 * file
	 */
	public static Uri collectLogs(Context context) {
		final String logfile = "log.txt";

		try {
			ArrayList<String> commandLine = new ArrayList<String>();
			commandLine.add("logcat");
			commandLine.add("-d");
			commandLine.add("AndroidRuntime:E");
			commandLine.add(LOGTAG + ":V");
			commandLine.add("*:S");

			BufferedInputStream fin = new BufferedInputStream(Runtime
					.getRuntime().exec(commandLine.toArray(new String[0]))
					.getInputStream());
			BufferedOutputStream fout = new BufferedOutputStream(
					context.openFileOutput(logfile, Context.MODE_WORLD_READABLE));

			// Copy output to a log file
			int i;
			do {
				i = fin.read();
				if (i != -1)
					fout.write(i);
			} while (i != -1);
			fin.close();
			fout.close();
		} catch (IOException e) {
			return null;
		} catch (SecurityException e) {
			return null;
		}

		return Uri.fromFile(context.getFileStreamPath(logfile));
	}

	/**
	 * Return current unread message count from system db (sms and mms)
	 * 
	 * @param context
	 * @return unread sms+mms message count
	 */
	public static int getUnreadMessagesCount(Context context) {
		return getUnreadMessagesCount(context, 0, null);
	}

	/**
	 * Return current unread message count from system db (sms and mms)
	 * 
	 * @param context
	 * @param timestamp
	 *            only messages before this timestamp will be counted
	 * @return unread sms+mms message count
	 */
	synchronized public static int getUnreadMessagesCount(Context context,
			long timestamp, String messageBody) {
		return getUnreadSmsCount(context, timestamp, messageBody)
				+ getUnreadMmsCount(context);
	}

	/**
	 * Return current unread message count from system db (sms only)
	 * 
	 * @param context
	 * @return unread sms message count
	 */
	private static int getUnreadSmsCount(Context context) {
		return getUnreadSmsCount(context, 0, null);
	}

	/**
	 * Return current unread message count from system db (sms only)
	 * 
	 * @param context
	 * @param timestamp
	 *            only messages before this timestamp will be counted
	 * @return unread sms message count
	 */
	private static int getUnreadSmsCount(Context context, long timestamp,
			String messageBody) {

		Log.v(LOGTAG, "getUnreadSmsCount()");

		final String[] projection = new String[] { SMSMMS_ID, "body" };
		final String selection = UNREAD_CONDITION;
		final String[] selectionArgs = null;
		final String sortOrder = "date DESC";

		int count = 0;

		Cursor cursor = context.getContentResolver().query(
				SMS_INBOX_CONTENT_URI, projection, selection, selectionArgs,
				sortOrder);

		if (cursor != null) {
			try {
				count = cursor.getCount();

				/*
				 * We need to check if the message received matches the most
				 * recent one in the db or not (to find out if our code ran
				 * before the system code or vice-versa)
				 */
				if (messageBody != null && count > 0) {
					if (cursor.moveToFirst()) {
						/*
						 * Check the most recent message, if the body does not
						 * match then it hasn't yet been inserted into the
						 * system database, therefore we need to add one to our
						 * total count
						 */
						if (!messageBody.equals(cursor.getString(1))) {

							Log.v(LOGTAG,
									"getUnreadSmsCount(): most recent message did not match body, adding 1 to count");
							count++;
						}
					}
				}
			} finally {
				cursor.close();
			}
		}

		/*
		 * If count is still 0 and timestamp is set then its likely the system
		 * db had not updated when this code ran, therefore let's add 1 so the
		 * notify will run correctly.
		 */
		if (count == 0 && timestamp > 0) {
			count = 1;
		}

		Log.v(LOGTAG, "getUnreadSmsCount(): unread count = " + count);
		return count;
	}

	/**
	 * Return current unread message count from system db (mms only)
	 * 
	 * @param context
	 * @return unread mms message count
	 */
	private static int getUnreadMmsCount(Context context) {

		final String selection = UNREAD_CONDITION;
		final String[] projection = new String[] { SMSMMS_ID };

		int count = 0;

		Cursor cursor = context.getContentResolver().query(
				MMS_INBOX_CONTENT_URI, projection, selection, null, null);

		if (cursor != null) {
			try {
				count = cursor.getCount();
			} finally {
				cursor.close();
			}
		}

		Log.v(LOGTAG, "mms unread count = " + count);
		return count;
	}

	public static String getMmsAddress(Context context, long messageId) {
		final String[] projection = new String[] { "address", "contact_id",
				"charset", "type" };
		final String selection = "type=137"; // "type="+ PduHeaders.FROM,

		Uri.Builder builder = MMS_CONTENT_URI.buildUpon();
		builder.appendPath(String.valueOf(messageId)).appendPath("addr");

		Cursor cursor = context.getContentResolver().query(builder.build(),
				projection, selection, null, null);

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					// Apparently contact_id is always empty in this table so we
					// can't get it from here

					// Just return the address
					return cursor.getString(0);
				}
			} finally {
				cursor.close();
			}
		}

		return context.getString(android.R.string.unknownName);
	}

	public static final Pattern NAME_ADDR_EMAIL_PATTERN = Pattern
			.compile("\\s*(\"[^\"]*\"|[^<>\"]+)\\s*<([^<>]+)>\\s*");

	public static final Pattern QUOTED_STRING_PATTERN = Pattern
			.compile("\\s*\"([^\"]*)\"\\s*");

	private static String extractAddrSpec(String address) {
		Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(address);

		if (match.matches()) {
			return match.group(2);
		}
		return address;
	}

	private static String getEmailDisplayName(String displayString) {
		Matcher match = QUOTED_STRING_PATTERN.matcher(displayString);
		if (match.matches()) {
			return match.group(1);
		}
		return displayString;
	}

	/**
	 * Get the display name of an email address. If the address already contains
	 * the name, parse and return it. Otherwise, query the contact database.
	 * Cache query results for repeated queries.
	 */
	public static String getDisplayName(Context context, String email) {
		Matcher match = NAME_ADDR_EMAIL_PATTERN.matcher(email);
		if (match.matches()) {
			// email has display name, return that
			return getEmailDisplayName(match.group(1));
		}

		// otherwise let's check the contacts list for a user with this email
		// Cursor cursor = context.getContentResolver().query(
		// ContactWrapper.getEmailContentUri(),
		// new String[] { Contacts.ContactMethods.NAME },
		// Contacts.ContactMethods.DATA + " = ?",
		// new String[] { email }, null);
		Cursor cursor = context.getContentResolver().query(
				Uri.withAppendedPath(
						ContactWrapper.getEmailLookupContentFilterUri(),
						Uri.encode(email)),
				new String[] { ContactWrapper
						.getColumn(ContactWrapper.COL_DISPLAY_NAME) }, null,
				null, null);

		if (cursor != null) {
			try {
				// int columnIndex =
				// cursor.getColumnIndexOrThrow(Contacts.ContactMethods.NAME);
				while (cursor.moveToNext()) {
					// String name = cursor.getString(columnIndex);
					String name = cursor.getString(0);
					if (!TextUtils.isEmpty(name)) {
						return name;
					}
				}
			} finally {
				cursor.close();
			}
		}
		return email;
	}

	/**
	 * Fetch the current device Android OS platform number.
	 * 
	 * TODO: once Cupcake support is no longer needed the system var
	 * android.os.Build.VERSION.SDK_INT can be used instead.
	 * 
	 * @return SDK version number
	 */
	public static int getSDKVersionNumber() {
		int version_sdk;
		try {
			version_sdk = Integer.valueOf(android.os.Build.VERSION.SDK);
		} catch (NumberFormatException e) {
			version_sdk = 0;
		}
		return version_sdk;
	}
}