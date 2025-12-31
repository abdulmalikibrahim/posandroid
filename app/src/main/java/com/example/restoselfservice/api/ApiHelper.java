package com.example.restoselfservice.api;

import android.content.Context;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.VolleyError;

import android.content.SharedPreferences;
import android.util.Log;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import java.util.ArrayList;
import com.example.restoselfservice.model.Category;
import com.example.restoselfservice.model.Menu;

public class ApiHelper {

	private static final String TAG = "API_DEBUG"; // Tag khusus buat filtering di Logcat
	private Context context;
	private SharedPreferences prefs;
	//	private static final String BASE_URL = "http://10.0.2.2:8080/resto_self_service/api/";

	// Constructor
	public ApiHelper(Context context) {
		this.context = context;
		this.prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
	}
	public String getBaseUrl() {
		// Ambil dari memori HP, kalau belum ada isinya pake default 10.0.2.2:8080
		String savedUrl = prefs.getString("base_url", "http://10.0.2.2:8080/resto_self_service/api/");
		return savedUrl;
	}

	private RequestQueue getRequestQueue() {
		return Volley.newRequestQueue(context);
	}

	// Method untuk ambil kategori
	public void getCategories(ApiCallback callback) {
		RequestQueue queue = Volley.newRequestQueue(context);
		final String URL = getBaseUrl() + "get_category";

		// 1. LOG: Mulai request
		Log.d(TAG, "Requesting Categories from URL: " + URL);

		JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, URL, null,
			response -> {
				// 2. LOG SUKSES: Data diterima
				Log.i(TAG, "Categories Response SUCCESS. Total items: " + response.length());

				ArrayList<Category> list = new ArrayList<>();
				try {
					for (int i = 0; i < response.length(); i++) {
						JSONObject obj = response.getJSONObject(i);
						list.add(new Category(obj.getString("id"), obj.getString("name")));
					}
					callback.onSuccess(list);
				} catch (Exception e) {
					// 3. LOG: Error saat Parsing JSON (kalau ada)
					Log.e(TAG, "JSON Parsing Error in getCategories: " + e.getMessage(), e);
					callback.onError(e);
				}
			},
			error -> {
				error.printStackTrace(); // print stacktrace ke Logcat

				// 4. LOG ERROR DETAIL:
				String errorMessage = getDetailedVolleyError(error);
				Log.e(TAG, "Categories Request FAILED: " + errorMessage);

				// Tambahkan log respons error body (jika ada)
				if (error.networkResponse != null && error.networkResponse.data != null) {
					try {
						String errorData = new String(error.networkResponse.data);
						Log.e(TAG, "Error Response Body (HTML/JSON): " + errorData);
					} catch (Exception e) {
						Log.e(TAG, "Could not read error response body.");
					}
				}

				callback.onError(new Exception(errorMessage));
			}
		);

