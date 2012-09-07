package com.dreamriverland.sms;

import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SmsDatabase extends SQLiteOpenHelper {

	public static final String LOGTAG = SmsDatabase.class.getSimpleName();

	public static final String DATABASE_NAME = "spamsms.db";

	private static final int DATABASE_VERSION = 1;

	public static final String[] TEXT_MESSAGE_COLS = new String[] { "_id",
			"content", "number" };

	public SmsDatabase(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		this._context = context;

	}

	private Context _context;

	/**
	 * Execute all of the SQL statements in the String[] array
	 * 
	 * @param db
	 *            The database on which to execute the statements
	 * @param sql
	 *            An array of SQL statements to execute
	 */
	private void execMultipleSQL(SQLiteDatabase db, String[] sql) {
		for (String s : sql)
			if (s.trim().length() > 0)
				db.execSQL(s);

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		Log.v(LOGTAG, "Creating the database");

		String[] sql = this._context.getString(
				R.string.SmsSpamDatabase_onCreate).split("\n");

		Log.v(LOGTAG,
				this._context.getString(R.string.SmsSpamDatabase_onCreate));
		db.beginTransaction();
		try {
			// Create tables & test data
			execMultipleSQL(db, sql);
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			Log.e("Error creating tables and debug data", e.toString());
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		this.onCreate(db);

	}

	public void addSms(String content, String sender) {

		String sql = String.format("INSERT INTO sms (_id, content, number) "
				+ "VALUES (          NULL, '%s', '%s')", content, sender);

		getWritableDatabase().execSQL(sql);

	}

	public Cursor getSms() {

		return getReadableDatabase().query("sms", TEXT_MESSAGE_COLS, null,
				null, null, null, null, null);

	}

	/**
	 * 
	 * Only show the latest received sms message
	 * 
	 * @return
	 */
	public Cursor getLatestOne() {

		return getReadableDatabase().query("sms", TEXT_MESSAGE_COLS, null,
				null, null, null, "_id desc", "1");

	}

	public void deleteAll() {
		SQLiteDatabase db = getWritableDatabase();
		try {

			db.execSQL("Delete from sms");
			db.close();

		} catch (SQLException e) {
			Log.v(LOGTAG, e.toString());
			db.close();

		}
	}

	public void deleteByID(String id) {

		SQLiteDatabase db = getWritableDatabase();
		try {

			db.delete("sms", "_id=" + id, null);
			Log.v(LOGTAG, "Deleted Msg of id " + id);

			db.close();
		} catch (SQLException e) {
			Log.v(LOGTAG, e.toString());
			db.close();

		}

	}

	private TextMessage messageFromCursor(Cursor cursor) {
		final TextMessage message = new TextMessage();

		// "id", "number", "text", "created", "direction", "status", "serverId"
		// };
		message.id = cursor.getLong(0);
		message.number = cursor.getString(1);
		message.text = cursor.getString(2);
		message.created = new Date(cursor.getLong(3));
		message.direction = cursor.getString(4).charAt(0);
		message.status = cursor.getString(5).charAt(0);
		message.serverId = cursor.getLong(6);

		return message;
	}

	public TextMessage firstFromCursor(Cursor cursor) {
		try {
			if (cursor.moveToNext()) {
				return messageFromCursor(cursor);
			} else {
				return null;
			}
		} finally {
			cursor.close();
		}
	}

	public TextMessage withId(Long id) {
		final SQLiteDatabase readableDatabase = getReadableDatabase();

		final Cursor cursor = readableDatabase.query("messages",
				TEXT_MESSAGE_COLS, "_id = ?", new String[] { id.toString() },
				null, null, null, null);

		return firstFromCursor(cursor);
	}

	public void createMessage(TextMessage msg) {
		// TODO Auto-generated method stub

	}

	public List<TextMessage> withStatus(Context applicationContext,
			char outgoing, char sent) {
		// TODO Auto-generated method stub
		return null;
	}

	public TextMessage withServerId(Context applicationContext, long serverId) {
		// TODO Auto-generated method stub
		return null;
	}

	public void updateMessage(TextMessage msg) {
		// TODO Auto-generated method stub

	}
}
