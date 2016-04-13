package com.circulate.models;

import java.util.ArrayList;

public class Patient {
	
	private String identifier;
	private ArrayList<Device> devices;
	
	public Patient(String identifier) {
		this.identifier = identifier;
		this.devices = new ArrayList<>();
	}
	
	public String getIdentifier()
	{
		return identifier;
	}

	public ArrayList<Device> getDevices() {
		return devices;
	}

	public void setDevices(ArrayList<Device> devices) {
		this.devices = devices;
		for (Device device : devices) {
			device.setPatient(this);
		}
	}
	
	public void addDevice(Device device)
	{
		this.devices.add(device);
		device.setPatient(this);
	}
	
	public void removeDevice(Device device)
	{
		this.devices.remove(device);
		device.setPatient(null);
	}
}
