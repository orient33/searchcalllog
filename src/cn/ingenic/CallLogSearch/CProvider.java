/**
 *  dfdun for search calls log 
 */
package cn.ingenic.CallLogSearch;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;

public class CProvider extends ContentProvider {

    private static final String TAG = CProvider.class.getSimpleName();
    private Context mContext;
    private ContentResolver mCR;
    private Cursor rCursor;
    
    private static final String[] COLUMNS = {
        "_id",
        SearchManager.SUGGEST_COLUMN_TEXT_1,
        SearchManager.SUGGEST_COLUMN_TEXT_2,
        //SearchManager.SUGGEST_COLUMN_INTENT_ACTION,// or define in searchable.xml
        SearchManager.SUGGEST_COLUMN_INTENT_DATA,    // or define in searchable.xml
        SearchManager.SUGGEST_COLUMN_ICON_1
    };

    @Override
    public boolean onCreate() {
    	mContext=getContext();
    	mCR=mContext.getContentResolver();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projectionIn, String selection,
            String[] selectionArgs, String sortOrder) {
    	/*key -- the string of user input..
    	 * uri is content://cn.ingenic.CallLogSearch_/search_suggest_query/key?limit=50
   	     */
    	String key = uri.getLastPathSegment(); //can get zh-cn rightly
    	//key is surely not null; when input "",key is "search_suggest_query"
		if (TextUtils.isEmpty(key)||"search_suggest_query".equals(key)) {
			selection = null;
		} else {
			if (selection != null) 
				selection += " and (" + Calls.NUMBER + " like '%"
						+ key + "%' or " + Calls.CACHED_NAME
						+ " like '%" + key + "%')";
			else
				selection = Calls.NUMBER + " like '%" + key + "%' or "
						+ Calls.CACHED_NAME + " like '%" + key + "%'";
		}
    	uri = Calls.CONTENT_URI; // content://call_log/calls
    	rCursor =mCR.query(uri, CallsLog.PRO_PRI,selection, null,Calls.DEFAULT_SORT_ORDER);
    	//Log.i("dfdun",TAG+"].query()-uri:"+uri+"; selection:"+selection+"; result:"+rCursor.getCount() );
    	return afterQuery();
    }

    @Override
    public String getType(Uri uri) {
        return SearchManager.SUGGEST_MIME_TYPE;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /* from calls cursor to display cursor(MatrixCursor) */
    private Cursor afterQuery(){
    	int len = rCursor.getCount();
    	rCursor.moveToFirst();
    	 MatrixCursor cursor = new MatrixCursor(COLUMNS);
    	 
         for (int i = 0; i < len; i++) {
        	 String name = rCursor.getString(5);
        	 String number = rCursor.getString(1);
        	 String geocode = rCursor.getString(9);
        	 String photo_id = rCursor.getString(10);
        	 String _id= rCursor.getString(0);
        	 if (TextUtils.isEmpty(geocode))
        		 geocode = "-";
        	 boolean isnameNull = TextUtils.isEmpty(name);
             cursor.addRow(new Object[]{
                 i,
                 isnameNull?number:name,
                 isnameNull?geocode:number,
               	 Uri.withAppendedPath(Calls.CONTENT_URI, _id),
                 getPhotoUri(photo_id,name,isnameNull)
             });
             //Log.i("dfdun", TAG+"] photeid no use "+ photo_id);
             rCursor.moveToNext();
         }
         rCursor.close();
    	return cursor;
    }

    /*  from name to PhoteUri--use table(DB.raw_contacts) get _id, then use _id to append photoUri */
    private String getPhotoUri(String photo_id,String name,boolean isnameNull){
    	Uri basePhotoUri = Contacts.CONTENT_URI;
    	String uri;
    	
    	if (photo_id.equals("0")||isnameNull) { //display default photo,,if has no (name or photo) 
    		uri = String.valueOf(R.drawable.ic_contact_picture);
    	} else {
    		//  has name and photo 
    		String selection = "display_name = '"+name + "' and raw_contact_type='0'"; //DB raw_contacts
        	Cursor c=mCR.query(RawContacts.CONTENT_URI, 
        			new String[]{"contact_id"} ,
        			selection,  null, null);// depend on number to get contactPhotoUir
        	c.moveToFirst(); 
        	if(c.getCount()==0){
        		uri = String.valueOf(R.drawable.ic_contact_picture);
        		Log.e("dfdun", TAG+"(nameToPhotoid) get 0 result");
        	}else{
        		String raw_contacts_id = c.getString(0);
        		uri=Uri.withAppendedPath(basePhotoUri, raw_contacts_id).toString();
        		uri=Uri.withAppendedPath(Uri.parse(uri), "photo").toString();
        		//Log.d("dfdun", "142 append uri is "+ uri);
        	}
        	c.close();
    	} //Log.i("dfdun", TAG+"]photo uri :"+uri);
    	return uri;
    }

    private static class CallsLog{
    	static String[] PRO_PRI = new String[]{
    		Calls._ID,       			// 0
    		Calls.NUMBER,				// 1
    		Calls.DATE,					// 2
    		Calls.DURATION,				// 3
    		Calls.TYPE,					// 4
    		Calls.CACHED_NAME,			// 5
    		Calls.CACHED_NUMBER_TYPE,	// 6
    		Calls.CACHED_NUMBER_LABEL,	// 7
    		Calls.IS_VT,				// 8
    		Calls.GEOCODED_LOCATION,   	// 9
    		Calls.CACHED_PHOTO_ID		//10
    	};
    }
    
}
