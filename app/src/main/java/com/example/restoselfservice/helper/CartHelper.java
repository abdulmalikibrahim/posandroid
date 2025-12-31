package com.example.restoselfservice.helper;

import com.example.restoselfservice.model.CartItem;
import java.util.ArrayList;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CartHelper {
	private static CartHelper instance;
	private ArrayList<CartItem> cartItems;

	private CartHelper() {
		cartItems = new ArrayList<>();
	}

	public static CartHelper getInstance() {
		if (instance == null) {
			instance = new CartHelper();
		}
		return instance;
	}

	public void addToCart(CartItem newItem) {
		for (CartItem item : cartItems) {
			if (item.getId().equals(newItem.getId())) {
				item.incrementQty();
				return;
			}
		}
		cartItems.add(newItem);
	}

	public ArrayList<CartItem> getCartItems() {
		return cartItems;
	}

	public int getTotalItems() {
		int total = 0;
		for (CartItem item : cartItems) {
			total += item.getQty(); // Hitung total QTY, bukan size list
		}
		return total;
	}

	public double getTotalPrice() {
		double total = 0;
		for (CartItem item : cartItems) {
			total += item.getPrice() * item.getQty(); // Harga kali Qty
		}
		return total;
	}

	public void clearCart() {
		cartItems.clear();
	}

	// --- METHOD BARU (WAJIB DITAMBAHKAN) ---
	public void removeItem(CartItem item) {
		cartItems.remove(item);
	}

	public void updateItemQty(CartItem item, int newQty) {
		for (CartItem cartItem : cartItems) {
			if (cartItem.getId().equals(item.getId())) {
				cartItem.setQty(newQty);
				return;
			}
		}
	}

	public String getCartItemsAsJson() {
		JSONArray jsonArray = new JSONArray();
		try {
			for (CartItem item : cartItems) {
				JSONObject jsonObject = new JSONObject();
				jsonObject.put("id", item.getId());
				jsonObject.put("name", item.getName());
				jsonObject.put("price", item.getPrice());
				jsonObject.put("qty", item.getQty()); // <--- Kirim Qty ke PHP
				// Subtotal biarin PHP yang itung biar aman, atau mau dikirim juga boleh
				jsonArray.put(jsonObject);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return jsonArray.toString();
	}
}
