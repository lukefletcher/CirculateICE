package com.circulate;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

import org.json.JSONObject;
import org.mdpnp.rtiapi.data.QosProfiles;
import org.mdpnp.rtiapi.qos.IceQos;

import com.circulate.ice.Configuration;
import com.circulate.ice.DeviceAdapterCommand;
import com.circulate.models.Device;
import com.circulate.models.Patient;
import com.rti.dds.domain.DomainParticipant;
import com.rti.dds.domain.DomainParticipantFactory;
import com.rti.dds.infrastructure.RETCODE_NO_DATA;
import com.rti.dds.infrastructure.ResourceLimitsQosPolicy;
import com.rti.dds.infrastructure.StatusKind;
import com.rti.dds.subscription.DataReader;
import com.rti.dds.subscription.DataReaderListener;
import com.rti.dds.subscription.InstanceStateKind;
import com.rti.dds.subscription.LivelinessChangedStatus;
import com.rti.dds.subscription.RequestedDeadlineMissedStatus;
import com.rti.dds.subscription.RequestedIncompatibleQosStatus;
import com.rti.dds.subscription.SampleInfo;
import com.rti.dds.subscription.SampleInfoSeq;
import com.rti.dds.subscription.SampleLostStatus;
import com.rti.dds.subscription.SampleRejectedStatus;
import com.rti.dds.subscription.SampleStateKind;
import com.rti.dds.subscription.SubscriberQos;
import com.rti.dds.subscription.SubscriptionMatchedStatus;
import com.rti.dds.subscription.ViewStateKind;
import com.rti.dds.topic.Topic;

import ice.NumericDataReader;
import ice.SampleArrayDataReader;

public class DDSController {
	
	CirculateICE circulateICE;
	
	DomainParticipant participant;
	
	ArrayList<Device> connectedDevices = new ArrayList<>();
	
	public DDSController(CirculateICE circulateICE)
	{
		// DDSController setup
		this.circulateICE = circulateICE;
    	System.setProperty("java.net.preferIPv4Stack" , "true");
    	
        // Setup ICE and hide output
        PrintStream original = System.out;
        try {
			System.setOut(new PrintStream(new FileOutputStream("/dev/null")));
		} catch (FileNotFoundException e) {
		}
        IceQos.loadAndSetIceQos();
        System.setOut(original);
        
        // Setup the participant
        this.participant = DomainParticipantFactory.get_instance().create_participant(15, DomainParticipantFactory.PARTICIPANT_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);

        // Create the subscriber
        SubscriberQos subscriberQos = new SubscriberQos();
        participant.get_default_subscriber_qos(subscriberQos);
        subscriberQos.partition.name.clear();
        subscriberQos.partition.name.add("*");
        participant.set_default_subscriber_qos(subscriberQos);

        // Register data types
        ice.SampleArrayTypeSupport.register_type(participant, ice.SampleArrayTypeSupport.get_type_name());
        ice.NumericTypeSupport.register_type(participant, ice.NumericTypeSupport.get_type_name());

        // Create topics
        Topic sampleArrayTopic = participant.create_topic(ice.SampleArrayTopic.VALUE, ice.SampleArrayTypeSupport.get_type_name(), DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);
        participant.create_datareader_with_profile(sampleArrayTopic, QosProfiles.ice_library, QosProfiles.waveform_data, new ICEDataReaderListener() {

            @Override
            public void on_data_available(DataReader reader) {
                ice.SampleArraySeq sa_data_seq = new ice.SampleArraySeq();
                SampleInfoSeq info_seq = new SampleInfoSeq();
                SampleArrayDataReader saReader = (SampleArrayDataReader) reader;
                try {
                    saReader.read(sa_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);
                    for (int i = 0; i < info_seq.size(); i++) {
                        SampleInfo si = (SampleInfo) info_seq.get(i);
                        ice.SampleArray data = (ice.SampleArray) sa_data_seq.get(i);
                        if (si.valid_data) {
                        	ArrayList<Float> values = new ArrayList<>();
                        	for (i = 0; i < data.values.userData.size(); i++) {
                        		values.add((Float)data.values.userData.get(i));
                        	}
                        	sample(data.unique_device_identifier,data.metric_id,data.unit_id,data.device_time.sec,data.frequency,values);
                        }
                    }
                } catch (Exception exception) {
                } finally {
                    saReader.return_loan(sa_data_seq, info_seq);
                }
            }
        }, StatusKind.STATUS_MASK_ALL);
        
        Topic numericTopic = participant.create_topic(ice.NumericTopic.VALUE, ice.NumericTypeSupport.get_type_name(), DomainParticipant.TOPIC_QOS_DEFAULT, null, StatusKind.STATUS_MASK_NONE);
        participant.create_datareader_with_profile(numericTopic, QosProfiles.ice_library, QosProfiles.numeric_data, new ICEDataReaderListener() {
            @Override
            public void on_data_available(DataReader reader) {
                ice.NumericSeq n_data_seq = new ice.NumericSeq();
                SampleInfoSeq info_seq = new SampleInfoSeq();
                NumericDataReader nReader = (NumericDataReader) reader;
                
                try {
                    nReader.read(n_data_seq,info_seq, ResourceLimitsQosPolicy.LENGTH_UNLIMITED, SampleStateKind.NOT_READ_SAMPLE_STATE, ViewStateKind.ANY_VIEW_STATE, InstanceStateKind.ALIVE_INSTANCE_STATE);
                    for (int i = 0; i < info_seq.size(); i++) {
                        SampleInfo si = (SampleInfo) info_seq.get(i);
                        ice.Numeric data = (ice.Numeric) n_data_seq.get(i);
                        if (si.valid_data) {
                        	ArrayList<Float> values = new ArrayList<>();
                        	values.add(data.value);
                        	sample(data.unique_device_identifier,data.metric_id,data.unit_id,data.device_time.sec,0,values);
                        }
                    }
                } catch (Exception exception) {
                } finally {
                    nReader.return_loan(n_data_seq, info_seq);
                }
            }
        }, StatusKind.STATUS_MASK_ALL);
        
		if (instance == null)
		{
			instance = this;
		}
		
    	System.out.println("CirculateICE/DDS: Ready");
	}
	
