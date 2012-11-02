/**
 * dfdun for search call log.
 * display a list of calls log ,as the result of key word that user input
 *  
 * 2012-7-12
 */

package cn.ingenic.CallLogSearch;

import java.text.SimpleDateFormat;

import android.app.ActionBar;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.telephony.CallerInfo;
import com.android.internal.telephony.VtIntents;

public class CLLauncher extends ListActivity {
	private static final String TAG = CLLauncher.class.getSimpleName();

	private Context mContext;
	private ContentResolver mCR;
	private Cursor mCursor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mContext == null)
			mContext = this.getApplicationContext();
		if (mCR == null)
			mCR = this.getContentResolver();
		// requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.cllauncher);

		Intent intent = getIntent();
		if (intent == null) {
			Log.i("dfdun", TAG + "] intent is null");
			return;
		}
		String action = intent.getAction(), data = intent.getDataString();
		Log.i("dfdun", TAG + "] ****-getIntent: " + intent);
		if (Intent.ACTION_SEARCH.equals(action)) {
			// data is null--- to searchResult UI,also,this Activity
			String querysKey = intent.getStringExtra(SearchManager.QUERY);

			// if (querysKey == null) querysKey="1";
			setTitle("'" + querysKey + "'"
					+ this.getString(R.string.search_result_display));
			ActionBar actionBar = getActionBar();
			if (actionBar != null)
				actionBar.setDisplayHomeAsUpEnabled(true);

			String key = querysKey;
			String selection = Calls.NUMBER + " like '%" + key + "%' or "
					+ Calls.CACHED_NAME + " like '%" + key + "%'";
			Uri uri = Calls.CONTENT_URI;
			// Log.i("dfdun",TAG+"]- selection is "+selection);
			mCursor = mCR.query(uri, CallsLog.PRO_PRI, selection, null,
					Calls.DEFAULT_SORT_ORDER);
			Log.i("dfdun", TAG + "]-.query()-WHERE:" + selection
					+ ", result : " + mCursor.getCount());

			startManagingCursor(mCursor);
			CLAdapter mAdapter = new CLAdapter(this, mCursor);
			setListAdapter(mAdapter);
		} else if (Intent.ACTION_VIEW.equals(action)) {
			// data is Uri (+calls._id)---to calls detail UI
			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(data));
			try {
				startActivity(i);
			} catch (ActivityNotFoundException ex) {
				Log.w("dfdun", TAG + "]Activity not found: " + data);
			}
			finish();
		}
	}
	
	@Override
	protected void onDestroy(){
		if (mCursor!=null)
			mCursor.close();
		super.onDestroy();
	} 

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (mCursor == null) {
			Log.e(TAG, "Got click on position " + position
					+ " but there is no cursor");
			return;
		}
		if (mCursor.isClosed()) {
			Log.e(TAG, "Got click on position " + position
					+ " but the cursor is closed");
			return;
		}
		if (!mCursor.moveToPosition(position)) {
			Log.e(TAG, "Failed to move to position " + position);
			return;
		}
		// Log.i("dfdun", TAG+"Got click on position " + position +"\nid="+id);
		String number = CLAdapter.getColumnString(mCursor, Calls.NUMBER);
		String isvt = CLAdapter.getColumnString(mCursor, Calls.IS_VT);
		if (isvt.equals("0")) { // 0 not vt,
			if (TextUtils.isEmpty(number)
					|| number.equals(CallerInfo.UNKNOWN_NUMBER)
					|| number.equals(CallerInfo.PRIVATE_NUMBER)
					|| number.equals(CallerInfo.PAYPHONE_NUMBER)) {
				// This number can't be called, do nothing
				Toast.makeText(mContext, number + " con not dial",
						Toast.LENGTH_LONG).show();
				return;
			}
			this.startActivity(new Intent(Intent.ACTION_CALL_PRIVILEGED, Uri
					.fromParts("tel", number, null)));
		} else { // vt , so start vt call
			Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED);
			intent.setData(Uri.fromParts("tel", number, null));
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        intent.putExtra(VtIntents.EXTRA_CALL_TYPE, VtIntents.TYPE_CALL_VT);
	        startActivity(intent);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// The user clicked on the Messaging icon in the action bar. Take
			// them back from
			// wherever they came from
			finish();
			return true;
		}
		return false;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent e) {
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			finish();
			return true;
		}
		return super.onKeyUp(keyCode, e);
	}

	private static class CallsLog {
		static String[] PRO_PRI = new String[] { Calls._ID, // 0
				Calls.NUMBER, // 1
				Calls.DATE, // 2
				Calls.DURATION, // 3
				Calls.TYPE, // 4
				Calls.CACHED_NAME, // 5
				Calls.CACHED_NUMBER_TYPE, // 6
				Calls.CACHED_NUMBER_LABEL, // 7
				Calls.IS_VT, // 8
				Calls.GEOCODED_LOCATION, // 9
				Calls.CACHED_PHOTO_ID, // 10
				Calls.PHONE_ID			//11
		};
	}
