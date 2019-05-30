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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import com.example.surbhit.seemassign.battery.BatteryManager;

import java.util.List;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import no.nordicsemi.android.ble.common.callback.hr.BodySensorLocationDataCallback;
import no.nordicsemi.android.ble.common.callback.hr.HeartRateMeasurementDataCallback;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.log.LogContract;
//import no.nordicsemi.android.nrftoolbox.battery.BatteryManager;
import com.example.surbhit.seemassign.parser.BodySensorLocationParser;
import com.example.surbhit.seemassign.parser.HeartRateMeasurementParser;

/**
 * HRSManager class performs BluetoothGatt operations for connection, service discovery,
 * enabling notification and reading characteristics.
 * All operations required to connect to device with BLE Heart Rate Service and reading
 * heart rate values are performed here.
 */
public class HRSManager extends BatteryManager<HRSManagerCallbacks> {
	static final UUID HR_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
	private static final UUID BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID = UUID.fromString("00002A38-0000-1000-8000-00805f9b34fb");
	private static final UUID HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");


	private BluetoothGattCharacteristic mHeartRateCharacteristic, mBodySensorLocationCharacteristic;

	private static HRSManager managerInstance = null;

	/**
	 * Singleton implementation of HRSManager class.
	 */
	public static synchronized HRSManager getInstance(final Context context) {
		if (managerInstance == null) {
			managerInstance = new HRSManager(context);
		}
		return managerInstance;
	}

	private HRSManager(final Context context) {
		super(context);
	}

	@NonNull
	@Override
	protected BatteryManagerGattCallback getGattCallback() {
		return mGattCallback;
	}

	/**
	 * BluetoothGatt callbacks for connection/disconnection, service discovery,
	 * receiving notification, etc.
	 */
	private final BatteryManagerGattCallback mGattCallback = new BatteryManagerGattCallback() {

		@Override
		protected void initialize() {
			super.initialize();
			readCharacteristic(mBodySensorLocationCharacteristic)
					.with(new BodySensorLocationDataCallback() {
						@Override
						public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
							log(LogContract.Log.Level.APPLICATION, "\"" + BodySensorLocationParser.parse(data) + "\" received");
							super.onDataReceived(device, data);
						}

						@Override
						public void onBodySensorLocationReceived(@NonNull final BluetoothDevice device,
																 final int sensorLocation) {
							mCallbacks.onBodySensorLocationReceived(device, sensorLocation);
						}
					})
					.fail((device, status) -> log(Log.WARN, "Body Sensor Location characteristic not found"))
					.enqueue();
			setNotificationCallback(mHeartRateCharacteristic)
					.with(new HeartRateMeasurementDataCallback() {
						@Override
						public void onDataReceived(@NonNull final BluetoothDevice device, @NonNull final Data data) {
							log(LogContract.Log.Level.APPLICATION, "\"" + HeartRateMeasurementParser.parse(data) + "\" received");
							super.onDataReceived(device, data);
						}

						@Override
						public void onHeartRateMeasurementReceived(@NonNull final BluetoothDevice device,
																   final int heartRate,
																   @Nullable final Boolean contactDetected,
																   @Nullable final Integer energyExpanded,
																   @Nullable final List<Integer> rrIntervals) {
							mCallbacks.onHeartRateMeasurementReceived(device, heartRate, contactDetected, energyExpanded, rrIntervals);
						}
					});
			enableNotifications(mHeartRateCharacteristic).enqueue();
		}

		@Override
		protected boolean isRequiredServiceSupported(@NonNull final BluetoothGatt gatt) {
			final BluetoothGattService service = gatt.getService(HR_SERVICE_UUID);
			if (service != null) {
				mHeartRateCharacteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_CHARACTERISTIC_UUID);
			}
			return mHeartRateCharacteristic != null;
		}

		@Override
		protected boolean isOptionalServiceSupported(@NonNull final BluetoothGatt gatt) {
			super.isOptionalServiceSupported(gatt);
			final BluetoothGattService service = gatt.getService(HR_SERVICE_UUID);
			if (service != null) {
				mBodySensorLocationCharacteristic = service.getCharacteristic(BODY_SENSOR_LOCATION_CHARACTERISTIC_UUID);
			}
			return mBodySensorLocationCharacteristic != null;
		}

		@Override
		protected void onDeviceDisconnected() {
			super.onDeviceDisconnected();
			mBodySensorLocationCharacteristic = null;
			mHeartRateCharacteristic = null;
		}
	};
}
