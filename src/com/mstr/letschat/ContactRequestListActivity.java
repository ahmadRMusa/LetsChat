package com.mstr.letschat;

import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import com.mstr.letschat.adapters.ContactRequestCursorAdapter;
import com.mstr.letschat.adapters.ContactRequestCursorAdapter.OnAcceptButtonClickListener;
import com.mstr.letschat.databases.ChatContract.ContactRequestTable;
import com.mstr.letschat.model.Contact;
import com.mstr.letschat.model.UserProfile;
import com.mstr.letschat.receivers.IncomingContactRequestReceiver;
import com.mstr.letschat.service.MessageService;
import com.mstr.letschat.tasks.AcceptContactRequestTask;
import com.mstr.letschat.tasks.Response.Listener;

public class ContactRequestListActivity extends ListActivity 
	implements LoaderManager.LoaderCallbacks<Cursor>, OnAcceptButtonClickListener {
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action != null && action.equals(MessageService.ACTION_CONTACT_REQUEST_RECEIVED)) {
				abortBroadcast();
			}
		}
	};
	
	private ContactRequestCursorAdapter adapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// cancel notification if any
		((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).cancel(
				IncomingContactRequestReceiver.INCOMING_CONTACT_REQUEST_NOTIFICATION_ID);
		
		getActionBar().setHomeButtonEnabled(true);
		
		// Create an empty adapter we will use to display the loaded data.
		adapter = new ContactRequestCursorAdapter(this, null, 0);
		adapter.setOnAcceptButtonClicklistener(this);
		setListAdapter(adapter);
		
		getListView().setItemsCanFocus(false);
		
		getLoaderManager().initLoader(0, null, this);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		IntentFilter filter = new IntentFilter(MessageService.ACTION_CONTACT_REQUEST_RECEIVED);
		filter.setPriority(10);
		registerReceiver(receiver, filter);
	}
	
	protected void onPause() {
		super.onPause();
		
		unregisterReceiver(receiver);
	}
	
	@Override
	public void onAcceptButtonClick(Uri uri) {
		new AcceptContactRequestTask(new Listener<Contact>() {
			@Override
			public void onResponse(Contact result){ 
				// start contact profile activity
				Intent intent = new Intent(ContactRequestListActivity.this, UserProfileActivity.class);
				intent.putExtra(UserProfileActivity.EXTRA_DATA_NAME_USER_PROFILE, UserProfile.newInstance(result));
				startActivity(intent);
			}
			
			@Override
			public void onErrorResponse(SmackInvocationException exception) {}
			
		}, this, uri).execute();
	}
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String[] PROJECTION = new String[] {
			ContactRequestTable._ID,
			ContactRequestTable.COLUMN_NAME_JID,
			ContactRequestTable.COLUMN_NAME_NICKNAME,
			ContactRequestTable.COLUMN_NAME_STATUS};
		return new CursorLoader(this, ContactRequestTable.CONTENT_URI, PROJECTION, null, null, null);
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		adapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		adapter.swapCursor(null);
	}
}