package com.t2.vas.activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.achartengine.chart.LineChart;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;

import com.t2.vas.ArraysExtra;
import com.t2.vas.Global;
import com.t2.vas.MathExtra;
import com.t2.vas.R;
import com.t2.vas.SharedPref;
import com.t2.vas.data.DataProvider;
import com.t2.vas.data.NotesDataProvider;
import com.t2.vas.db.tables.Note;
import com.t2.vas.view.OffsetGraphicalChartView;
import com.t2.vas.view.SimpleCursorDateSectionAdapter;
import com.t2.vas.view.ToggledButton;

public abstract class ABSResultsActivity extends ABSNavigationActivity implements OnClickListener, OnItemClickListener, GestureDetector.OnGestureListener {
	public static final String EXTRA_TIME_START = "timeStart";
	public static final String EXTRA_CALENDAR_FIELD = "calendarField";
	public static final String EXTRA_REVERSE_DATA = "reverseData";

	private static final int ADD_EDIT_NOTE_ACTIVITY = 30958;
	private static final String NOTES_CACHE = "notes";

	private static final String KEY_NAME = "results_visible_ids_";

	private static final int DIRECTION_PREVIOUS = -1;
	private static final int DIRECTION_NONE = 0;
	private static final int DIRECTION_NEXT = 1;

	private static final int KEYS_TAB = 1;
	private static final int NOTES_TAB = 2;
	private int selectedTab = 0;

	private ListView keysList;
	private ListView notesList;

	private ArrayList<KeyItem> keyItems;

	private KeyItemAdapter keysAdapter;
	private SimpleCursorDateSectionAdapter notesAdapter;

	private ToggledButton keysTabButton;
	private ToggledButton notesTabButton;

	protected Calendar startCal;
	protected Calendar endCal;
	protected int calendarField;
	private TextView monthNameTextView;

	SimpleDateFormat monthNameFormatter = new SimpleDateFormat("MMMM, yyyy");
	private ViewSwitcher chartSwitcher;
	private Animation slideInFromLeftAnimation;
	private Animation slideOutToRightAnimation;
	private Animation slideInFromRightAnimation;
	private Animation slideOutToLeftAnimation;
	private Animation fadeInAnimation;
	private Animation fadeOutAnimation;
	private GestureDetector gestureDetector;
	private DataProvider dataProvider;
	private Cursor notesCursor;
	private NotesDataProvider notesDataProvider;
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	private DataPointCache dataPointCache;
	protected boolean reverseLabels;
	private ViewGroup chartWrapper;
	private ViewGroup chartLabels;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

		dataPointCache = new DataPointCache();
		notesDataProvider = new NotesDataProvider(dbAdapter);
		dataProvider = this.getDataProvider();
		gestureDetector = new GestureDetector(this, this);

		long startTime = this.getIntent().getLongExtra(EXTRA_TIME_START, 0);
		if(startTime == 0) {
			startTime = Calendar.getInstance().getTimeInMillis();
		}

		Intent intent = this.getIntent();
		this.calendarField = intent.getIntExtra(EXTRA_CALENDAR_FIELD, Calendar.DAY_OF_MONTH);
		this.reverseLabels = intent.getBooleanExtra(EXTRA_REVERSE_DATA, false);

		// Set the time ranges.
		startCal = Calendar.getInstance();
		startCal.setTimeInMillis(MathExtra.roundTime(startTime, calendarField));
		startCal.set(calendarField, startCal.getMinimum(calendarField));

		endCal = Calendar.getInstance();
		endCal.setTimeInMillis(startCal.getTimeInMillis());
		endCal.add(Calendar.MONTH, 1);


		// Set the content view.
		this.setContentView(R.layout.abs_results_activity);
		this.monthNameTextView = (TextView) this.findViewById(R.id.monthName);
		//this.chartsContainer = (ViewGroup) this.findViewById(R.id.chartContainer);
		//this.backgroundChartContainer = (ViewGroup) this.findViewById(R.id.backgroundChartContainer);
		//this.foregroundChartContainer = (ViewGroup) this.findViewById(R.id.foregroundChartContainer);
		this.chartSwitcher = (ViewSwitcher) this.findViewById(R.id.chartSwitcher);
		this.chartWrapper = (ViewGroup) this.findViewById(R.id.chartWrapper);
		this.chartLabels = (ViewGroup) this.findViewById(R.id.chartLabels);

