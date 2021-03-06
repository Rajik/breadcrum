package helpers.messaging;


import helpers.database.DataStoreHelper;
import helpers.location.LocationHelper;
import helpers.notification.NotificationHelper;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import models.Contact;
import models.MessageLog;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.SmsManager;
import android.util.Log;

public class Messenger {

    private Context context;
    private LocationHelper locationHelper;
    private DataStoreHelper dataStoreHelper;
    public Messenger(Context context){
        this.context = context;
        this.locationHelper = new LocationHelper(context);
        this.dataStoreHelper = new DataStoreHelper(context);
    }

    public void sendMessage(Contact contact) {
    	String phoneNumber = contact.getPhoneNumber();
    	Location location = locationHelper.getLocation();
    	
        SmsManager smsManager = SmsManager.getDefault();
        String content = constructMessage(location);
        String signature = "\n\nSent by BreadCrumb";
        ArrayList<String> message = smsManager.divideMessage(content+signature);
        smsManager.sendMultipartTextMessage(phoneNumber, null, message, null, null);
        
        java.util.Date now = new java.util.Date();
        MessageLog messageLog = new MessageLog(contact.getName(),phoneNumber,content, new Timestamp(now.getTime()));
        dataStoreHelper.addMessageLog(messageLog);
        
        NotificationHelper.sendMessageNotification(context,messageLog);        
    }

	private String constructMessage(Location location) {
		String geofencePosition =  locationHelper.getUserPosition() !=null ? locationHelper.getUserPosition() : "";
		String content = "Not able to pick the phone. ";
		String signature = "\nSent via BreadCrumb";
		if(!geofencePosition.contains("Entered"))
		if(location!=null){
            String address = getAddress(location);
            content += "I am near " + address + " http://www.google.co.in/maps/place/" + location.getLatitude() + "," + location.getLongitude();
		}
		return geofencePosition + content;
	}

	private String getAddress(Location location) {
		Geocoder geocoder = new Geocoder(context, Locale.getDefault());
		String address = "";
		if(isConnectingToInternet()){
 			try {
				List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
				if(addresses.size()!=0) {
					Address currentAddress = addresses.get(0);
					int totalAddressLines = currentAddress.getMaxAddressLineIndex();
					if(totalAddressLines > 0)
						address += currentAddress.getAddressLine(0);
					if(currentAddress.getLocality()!=null)
						address += ", " + currentAddress.getLocality(); 
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return address;
	}

	private boolean isConnectingToInternet(){
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null)
        {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED)
                    {
                        return true;
                    }
        }
        return false;
    }
}
