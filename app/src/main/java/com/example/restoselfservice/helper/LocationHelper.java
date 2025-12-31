package com.example.restoselfservice.helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.List;
import java.util.Locale;

public class LocationHelper {

	private static final String TAG = "LocationHelper";
	private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
	private final Activity activity;
	private final LocationCallback callback;

	public interface LocationCallback {
		void onLocationDetected(boolean isInIndonesia, String locationInfo, String countryCode);
	}

	public LocationHelper(Activity activity, LocationCallback callback) {
		this.activity = activity;
		this.callback = callback;
	}

	public void checkPermissionAndGetLocation() {
		if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
			!= PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(activity,
				new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
				LOCATION_PERMISSION_REQUEST_CODE);
		} else {
			getUserLocation();
		}
	}

	@SuppressLint("MissingPermission")
	public void getUserLocation() {
		LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

		// -- LOGIC PENGAMBILAN LOKASI LEBIH BAIK --
		Location location = null;
		try {
			// Coba GPS Provider (Paling akurat)
			if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				Log.d(TAG, "Attempting GPS Provider: " + (location != null));
			}
		} catch (Exception e) { Log.e(TAG, "GPS Error", e); }

		if (location == null) {
			try {
				// Coba Network Provider (Sering dipakai di emulator)
				if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
					location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
					Log.d(TAG, "Attempting Network Provider: " + (location != null));
				}
			} catch (Exception e) { Log.e(TAG, "Network Error", e); }
		}

		if (location == null) {
			try {
				// Coba Passive Provider (Fallback terakhir, sering dapat lokasi Mocked/Injected)
				if (locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
					location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
					Log.d(TAG, "Attempting Passive Provider: " + (location != null));
				}
			} catch (Exception e) { Log.e(TAG, "Passive Error", e); }
		}
		// -- END OF LOGIC --

		if (location != null) {
			Log.d(TAG, "Location Found: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
			geocodeLocation(location);
		} else {
			Log.e(TAG, "Gagal mendapatkan lokasi dari semua provider.");
			activity.runOnUiThread(() -> Toast.makeText(activity, "Gagal mendeteksi lokasi dari provider", Toast.LENGTH_SHORT).show());
			callback.onLocationDetected(false, "Gagal Provider", "Unknown");
		}
	}

	private void geocodeLocation(Location location) {
		// Harus dijalankan di thread terpisah agar UI tidak nge-lag (Blocking)
		new Thread(() -> {
			try {
				// Gunakan Locale default (ID/EN)
				Geocoder geocoder = new Geocoder(activity, Locale.getDefault());
				List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

				if (addresses != null && !addresses.isEmpty()) {
					Address address = addresses.get(0);
					String countryCode = address.getCountryCode();
					boolean isIndonesia = "ID".equalsIgnoreCase(countryCode);

					// Ambil detail lokasi (Kecamatan atau Kota)
					String subAdminArea = address.getSubAdminArea(); // Biasanya Kecamatan/Kota
					String adminArea = address.getAdminArea();       // Biasanya Provinsi

					String locationInfo;
					if (subAdminArea != null) {
						locationInfo = subAdminArea;
					} else if (adminArea != null) {
						locationInfo = adminArea;
					} else {
						locationInfo = "Lokasi Umum";
					}

					Log.d(TAG, "Geocode Success. Country: " + countryCode + ", Info: " + locationInfo);
					activity.runOnUiThread(() -> callback.onLocationDetected(isIndonesia, locationInfo, countryCode));
				} else {
					Log.e(TAG, "Geocoder: Tidak ada alamat ditemukan.");
					activity.runOnUiThread(() -> callback.onLocationDetected(false, "No Address", "Unknown"));
				}
			} catch (Exception e) {
				// Geocoder sering error di emulator, handle di sini
				Log.e(TAG, "Geocoder Exception:", e);
				activity.runOnUiThread(() -> Toast.makeText(activity, "Geocoder error: " + e.getMessage(), Toast.LENGTH_LONG).show());
				activity.runOnUiThread(() -> callback.onLocationDetected(false, "Geocoder Error", "Unknown"));
			}
		}).start();
	}

	// dipanggil dari Activity onRequestPermissionsResult
	public void handlePermissionResult(int requestCode, int[] grantResults) {
		if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
			if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				getUserLocation();
			} else {
				Toast.makeText(activity, "Izin lokasi ditolak. Lanjutkan manual.", Toast.LENGTH_SHORT).show();
				callback.onLocationDetected(false, "Izin Ditolak", "Unknown");
			}
		}
	}
}
