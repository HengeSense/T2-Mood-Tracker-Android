package com.t2.vas.activity.editor;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.t2.vas.Global;
import com.t2.vas.R;
import com.t2.vas.VASAnalytics;
import com.t2.vas.activity.ABSActivity;
import com.t2.vas.db.DBAdapter;
import com.t2.vas.db.tables.Group;

public class GroupActivity extends ABSActivity implements OnClickListener {
	private Group currentGroup;
	private Toast toastPopup;

	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VASAnalytics.onEvent(VASAnalytics.EVENT_GROUP_ACTIVITY);
        
        // init global variables.
		currentGroup = ((Group)dbAdapter.getTable("group")).newInstance();
		toastPopup = Toast.makeText(this, R.string.activity_group_saved, 2000);

		Intent intent = this.getIntent();

		currentGroup._id = intent.getLongExtra("group_id", -1);

		// Load the note from the DB
		if(currentGroup._id > 0) {
			currentGroup.load();
		}

        this.setContentView(R.layout.group_activity);
        this.findViewById(R.id.cancelButton).setOnClickListener(this);
		this.findViewById(R.id.saveButton).setOnClickListener(this);
		//this.findViewById(R.id.deleteButton).setOnClickListener(this);



		// Set the note text
		((TextView)this.findViewById(R.id.title)).setText(currentGroup.title);

		/*// This is a new group, remove the delete button.
		if(currentGroup._id <= 0) {
			((ViewGroup)this.findViewById(R.id.deleteButton).getParent()).removeView(
					this.findViewById(R.id.deleteButton)
			);
		}*/

		// Focus on the text box will result in the keyboard appearing.
		InputMethodManager imm = (InputMethodManager)this.getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.showSoftInput(this.findViewById(R.id.title), 0);
		((TextView)this.findViewById(R.id.title)).requestFocus();
	}

	@Override
	public void onClick(View v) {
		Intent i;
		String mode;

		switch(v.getId()) {
			/*// Start the delete intent
			case R.id.deleteButton:
				i = new Intent(this, DeleteGroupActivity.class);
				i.putExtra("mode", "delete");
				i.putExtra("group_id", currentGroup._id);

				this.startActivity(i);
				this.finish();
				break;
*/
			// exit this activity
			case R.id.cancelButton:
				this.setResult(Activity.RESULT_CANCELED);
				this.finish();
				break;

			// save the note and exit this activity
			case R.id.saveButton:
				if(currentGroup._id > 0) {
					this.getIntent().putExtra("mode", "update");
				} else {
					this.getIntent().putExtra("mode", "insert");
				}

				currentGroup.title = ((TextView)this.findViewById(R.id.title)).getText().toString().trim();
				currentGroup.save();

				toastPopup.show();

				i = new Intent();
				i.putExtra("group_id", currentGroup._id);
				this.setResult(Activity.RESULT_OK, i);
				this.finish();
				break;
		}
	}
}
