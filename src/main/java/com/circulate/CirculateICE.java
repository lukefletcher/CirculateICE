package com.circulate;

import java.util.ArrayList;

import com.circulate.models.Device;
import com.circulate.models.Patient;

public class CirculateICE {
	
	public DDSController ddsController;
	public SocketController socketController;
	
	private ArrayList<Patient> patients = new ArrayList<>();
	private ArrayList<Device> devices = new ArrayList<>();
	
    public static void main(String[] args) throws Exception {
    	System.out.println("CirculateICE: Starting...");
    	String port = (args.length > 0) ? args[0] : "12080";
    	instance = new CirculateICE(port);
    	System.out.println("CirculateICE: Running");
    }
    
	public CirculateICE(String port) {
		this.ddsController = new DDSController(this);
		this.socketController = new SocketController(this, port);
	}

	private static CirculateICE instance = null;

	// Patients
	
	public ArrayList<Patient> getPatients() {
		return this.patients;
	}
	
	public Patient getPatient(String identifier) {
		for (Patient patient : this.patients) {
			if (patient.getIdentifier().equals(identifier)) {
				return patient;
			}
		}
		return null;
	}

	public void setPatients(ArrayList<Patient> patients) {
		for (Patient patient : this.patients) {
			if (!patients.contains(patient)) {
				this.removePatient(patient);
			}
		}
		
		for (Patient patient: patients) {
			if (!this.patients.contains(patient)) {
				this.addPatient(patient);
			}
		}
	}
	
	public void addPatient(Patient patient)	{
		if (patient == null) {
			return;
		}
		
		if (this.getPatient(patient.getIdentifier()) == null) {
			this.patients.add(patient);
		}
	}
	
	public void addPatients(ArrayList<Patient> patients) {
		for (Patient patient : patients) {
			this.addPatient(patient);
		}
	}
	
	public void removePatient(Patient patient) {
		if (patient == null) {
			return;
		}
		
		for (Device device : patient.getDevices()) {
			this.removeDevice(device);
		}
		this.patients.remove(patient);
	}
	
	public void removePatients(ArrayList<Patient>patients) {
		for (Patient patient : patients) {
			this.removePatient(patient);
		}
	}
	
	
	// Devices
	
	public ArrayList<Device> getDevices() {
		return this.devices;
	}
	
	public Device getDevice(String identifier) {
		for (Device device : this.devices) {
			if (device.getIdentifier().equals(identifier)) {
				return device;
			}
		}
		return null;
	}

	public Device getDeviceForUDI(String uid) {
		for (Device device : this.devices) {
			if (device.getUDI().equals(uid)) {
				return device;
			}
		}
		return null;
	}

	public void setDevicesForPatient(ArrayList<Device> devices, Patient patient)
	{
		for (Device device : patient.getDevices()) {
			if (!devices.contains(device)) {
				this.removeDevice(device);
			}
		}
		
		for (Device device: devices) {
			if (!patient.getDevices().contains(device)) {
				this.addDevice(device,patient);
			}
		}
	}
	
	public void addDevice(Device device, Patient patient) {
		if (device == null) {
			return;
		}

		if (!this.devices.contains(device)) {
			device.setPatient(patient);
			device.connect();
			this.devices.add(device);
		}
	}
	
	public void removeDevice(Device device) {
		if (device == null) {
			return;
		}
		
		if (this.devices.contains(device)) {
			device.disconnect();
			device.getPatient().removeDevice(device);
			this.devices.remove(device);
		}
	}
}
