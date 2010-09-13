package com.example.android.contactmanager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

public final class ContactManager extends Activity {

	private Menu mMenu;
	private Stack<String[]> filterHistory = new Stack<String[]>();
	private HashMap<String, Bitmap> iconMap=new HashMap<String, Bitmap>();

	@Override
	public void onBackPressed() {
		if (filterHistory.size() < 2) {
			filterHistory.empty();
			populateContactList(null);
		} else {
			filterHistory.pop();
			populateContactList(mSegmentor.getIntersection(filterHistory.pop()));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mMenu = menu;
		mMenu.clear();
		if (mSegmentor != null)
			for (String key : mSegmentor.getKeys()) {
				MenuItem item=mMenu.add(key);
				if (key.equals("cirquent"))
				item.setIcon( getResources().getDrawable(R.drawable.icon));
			}
		return true;

	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		populateContactList(mSegmentor.getIntersection(new String[] { ""
				+ item.getTitle() }));
		filterHistory.push(new String[] { "" + item.getTitle() });
		return true;
	}

	@Override
	public boolean onSearchRequested() {
		// TODO Auto-generated method stub
		return super.onSearchRequested();
	}

	private final class ImageAdapter extends BaseAdapter {
		private Context mContext;
		private Cursor mCursor;

		/**
		 * 
		 */
		public ImageAdapter(Context context, Cursor cursor) {
			mContext = context;
			mCursor = cursor;
		}

		@Override
		public int getCount() {

			return mCursor.getCount();
		}

		@Override
		public Object getItem(int position) {

			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			ImageView imageView;
			mCursor.moveToPosition(position);
			mContactList.setHapticFeedbackEnabled(true);
			final int personID = mCursor.getInt(mCursor
					.getColumnIndex(ContactsContract.Contacts._ID));
			if (convertView == null) {
				imageView = new ImageView(mContext);
				imageView.setHapticFeedbackEnabled(true);
				imageView.setLayoutParams(new GridView.LayoutParams(135, 135));
				imageView.setAdjustViewBounds(false);
				imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
				imageView.setPadding(0, 0, 0, 0);

			} else {
				imageView = (ImageView) convertView;
			}
			;
			imageView.setImageBitmap(loadContactPhoto(getContentResolver(),
					personID));

			imageView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {

					Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris
							.withAppendedId(
									ContactsContract.Contacts.CONTENT_URI,
									personID));
					startActivity(intent);

				}

			});
			return imageView;
		}

	}

	public Bitmap loadContactPhoto(ContentResolver cr, long id) {
		Uri uri = ContentUris.withAppendedId(
				ContactsContract.Contacts.CONTENT_URI, id);
		InputStream input = ContactsContract.Contacts
				.openContactPhotoInputStream(cr, uri);
		if (input == null)
			return BitmapFactory
					.decodeResource(getResources(), R.drawable.icon);
		return BitmapFactory.decodeStream(input);

	}

	public static final String TAG = "ContactManager";

	private GridView mContactList;
	private Segmentor mSegmentor;

	/**
	 * Called when the activity is first created. Responsible for initializing
	 * the UI.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contact_manager);
		mContactList = (GridView) findViewById(R.id.contactList);
		mSegmentor = getSecondLevelEmailDomains();
		populateContactList(null);

	}

	/**
	 * Populate the contact list based on account currently selected in the
	 * account spinner.
	 */
	private void populateContactList(HashSet<String> personSet) {
		// Build adapter with contact entries

		final Cursor cursor = getContacts(personSet);

		ImageAdapter adapter = new ImageAdapter(this, cursor);// ,
																// R.layout.contact_entry,
																// cursor,
																// fields, new
																// int[]
																// {R.id.contactEntryText});

		mContactList.setAdapter(adapter);
	}

	/**
	 * Obtains the contact list for the currently selected account.
	 * 
	 * @param personSet
	 * 
	 * @return A cursor for for accessing the contact list.
	 */
	private Cursor getContacts(HashSet<String> personSet) {
		Uri uri = ContactsContract.Contacts.CONTENT_URI;
		String[] projection = new String[] { ContactsContract.Contacts._ID,
				ContactsContract.Contacts.DISPLAY_NAME,
				ContactsContract.Contacts.PHOTO_ID

		};
		String personsConcat = "'0'";
		if (personSet != null)
			for (String id : personSet) {
				personsConcat += ",'" + id + "'";
			}
		String selection = personSet == null ? ""
				: (ContactsContract.Contacts._ID + " in (" + personsConcat + ") and ")
						+ ContactsContract.Contacts.PHOTO_ID + " !=''";

		String[] selectionArgs = new String[] {};
		String sortOrder = ContactsContract.Contacts.TIMES_CONTACTED
				+ " COLLATE LOCALIZED DESC";

		return managedQuery(uri, projection, selection, selectionArgs,
				sortOrder);
	}

	private Segmentor getSecondLevelEmailDomains() {
		Segmentor retval = new Segmentor();
		Cursor emailCur = managedQuery(
				ContactsContract.CommonDataKinds.Email.CONTENT_URI, null, null,
				null, null);
		while (emailCur.moveToNext()) {
			String email = emailCur
					.getString(emailCur
							.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
			String domain = email.substring(email.indexOf("@") + 1)
					.toLowerCase();
			String[] domainLevels = domain.split("\\.");
			retval
					.add(
							domainLevels[domainLevels.length - 2],
							emailCur
									.getString(emailCur
											.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)));
		}
		emailCur.close();
		return retval;
	}

	private Segmentor getCountryCodes() {
		Segmentor retval = new Segmentor();
		Cursor pCur = managedQuery(
				ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null,
				null, null);
		while (pCur.moveToNext()) {

			retval
					.add(
							pCur
									.getString(pCur
											.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)),
							pCur
									.getString(pCur
											.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)));
		}
		pCur.close();
		return retval;
	}

}
