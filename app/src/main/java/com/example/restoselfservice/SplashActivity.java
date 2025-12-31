package com.example.restoselfservice;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.restoselfservice.helper.LocationHelper;

public class SplashActivity extends AppCompatActivity implements LocationHelper.LocationCallback {

	private ImageView flagIndonesia;
	private TextView txtCountry, txtStatus;
	private Button btnLanjutkan;
	private ProgressBar progressBar;
	private LocationHelper locationHelper;

	// Tambah delay minimal
	private static final long MIN_DISPLAY_TIME = 2000; // 2 detik minimal
	private long startTime;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		flagIndonesia = findViewById(R.id.flagIndonesia);
		txtCountry = findViewById(R.id.txtCountry);
		txtStatus = findViewById(R.id.txtStatus);
		btnLanjutkan = findViewById(R.id.btnLanjutkan);
		progressBar = findViewById(R.id.progressBar);

		// Langsung mulai prosesnya
		startTime = System.currentTimeMillis();
		locationHelper = new LocationHelper(this, this);
		locationHelper.checkPermissionAndGetLocation();

		// Listener Tombol
		btnLanjutkan.setOnClickListener(v -> navigateToMain());
	}

	private void navigateToMain() {
		Intent intent = new Intent(SplashActivity.this, MainActivity.class);
		startActivity(intent);
		finish(); // Biar splash gak bisa di-back
	}

	@Override
	public void onLocationDetected(final boolean isInIndonesia, final String locationInfo, final String countryCode) {
		long endTime = System.currentTimeMillis();
		long elapsedTime = endTime - startTime;
		long remainingTime = MIN_DISPLAY_TIME - elapsedTime;

		// Pastikan tampilan splash minimal 2 detik (biar nggak kedip-kedip)
		new android.os.Handler(getMainLooper()).postDelayed(() -> {
			progressBar.setVisibility(View.GONE);

			if (isInIndonesia) {
				// Tampilkan Bendera & Info
				flagIndonesia.setVisibility(View.VISIBLE);
				txtCountry.setVisibility(View.VISIBLE);
				txtStatus.setText("Lokasi Terdeteksi: " + locationInfo);

				// Tampilkan Tombol Lanjutkan
				btnLanjutkan.setVisibility(View.VISIBLE);

			} else {
				// Lokasi di luar Indonesia
				txtStatus.setText("Lokasi: " + countryCode + ". Lanjutkan manual.");
				btnLanjutkan.setVisibility(View.VISIBLE);
				Toast.makeText(SplashActivity.this, "Anda berada di luar Indonesia.", Toast.LENGTH_LONG).show();
			}
		}, remainingTime > 0 ? remainingTime : 0);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		locationHelper.handlePermissionResult(requestCode, grantResults);
	}
}
