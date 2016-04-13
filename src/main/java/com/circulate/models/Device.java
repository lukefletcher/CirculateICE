package com.circulate.models;

import org.mdpnp.devices.DeviceDriverProvider;
import org.springframework.context.support.AbstractApplicationContext;

import com.circulate.DDSController;
import com.circulate.ice.Configuration;
import com.circulate.ice.Configuration.Application;
import com.circulate.ice.DeviceAdapterCommand.HeadlessAdapter;
import com.circulate.ice.DeviceFactory;

public class Device {
	
	private String identifier;
	private Patient patient;
	private String address = "";
	private DeviceDriverProvider driver;
	private String udi;
	
	public Device(String identifier) {
		this.identifier = identifier;
		try {
			this.driver = DeviceFactory.MultiparameterProvider.class.newInstance();
			//this.driver = DeviceFactory.IntellivueEthernetProvider.class.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public Patient getPatient()
	{
		return this.patient;
	}
	
	public void setPatient(Patient patient)
	{
		this.patient = patient;
		if (!this.patient.getDevices().contains(this))
		{
			this.patient.addDevice(this);
		}
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
	public DeviceDriverProvider getDriver()
	{
		return this.driver;
	}
	
	public void setDriver(DeviceDriverProvider driver)
	{
		this.driver = driver;
	}
	
	public Configuration getConfiguration()
	{
		return new Configuration(false, Application.ICE_Device_Interface, 15, getDriver(), address, "");
	}
	
	public boolean isConnected()
	{
		return (this.thread != null);
	}
	
	public void connect()
	{
		DDSController.sharedController().connectDevice(this);
	}
	
	public void disconnect()
	{
		DDSController.sharedController().disconnectDevice(this);
	}
	
	public Thread thread;
	public AbstractApplicationContext context;
	public HeadlessAdapter adapter;

	public String getUDI() {
		return udi;
	}

	public void setUDI(String udi) {
		this.udi = udi;
	}
}

