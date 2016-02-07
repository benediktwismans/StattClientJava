package systems.sdw.statt;

import org.json.JSONObject;

public class Exceptions {

	static public class NoSessionAvailableException extends Exception {
		private static final long serialVersionUID = 8251491692544430773L;
		private String klasse=null;
		private String text=null;

		public NoSessionAvailableException(JSONObject exception) {
			super();
			this.klasse=exception.getString("klasse");
			this.text=exception.getString("text");
		}
		public String getKlasse() {return this.klasse;}
		public String getText() {return this.text;}
	}
	
	
	static public class WrongParamException extends Exception {

		private static final long serialVersionUID = 8251491692544430772L;
		private String param="";
		private String value="";
		private String text="?";

		public WrongParamException(JSONObject exception) {
			super();
			this.param=exception.getString("param");
			this.value=exception.getString("wert");
			this.text=exception.getString("beschreibung");
		}

		public String getParam() {return param;}
		public String getValue() {return this.value;}
		public String getText() {return this.text;}
	}


}
