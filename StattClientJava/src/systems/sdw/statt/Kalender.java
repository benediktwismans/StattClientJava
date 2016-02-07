package systems.sdw.statt;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import systems.sdw.statt.Exceptions;
import systems.sdw.statt.Client;
import systems.sdw.statt.Converter;

public class Kalender {

	private static final String opcGetKalenderliste="255001";	
	private static final String opcGetEventCalendarItems="255115";	

	
	public static void main(String[] args) {	

		Properties props=new Properties();
		
		props.setProperty(Client.propLogin, "?");
		props.setProperty(Client.propPassword, "?");


		Client client=new Client(props);
		try {
			if (client.connect())	{
				System.out.println("Connect OK");
				//
				//	Einfachstes Beispiel: Unparametrisierter GET-Aufruf 
				JSONObject kalenderListe=client.get(opcGetKalenderliste);
				System.out.println(kalenderListe.toString(2));

				//	Jetzt die Kalendereinträge holen. Es sollen alle Kalender geholt werden, die defaultmäßig visible==true
 				//	in der Kalenderlisze voreingestellt haben. Wir wollen einen Monat zurück und 1 Jahr in die Zukunft gehen. 
				
				//	1 Monat zurück
				Calendar von=Calendar.getInstance();
				von.set(Calendar.MONTH, von.get(Calendar.MONTH)-1);
				
				//	1 Jahr nach vorn
				Calendar bis=Calendar.getInstance();
				bis.set(Calendar.YEAR, bis.get(Calendar.YEAR)+1);

				//	Zähler für die Anzahl Kalender, die visible=true haben
				int count=0;

				//	==========================================================================================
				//	Beispiel als GET-Aufruf mit Parametern: Liste der Kalendereinträge
				//	==========================================================================================
				
				List<NameValuePair> nvps=new ArrayList <NameValuePair>();
				
				nvps.add(new BasicNameValuePair("von", Converter.Timestamp2ISO8601String(von)));
				nvps.add(new BasicNameValuePair("bis", Converter.Timestamp2ISO8601String(bis)));
				
				for (int i=0; i<=kalenderListe.getJSONArray("items").length()-1; i++) {
					JSONObject item=kalenderListe.getJSONArray("items").getJSONObject(i);
					if (item.getBoolean("visible")==true) {
						nvps.add(new BasicNameValuePair("oidItem["+(count++)+"]", new Long(item.getLong("oid")).toString()));
					}
				}
				nvps.add(new BasicNameValuePair("itemCount", new Integer(count).toString()));

				JSONObject getResponse1=client.get(opcGetEventCalendarItems, nvps);
				System.out.println(getResponse1.toString(2));
				
				
				//	==========================================================================================
				//	Beispiel als POST-Aufruf mit Parameterobjekt: Liste der Kalendereinträge
				//	==========================================================================================
				
				JSONObject params=new JSONObject();
				params.put("von", Converter.Timestamp2ISO8601String(von));
				params.put("bis", Converter.Timestamp2ISO8601String(bis));
				
				count=0;
				for (int i=0; i<=kalenderListe.getJSONArray("items").length()-1; i++) {
					JSONObject item=kalenderListe.getJSONArray("items").getJSONObject(i);
					if (item.getBoolean("visible")==true) {
						params.put("oidItem["+(count++)+"]", item.getLong("oid"));
					}
				}
				params.put("itemCount", count);

				JSONObject postResponse=client.post(opcGetEventCalendarItems, params);
				System.out.println(postResponse.toString(2));

				
				//	dont forget this!! otherwise there will be no remaining free session available!
				client.disconnect();
			}
		} catch (Exceptions.NoSessionAvailableException e) {
			System.out.println(e.getText());
		} catch (Exceptions.WrongParamException e) {
			System.out.println(e.getText());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
