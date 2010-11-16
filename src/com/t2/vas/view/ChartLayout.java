package com.t2.vas.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.t2.chart.widget.Chart;
import com.t2.vas.R;
import com.t2.vas.db.tables.Scale;

public class ChartLayout extends LinearLayout {
	private static final String TAG = "VAS";
	Chart chart;
	private TextView yMaxLabel;
	private TextView yMinLabel;
	private HorizontalScrollView scroll;
	private Scale scale;
	private LinearLayout chartWrapper;

	private boolean showLabels = true;
	private double maxYValue = 0;

	public ChartLayout(Context context) {
		super(context);
	}

	public ChartLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();

		chart = (Chart)this.findViewById(R.id.chart);
		yMaxLabel = (TextView)this.findViewById(R.id.max);
		yMinLabel = (TextView)this.findViewById(R.id.min);
		//keyTable = (TableLayout)this.findViewById(R.id.key);
		chartWrapper = (LinearLayout)this.findViewById(R.id.chartWrapper);
	}

	public void setShowLabels(boolean showLabels) {
		this.showLabels = showLabels;
		if(this.showLabels) {
			this.yMaxLabel.setVisibility(View.VISIBLE);
			this.yMinLabel.setVisibility(View.VISIBLE);
		} else {
			this.yMaxLabel.setVisibility(View.INVISIBLE);
			this.yMinLabel.setVisibility(View.INVISIBLE);
		}
	}

	public boolean isShowLabels() {
		return showLabels;
	}

	public void setYMaxLabel(String s) {
		this.yMaxLabel.setText(s);
	}

	public void setYMinLabel(String s) {
		this.yMinLabel.setText(s);
	}

	public int getLabelsColor() {
		return this.yMaxLabel.getTextColors().getDefaultColor();
	}

	public void setLabelsColor(int color) {
		this.yMaxLabel.setTextColor(color);
		this.yMinLabel.setTextColor(color);
	}

	public Chart getChart() {
		return this.chart;
	}

	public void setScale(Scale scale) {
		this.scale = scale;
	}

	public Scale getScale() {
		return scale;
	}
	
	public void setMaxYValue(double d) {
		this.chart.setMaxYValue(d);
	}
}
