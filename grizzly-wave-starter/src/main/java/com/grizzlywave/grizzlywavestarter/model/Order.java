package com.grizzlywave.grizzlywavestarter.model;


/**
 * our model class : Order 
 * which contain id, customerId  and price as attribute
 *
 */
public class Order {
	private int id;
	private int customerId;
	private int price;
	private String state= "pandding";

	public Order(int id, int customerId, int price) {
		this.id = id;
		this.customerId = customerId;
		this.price = price;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getCustomerId() {
		return customerId;
	}

	public void setCustomerId(int customerId) {
		this.customerId = customerId;
	}

	@Override
	public String toString() {
		return "Order [id=" + id + ", customerId=" + customerId + ", price=" + price + ", state=" + state + "]";
	}

	public Order() {
		super();
		// TODO Auto-generated constructor stub
	}

}