	private static DDSController instance = null;
	public static DDSController sharedController() {
		return instance;
	}
	
	public void connectDevice(Device device)
	{
		Configuration c = device.getConfiguration();
		
		device.thread = new Thread(() -> {
            try {
            	device.context = c.createContext("DeviceAdapterContext.xml");
                
                device.adapter = new DeviceAdapterCommand.HeadlessAdapter(c.getDeviceFactory(), device.context, false) {
                    public void stop() {
                        super.stop();
                        device.context.destroy();
                    };
                };
                
                String[] partition = {"MRN=" + device.getPatient().getIdentifier()};
                device.adapter.setPartition(partition);
                device.adapter.setAddress(c.getAddress());
                device.adapter.init();
                device.adapter.connect();
                device.setUDI(device.adapter.getDevice().getUniqueDeviceIdentifier());
                
            } catch (Exception e) {
            }
        });
		device.thread.setDaemon(true);
		device.thread.start();
    	System.out.println("CirculateICE/DDS: Device connected - " + device.getIdentifier());
	}
	
	public void disconnectDevice(Device device)
	{
		device.context.destroy();
		device.adapter.disconnect();
		device.thread.stop();
		device.thread = null;
    	System.out.println("CirculateICE/DDS: Device disconnected - " + device.getIdentifier());
	}
	
	public void sample(String udi, String metric, String unit, int time, int frequency, ArrayList<Float> data)
	{
		Device device = circulateICE.getDeviceForUDI(udi);
		Patient patient = device.getPatient();
		
    	JSONObject payload = new JSONObject();
    	payload.put("patient", patient.getIdentifier());
    	payload.put("device", device.getIdentifier());
    	payload.put("metric", metric);
    	//payload.put("frequency", frequency);
    	payload.put("values", data);
    	payload.put("unit", unit);
    	payload.put("time", time);
    	
		SocketController.sharedController().sample(payload);
	}
	
    public static abstract class ICEDataReaderListener implements DataReaderListener
    {
		@Override
		public void on_liveliness_changed(DataReader arg0, LivelinessChangedStatus arg1) {
		}

		@Override
		public void on_requested_deadline_missed(DataReader arg0, RequestedDeadlineMissedStatus arg1) {
		}

		@Override
		public void on_requested_incompatible_qos(DataReader arg0, RequestedIncompatibleQosStatus arg1) {
		}

		@Override
		public void on_sample_lost(DataReader arg0, SampleLostStatus arg1) {
		}

		@Override
		public void on_sample_rejected(DataReader arg0, SampleRejectedStatus arg1) {
		}

		@Override
		public void on_subscription_matched(DataReader arg0, SubscriptionMatchedStatus arg1) {
		}
    }

}
