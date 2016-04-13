package com.circulate;

import java.net.URISyntaxException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

import com.circulate.models.Device;
import com.circulate.models.Patient;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class SocketController {

	CirculateICE circulateICE;
	Socket socket;
	
	public SocketController(CirculateICE circulateICE, String port)
	{
		this.circulateICE = circulateICE;
		
		IO.Options opts = new IO.Options();
		opts.reconnection = false;

		try {
			this.socket = IO.socket("http://localhost:" + port,opts);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		this.socket.on(Socket.EVENT_CONNECT, new OnConnect());
		this.socket.on("list_patients", new OnListPatients());
		this.socket.on("set_patients_and_devices", new OnSetPatientsAndDevices());
		this.socket.on("add_patients", new OnAddPatients());
		this.socket.on("remove_patients", new OnRemovePatients());
		this.socket.on("add_devices", new OnAddDevices());
		this.socket.on("remove_devices", new OnRemoveDevices());
		
		this.socket.connect();
		
		if (instance == null)
		{
			instance = this;
		}
    	System.out.println("CirculateICE/SocketIO: Connecting...");
	}
	
	private static SocketController instance = null;
	public static SocketController sharedController() {
		return instance;
	}
	
	public void sample(JSONObject payload)
	{
		this.socket.emit("sample",payload);
	}
	
	public class OnConnect implements Emitter.Listener {
		@Override
		public void call(Object... args) {
			socket.emit("circulate_ice_start");
	    	System.out.println("CirculateICE/SocketIO: Connected");
		}
		
	}
	
	public class OnListPatients implements Emitter.Listener
	{
		@Override
		public void call(Object... args) {
			
	    	System.out.println("CirculateICE/SocketIO: list_patients");
			JSONArray payload = new JSONArray();
			ArrayList<Patient> patients = circulateICE.getPatients();
			
			for (Patient patient : patients) {
		    	JSONObject patientObject = new JSONObject();
		    	patientObject.put("identifier", patient.getIdentifier());
		    	payload.put(patientObject);
			}
			
			socket.emit("list_patients", payload);
		}
	}
	
	public class OnSetPatientsAndDevices implements Emitter.Listener
	{
		@Override
		public void call(Object... args) {
			
	    	System.out.println("CirculateICE/SocketIO: set_patients_and_devices");
			circulateICE.removePatients(circulateICE.getPatients());
			
			JSONArray patients = (JSONArray)args[0];
			for (int i = 0; i < patients.length(); i++)
			{
				JSONObject patientObject = (JSONObject) patients.get(i);
				Patient patient = new Patient(patientObject.getString("identifier"));
				circulateICE.addPatient(patient);
				
				JSONArray devices = (JSONArray) patientObject.get("devices");
				ArrayList<Device> devicesForPatient = new ArrayList<>();
				for (int j = 0; j < devices.length(); j++)
				{
					JSONObject deviceObject = (JSONObject) devices.get(j);
					Device device = new Device(deviceObject.getString("identifier"));
					device.setAddress(deviceObject.getString("address"));
					devicesForPatient.add(device);
				}
				
				circulateICE.setDevicesForPatient(devicesForPatient, patient);
			}
		}
	}
	
	public class OnAddPatients implements Emitter.Listener
	{
		@Override
		public void call(Object... args) {
	    	System.out.println("CirculateICE/SocketIO: Added patient(s)");
			JSONArray patients = (JSONArray)args[0];
			ArrayList<Patient> patientsToAdd = new ArrayList<>();
			for (int i = 0; i < patients.length(); i++)
			{
				JSONObject object = (JSONObject) patients.get(i);
				String identifier = object.getString("identifier");
				Patient patient = new Patient(identifier);
				patientsToAdd.add(patient);
			}
			circulateICE.addPatients(patientsToAdd);
		}
	}

	public class OnRemovePatients implements Emitter.Listener
	{
		@Override
		public void call(Object... args) {
	    	System.out.println("CirculateICE/SocketIO: Removed patient(s)");
			JSONArray patients = (JSONArray)args[0];
			ArrayList<Patient> patientsToDelete = new ArrayList<Patient>();
			for (int i = 0; i < patients.length(); i++)
			{
				JSONObject object = (JSONObject) patients.get(i);
				String identifier = object.getString("identifier");
				Patient patient = circulateICE.getPatient(identifier);
				if (patient != null)
				{
					patientsToDelete.add(patient);
				}
			}
			circulateICE.removePatients(patientsToDelete);
		}
	}

	public class OnAddDevices implements Emitter.Listener
	{
		@Override
		public void call(Object... args) {
	    	System.out.println("CirculateICE/SocketIO: Added device(s)");
			JSONArray devices = (JSONArray)args[0];
			for (int i = 0; i < devices.length(); i++)
			{
				JSONObject object = (JSONObject) devices.get(i);
				String identifier = object.getString("identifier");
				String address = object.getString("address");
				String patientIdentifier = object.getString("patient");
				
				Patient patient = circulateICE.getPatient(patientIdentifier);
				
				if (patient != null)
				{
					Device device = new Device(identifier);
					device.setAddress(address);
					circulateICE.addDevice(device, patient);
				}
			}
		}
	}

	public class OnRemoveDevices implements Emitter.Listener
	{
		@Override
		public void call(Object... args) {
	    	System.out.println("CirculateICE/SocketIO: Removed device(s)");
			JSONArray devices = (JSONArray)args[0];
			for (int i = 0; i < devices.length(); i++)
			{
				JSONObject object = (JSONObject) devices.get(i);
				String identifier = object.getString("identifier");
				Device device = circulateICE.getDevice(identifier);
				circulateICE.removeDevice(device);
			}
		}
	}
}
