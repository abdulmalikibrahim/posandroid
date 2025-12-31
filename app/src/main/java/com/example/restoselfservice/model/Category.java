package com.example.restoselfservice.model;

public class Category {
	private String id;
	private String name;

	// Constructor
	public Category(String id, String name) {
		this.id = id;
		this.name = name;
	}

	// Getter
	public String getId() { return id; }
	public String getName() { return name; }
}

