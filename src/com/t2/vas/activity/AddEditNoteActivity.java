package com.t2.vas.activity;

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.DatePicker.OnDateChangedListener;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.t2.vas.R;
import com.t2.vas.VASAnalytics;
import com.t2.vas.db.tables.Note;

public class AddEditNoteActivity extends ABSNavigation implements OnClickListener, OnDateChangedListener {
	public static final String EXTRA_NOTE_ID = "noteId";
	private Note currentNote;
	private Toast toastPopup;
	private DatePicker datePicker;
	private TimePicker timePicker;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		VASAnalytics.onEvent(VASAnalytics.EVENT_ADD_EDIT_NOTE_ACTIVITY);

		// init global variables.
		currentNote = ((Note)dbAdapter.getTable("note")).newInstance();
		toastPopup = Toast.makeText(this, R.string.note_saved, 2000);

		// init the local variables;
		Calendar cal = Calendar.getInstance();
		Intent intent = this.getIntent();
		long dateTimestamp = intent.getLongExtra("timestamp", cal.getTimeInMillis());

		currentNote._id = intent.getLongExtra(EXTRA_NOTE_ID, -1);

		// Load the note from the DB
		if(currentNote._id > 0) {
			currentNote.load();

		// This is a new note, set the date.
		} else {
			cal.setTimeInMillis(dateTimestamp);
			currentNote.timestamp = cal.getTimeInMillis();
		}

		cal.setTimeInMillis(currentNote.timestamp);

		// Init the view
		this.setContentView(R.layout.add_edit_note_activity);
		//this.findViewById(R.id.cancelButton).setOnClickListener(this);
		//this.findViewById(R.id.saveButton).setOnClickListener(this);
		this.findViewById(R.id.deleteButton).setOnClickListener(this);
		this.setRightButtonText(getString(R.string.save));

		// Set the date
		datePicker = (DatePicker)this.findViewById(R.id.date);
		datePicker.init(
			cal.get(Calendar.YEAR),
			cal.get(Calendar.MONTH),
			cal.get(Calendar.DAY_OF_MONTH),
			this
		);
		
		// Set the time
		timePicker = (TimePicker)this.findViewById(R.id.time);
		timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
		timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
		
		// Set the note text
		((TextView)this.findViewById(R.id.note)).setText(currentNote.note);

		// This is a new note, remove the delete button.
		if(currentNote._id <= 0) {
			((ViewGroup)this.findViewById(R.id.deleteButton).getParent()).removeView(
					this.findViewById(R.id.deleteButton)
			);
		// This is an existing note, don't show the keyboard by default.
		} else {
			// Hide the keyboard unless the user chooses a text view.
			getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
			// Start the delete intent
			case R.id.deleteButton:
				new AlertDialog.Builder(this)
					.setMessage(R.string.note_delete_confirm)
					.setPositiveButton(R.string.yes, new Dialog.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							currentNote.delete();
							finish();
						}
					})
					.setNegativeButton(R.string.no, new Dialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					})
					.create()
					.show();
				break;
		}
	}
	
	@Override
	protected void onRightButtonPresed() {
		Calendar cal = Calendar.getInstance();

		cal.set(
			datePicker.getYear(),
			datePicker.getMonth(),
			datePicker.getDayOfMonth(),
			timePicker.getCurrentHour(),
			timePicker.getCurrentMinute()
		);

		currentNote.note = ((TextView)this.findViewById(R.id.note)).getText().toString();
		currentNote.timestamp = cal.getTimeInMillis();
		currentNote.save();

		toastPopup.show();

		this.getIntent().putExtra(EXTRA_NOTE_ID, currentNote._id);
		this.setResult(Activity.RESULT_OK, this.getIntent());
		this.finish();
	}

	@Override
	public void onDateChanged(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
	}
}