		// add extra button
		this.setRightButtonText(getString(R.string.add_note));

		// init animations
		this.slideInFromLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
		this.slideOutToRightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
		this.slideInFromRightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
		this.slideOutToLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
		this.fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		this.fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_out);

		// Set the month name.
		this.monthNameTextView.setText(monthNameFormatter.format(startCal.getTime()));

		// Prepare the notes adapter.
		notesCursor = new Note(dbAdapter).queryForNotes(-1, -1, "timestamp DESC");
		this.startManagingCursor(notesCursor);
		notesAdapter = SimpleCursorDateSectionAdapter.buildNotesAdapter(
				this, 
				notesCursor,
				new SimpleDateFormat(Global.NOTES_LONG_DATE_FORMAT),
				new SimpleDateFormat(Global.NOTES_SECTION_DATE_FORMAT)
				);

		// Prepare the keys adapter.
		keyItems = getKeyItems();

		// set the color and visibility for each key item.
		ArrayList<Long> visibleIds = getVisibleIds(getSettingSuffix());
		int keyCount = keyItems.size();
		for(int i = 0; i < keyCount; ++i) {
			KeyItem item = keyItems.get(i);

			item.color = getKeyColor(i, keyCount);
			item.visible = visibleIds.contains(item.id);
		}

		keysAdapter = new KeyItemAdapter(this, getKeyItemViewType(), keyItems);

		keysList = (ListView) this.findViewById(R.id.keysList);
		keysList.setAdapter(keysAdapter);
		if(isKeyItemsClickable()) {
			keysList.setOnItemClickListener(this);
		}

		notesList = (ListView) this.findViewById(R.id.notesList);
		notesList.setAdapter(notesAdapter);
		notesList.setOnItemClickListener(this);
		notesList.setFastScrollEnabled(true);

		keysTabButton = (ToggledButton)this.findViewById(R.id.keysTabButton);
		keysTabButton.setOnClickListener(this);
		keysTabButton.setText(getKeyTabText());

		notesTabButton = (ToggledButton)this.findViewById(R.id.notesTabButton);
		notesTabButton.setOnClickListener(this);

		this.findViewById(R.id.monthMinusButton).setOnClickListener(this);
		this.findViewById(R.id.monthPlusButton).setOnClickListener(this);

		generateChart(DIRECTION_NEXT);

		if(savedInstanceState != null) {
			int selTab = savedInstanceState.getInt("selectedTab");
			if(selTab == NOTES_TAB)  {
				showNotesTab();
			} else {
				showKeysTab();
			}
		} else {
			showKeysTab();
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("selectedTab", selectedTab);
	}

	@Override
	protected void onRightButtonPressed() {
		long noteTimestamp = startCal.getTimeInMillis();
		Calendar nowCal = Calendar.getInstance();
		if(nowCal.get(Calendar.MONTH) == startCal.get(Calendar.MONTH)) {
			noteTimestamp = nowCal.getTimeInMillis();
		}

		Intent i = new Intent(this, AddEditNoteActivity.class);
		i.putExtra(AddEditNoteActivity.EXTRA_TIMESTAMP, noteTimestamp);
		this.startActivityForResult(i, ADD_EDIT_NOTE_ACTIVITY);
	}



	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ADD_EDIT_NOTE_ACTIVITY) {
			if(resultCode == RESULT_OK) {
				dataPointCache.clearCache(NOTES_CACHE);
				this.generateChart(DIRECTION_NONE);
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}

	protected abstract String getKeyTabText();

	protected abstract String getSettingSuffix();

	protected abstract ArrayList<KeyItem> getKeyItems();

	protected abstract int getKeyItemViewType();

	protected abstract DataProvider getDataProvider();

	private ArrayList<DataPoint> loadData(KeyItem item, long startTime, long endTime, int calendarGroupByField) {
		// try to retrieve the data from cache
		String cacheKey = "key-"+item.id;
		ArrayList<DataPoint> outPoints = dataPointCache.getCache(cacheKey, startTime, endTime, calendarGroupByField);
		if(outPoints != null) {
			return outPoints;
		}

		// rebuild the cache.
		outPoints = new ArrayList<DataPoint>();
		ArrayList<Long> xValues = getDataPoints(startTime, endTime, calendarGroupByField);
		LinkedHashMap<Long,DataPoint> points = new LinkedHashMap<Long, DataPoint>();

		for(int i = 0; i < xValues.size(); ++i) {
			DataPoint dp = new DataPoint(xValues.get(i), 50);
			points.put(xValues.get(i), dp);
			outPoints.add(dp);
		}

		HashMap<Long,Double> data = dataProvider.getData(item.id, startTime, endTime);
		for(Entry<Long,Double> entry: data.entrySet()) {
			long roundedTime = MathExtra.roundTime(entry.getKey(), calendarGroupByField);
			DataPoint dp = points.get(roundedTime);
			if(dp != null) {
				dp.addValue(getValue(item, entry.getValue()));
			}
		}

		// set the cache
		dataPointCache.setCache(cacheKey, outPoints, startTime, endTime, calendarGroupByField);

		return outPoints;
	}

	protected double getValue(KeyItem item, double value) {
		return value;
	}

	private ArrayList<DataPoint> loadNotesData(long startTime, long endTime, int calendarGroupByField) {
		ArrayList<DataPoint> dataPoints = dataPointCache.getCache(NOTES_CACHE, startTime, endTime, calendarGroupByField);
		if(dataPoints != null) {
			return dataPoints;
		}

		// rebuild the notes 
		dataPoints = new ArrayList<DataPoint>();
		LinkedHashMap<Long, Double> data = notesDataProvider.getData(0, startTime, endTime);
		for(Entry<Long,Double> entry: data.entrySet()) {
			dataPoints.add(new DataPoint(entry.getKey(), entry.getValue()));
		}

		// set the cache
		dataPointCache.setCache(NOTES_CACHE, dataPoints, startTime, endTime, calendarGroupByField);

		return dataPoints;
	}

	private ArrayList<Long> getDataPoints(long startTime, long endTime, int calendarGroupByField) {
		Calendar startCal = Calendar.getInstance();
		Calendar endCal = Calendar.getInstance();
		startCal.setTimeInMillis(startTime);
		endCal.setTimeInMillis(endTime);

		startCal.setTimeInMillis(MathExtra.roundTime(startCal.getTimeInMillis(), calendarGroupByField));
		endCal.setTimeInMillis(MathExtra.roundTime(endCal.getTimeInMillis(), calendarGroupByField));

		ArrayList<Long> dataPoints = new ArrayList<Long>();
		Calendar runningCal = Calendar.getInstance();
		runningCal.setTimeInMillis(startCal.getTimeInMillis());
		while(true) {
			if(runningCal.getTimeInMillis() >= endTime) {
				break;
			}

			switch(calendarGroupByField) {
			case Calendar.MONTH:
				dataPoints.add(runningCal.getTimeInMillis());
				runningCal.add(Calendar.MONTH, 1);
				break;
			case Calendar.DAY_OF_MONTH:
				dataPoints.add(runningCal.getTimeInMillis());
				runningCal.add(Calendar.DAY_OF_MONTH, 1);
				break;
			}
		}

		return dataPoints;
	}

	protected abstract boolean isKeyItemsClickable();

	protected int getKeyColor(int currentIndex, int totalCount) {
		float hue = currentIndex / (1.00f * totalCount) * 360.00f;

		return Color.HSVToColor(
				255,
				new float[]{
						hue,
						1.0f,
						1.0f
				}
				);
	}

	protected final void showKeysTab() {
		selectedTab = KEYS_TAB;
		keysTabButton.setChecked(true);
		notesTabButton.setChecked(false);

		keysList.setVisibility(View.VISIBLE);
		notesList.setVisibility(View.INVISIBLE);
	}

	protected final void showNotesTab() {
		selectedTab = NOTES_TAB;
		keysTabButton.setChecked(false);
		notesTabButton.setChecked(true);

		keysList.setVisibility(View.INVISIBLE);
		notesList.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		if(arg0 == keysList) {
			onKeysItemClicked(keyItems.get(arg2), arg1, arg2, arg3);

		} else if(arg0 == notesList) {
			onNotesItemClicked(arg1, arg2, arg3);
		}
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.monthMinusButton:
			monthMinusButtonPressed();
			break;

		case R.id.monthPlusButton:
			monthPlusButtonPressed();
			break;

		case R.id.keysTabButton:
			showKeysTab();
			break;

		case R.id.notesTabButton:
			showNotesTab();
			break;
		}
	}

	protected void monthMinusButtonPressed() {
		startCal.add(Calendar.MONTH, -1);
		endCal.add(Calendar.MONTH, -1);
		this.monthNameTextView.setText(monthNameFormatter.format(startCal.getTime()));
		generateChart(DIRECTION_PREVIOUS);

		notesList.setSelection(notesAdapter.getPositionForTimestamp(endCal.getTimeInMillis()));
	}

	protected void monthPlusButtonPressed() {
		startCal.add(Calendar.MONTH, 1);
		endCal.add(Calendar.MONTH, 1);
		this.monthNameTextView.setText(monthNameFormatter.format(startCal.getTime()));
		generateChart(DIRECTION_NEXT);

		notesList.setSelection(notesAdapter.getPositionForTimestamp(endCal.getTimeInMillis()));
	}

	protected abstract void onKeysItemClicked(KeyItem keyItem, View view, int pos, long id);

	private void onNotesItemClicked(View view, int pos, long id) {
		Note note = new Note(dbAdapter);
		note._id = id;
		note.load();

		// Show a dialog with the full note.
		new AlertDialog.Builder(this)
		.setTitle(getString(R.string.note_details_title))
		.setMessage(note.note)
		.setCancelable(true)
		.setPositiveButton(R.string.close, new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		})
		.create()
		.show();
	}

	private void onKeyToggleButtonCheckedChanged() {
		saveVisibleKeyIds();
		generateChart(DIRECTION_NONE);
	}

	private void generateChart(int direction) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		LineChart chart = new LineChart(dataSet, renderer);

		//Log.v(TAG, "StartCal:"+startCal.getTime() +" "+ startCal.get(Calendar.MILLISECOND));
		//Log.v(TAG, "EndCal:"+endCal.getTime() +" "+ endCal.get(Calendar.MILLISECOND));

		// Build the data series for each enabled key.
		ArrayList<DataPoint> dataPoints = null;
		long startTime = startCal.getTimeInMillis();
		long endTime = endCal.getTimeInMillis();
		for(int i = 0; i < keyItems.size(); ++i) {
			KeyItem item = keyItems.get(i);

			if(!item.visible) {
				continue;
			}

			XYSeries series = new XYSeries(item.title1);

			// Get the data points
			dataPoints = loadData(keyItems.get(i), startTime, endTime, calendarField);
			//Log.v("", "DataPoints:"+dataPoints.size());
			for(int j = 0; j < dataPoints.size(); ++j) {
				DataPoint dp = dataPoints.get(j);
				Calendar cal = Calendar.getInstance();
				cal.setTimeInMillis(dp.time);
				cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE), 0, 0);
				long stime = cal.getTimeInMillis();
				cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
						cal.get(Calendar.DATE), cal.getMaximum(Calendar.HOUR_OF_DAY),
						                                               cal.getMaximum(Calendar.MINUTE), cal.getMaximum(Calendar.SECOND));
				long etime = cal.getTimeInMillis();
				if(dp.getValues().length > 0) {

					//(HACK) Averaging corrected for Group display only - Steve Ody
					if(!(this instanceof GroupResultsActivity))
						series.add(cal.get(calendarField), dp.getAverageValue());
					else
						series.add(cal.get(calendarField), dataProvider.GetGroupAverage(item.id, stime, etime));

				}
			}

			XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
			seriesRenderer.setColor(item.color);
			seriesRenderer.setPointStyle(PointStyle.CIRCLE);
			seriesRenderer.setFillPoints(true);
			seriesRenderer.setLineWidth(2 * displayMetrics.density);

			renderer.addSeriesRenderer(seriesRenderer);
			dataSet.addSeries(series);
		}

		// Disable the switch animation.
		chartSwitcher.setInAnimation(null);
		chartSwitcher.setOutAnimation(null);

		// only contine making the chart if there is data in the series.
		if(dataSet.getSeriesCount() > 0) {

			// Make the renderer for the weekend blocks
			Calendar weekendCal = Calendar.getInstance();
			weekendCal.setTimeInMillis(dataPoints.get(0).time);

			Calendar weekCal = Calendar.getInstance();
			weekCal.setTimeInMillis(startCal.getTimeInMillis());
			int dow = weekCal.get(Calendar.DAY_OF_WEEK);
			weekCal.add(Calendar.DAY_OF_MONTH, 7 - dow + 2);

			int lastDayOfMonth = weekendCal.getActualMaximum(Calendar.DAY_OF_MONTH);
			int firstMondayOfMonth = weekCal.get(Calendar.DAY_OF_MONTH);

			renderer.setShowGrid(false);
			renderer.setAxesColor(Color.WHITE);
			renderer.setLabelsColor(Color.WHITE);
			renderer.setAntialiasing(true);
			renderer.setShowLegend(false);
			renderer.setYLabels(0);
			renderer.setXLabels(15);
			renderer.setYAxisMax(100.00);
			renderer.setYAxisMin(0.00);
			renderer.setXAxisMin(1.00);
			renderer.setXAxisMax(lastDayOfMonth);

			// Add the weekend background colors.
			for(int i = firstMondayOfMonth-18; i < lastDayOfMonth; i+=7) {
				int xStart = i;
				int xEnd = i+2;
				int y = 100;

				if(xStart < 1) {
					xStart = 1;
				}

				if(xEnd <= 1) {
					continue;
				}

				XYSeries weekSeries = new XYSeries("week "+i);
				weekSeries.add(xStart, y);
				weekSeries.add(xEnd, y);

				XYSeriesRenderer weekSeriesRenderer = new XYSeriesRenderer();
				weekSeriesRenderer.setColor(Color.TRANSPARENT);
				weekSeriesRenderer.setLineWidth(0.0f);
				weekSeriesRenderer.setFillBelowLine(true);
				weekSeriesRenderer.setFillBelowLineColor(Color.argb(10, 0, 0, 0));

				renderer.addSeriesRenderer(weekSeriesRenderer);
				dataSet.addSeries(weekSeries);
			}

			// Add an indicator showing where each note exists.
			ArrayList<DataPoint> notesPoints = loadNotesData(startTime, endTime, calendarField);
			for(int i = 0; i < notesPoints.size(); ++i) {
				DataPoint dp = notesPoints.get(i);
				Calendar tmpCal = Calendar.getInstance();
				tmpCal.setTimeInMillis(dp.time);

				// Create the series
				XYSeries noteSeries = new XYSeries("note "+i);
				noteSeries.add(tmpCal.get(calendarField), 100);

				// Create the renderer
				XYSeriesRenderer noteRenderer = new XYSeriesRenderer();
				noteRenderer.setColor(Color.YELLOW);
				noteRenderer.setPointStyle(PointStyle.TRIANGLE);
				noteRenderer.setFillPoints(true);
				noteRenderer.setLineWidth(0.0f);

				// Add the renderer and series.
				renderer.addSeriesRenderer(noteRenderer);
				dataSet.addSeries(noteSeries);
			}

			// Create the chart view.
			OffsetGraphicalChartView chartView = new OffsetGraphicalChartView(this, chart);
			if(chartSwitcher.getChildCount() > 1) {
				chartSwitcher.removeViewAt(0);
			}

			// Make the switcher appear.
			chartSwitcher.addView(chartView, LayoutParams.FILL_PARENT,LayoutParams.FILL_PARENT);
			if(chartWrapper.getVisibility() == View.INVISIBLE) {
				chartWrapper.setVisibility(View.VISIBLE);
				chartWrapper.startAnimation(fadeInAnimation);

				chartLabels.setVisibility(View.VISIBLE);
			}

			// Set the chart transition animation.
			switch(direction) {
			case DIRECTION_NONE:
				chartSwitcher.setInAnimation(null);
				chartSwitcher.setOutAnimation(null);
				break;

			case DIRECTION_NEXT:
				chartSwitcher.setInAnimation(slideInFromRightAnimation);
				chartSwitcher.setOutAnimation(slideOutToLeftAnimation);
				break;

			case DIRECTION_PREVIOUS:
				chartSwitcher.setInAnimation(slideInFromLeftAnimation);
				chartSwitcher.setOutAnimation(slideOutToRightAnimation);
				break;
			}

			chartSwitcher.showNext();

			// Fade out the existing chart to the instructions are visible.
		} else {
			chartLabels.setVisibility(View.INVISIBLE);

			chartWrapper.setVisibility(View.INVISIBLE);
			chartWrapper.startAnimation(fadeOutAnimation);

			chartSwitcher.removeAllViews();
		}
	}


	private void saveVisibleKeyIds() {
		String keySuffix = getSettingSuffix();
		ArrayList<Long> toggledIds = new ArrayList<Long>();
		for(int i = 0; i < keyItems.size(); ++i) {
			KeyItem item = keyItems.get(i);
			if(item.visible) {
				toggledIds.add(item.id);
			}
		}
		setVisibleIds(keySuffix, toggledIds);
	}

	private ArrayList<Long> getVisibleIds(String keySuffix) {
		String[] idsStrArr = SharedPref.getValues(
				sharedPref, 
				KEY_NAME+keySuffix, 
				",",
				new String[0]
				);

		return new ArrayList<Long>(
				Arrays.asList(
						ArraysExtra.toLongArray(idsStrArr)
						)
				);
	}

	private void setVisibleIds(String keySuffix, ArrayList<Long> ids) {
		SharedPref.setValues(
				sharedPref, 
				KEY_NAME+keySuffix, 
				",", 
				ArraysExtra.toStringArray(ids.toArray(new Long[ids.size()]))
				);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return gestureDetector.onTouchEvent(event);
	}

	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
			float velocityY) {
		//Log.v(TAG, "v:"+ velocityX +","+ velocityY);
		if(Math.abs(velocityY) < 50 || Math.abs(velocityX) < 200) {
			return false;
		}

		if(velocityX > 200) {
			monthMinusButtonPressed();
			return true;

		} else if(velocityX < -200) {
			monthPlusButtonPressed();
			return true;
		}

		return false;
	}

	@Override
	public void onLongPress(MotionEvent e) {

	}

	@Override
	public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
			float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent e) {

	}

	@Override
	public boolean onSingleTapUp(MotionEvent e) {
		return false;
	}


	static class KeyItem {
		public long id;
		public String title1;
		public String title2;
		public int color;
		public boolean visible;
		public boolean reverseData = false; 

		public KeyItem(long id, String title1, String title2) {
			this.id = id;
			this.title1 = title1;
			this.title2 = title2;
		}


		public HashMap<String,Object> toHashMap() {
			HashMap<String,Object> data = new HashMap<String,Object>();
			data.put("id", id);
			data.put("title1", title1);
			data.put("title2", title2);
			data.put("color", color);
			data.put("visible", visible);
			return data;
		}
	}

	class KeyItemAdapter extends ArrayAdapter<KeyItem> {
		public static final int VIEW_TYPE_ONE_LINE = 1;
		public static final int VIEW_TYPE_TWO_LINE = 2;

		private LayoutInflater layoutInflater;
		private int layoutId;

		public KeyItemAdapter(Context context, int viewType,
				List<KeyItem> objects) {
			super(context, viewType, objects);

			layoutInflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
			if(viewType == VIEW_TYPE_TWO_LINE) {
				layoutId = R.layout.list_item_result_key_2;
			} else {
				layoutId = R.layout.list_item_result_key_1;
			}
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = layoutInflater.inflate(layoutId, null);
			}

			final KeyItem item = this.getItem(position);
			TextView tv1 = (TextView)convertView.findViewById(R.id.text1);
			TextView tv2 = (TextView)convertView.findViewById(R.id.text2);
			ToggleButton tb = (ToggleButton)convertView.findViewById(R.id.showKeyToggleButton);
			View keyBox = convertView.findViewById(R.id.keyBox);

			boolean tv1Null = tv1 == null;
			boolean tv2Null = tv2 == null;
			if(reverseLabels && !tv1Null && !tv2Null) {
				if(!tv1Null) {
					tv1.setText(item.title2);
				}
				if(!tv2Null) {
					tv2.setText(item.title1);
				}
			} else {
				if(!tv1Null) {
					tv1.setText(item.title1);
				}
				if(!tv2Null) {
					tv2.setText(item.title2);
				}				
			}

			if(tb != null) {
				if(isKeyItemsClickable()) {
					tb.setFocusable(false);
				}
				tb.setOnCheckedChangeListener(null);
				tb.setChecked(item.visible);
				tb.setOnCheckedChangeListener(new OnCheckedChangeListener(){
					@Override
					public void onCheckedChanged(
							CompoundButton buttonView, boolean isChecked) {
						item.visible = isChecked;
						onKeyToggleButtonCheckedChanged();
					}
				});
			}

			if(keyBox != null) {
				keyBox.setBackgroundColor(item.color);
			}

			return convertView;
		}
	}
}
