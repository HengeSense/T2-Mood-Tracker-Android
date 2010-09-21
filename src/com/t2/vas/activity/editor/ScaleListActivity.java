package com.t2.vas.activity.editor;

import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.t2.vas.Global;
import com.t2.vas.R;
import com.t2.vas.VASAnalytics;
import com.t2.vas.activity.ABSActivity;
import com.t2.vas.db.DBAdapter;
import com.t2.vas.db.tables.Group;
import com.t2.vas.db.tables.Scale;

public class ScaleListActivity extends ABSActivity implements OnItemClickListener, OnItemLongClickListener {
	private Group currentGroup;
	private ArrayList<Scale> scaleList;
	private ArrayAdapter<String> adapter;
	private ArrayList<String> scaleListString = new ArrayList<String>();
	
//	private static final int START_SCALE_ACTIVITY = 16876;
//	private static final int START_SCALE_GATEWAY_ACTIVITY = 16875;

	private static final int SCALE_GATEWAY_ACTIVITY = 236;
	private static final int SCALE_ACTIVITY = 237;
	
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VASAnalytics.onEvent(VASAnalytics.EVENT_SCALE_LIST_ACTIVITY);

        this.setContentView(R.layout.scale_list_activity);

        Intent intent = this.getIntent();

        currentGroup = (Group)dbAdapter.getTable("group");
        currentGroup._id = intent.getLongExtra("group_id", -1);

        if(currentGroup._id < 0 || !currentGroup.load()) {
        	this.finish();
        	return;
        }

        initAdapterData();

        adapter = new ArrayAdapter<String>(
        		this,
        		android.R.layout.simple_list_item_1,
        		scaleListString
        );

        LinearLayout addViewItem = (LinearLayout)ListView.inflate(this, R.layout.simple_list_item_3, null);
        ((TextView)addViewItem.findViewById(R.id.text1)).setText(R.string.add_scale);
        ((ImageView)addViewItem.findViewById(R.id.image1)).setImageResource(android.R.drawable.ic_menu_add);

        ListView listView = (ListView)this.findViewById(R.id.list);
        listView.addHeaderView(addViewItem);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
        listView.setOnItemLongClickListener(this);
	}

	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		
		// Start the add activity if no scales are present.
        if(scaleList.size() < 1) {
        	startAddActivity(currentGroup._id);
        }
	}


	private void startAddActivity(long groupId) {
		Intent i = new Intent(this, AddScaleGateway.class);
		i.putExtra("group_id", currentGroup._id);
		this.startActivityForResult(i, SCALE_GATEWAY_ACTIVITY);
	}

	private void startEditActivity(long scaleId) {
		Intent i = new Intent(this, ScaleActivity.class);
		i.putExtra("scale_id", scaleId);
		this.startActivityForResult(i, SCALE_ACTIVITY);
	}

	private void initAdapterData() {
		scaleList = currentGroup.getScales();

		scaleListString.clear();
        for(int i = 0; i < scaleList.size(); i++) {
        	scaleListString.add(scaleList.get(i).min_label+" "+scaleList.get(i).max_label);
        }

        if(adapter != null) {
        	adapter.notifyDataSetChanged();
        }
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if(requestCode == SCALE_GATEWAY_ACTIVITY && data != null) {
			int id = data.getIntExtra("action", 0);
			if(id == AddScaleGateway.ADD_SCALE_ACTIVITY) {
				Intent i = new Intent(this, ScaleActivity.class);
				i.putExtra("group_id", currentGroup._id);
				this.startActivityForResult(i, SCALE_ACTIVITY);
				
			} else if(id == AddScaleGateway.COPY_SCALE_ACTIVITY) {
				Intent i = new Intent(this, CopyScale.class);
				i.putExtra("group_id", currentGroup._id);
				this.startActivityForResult(i, SCALE_ACTIVITY);
			}
		} else {
			initAdapterData();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		Intent i;

		// The add scale button was pressed
		if(arg2 == 0) {
			this.startAddActivity(currentGroup._id);
			return;
		}

		// Load the edit scale activity.
		startEditActivity(this.scaleList.get(arg2 - 1)._id);
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		onItemClick(arg0, arg1, arg2, arg3);
		return false;
	}
}