//
}

/**
 * dfdun for search call log.2012-7-12
 */
class CLAdapter extends ResourceCursorAdapter {

	private static final boolean DBG = true;
	private static final String TAG = "CLAdapter";
	public ContactPhotoManager cpm;
	public boolean isDoubleCard;

	public CLAdapter(Context context, Cursor c) {
		super(context, R.layout.call_log_list_item, c);
		cpm = ContactPhotoManager.getInstance(context);
		isDoubleCard = TelephonyManager.getAll().size()>1;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {

		QuickContactBadge icon1 = (QuickContactBadge) view
				.findViewById(R.id.icon1);
		TextView nameT = (TextView) view.findViewById(R.id.name);
		TextView numberT = (TextView) view.findViewById(R.id.number);
		TextView dateT = (TextView) view.findViewById(R.id.date);
		ImageView typeT = (ImageView) view.findViewById(R.id.call_type);
		TextView durT = (TextView) view.findViewById(R.id.duration);
		ImageView dialImg = (ImageView) view.findViewById(R.id.dialImg);

		// Uri iconUri = getColumnUri(cursor, Calls.CACHED_PHOTO_ID);
		String photo_id = getColumnString(cursor, Calls.CACHED_PHOTO_ID);
		String name = getColumnString(cursor, Calls.CACHED_NAME);
		String number = getColumnString(cursor, Calls.NUMBER);
		String geo = getColumnString(cursor, Calls.GEOCODED_LOCATION);
		String date = getColumnString(cursor, Calls.DATE);
		String type = getColumnString(cursor, Calls.TYPE);
		String dur = getColumnString(cursor, Calls.DURATION);
		String isvt = getColumnString(cursor, Calls.IS_VT);
		String phoneid = getColumnString(cursor, Calls.PHONE_ID);

		// set photo
		icon1.assignContactFromPhone(number, true);
		cpm.loadPhoto(icon1, Long.parseLong(photo_id), false, true);
		if(isDoubleCard){
			number += "<"+phoneid+">"; 
		}

		// set name, number, geocode
		if (TextUtils.isEmpty(name)) {
			nameT.setText(number);
			numberT.setText(TextUtils.isEmpty(geo) ? "-" : geo);
		} else {
			nameT.setText(name);
			numberT.setText(number);
		}
		// set calls date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd   HH:mm:ss");
		dateT.setText(sdf.format(Long.parseLong(date)));
		// set calls type and duration
		if (type.equals("1")) { // incoming call
			typeT.setImageResource(R.drawable.ic_call_log_list_incoming_call);
		} else if (type.equals("2")) {// outgoing call
			typeT.setImageResource(R.drawable.ic_call_log_list_outgoing_call);
		} else { // if (type.equals("3")) {//missed call
			typeT.setImageResource(R.drawable.ic_call_log_list_missed_call);
		}
		durT.setText(getDurationFromDur(dur));

		// set calls type, is vt ?
		if (!isvt.equals("0")) {
			dialImg.setImageResource(R.drawable.dial_vt_action);
		}
	}

	private String getDurationFromDur(String dur) {
		StringBuffer result = new StringBuffer();
		int time = Integer.parseInt(dur);
		int hour = 0, minute = 0, second = 0;
		hour = time / 3600;
		minute = (time - hour * 3600) / 60;
		second = time - hour * 3600 - minute * 60;
		if (hour > 0) // hour
			result.append(hour + mContext.getString(R.string.call_h) + " ");
		if (minute > 0) // minute
			result.append(minute + mContext.getString(R.string.call_m) + " ");
		// second must display
		result.append(second + mContext.getString(R.string.call_s) + " ");
		return result.toString();
	}

	public static Uri getColumnUri(Cursor cursor, String columnName) {
		String uriString = getColumnString(cursor, columnName);
		if (TextUtils.isEmpty(uriString))
			return null;
		return Uri.parse(uriString);
	}

	public static String getColumnString(Cursor cursor, String columnName) {
		int col = cursor.getColumnIndex(columnName);
		return getStringOrNull(cursor, col);
	}

	private static String getStringOrNull(Cursor cursor, int col) {
		if (col < 0)
			return null;
		try {
			return cursor.getString(col);
		} catch (RuntimeException e) {
			if (DBG)
				Log.e("dfdun", TAG + "] Failed to get column " + col
						+ " from cursor", e);
			return null;
		}
	}
}
