/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.example.surbhit.seemassign;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;


import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.surbhit.seemassign.profile.BleProfileActivity;
import com.example.surbhit.seemassign.profile.LoggableBleManager;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

/**
 * HRSActivity is the main Heart rate activity. It implements HRSManagerCallbacks to receive callbacks from HRSManager class. The activity supports portrait and landscape orientations. The activity
 * uses external library AChartEngine to show real time graph of HR values.
 */
// TODO The HRSActivity should be rewritten to use the service approach, like other do.
public class HRSActivity extends BleProfileActivity implements HRSManagerCallbacks {
	@SuppressWarnings("unused")
	private final String TAG = "HRSActivity";

	private final static String GRAPH_STATUS = "graph_status";
	private final static String GRAPH_COUNTER = "graph_counter";
	private final static String HR_VALUE = "hr_value";

	private final static int REFRESH_INTERVAL = 1000; // 1 second interval

	private Handler mHandler = new Handler();

	private boolean isGraphInProgress = false;

	//private GraphicalView mGraphView;
	//private LineGraphView mLineGraph;
	private TextView mHRSValue, mHRSPosition;
	private TextView mBatteryLevelView;

	private int mHrmValue = 0;
	private int mCounter = 0;

	public LineChart heartRateChart;
	public int actualHeartRateValue;


	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		setContentView(R.layout.activity_feature_hrs);
		setGUI();
	}

	private void setGUI() {
		//mLineGraph = LineGraphView.getLineGraphView();
		mHRSValue = findViewById(R.id.text_hrs_value);
		mHRSPosition = findViewById(R.id.text_hrs_position);
//		mBatteryLevelView = findViewById(R.id.battery);
		heartRateChart = findViewById(R.id.heartRateChart);

			showGraph();
	}

	private void showGraph() {
		setHeartRateChart();
	}

















	@Override
	protected int getLoggerProfileTitle() {
		return R.string.hrs_feature_title;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.hrs_about_text;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.hrs_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		return HRSManager.HR_SERVICE_UUID;
	}


	private Runnable mRepeatTask = new Runnable() {
		@Override
		public void run() {
			if (mHrmValue > 0)
			//	updateGraph(mHrmValue);
			if (isGraphInProgress)
				mHandler.postDelayed(mRepeatTask, REFRESH_INTERVAL);
		}
	};



	@Override
	protected LoggableBleManager<HRSManagerCallbacks> initializeManager() {
		final HRSManager manager = HRSManager.getInstance(getApplicationContext());
		manager.setGattCallbacks(this);
		return manager;
	}

	@Override
	public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {
		// this may notify user or show some views
	}

//	@Override
//	public void onDeviceReady(@NonNull final BluetoothDevice device) {
//		startShowGraph();
//	}

	@Override
	public void onBatteryLevelChanged(@NonNull final BluetoothDevice device, final int batteryLevel) {
		runOnUiThread(() -> mBatteryLevelView.setText(getString(R.string.battery, batteryLevel)));
	}

	@Override
	public void onBodySensorLocationReceived(@NonNull final BluetoothDevice device, final int sensorLocation) {
		runOnUiThread(() -> {
			if (sensorLocation >= SENSOR_LOCATION_FIRST && sensorLocation <= SENSOR_LOCATION_LAST) {
				mHRSPosition.setText(getResources().getStringArray(R.array.hrs_locations)[sensorLocation]);
			} else {
				mHRSPosition.setText(R.string.hrs_location_other);
			}
		});
	}

	@Override
	public void onHeartRateMeasurementReceived(@NonNull final BluetoothDevice device, final int heartRate,
											   @Nullable final Boolean contactDetected,
											   @Nullable final Integer energyExpanded,
											   @Nullable final List<Integer> rrIntervals) {
		mHrmValue = heartRate;
		actualHeartRateValue=mHrmValue;
		addHeartRateEntry();
		runOnUiThread(() -> mHRSValue.setText(getString(R.string.hrs_value, heartRate)));
	}

	@Override
	public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
		super.onDeviceDisconnected(device);
		runOnUiThread(() -> {
			mHRSValue.setText(R.string.not_available_value);
			mHRSPosition.setText(R.string.not_available);
			mBatteryLevelView.setText(R.string.not_available);

		});
	}

	@Override
	protected void setDefaultUI() {
		mHRSValue.setText(R.string.not_available_value);
		mHRSPosition.setText(R.string.not_available);

	}





	public void setHeartRateChart() {
		heartRateChart.getDescription().setEnabled(true);
		heartRateChart.setDrawGridBackground(false);
		heartRateChart.setBackgroundColor(Color.TRANSPARENT);

		LineData heartRateData = new LineData();
		heartRateChart.setData(heartRateData);
		heartRateChart.setMinimumHeight(350);

		XAxis xAxis = heartRateChart.getXAxis();
		xAxis.setTextColor(Color.BLACK);
		xAxis.setDrawGridLines(false);
		xAxis.setAvoidFirstLastClipping(true);
		xAxis.setEnabled(true);

		YAxis yAxis = heartRateChart.getAxisLeft();
		yAxis.setTextColor(Color.BLACK);
		yAxis.setAxisMaximum(140f);
		yAxis.setAxisMinimum(20f);
		yAxis.setDrawGridLines(true);

		Legend legend = heartRateChart.getLegend();
		legend.setEnabled(false);

		YAxis rightAxis = heartRateChart.getAxisRight();
		rightAxis.setEnabled(false);
	}

	private void addHeartRateEntry() {
		LineData data = heartRateChart.getData();

		if (data != null) {
			ILineDataSet set = data.getDataSetByIndex(0);

			if (set == null) {
				set = createSet();
				data.addDataSet(set);
			}
			data.addEntry(new Entry(set.getEntryCount(), actualHeartRateValue), 0);
			data.notifyDataChanged();

			heartRateChart.notifyDataSetChanged();
			heartRateChart.setVisibleXRangeMaximum(120);
			heartRateChart.moveViewToX(data.getEntryCount());
		}
	}

	/**
	 * Set the parameters for the LineDataSet
	 * @return a {@link LineDataSet} for the heart rate chart
	 */
	private LineDataSet createSet() {
		LineDataSet set = new LineDataSet(null, "Heart Rate Data");

		set.setAxisDependency(YAxis.AxisDependency.LEFT);
		set.setColor(Color.RED);
		set.setCircleColor(Color.RED);
		set.setLineWidth(2f);
		set.setCircleRadius(1f);
		set.setFillAlpha(65);
		set.setFillColor(Color.RED);
		set.setHighLightColor(Color.RED);
		set.setValueTextColor(Color.RED);
		set.setValueTextSize(9f);
		set.setDrawValues(false);
		return set;
	}






















}
