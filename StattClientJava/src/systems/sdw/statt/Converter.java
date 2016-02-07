package systems.sdw.statt;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Converter {
	
	public static String Timestamp2ISO8601String(Timestamp timestamp) {
		if (timestamp==null) return null;
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		return dateFormat.format(timestamp);
	}
	
	public static String Timestamp2ISO8601String(Calendar cal) {
		return Timestamp2ISO8601String(new java.sql.Timestamp(cal.getTime().getTime()));
	}
	
	public static String Date2ISO8601String(Date date) {
		if (date==null) return null;
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		return dateFormat.format(date);
	}

	public static String TimeISO8601String(Timestamp timestamp) {
		if (timestamp==null) return null;
		DateFormat format = new SimpleDateFormat("'T'HH:mm:ss");
		return format.format(timestamp);
	}
	
	public static Timestamp ISO8601String2Timestamp(String string) {
		if (string==null || string.length()==0) return null;
		Calendar cal=javax.xml.bind.DatatypeConverter.parseDateTime(string);
		return new Timestamp(cal.getTimeInMillis());
	}
	
	public static Date ISO8601String2Date(String string) {
		if (string==null || string.length()==0) return null;
		Calendar cal=javax.xml.bind.DatatypeConverter.parseDateTime(string);
		return new Date(cal.getTimeInMillis());
	}

	public static Time ISO8601String2Time(String string) {
		if (string==null || string.length()==0) return null;
		Calendar cal=javax.xml.bind.DatatypeConverter.parseDateTime(string);
		return new Time(cal.getTimeInMillis());
	}


}
