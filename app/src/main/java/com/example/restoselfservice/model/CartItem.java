package com.example.restoselfservice.model;

public class CartItem {
	private String id;
	private String name;
	private double price;
	private String imageUrl;
	private int qty; // jumlah item

	public CartItem(String id, String name, double price, String imageUrl) {
		this.id = id;
		this.name = name;
		this.price = price;
		this.imageUrl = imageUrl;
		this.qty = 1; // default 1
	}

	// getter & setter
	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public double getPrice() {
		return price;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public int getQty() {
		return qty;
	}

	public void setQty(int qty) {
		this.qty = qty;
	}

	// fungsi untuk menambah qty
	public void incrementQty() {
		this.qty++;
	}

	// fungsi untuk mengurangi qty
	public void decrementQty() {
		if (this.qty > 0) this.qty--;
	}
}

