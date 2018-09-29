import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.produban.openbus.console.domain.MetricaOnLine;
import com.produban.openbus.console.dto.QueryDTO;



public class Test {

    public static void main(String[] args) {/*
	String s = "SELECT " +
	"CONCAT(YEAR(eventTimeStamp),MONTH(eventTimeStamp)) as ID,"+
	"MONTH(eventTimeStamp) as MES,"+
	"YEAR(eventTimeStamp) as ANO,"+
	"MAX(eventTimeStamp) as  ULTIMO,"+
	"test as  TEST,"+
	"MIN(eventTimeStamp) as PRIMERO";
	
	String ss = "SELECT "+
	"CONCAT(ANO,MES) as ID,	MES,ANO,SUM(TAMANO*cuenta) TAMANO_ok,sum(cuenta) as CUENTA_OK "+
	"FROM(SELECT MSGID,MONTH(eventTimeStamp) as MES,YEAR(eventTimeStamp) as ANO,count(1) as cuenta "+
	"FROM POSTFIX_LOGS WHERE DSN in('2.0.0','2.6.0','2.4.0') and AMAVISID ='null' group by MSGID,MONTH(eventTimeStamp),YEAR(eventTimeStamp)) correo "+
	"JOIN (SELECT MSGID,SUM(SIZE) as TAMANO";
	
	Map<String,String> hmSelectFields = new LinkedHashMap<String,String>();
	Map<String,String> hmSelectFieldsModif = new LinkedHashMap<String,String>();
	ss = ss.substring(ss.indexOf("ID,")+3,ss.length());
	String key = null;
    	String value = null;
	for (String firstCharacter : ss.split(",")){
	    for (String secondCharacter : firstCharacter.split(" as ")){
		if (key != null && value != null){
		    hmSelectFields.put(key, value);
		    key = null;
		    value = null;
		}
		if (key == null){
		    key = secondCharacter;
		}
		else if(value == null){
		    value = secondCharacter;
		}
	    }
	}
	hmSelectFields.put(key, value);
	System.out.println(hmSelectFields);
	for (Map.Entry entry : hmSelectFields.entrySet()){
	    String keyModif = entry.getKey().toString();
	    String valueModif = entry.getValue().toString();
	    if (keyModif.indexOf("(") != -1){
		keyModif = keyModif.substring(0,keyModif.indexOf("("));
	    }
	    hmSelectFieldsModif.put(keyModif,valueModif);
	}
	System.out.println(hmSelectFieldsModif);
	*/
	
	/*
	HttpConnector httpConnector = new HttpConnector();
	String existsUrl = "http://localhost:9200/_stats/_indexes?pretty";
	HttpEntity entity = null;
	try {
	    entity = httpConnector.launchHttp(existsUrl,"GET",null);
	}
	catch (Exception e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	
	JSONParser parser = new JSONParser();
	Object obj = null;
	try {
	    try {
		obj = parser.parse(new BufferedReader(new InputStreamReader(entity.getContent())));
	    }
	    catch (IllegalStateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }	    
	}
	catch (ParseException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	}
	JSONObject jsonObject = (JSONObject) obj;
	jsonObject = (JSONObject)jsonObject.get("indices");
	
	String json = null;
	String strTypeQuery = "ID string,mes bigint,ano bigint,ultimo string,primero string";
	String type = "uno";
	
	Map<String, Map> map1 = new HashMap<String, Map>();
	Map<String, Map> map2 = new HashMap<String, Map>();
	Map<String, Map> map3 = new HashMap<String, Map>();
	
	map1.put(type,map2);
	map2.put("properties", map3);
	String [] array1 = strTypeQuery.split(",");
	Map<String, String> valuesMap = null;
	for (int i=0;i<array1.length;i++){
	    valuesMap = new HashMap<String, String>();
	    String [] array2 = array1[i].split(" ");
	    array2[1] = array2[1].toLowerCase();
	    array2[1] = array2[1].replaceAll("\n", "");
	    array2[0] = array2[0].replaceAll("\n", ""); 
	    
	    if("bigint".equals(array2[1]) || "int".equals(array2[1])){
		array2[1] = "long";
	    }
	    valuesMap.clear();
	    valuesMap.put("type", array2[1]);
	    if ("string".equals(array2[1])){
		valuesMap.put("index", "not_analyzed");		
	    }
	    map3.put(array2[0], valuesMap);
	}
	if (" " != null && (! " ".equals(""))){
	    valuesMap = new HashMap<String, String>();
	    valuesMap.put("type", "date");
	    valuesMap.put("format", "dateOptionalTime");
	    map3.put("@timestamp", valuesMap);
	}		
	
	ObjectMapper objectMapper = new ObjectMapper();
	try {
	    json = objectMapper.writeValueAsString(map1);
	}
	catch (JsonGenerationException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	catch (JsonMappingException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	if (jsonObject.get("new4") != null){ // Existe el indice, se lanza PUT
	    String putUrl = "http://localhost:9200/new/uno/_mapping";
	    try {
		entity = httpConnector.launchHttp(putUrl,"PUT",json);
	    }
	    catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}
	else{ // No existe el indice, se lanza POST
	    String postUrl = "http://localhost:9200/new4/";
	    try {
		entity = httpConnector.launchHttp(postUrl,"POST",json);
	    }
	    catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	}*/
	String xx = "tam.MSGID AS tam,xx as c, ss as s,resp.DSN as dsn,sum(tam.SIZE) as TAMANO";
	xx = xx.toLowerCase();
	String [] aa = xx.split(" as ");
	List bb = new ArrayList();
	for(int i=0;i<aa.length;i++){
	    if (i == aa.length -1){
		break;
	    }
	    else{
		if (i == 0){
		    bb.add(aa[i]);
		}
		else{
		    String [] cc = aa[i].split(",");
		    bb.add(cc[1]);
		}
	    }
	}
	System.out.println(bb);

/*	
	String url = "http://localhost:8080//web_console/online/findAllOnlineMetrics";
	HttpClient httpClient = new DefaultHttpClient();
	HttpGet requestGet = new HttpGet(url);
	HttpResponse response;
	try {
	    response = httpClient.execute(requestGet);
	    HttpEntity entity = response.getEntity();
	    String json = EntityUtils.toString(entity);
	    ObjectMapper mapper = new ObjectMapper();
	    List<MetricaOnLine> lstMetricaOnLine = mapper.readValue(json,mapper.getTypeFactory().constructCollectionType(List.class, MetricaOnLine.class));	    
	    System.out.println(lstMetricaOnLine.get(0).getOnLineMetricName());
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
	*/
	/*
	    List lstIds = new ArrayList();
	    lstIds.add("1");
	    lstIds.add("5");
	    lstIds.add("9");
	    lstIds.add("3");
	    lstIds.add("2");
	    lstIds.add("4");
	    Collections.sort(lstIds);
	    String lastId = (String) lstIds.get(lstIds.size() - 1);
	    int idQuery = Integer.valueOf(lastId) + 1 ;
	    System.out.println(idQuery);*/

    }
}