		queue.add(request);
	}

	// Helper method untuk mendapatkan pesan error Volley yang lebih detail
	private String getDetailedVolleyError(VolleyError error) {
		String errorMessage = "Volley Error: ";

		// CEK APAKAH ADA BODY RESPONSE DARI SERVER (Ini kuncinya!)
		if (error.networkResponse != null && error.networkResponse.data != null) {
			try {
				String errorBody = new String(error.networkResponse.data, "UTF-8");
				Log.e(TAG, "SERVER ERROR DETAIL: " + errorBody); // Liat di Logcat!
				return "Server Error: " + errorBody;
			} catch (Exception e) {
				Log.e(TAG, "Gagal baca error body");
			}
		}

		if (error instanceof com.android.volley.NoConnectionError) {
			errorMessage += "No Connection";
		} else if (error instanceof com.android.volley.TimeoutError) {
			errorMessage += "Timeout";
		} else if (error instanceof com.android.volley.ServerError) {
			errorMessage += "Server Error (500/404)";
		}
		return errorMessage;
	}

	// Interface callback
	public interface ApiCallback {
		void onSuccess(ArrayList<Category> categories);
		void onError(Exception e);
	}

	public void getMenus(String categoryId, ApiMenuCallback callback) {
		RequestQueue queue = Volley.newRequestQueue(context);
		final String URL = getBaseUrl() + "get_menus/" + categoryId;
		Log.d(TAG, "Requesting Menus from URL: " + URL);

		JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, URL, null,
			response -> {
				Log.i(TAG, "Menus Response SUCCESS for category " + categoryId + ". Total items: " + response.length());

				ArrayList<Menu> list = new ArrayList<>();
				try {
					for (int i = 0; i < response.length(); i++) {
						JSONObject obj = response.getJSONObject(i);
						list.add(new Menu(
							obj.getString("id"),
							obj.getString("id_category"),
							obj.getString("stock"),
							obj.getString("name"),
							obj.getString("price"),
							obj.getString("link_image")
						));
					}
					callback.onSuccess(list);
				} catch (Exception e) {
					Log.e(TAG, "JSON Parsing Error in getMenus: " + e.getMessage(), e);
					callback.onError(e);
				}
			},
			error -> {
				Log.e(TAG, "Menus Request FAILED: " + getDetailedVolleyError(error));
				callback.onError(error);
			}
		);

		queue.add(request);
	}

	public interface ApiMenuCallback {
		void onSuccess(ArrayList<Menu> menus);
		void onError(Exception e);
	}

	public interface QrisCallback {
		void onSuccess(String qrisUrl, String externalId);
		void onError(String message);
	}

	// Method untuk memanggil backend lo agar meng-generate QRIS
	public void generateQris(double totalAmount, String orderId, QrisCallback callback) {
		String url = getBaseUrl() + "api/qris/generate"; // <<< GANTI dengan endpoint backend lo

		JSONObject requestBody = new JSONObject();
		try {
			requestBody.put("total_amount", totalAmount);
			requestBody.put("order_id", orderId); // Kirim ID order/transaksi yang unik
			// Lo mungkin juga perlu mengirim item list, tapi kita simplifikasi dengan totalAmount saja
		} catch (JSONException e) {
			e.printStackTrace();
		}

		JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, requestBody,
			response -> {
				try {
					// Asumsi backend lo merespons dengan data ini
					String qrisUrl = response.getString("qris_url");
					String externalId = response.getString("external_id");
					callback.onSuccess(qrisUrl, externalId);
				} catch (JSONException e) {
					callback.onError("Gagal parse respons QRIS: " + e.getMessage());
				}
			},
			error -> {
				callback.onError("Gagal koneksi ke backend: " + error.getMessage());
			}
		);

		// Tambahkan request ke Volley Queue (asumsi lo menggunakan Volley)
		Volley.newRequestQueue(context).add(jsonObjectRequest);
	}

	// ==========================================================
	// BAGIAN BARU UNTUK PAYMENT QRIS
	// ==========================================================

	// --- 1. INTERFACE CALLBACK BARU ---

	public interface QrisChargeCallback {
		void onSuccess(String qrisUrl, String externalId, long remainingSeconds, String id); // Ganti ke long
		void onError(String message);
	}

	public interface PaymentStatusCallback {
		void onStatusCheck(String status); // status: PAID, PENDING, EXPIRED
		void onError(String message);
	}

	// --- 2. METHOD BARU: POST REQUEST UNTUK MEMBUAT QRIS CHARGE (create_qris_charge) ---

	public void postQrisCharge(double amount, QrisChargeCallback callback) {
		String url = getBaseUrl() + "create_qris_charge";
		Log.d(TAG, "Requesting QRIS Charge URL: " + url);

		StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
			response -> {
				try {
					JSONObject jsonResponse = new JSONObject(response);
					if (jsonResponse.optBoolean("success", false)) {
						String qrisUrl = jsonResponse.optString("qris_url");
						String externalId = jsonResponse.optString("external_id");
						String id = jsonResponse.optString("id");

						// Ambil field remaining_seconds dari JSON CI3 lo
						long remainingSeconds = jsonResponse.optLong("remaining_seconds", 0);

						callback.onSuccess(qrisUrl, externalId, remainingSeconds, id);
					} else {
						// Jika CI3 mengembalikan success: false
						String message = jsonResponse.optString("message", "Permintaan QRIS ditolak server.");
						Log.e(TAG, "QRIS Charge FAILED: " + message);
						callback.onError(message);
					}
				} catch (JSONException e) {
					Log.e(TAG, "JSON Parsing Error (QrisCharge): " + e.getMessage());
					callback.onError("Respon server tidak valid: " + e.getMessage());
				}
			},
			error -> {
				String errorMessage = getDetailedVolleyError(error);
				Log.e(TAG, "QRIS Charge Request FAILED: " + errorMessage);
				callback.onError(errorMessage);
			})
		{
			@Override
			protected Map<String, String> getParams() {
				// Data yang dikirim ke Controller CI3 (sesuai yang dibutuhkan Payment.php)
				Map<String, String> params = new HashMap<>();
				params.put("amount", String.valueOf(amount));
				params.put("description", "Pembayaran Pesanan #" + System.currentTimeMillis());
				return params;
			}
		};

		getRequestQueue().add(stringRequest);
	}

	public void checkPaymentStatus(String idQRIS, PaymentStatusCallback callback) {
		String url = getBaseUrl() + "check_status";
		Log.d(TAG, "Requesting Payment Status for ID: " + idQRIS + " from URL: " + url);

		StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
			response -> {
				try {
					JSONObject jsonResponse = new JSONObject(response);
					if (jsonResponse.optBoolean("success", false)) {
						String status = jsonResponse.optString("status");
						Log.i(TAG, "Status Check SUCCESS: " + status);
						callback.onStatusCheck(status);
					} else {
						String message = jsonResponse.optString("message", "Gagal cek status.");
						Log.e(TAG, "Status Check FAILED: " + message);
						callback.onError(message);
					}
				} catch (JSONException e) {
					Log.e(TAG, "JSON Parsing Error (CheckStatus): " + e.getMessage());
					callback.onError("Respon status tidak valid: " + e.getMessage());
				}
			},
			error -> {
				String errorMessage = getDetailedVolleyError(error);
				Log.e(TAG, "Status Check Request FAILED: " + errorMessage);
				callback.onError(errorMessage);
			})
		{
			@Override
			protected Map<String, String> getParams() {
				// Data yang dikirim ke Controller CI3
				Map<String, String> params = new HashMap<>();
				params.put("id", idQRIS);
				return params;
			}
		};

		getRequestQueue().add(stringRequest);
	}

	// Tambahin parameter String paymentMethod
	public void savePesanan(String externalId, double total, String itemsJson, String paymentMethod, SimpleCallback callback) {
		String url = getBaseUrl() + "save_pesanan";

		StringRequest request = new StringRequest(Request.Method.POST, url,
			response -> {
				try {
					JSONObject jsonObject = new JSONObject(response);
					if (jsonObject.optBoolean("success", false)) {
						callback.onSuccess();
					} else {
						callback.onError(jsonObject.optString("message", "Gagal simpan"));
					}
				} catch (JSONException e) {
					callback.onError("Parsing error");
				}
			},
			error -> callback.onError(getDetailedVolleyError(error))) {
			@Override
			protected Map<String, String> getParams() {
				Map<String, String> params = new HashMap<>();
				params.put("external_id", externalId);
				params.put("total_price", String.valueOf(total));
				params.put("items", itemsJson);

				// INI TAMBAHANNYA: Kirim ke PHP/Server dengan key 'payment_method'
				params.put("payment_method", paymentMethod);

				return params;
			}
		};

		getRequestQueue().add(request);
	}

	public interface SimpleCallback {
		void onSuccess();
		void onError(String message);
	}

	public interface StatusCookingCallback {
		// Sekarang bawa list detail buat di RecyclerView
		void onFinished(boolean allFinished, ArrayList<Menu> details);
		void onError(String message);
	}
}
