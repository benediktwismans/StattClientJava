package systems.sdw.statt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import systems.sdw.statt.Exceptions;


public class Client {
	// 
	CloseableHttpClient httpclient=null;
	//	default encoding
	public static String encodingUTF8="UTF-8";
	// properties map
	public static final String propServer="server";
	public static final String propPort="port";
	public static final String propScheme="scheme";
	public static final String propLogin="login";
	public static final String propPassword="password";
	//
	private String scheme="https";
	private String server="stattbuchung.de";
	private String port="443";

	private String path="/SDW/statt";
	private String login="?";
	private String password="?";

	// opcodes
	private static final String opcLogon="1003";	// connect and start session
	private static final String opcLogoff="1002";	// close session properly and disconnect

	// challange url is prohibited so server starts authentification process  when calling this (1. step)
	private String getLoginURL() {
		return scheme+"://"+server+":"+port+path+"?opc="+opcLogon;
	}
	// login form url. call this as 2. step
	private String getFormURL() {
		return scheme+"://"+server+":"+port+path+"/j_security_check";
	}
	//	use this url for all communication after login in succeeded
	private String getFcURL(String opc) {
		return scheme+"://"+server+":"+port+path+"?opc="+opc;
	}
	private String getFcURL(String opc, List <NameValuePair> nvps) {
		StringBuffer buf=new StringBuffer(getFcURL(opc));
		ListIterator<NameValuePair> it=nvps.listIterator();
		while (it.hasNext()) {
			try {
				NameValuePair nvp=it.next();
				buf.append("&"+nvp.getName()+"="+URLEncoder.encode(nvp.getValue(), encodingUTF8));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return buf.toString();
	}
	// quit session by calling this url
	private String getLogoutURL() {
		return scheme+"://"+server+":"+port+path+"?opc="+opcLogoff;
	}

	// default constructor with properties map
	public Client(Properties props) {
		//	provide at least login and password
		if (props.getProperty(propServer)!=null) this.setServer((String) props.getProperty(propServer));
		if (props.getProperty(propScheme)!=null) this.setScheme((String) props.getProperty(propScheme));
		if (props.getProperty(propPort)!=null) this.setPort((String) props.getProperty(propPort));
		if (props.getProperty(propLogin)!=null) this.setKennung((String) props.getProperty(propLogin));
		if (props.getProperty(propPassword)!=null) this.setKennwort((String) props.getProperty(propPassword));
		//
		try {
			this.httpclient=HttpClients.createDefault();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean connect() throws Exception {

		System.out.println("Client trying to connect with given params:");
		System.out.println(server);
		System.out.println(port);
		System.out.println(scheme);
		System.out.println(login);
		System.out.println(password);

		try {
			// call prohibited url to log in
			HttpGet httpGet=new HttpGet(getLoginURL());
			HttpResponse response=httpclient.execute(httpGet);
			System.out.println("HTTPClient connect 1/3 "+getLoginURL()+" "+response.getStatusLine());
			if (response.getStatusLine().getStatusCode()!=200) {
				System.out.println("Server-Fehler beim Zugriff auf die URL "+getLoginURL());
				return false;
			}
			HttpEntity entity=response.getEntity();
			EntityUtils.consume(entity);
			//	call login form url and prived credentials
			HttpPost httpPost=new HttpPost(getFormURL());
			List <NameValuePair> nvps=new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("j_username", this.login));
			nvps.add(new BasicNameValuePair("j_password", this.password));
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, encodingUTF8));

			response=httpclient.execute(httpPost);
			System.out.println("HTTPClient connect 2/3 "+getFormURL()+" "+nvps.toString()+ " " + response.getStatusLine());
			entity=response.getEntity();
			EntityUtils.consume(entity);

			//	if credentials are ok server maps desired url so we can access it: check also for status 302=moved temporarily
			if (response.getStatusLine().getStatusCode()==302 || response.getStatusLine().getStatusCode()==200) {
				// no we can call login url again to establish session
				httpGet=new HttpGet(getLoginURL());
				response=httpclient.execute(httpGet);
				System.out.println("HTTPClient connect 3/3 "+getLoginURL()+" "+response.getStatusLine());
				//	it's always a good idea to check for successfull login on transport layer
				if (response.getStatusLine().getStatusCode()!=200) {
					System.out.println("Server-Fehler beim Zugriff auf die URL "+getLoginURL());
					return false;
				}
				//	check for any exception reported from server on logical layer after successfull login 
				//	dont use a connection when any exception is reported because there is no valid session
				checkExceptions(response.getEntity());
				//	ok, login successfull and session established
				EntityUtils.consume(entity);
				return true;
			}
			else {
				// otherwise: credentials not ok!
				System.out.println("Die Kennung/Kennwort-Kombination "+login+"/"+password+"  ist vom Server nicht akzeptiert worden.");
				return false;
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}


	public static void checkExceptions(HttpEntity responseEntity) throws Exception {
		checkExceptions(getResponseAsJSON(responseEntity));
	}

	public static void checkExceptions(JSONObject response) throws Exception {

		try {
			JSONObject exception=response.getJSONObject("exception");
			if (exception!=null) {
				if (exception.getString("exception").equalsIgnoreCase("NoSessionAvailableException")) {
					throw new Exceptions.NoSessionAvailableException(exception);
				}
				else if (exception.getString("exception").equalsIgnoreCase("WrongParamException")) {
					throw new Exceptions.WrongParamException(exception);
				}
				else throw new Exception(exception.getString("exception"));
			}
		} catch (JSONException e) {;}
	}


	public static JSONObject getResponseAsJSON(HttpEntity responseEntity) {
		if (responseEntity != null){
			try {
				BufferedReader streamReader = new BufferedReader(new InputStreamReader(responseEntity.getContent(), "UTF-8")); 
				StringBuilder responseStrBuilder = new StringBuilder();
				String inputStr=null;
				while ((inputStr = streamReader.readLine()) != null) responseStrBuilder.append(inputStr);
				JSONObject json=new JSONObject(responseStrBuilder.toString());
				//System.out.println(json.toString(4));
				return json;
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	// use this to dump response to stdout for debugging purposes
	@SuppressWarnings("unused")
	private static void dump(HttpEntity entity) throws IllegalStateException, IOException {
		BufferedReader  br = new BufferedReader(new InputStreamReader(entity.getContent()));
		String readLine;
		while(((readLine = br.readLine()) != null)) {
			System.err.println(readLine);
		}
	}

	private static boolean checkStatus(HttpResponse response) {
		switch (response.getStatusLine().getStatusCode()) {
		case 200:
		case 302:
			return true;
		default:
			return false;
		}
	}

	public boolean disconnect() {
		try {
			HttpGet httpGet=new HttpGet(getLogoutURL());
			HttpResponse response=httpclient.execute(httpGet);
			System.out.println("HTTPClient disconnect 1/1 "+getLogoutURL()+" "+response.getStatusLine());
			if (response.getStatusLine().getStatusCode()!=200) return false;
			HttpEntity entity=response.getEntity();
			EntityUtils.consume(entity);
			httpclient=null;
			return true;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public JSONObject get(String opc) throws Exception {
		return get(opc, new ArrayList <NameValuePair>());
	}

	public JSONObject get(String opc, List <NameValuePair> nvps) throws Exception {
		JSONObject result=null;
		HttpGet httpGet= new HttpGet(this.getFcURL(opc, nvps));
		try {
			HttpResponse response=httpclient.execute(httpGet);
			System.out.println("HTTPClient get 1/1 "+getFcURL(opc, nvps)+" "+response.getStatusLine());

			if (checkStatus(response)) {
				HttpEntity entity=response.getEntity();
				result=getResponseAsJSON(entity);
				EntityUtils.consume(entity);
				checkExceptions(result);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;		
	}

	public JSONObject post(String opc, JSONObject json) throws Exception {
		JSONObject result=null;
		HttpPost httpPost=new HttpPost(this.getFcURL(opc));
		try {
			List <NameValuePair> nvps=new ArrayList <NameValuePair>();
			nvps.add(new BasicNameValuePair("data", json.toString()));
			httpPost.setEntity(new UrlEncodedFormEntity(nvps, encodingUTF8));
			            UrlEncodedFormEntity x=new UrlEncodedFormEntity(nvps, encodingUTF8);
			            x.writeTo(System.out);
			HttpResponse response=httpclient.execute(httpPost);
			System.out.println("HTTPClient post 1/1 "+response.getStatusLine());

			if (checkStatus(response)) {
				HttpEntity entity=response.getEntity();
				result=getResponseAsJSON(entity);
				EntityUtils.consume(entity);
				checkExceptions(result);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;		
	}

	public String getScheme() {return scheme;}
	public void setScheme(String scheme) {this.scheme=scheme;}
	public void setKennung(String login) {this.login= login;}
	public void setKennwort(String password) {this.password= password;}
	public String getServer() {return server;}
	public void setServer(String server) {this.server=server;}
	public String getPort() {return port;}
	public void setPort(String port) {this.port=port;}
	public String getPath() {return path;}
	public void setPath(String path) {this.path=path;}


}
