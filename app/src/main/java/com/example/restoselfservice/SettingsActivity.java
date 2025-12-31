package com.example.restoselfservice;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

	private TextInputEditText etBaseUrl, etOldPin, etNewPin;
	private Button btnSaveUrl, btnSavePin, btnSelectPrinter;
	private TextView tvCurrentPrinter;
	private SharedPreferences prefs;
	private BluetoothAdapter bluetoothAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);

		// 1. Inisialisasi View (URL & PIN)
		etBaseUrl = findViewById(R.id.etBaseUrl);
		etOldPin = findViewById(R.id.etOldPin);
		etNewPin = findViewById(R.id.etNewPin);
		btnSaveUrl = findViewById(R.id.btnSaveUrl);
		btnSavePin = findViewById(R.id.btnSavePin);

		// 2. Inisialisasi View (Bluetooth Printer)
		btnSelectPrinter = findViewById(R.id.btnSelectPrinter);
		tvCurrentPrinter = findViewById(R.id.tvCurrentPrinter);

		prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// 3. Load Data Awal
		String currentUrl = prefs.getString("base_url", "http://10.0.2.2:8080/resto_self_service/api/");
		etBaseUrl.setText(currentUrl);
		loadSavedPrinterName();

		// 4. Listeners
		btnSaveUrl.setOnClickListener(v -> updateUrl());
		btnSavePin.setOnClickListener(v -> updatePin());
		btnSelectPrinter.setOnClickListener(v -> checkBluetoothPermissionAndShowDevices());
	}

	private void loadSavedPrinterName() {
		SharedPreferences printerPrefs = getSharedPreferences("PRINTER_SETTINGS", MODE_PRIVATE);
		String printerName = printerPrefs.getString("printer_name", "Belum Terhubung");
		tvCurrentPrinter.setText("Printer: " + printerName);
	}

	private void updateUrl() {
		String newUrl = etBaseUrl.getText().toString().trim();
		if (newUrl.isEmpty() || !newUrl.startsWith("http")) {
			Toast.makeText(this, "URL harus valid (mulai http:// atau https://)", Toast.LENGTH_SHORT).show();
			return;
		}
		prefs.edit().putString("base_url", newUrl).apply();
		Toast.makeText(this, "Base URL diperbarui! ✅", Toast.LENGTH_SHORT).show();
	}

	private void updatePin() {
		String inputOldPin = etOldPin.getText().toString();
		String inputNewPin = etNewPin.getText().toString().trim();
		String savedPin = prefs.getString("app_pin", "123123");

		if (inputOldPin.isEmpty() || inputNewPin.isEmpty()) {
			Toast.makeText(this, "Isi PIN lengkap ya!", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!inputOldPin.equals(savedPin)) {
			Toast.makeText(this, "PIN Lama Salah!", Toast.LENGTH_SHORT).show();
			return;
		}

		if (inputNewPin.length() < 6) {
			Toast.makeText(this, "PIN baru min 6 digit!", Toast.LENGTH_SHORT).show();
			return;
		}

		prefs.edit().putString("app_pin", inputNewPin).apply();
		etOldPin.setText("");
		etNewPin.setText("");
		Toast.makeText(this, "PIN diganti! 🔐", Toast.LENGTH_SHORT).show();
	}

	// --- LOGIC BLUETOOTH PRINTER ---

	private void checkBluetoothPermissionAndShowDevices() {
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "HP lo gak support Bluetooth!", Toast.LENGTH_SHORT).show();
			return;
		}

		if (!bluetoothAdapter.isEnabled()) {
			Toast.makeText(this, "Nyalain dulu Bluetooth-nya, Bro!", Toast.LENGTH_SHORT).show();
			return;
		}

		// Cek Izin Android 12+ (API 31+)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
				ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 101);
				return;
			}
		}

		showPrinterDiscoveryDialog();
	}

	private void showPrinterDiscoveryDialog() {
		// Hanya ambil yang sudah PAIRED agar cepat
		Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
		ArrayList<String> deviceNames = new ArrayList<>();
		ArrayList<BluetoothDevice> devicesList = new ArrayList<>();

		if (pairedDevices == null || pairedDevices.isEmpty()) {
			Toast.makeText(this, "Pairing printer dulu di setting Bluetooth HP lo!", Toast.LENGTH_LONG).show();
			return;
		}

		for (BluetoothDevice device : pairedDevices) {
			deviceNames.add(device.getName() + "\n" + device.getAddress());
			devicesList.add(device);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Pilih Bluetooth Printer");
		builder.setItems(deviceNames.toArray(new String[0]), (dialog, which) -> {
			BluetoothDevice selected = devicesList.get(which);

			// Simpan Address dan Nama
			getSharedPreferences("PRINTER_SETTINGS", MODE_PRIVATE)
				.edit()
				.putString("printer_address", selected.getAddress())
				.putString("printer_name", selected.getName())
				.apply();

			tvCurrentPrinter.setText("Printer: " + selected.getName());
			Toast.makeText(this, "Printer Diset ke: " + selected.getName(), Toast.LENGTH_SHORT).show();
		});
		builder.show();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
			showPrinterDiscoveryDialog();
		} else {
			Toast.makeText(this, "Izin Bluetooth ditolak!", Toast.LENGTH_SHORT).show();
		}
	}
}
