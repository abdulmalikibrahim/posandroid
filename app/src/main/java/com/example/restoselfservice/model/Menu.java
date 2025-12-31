package com.example.restoselfservice.model;

public class Menu {
	private String id;
	private String idCategory;
	private String name;
	private String price;
	private String linkImage;
	private String stock;
	private String qty;    // Udah ada, tinggal kita buat setternya
	private String status; // Tambahin buat status masak

	// 1. Constructor Kosong (WAJIB biar bisa new Menu() di ApiHelper)
	public Menu() {
	}

	// 2. Constructor Lengkap (Biar code lama lo di Main/Category gak rusak)
	public Menu(String id, String id_category, String stock, String name, String price, String link_image) {
		this.id = id;
		this.idCategory = id_category;
		this.stock = stock;
		this.name = name;
		this.price = price;
		this.linkImage = link_image;
	}

	// --- GETTER (Buat ambil data) ---
	public String getId() { return id; }
	public String getIdCategory() { return idCategory; }
	public String getName() { return name; }
	public String getPrice() { return price; }
	public String getStock() { return stock; }
	public String getLinkImage() { return linkImage; }
	public String getQty() { return qty; }
	public String getStatus() { return status; }

	// --- SETTER (Ini kuncinya biar ApiHelper bisa update data per item) ---
	public void setId(String id) { this.id = id; }
	public void setIdCategory(String idCategory) { this.idCategory = idCategory; }
	public void setName(String name) { this.name = name; }
	public void setPrice(String price) { this.price = price; }
	public void setLinkImage(String linkImage) { this.linkImage = linkImage; }
	public void setQty(String qty) { this.qty = qty; }
	public void setStatus(String status) { this.status = status; }
}
