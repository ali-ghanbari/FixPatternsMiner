package com.produban.openbus.console.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

import org.apache.http.HttpEntity;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.produban.openbus.console.domain.CamposOrigen;
import com.produban.openbus.console.domain.MetricaBatch;
import com.produban.openbus.console.domain.MetricaOnLine;
import com.produban.openbus.console.domain.OrigenEstructurado;
import com.produban.openbus.console.domain.QueryCep;
import com.produban.openbus.console.domain.StreamCep;
import com.produban.openbus.console.dto.CreateOnLineForm;
import com.produban.openbus.console.dto.QueryDTO;
import com.produban.openbus.console.hive.HiveConnector;
import com.produban.openbus.console.service.MetricaBatchService;
import com.produban.openbus.console.service.MetricaOnLineService;
import com.produban.openbus.console.service.OrigenEstructuradoService;
import com.produban.openbus.console.service.QueryCepService;
import com.produban.openbus.console.util.HttpConnector;

@RequestMapping("/console/**")
@Controller
public class ConsoleController {

    private final static String ES_MAPPING_ID = "ID";
    private final static String ESTADO_EN_EJECUCION = "Ejecuci&oacute;n";
    private final static String ESTADO_ERROR = "Error";
    private final static String ESTADO_OK = "Ok";

    private final static String ESTADO_ONLINE_EN_CREACION = "En Creaci&oacute;n";
    private final static String ESTADO_ONLINE_EN_USO = " En Uso";
    private final static String ESTADO_ONLINE_PARA_BORRAR = "Para Borrar";
    private final static String ESTADO_ONLINE_BORRADA = "Borrada";
    
    private static Logger LOG = Logger.getLogger(ConsoleController.class);

    @Autowired
    private OrigenEstructuradoService origenEstructuradoService;

    @Autowired
    private MetricaBatchService metricaBatchService;

    @Autowired
    private MetricaOnLineService metricaOnLineService;

    @Autowired
    private QueryCepService queryCepService;
    
    // ***************** CREATE BATCH *****************

    @RequestMapping("/createbatch")
    public String getSources(Model model) {
	List<OrigenEstructurado> lstSources = origenEstructuradoService.findAllOrigenEstructuradoes();
	model.addAttribute("lstSources", lstSources);
	model.addAttribute("metricaBatch", new MetricaBatch());
	return "/console/createbatch";
    }

    @RequestMapping(value = "/getFieldsBySource")
    public String refreshFields(@RequestParam String idSource, Model model) {
	OrigenEstructurado origenEstructurado = origenEstructuradoService.findOrigenEstructurado(Long.valueOf(idSource));
	origenEstructurado.getHsCamposOrigen().size();
	Set<CamposOrigen> hsFields = origenEstructurado.getHsCamposOrigen();

	List<CamposOrigen> lstFields = new ArrayList<CamposOrigen>();
	for (CamposOrigen field : hsFields) {
	    lstFields.add(field);
	}
	Collections.sort(lstFields, new Comparator<CamposOrigen>(){
	    public int compare(CamposOrigen s1, CamposOrigen s2) {
	        return s1.getNombreCampo().compareTo(s2.getNombreCampo());
	    }
	});
	model.addAttribute("lstFields", lstFields);
	return "/console/createbatch :: #selFields";
    }

    
    @RequestMapping(value = "/createMetricBBDDES", method = RequestMethod.POST)
    public @ResponseBody CreateForm createMetricBBDDES(Model model, HttpSession session, @RequestBody final CreateForm form) {
	try {
	    MetricaBatch metricaBatch = createMetricBBDD(session, form);
	    metricaBatch = createMetricES(metricaBatch);

	    String isModif = form.getHidModif();
	    metricaBatch.setFechaUltModif(new Date());
	    if (isModif.equals("0")) {
		metricaBatch.setIsCreated(true);
		metricaBatch.setFechaCreacion(new Date());
		LOG.info("SAVE BBDD running....");
		metricaBatchService.saveMetricaBatch(metricaBatch);
		LOG.info("SAVE BBDD done");
	    }
	    else {
		metricaBatch.setIsUpdated(true);
		LOG.info("UPDATE BBDD running....");
		metricaBatchService.updateMetricaBatch(metricaBatch);
		LOG.info("UPDATE BBDD done");
	    }
	    form.setId(metricaBatch.getId().toString());
	}
	catch (Exception e) {
	    form.setId("ERROR");
	    form.setError(e.toString());
	}
	return form;
    }

    private MetricaBatch createMetricBBDD(HttpSession session, CreateForm form) throws Exception {
	MetricaBatch metricaBatch = new MetricaBatch();
	String isBatch = form.getRdMetricType();
	metricaBatch.setBatchMetricName(form.getBatchMetricName());
	metricaBatch.setBatchMetricDesc(form.getBatchMetricDesc());
	metricaBatch.setSourceId(form.getSourceId());
	metricaBatch.setEsCamposId(ES_MAPPING_ID);
	metricaBatch.setEsIndex(form.getSelSourceName());
	metricaBatch.setEsTimestamp(form.getEsTimestamp());
	metricaBatch.setEsType(form.getBatchMetricName());
	metricaBatch.setFechaUltModif(new Date());
	metricaBatch.setUsuarioCreacion((String) session.getAttribute("username"));
	metricaBatch.setTypeQuery(form.getTypeQuery());
	metricaBatch.setFromQuery(form.getFromQuery());
	metricaBatch.setSelectQuery(form.getSelectQuery());
	metricaBatch.setWhereQuery(form.getWhereQuery());
	metricaBatch.setPlanificacion(form.getPlanificacion());
	metricaBatch.setEsId(form.getEsId());

	String strSelectQuery = metricaBatch.getSelectQuery();
	String strFromQuery = metricaBatch.getFromQuery();
	String strWhereQuery = metricaBatch.getWhereQuery();

	StringBuilder insertQuery = new StringBuilder();
	insertQuery.append("INSERT OVERWRITE TABLE " + metricaBatch.getEsType() + " ");
	insertQuery.append(strSelectQuery + " ");
	insertQuery.append(strFromQuery + " ");
	insertQuery.append(strWhereQuery);

	metricaBatch.setQueryCode(insertQuery.toString());
	metricaBatch.setEstado(ESTADO_EN_EJECUCION);

	if (isBatch.equals("1")) {
	    metricaBatch.setIsBatch(true);
	}
	else {
	    metricaBatch.setIsBatch(false);
	}
	return metricaBatch;
    }

    private MetricaBatch createMetricES(MetricaBatch metricaBatch) throws Exception {
	Properties prop = new Properties();
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	InputStream resourceStream = loader.getResourceAsStream("META-INF/spring/environment.properties");
	prop.load(resourceStream);

	String strTypeQuery = metricaBatch.getTypeQuery();
	String strTimestamp = metricaBatch.getEsTimestamp();
	String strQuerySelect = metricaBatch.getSelectQuery();

	// Se crea el indice en elasticsearch
	createESIndex(metricaBatch.getEsIndex(), metricaBatch.getEsType(), strTypeQuery, prop, strTimestamp);

	String dropQuery = "DROP TABLE IF EXISTS " + metricaBatch.getEsType();

	StringBuilder externalQuery = new StringBuilder();
	externalQuery.append("CREATE EXTERNAL TABLE ");
	externalQuery.append(metricaBatch.getEsType());
	externalQuery.append("(");
	externalQuery.append(strTypeQuery);
	externalQuery.append(") ");
	externalQuery.append("STORED BY 'org.elasticsearch.hadoop.hive.EsStorageHandler' TBLPROPERTIES('es.resource' = '");
	externalQuery.append(metricaBatch.getEsIndex() + "/" + metricaBatch.getEsType());
	if (strQuerySelect.indexOf(" as ID") != -1) {
	    externalQuery.append("', 'es.mapping.id' = '");
	    externalQuery.append(metricaBatch.getEsCamposId());
	    externalQuery.append("', 'es.id.field' = '");
	    externalQuery.append(metricaBatch.getEsCamposId());
	}
	if (metricaBatch.getEsId() != null){
	    externalQuery.append("', 'es.mapping.id' = '");
	    externalQuery.append(metricaBatch.getEsId());
	    externalQuery.append("', 'es.id.field' = '");
	    externalQuery.append(metricaBatch.getEsId());
	}
	externalQuery.append("', 'es.index.auto.create' = 'true','es.nodes' = '");
	externalQuery.append(prop.getProperty("elastic.url.datanode1") + "," + prop.getProperty("elastic.url.datanode2") + "," + prop.getProperty("elastic.url.datanode3"));
	externalQuery.append("', 'es.port' = '" + prop.getProperty("elastic.port.datanodes"));
	if (strTimestamp != null && (!strTimestamp.equals(""))) {
	    externalQuery.append("', 'es.mapping.names' = '" + strTimestamp + ":@timestamp");
	}
	externalQuery.append("')");
	HiveConnector hiveConnector = new HiveConnector();
	hiveConnector.executeQuery(dropQuery);
	hiveConnector.executeQuery(externalQuery.toString());
	metricaBatch.setCreateCode(externalQuery.toString());
	LOG.info("UPDATE BBDD running....");
	metricaBatchService.updateMetricaBatch(metricaBatch);
	LOG.info("UPDATE BBDD done");
	return metricaBatch;
    }

    private void createESIndex(String index, String type, String strTypeQuery, Properties prop, String timestamp) throws Exception {
	HttpConnector httpConnector = new HttpConnector();

	Map<String, Map> mapPost = new HashMap<String, Map>();
	Map<String, Map> mapPut = new HashMap<String, Map>();
	Map<String, Map> map2 = new HashMap<String, Map>();
	Map<String, Map> map3 = new HashMap<String, Map>();
	boolean bTimeStamp = false;

	ObjectMapper objectMapper = new ObjectMapper();

	String existsUrl = "http://" + prop.getProperty("elastic.url.datanode1") + ":" + prop.getProperty("elastic.port.datanodes") + "/_stats/_indexes?pretty";
	HttpEntity entity = httpConnector.launchHttp(existsUrl, "GET", null);

	JSONParser parser = new JSONParser();
	Object obj = parser.parse(new BufferedReader(new InputStreamReader(entity.getContent())));
	JSONObject jsonObject = (JSONObject) obj;
	jsonObject = (JSONObject) jsonObject.get("indices");
	String json = null;

	mapPost.put("mappings", mapPut);
	mapPut.put(type, map2);
	map2.put("properties", map3);
	String[] array1 = strTypeQuery.split(",");
	Map<String, String> valuesMap = null;
	if (timestamp == null){
	    timestamp = "";
	}
	for (int i = 0; i < array1.length; i++) {
	    valuesMap = new HashMap<String, String>();
	    String[] array2 = array1[i].split(" ");
	    array2[1] = array2[1].toLowerCase();
	    array2[0] = array2[0].toLowerCase();
	    array2[1] = array2[1].replaceAll("\n", "");
	    array2[0] = array2[0].replaceAll("\n", "");

	    if ("string".equals(array2[1])) {
		valuesMap.put("type", "string");
		valuesMap.put("index", "not_analyzed");
		map3.put(array2[0], valuesMap);		
	    }
	    else if ("bigint".equals(array2[1]) || "int".equals(array2[1])) {
		valuesMap.put("type", "long");
		map3.put(array2[0], valuesMap);
	    }
	    else if ("timestamp".equals(array2[1])){ 
		valuesMap.put("type", "date");
		valuesMap.put("format", "dateOptionalTime");
		if(timestamp.equals(array2[1])){
		    bTimeStamp = true;
		    map3.put("@timestamp", valuesMap);
		}
		else{
		    map3.put(array2[0], valuesMap);
		}
	    }
	}

	if ((!bTimeStamp) && (!timestamp.equals(""))) {
	    valuesMap = new HashMap<String, String>();
	    valuesMap.put("type", "date");
	    valuesMap.put("format", "dateOptionalTime");
	    map3.put("@timestamp", valuesMap);
	}

	if (jsonObject.get(index) != null) { // Existe el indice, se lanza PUT
	    String putUrl = "http://" + prop.getProperty("elastic.url.datanode1") + ":" + prop.getProperty("elastic.port.datanodes") + "/" + index + "/" + type + "/_mapping";
	    json = objectMapper.writeValueAsString(mapPut);

	    entity = httpConnector.launchHttp(putUrl, "PUT", json);
	}
	else { // No existe el indice, se lanza POST
	    String postUrl = "http://" + prop.getProperty("elastic.url.datanode1") + ":" + prop.getProperty("elastic.port.datanodes") + "/" + index + "/";
	    json = objectMapper.writeValueAsString(mapPost);

	    entity = httpConnector.launchHttp(postUrl, "POST", json);
	}
    }

    @RequestMapping(value = "/insertIntoHive", method = RequestMethod.GET)
    public @ResponseBody String insertIntoHive(@RequestParam String idMetric) throws Exception {
	String response = "";
	MetricaBatch metricaBatch = null;
	try {
	    metricaBatch = metricaBatchService.findMetricaBatch(new Long(idMetric));
	    HiveConnector hiveConnector = new HiveConnector();
	    hiveConnector.executeQuery(metricaBatch.getQueryCode());
	    metricaBatch.setFechaUltModif(new Date());
	    metricaBatch.setEstado(ESTADO_OK);
	}
	catch (Exception e) {
	    metricaBatch.setEstado(ESTADO_ERROR);
	    metricaBatch.setError(e.toString());
	    response = "Error al insertar en Hive : " + e.toString();
	    LOG.info("UPDATE BBDD running....");
	    metricaBatchService.updateMetricaBatch(metricaBatch);
	    LOG.info("UPDATE BBDD done");
	    throw e;
	}
	LOG.info("UPDATE BBDD running....");
	metricaBatchService.updateMetricaBatch(metricaBatch);
	LOG.info("UPDATE BBDD done");
	return response;
    }

    // ***************** UPDATE BATCH *****************

    @RequestMapping(value = "/updateMetricBBDDES", method = RequestMethod.POST)
    public @ResponseBody CreateForm updateMetricBBDDES(Model model, HttpSession session, @RequestBody final CreateForm form) {
	try {
	    MetricaBatch metricaBatch = null;
	    metricaBatch = metricaBatchService.findMetricaBatch(new Long(form.getHidModif()));
	    Properties prop = new Properties();
	    ClassLoader loader = Thread.currentThread().getContextClassLoader();
	    InputStream resourceStream = loader.getResourceAsStream("META-INF/spring/environment.properties");
	    prop.load(resourceStream);
	    
	    // Se borra el indice de elasticsearch si existe
	    HttpConnector httpConnector = new HttpConnector();
	    String url = "http://" + prop.getProperty("elastic.url.datanode1") + ":" + prop.getProperty("elastic.port.datanodes") + "/" + metricaBatch.getEsIndex() + "/"
			+ metricaBatch.getEsType();	    
	    try {
		httpConnector.launchHttp(url, "DELETE", null);
	    }
	    catch (Exception e) {
		LOG.warn("Index not found in elasticsearch");
	    }	    

	    try {
		url = "http://" + prop.getProperty("elastic.url.datanode1") + ":" + prop.getProperty("elastic.port.datanodes") + "/" + metricaBatch.getEsIndex() + "/"
				+ form.getBatchMetricName();	    
		httpConnector.launchHttp(url, "DELETE", null);
	    }
	    catch (Exception e) {
		LOG.warn("Index not found in elasticsearch");
	    }	    
	    
	    // Se crea el indice en elasticsearch
	    createESIndex(metricaBatch.getEsIndex(), form.getBatchMetricName(), form.getTypeQuery(), prop, form.getEsTimestamp());

	    // Se borra la tabla de hive antigua
	    String dropQueryOld = "DROP TABLE IF EXISTS " + metricaBatch.getEsType();
	    // Se borra la tabla de hive antigua
	    String dropQueryNew = "DROP TABLE IF EXISTS " + form.getBatchMetricName();

	    
	    // Se crea la tabla de hive
	    StringBuilder externalQuery = new StringBuilder();
	    externalQuery.append("CREATE EXTERNAL TABLE ");
	    externalQuery.append(form.getBatchMetricName());
	    externalQuery.append("(");
	    externalQuery.append(form.getTypeQuery());
	    externalQuery.append(") ");
	    externalQuery.append("STORED BY 'org.elasticsearch.hadoop.hive.EsStorageHandler' TBLPROPERTIES('es.resource' = '");
	    externalQuery.append(metricaBatch.getEsIndex() + "/" + form.getBatchMetricName());
	    if (form.getSelectQuery().indexOf(" as ID") != -1) {
		externalQuery.append("', 'es.mapping.id' = '");
		externalQuery.append(metricaBatch.getEsCamposId());
		externalQuery.append("', 'es.id.field' = '");
		externalQuery.append(metricaBatch.getEsCamposId());
	    }
	    if (form.getEsId() != null){
		externalQuery.append("', 'es.mapping.id' = '");
		externalQuery.append(form.getEsId());
		externalQuery.append("', 'es.id.field' = '");
		externalQuery.append(form.getEsId());
	    }
	    
	    externalQuery.append("', 'es.index.auto.create' = 'true','es.nodes' = '");
	    externalQuery.append(prop.getProperty("elastic.url.datanode1") + "," + prop.getProperty("elastic.url.datanode2") + "," + prop.getProperty("elastic.url.datanode3"));
	    externalQuery.append("', 'es.port' = '" + prop.getProperty("elastic.port.datanodes"));
	    if (form.getEsTimestamp() != null && (!form.getEsTimestamp().equals(""))) {
		externalQuery.append("', 'es.mapping.names' = '" + form.getEsTimestamp() + ":@timestamp");
	    }
	    externalQuery.append("')");
	    HiveConnector hiveConnector = new HiveConnector();
	    hiveConnector.executeQuery(dropQueryOld);
	    hiveConnector.executeQuery(dropQueryNew);
	    hiveConnector.executeQuery(externalQuery.toString());

	    // Se actualiza en MYSQL
	    String strSelectQuery = form.getSelectQuery();
	    String strFromQuery = form.getFromQuery();
	    String strWhereQuery = form.getWhereQuery();

	    StringBuilder insertQuery = new StringBuilder();
	    insertQuery.append("INSERT OVERWRITE TABLE " + form.getBatchMetricName() + " ");
	    insertQuery.append(strSelectQuery + " ");
	    insertQuery.append(strFromQuery + " ");
	    insertQuery.append(strWhereQuery);
	    metricaBatch.setFechaUltModif(new Date());
	    metricaBatch.setIsUpdated(true);
	    metricaBatch.setSelectQuery(form.getSelectQuery());
	    metricaBatch.setWhereQuery(form.getWhereQuery());
	    metricaBatch.setFechaUltModif(new Date());
	    metricaBatch.setQueryCode(insertQuery.toString());
	    metricaBatch.setTypeQuery(form.getTypeQuery());
	    metricaBatch.setEstado(ESTADO_EN_EJECUCION);
	    metricaBatch.setBatchMetricDesc(form.getBatchMetricDesc());
	    metricaBatch.setBatchMetricName(form.getBatchMetricName());
	    metricaBatch.setEsTimestamp(form.getEsTimestamp());
	    metricaBatch.setPlanificacion(form.getPlanificacion());
	    metricaBatch.setUsuarioModificacion((String) session.getAttribute("username"));
	    metricaBatch.setEsId(form.getEsId());
	    String isBatch = form.getRdMetricType();
	    if (isBatch.equals("1")) {
		metricaBatch.setIsBatch(true);
	    }
	    else {
		metricaBatch.setIsBatch(false);
	    }
	    LOG.info("UPDATE BBDD running....");
	    metricaBatchService.updateMetricaBatch(metricaBatch);
	    LOG.info("UPDATE BBDD done");
	}
	catch (Exception e) {
	    form.setId("ERROR");
	    form.setError(e.toString());
	}
	return form;
    }

    @RequestMapping(value = "/insertIntoHiveRel", method = RequestMethod.POST)
    public @ResponseBody String insertIntoHiveRel(@RequestBody final CreateForm form) throws Exception {
	String response = "";
	MetricaBatch metricaBatch = null;
	try {
	    metricaBatch = metricaBatchService.findMetricaBatch(new Long(form.getHidModif()));
	    HiveConnector hiveConnector = new HiveConnector();
	    hiveConnector.executeQuery(metricaBatch.getQueryCode());
	    metricaBatch.setFechaUltModif(new Date());
	    metricaBatch.setEstado(ESTADO_OK);
	}
	catch (Exception e) {
	    metricaBatch.setEstado(ESTADO_ERROR);
	    metricaBatch.setError(e.toString());
	    response = "Error al insertar en Hive : " + e.toString();
	    LOG.info("UPDATE BBDD running....");
	    metricaBatchService.updateMetricaBatch(metricaBatch);
	    LOG.info("UPDATE BBDD done");
	    throw e;
	}
	LOG.info("UPDATE BBDD running....");
	metricaBatchService.updateMetricaBatch(metricaBatch);
	LOG.info("UPDATE BBDD done");
	return response;
    }

    @RequestMapping(value = "/reLaunchMetric", method = RequestMethod.GET)
    public @ResponseBody String reLaunchMetric(@RequestParam String idMetric, Model model) throws Exception {
	MetricaBatch metricaBatch = null;
	try {
	    metricaBatch = metricaBatchService.findMetricaBatch(new Long(idMetric));
	    metricaBatch.setFechaUltModif(new Date());
	    metricaBatch.setEstado(ESTADO_EN_EJECUCION);
	}
	catch (Exception e) {
	    metricaBatch.setEstado(ESTADO_ERROR);
	    metricaBatch.setError(e.toString());
	    LOG.info("UPDATE BBDD running....");
	    metricaBatchService.updateMetricaBatch(metricaBatch);
	    LOG.info("UPDATE BBDD done");
	    throw e;
	}
	LOG.info("UPDATE BBDD running....");
	metricaBatchService.updateMetricaBatch(metricaBatch);
	LOG.info("UPDATE BBDD done");
	return "";
    }

    @RequestMapping("/showbatch")
    public String showMetrics(Model model, HttpServletRequest request) {
	List<MetricaBatch> lstMetrics = metricaBatchService.findAllMetricaBatches();
	model.addAttribute("lstMetrics", lstMetrics);
	model.addAttribute("search", request.getParameter("hidSearch"));
	return "/console/showbatch";
    }

    @RequestMapping("/refresh")
    public String refreshMetrics(@RequestParam String search, Model model) {
	List<MetricaBatch> lstMetrics = metricaBatchService.findAllMetricaBatches();
	model.addAttribute("lstMetrics", lstMetrics);
	model.addAttribute("search", search);
	return "/console/showbatch";
    }

    @RequestMapping("/updateMetric")
    public String updateMetric(@RequestParam String idMetric, Model model) {
	List<OrigenEstructurado> lstSources = origenEstructuradoService.findAllOrigenEstructuradoes();
	model.addAttribute("lstSources", lstSources);
	MetricaBatch metricaBatch = metricaBatchService.findMetricaBatch(new Long(idMetric));
	model.addAttribute("metricaBatch", metricaBatch);
	return "/console/createbatch";
    }

    @RequestMapping(value = "/deleteMetric", method = RequestMethod.GET)
    public @ResponseBody String deleteMetric(@RequestParam String idMetric, Model model) throws Exception {
	Properties prop = new Properties();
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	InputStream resourceStream = loader.getResourceAsStream("META-INF/spring/environment.properties");
	prop.load(resourceStream);

	MetricaBatch metricaBatch = metricaBatchService.findMetricaBatch(new Long(idMetric));
	HttpConnector httpConnector = new HttpConnector();
	String url = "http://" + prop.getProperty("elastic.url.datanode1") + ":" + prop.getProperty("elastic.port.datanodes") + "/" + metricaBatch.getEsIndex() + "/"
		+ metricaBatch.getEsType();
	try {
	    httpConnector.launchHttp(url, "DELETE", null);
	}
	catch (Exception e) {
	    LOG.warn("Index not found in elasticsearch");
	}	    

	HiveConnector hiveConnector = new HiveConnector();
	hiveConnector.executeQuery("DROP TABLE " + metricaBatch.getEsType());
	LOG.info("DELETE BBDD running....");
	metricaBatchService.deleteMetricaBatch(metricaBatch);
	LOG.info("DELETE BBDD done");
	return "";
    }

    @RequestMapping("/menu")
    public String menu(Model model, HttpSession session) {
	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	String name = auth.getName();
	session.setAttribute("username", name);
	return "/console/menu";
    }

 // ***************** CREATE ONLINE *****************    
    @RequestMapping("/createonline")
    public String getSourcesOnLine(Model model, HttpSession session) {
	List<OrigenEstructurado> lstSources = origenEstructuradoService.findAllOrigenEstructuradoes();
	model.addAttribute("lstSources", lstSources);
	model.addAttribute("metricaOnLine", new MetricaOnLine());
	
	return "/console/createonline";
    }

    @RequestMapping(value = "/getFieldsBySourceOnLine")
    public String refreshFieldsOnLine(@RequestParam String idSource, Model model) {
	OrigenEstructurado origenEstructurado = origenEstructuradoService.findOrigenEstructurado(Long.valueOf(idSource));
	origenEstructurado.getHsCamposOrigen().size();
	Set<CamposOrigen> hsFields = origenEstructurado.getHsCamposOrigen();

	List<CamposOrigen> lstFields = new ArrayList<CamposOrigen>();
	for (CamposOrigen field : hsFields) {
	    lstFields.add(field);
	}
	Collections.sort(lstFields, new Comparator<CamposOrigen>(){
	    public int compare(CamposOrigen s1, CamposOrigen s2) {
	        return s1.getNombreCampo().compareTo(s2.getNombreCampo());
	    }
	});
	model.addAttribute("lstFields", lstFields);
	return "/console/createonline :: #selFields";
    }    
    
    @RequestMapping(value = "/saveQuery", method = RequestMethod.POST)
    public @ResponseBody QueryDTO saveQuery(HttpServletRequest request, @RequestBody final QueryDTO queryDTO, Model model) {
	List<QueryDTO> queriesSession = (List<QueryDTO>) request.getSession().getAttribute("queriesSession");
	int idQuery = 1;
	if (queriesSession == null){
	    queriesSession = new ArrayList<QueryDTO>();
	}
	else if(queriesSession.size() > 0){
	    List lstIds = new ArrayList();
	    for (QueryDTO queryDTOAux : queriesSession){
		lstIds.add(queryDTOAux.getId());
	    }
	    Collections.sort(lstIds);
	    String lastId = (String) lstIds.get(lstIds.size() - 1);
	    idQuery = Integer.valueOf(lastId) + 1 ;	    
	}
	queryDTO.setId(new Integer(idQuery).toString());
	queriesSession.add(queryDTO);
	request.getSession().setAttribute("queriesSession", queriesSession);
	model.addAttribute("queriesSession", request.getSession().getAttribute("queriesSession"));
	return queryDTO;
    }

    @RequestMapping(value = "/updateQuery", method = RequestMethod.POST)
    public @ResponseBody QueryDTO updateQuery(HttpServletRequest request, @RequestBody final QueryDTO queryDTO, Model model) {
	List<QueryDTO> queriesSession = (List<QueryDTO>) request.getSession().getAttribute("queriesSession");
	for (QueryDTO queryDTOAux : queriesSession){
	    if(queryDTOAux.getId().equals(queryDTO.getId())){
		queryDTOAux.setQueryAs(queryDTO.getQueryAs());
		queryDTOAux.setQueryFrom(queryDTO.getQueryFrom());
		queryDTOAux.setQueryGroupBy(queryDTO.getQueryGroupBy());
		queryDTOAux.setQueryInto(queryDTO.getQueryInto());
		queryDTOAux.setQueryName(queryDTO.getQueryName());
		queryDTOAux.setRdCallback(queryDTO.getRdCallback());
	    }
	}
	request.getSession().setAttribute("queriesSession", queriesSession);
	model.addAttribute("queriesSession", request.getSession().getAttribute("queriesSession"));
	return queryDTO;
    }

    @RequestMapping(value = "/findQueryById", method = RequestMethod.POST)
    public @ResponseBody QueryDTO findQueryById(@RequestParam String idQuery, HttpServletRequest request) {
	QueryDTO queryDTO = null;
	List<QueryDTO> queriesSession = (List<QueryDTO>) request.getSession().getAttribute("queriesSession");
	for (QueryDTO queryDTOAux : queriesSession){
	    if(queryDTOAux.getId().equals(idQuery)){
		queryDTO = queryDTOAux;
		break;
	    }
	}
	return queryDTO;
    }
    
    @RequestMapping(value= "/deleteQuery")
    public String deleteQuery(@RequestParam String idQuery, HttpServletRequest request, Model model) {
	List<QueryDTO> queriesSession = (List<QueryDTO>) request.getSession().getAttribute("queriesSession");
	int index = 0;
	for (QueryDTO queryDTOAux : queriesSession){
	    if(queryDTOAux.getId().equals(idQuery)){
		break;
	    }
	    index++;
	}
	queriesSession.remove(index);
	request.getSession().setAttribute("queriesSession", queriesSession);
	model.addAttribute("queriesSession", request.getSession().getAttribute("queriesSession"));
	return "/console/createonline :: #divTable";
    }

    @RequestMapping("/getQueries")
    public String getQueries(Model model, HttpServletRequest request) {
	model.addAttribute("queriesSession", request.getSession().getAttribute("queriesSession"));
	return "/console/createonline :: #divTable";
    }
    
    
    @RequestMapping(value = "/createOnLineMetric", method = RequestMethod.POST)
    public @ResponseBody CreateOnLineForm createOnLineMetric(Model model, HttpServletRequest request, @RequestBody final CreateOnLineForm form) {
	try {
	    String isModif = form.getHidModif();
	    MetricaOnLine metricaOnLine = null;
	    StreamCep streamCep = null;
	    if (isModif.equals("0")) {
		metricaOnLine = new MetricaOnLine();
		streamCep = new StreamCep();
	    }
	    else{
		metricaOnLine = metricaOnLineService.findMetricaOnLine(new Long(isModif));
		streamCep = metricaOnLine.getStreamCep();
	    }
	    

	    OrigenEstructurado origen = origenEstructuradoService.findOrigenEstructurado(new Long(form.getSourceId()));
	    streamCep.setOrigenEstructurado(origen);
	    streamCep.setStreamName(form.getStreamName());
	    streamCep.setStreamFields(form.getStreamFields());
	    streamCep.setStreamFinal("define stream " + streamCep.getStreamName() + " (" + streamCep.getStreamFields() + ");");
	    streamCep.setStreamCepId(null);
	    
	    QueryCep queryCep = null;
	    String queryFinal = null;
	    String outputFieldNames = null;
	    Set<QueryCep> sQueryCep = new HashSet<QueryCep>();

	    List<QueryDTO> queriesSession = (List<QueryDTO>) request.getSession().getAttribute("queriesSession");
	    form.setQueries(queriesSession);
	    for(QueryDTO queryDTO : form.getQueries()){
		queryCep = new QueryCep();
		
		outputFieldNames = queryDTO.getQueryAs();		    
		outputFieldNames = outputFieldNames.toLowerCase();
		String [] arrOutputFieldNames = outputFieldNames.split(" as ");
		List lstOutputFieldNames = new ArrayList();
		for(int i=0;i<arrOutputFieldNames.length;i++){
		    if (i == arrOutputFieldNames.length -1){
			break;
		    }
		    else{
			if (i == 0){
			    lstOutputFieldNames.add(arrOutputFieldNames[i]);
			}
			else{
			    String [] cc = arrOutputFieldNames[i].split(",");
			    lstOutputFieldNames.add(cc[1]);
			}
		    }
		}
		
		queryCep.setOutputFieldNames(lstOutputFieldNames.toString());
		
		if (queryDTO.getRdCallback() == null){
			queryCep.setHasCallback(false);
		}
		else{
		    if (queryDTO.getRdCallback().equals("") || queryDTO.getRdCallback().equals("0")){
			queryCep.setHasCallback(false);
		    }
		    else{
			queryCep.setHasCallback(true);
		    }
		}
		
		queryCep.setGroupBy(queryDTO.getQueryGroupBy());
		queryCep.setOutputFieldUser(queryDTO.getQueryAs());
		queryCep.setOutputStream(queryDTO.getQueryInto());
		queryCep.setQueryCepId(null);
		queryCep.setQueryDefinition(queryDTO.getQueryFrom());
		queryCep.setQueryName(queryDTO.getQueryName());
		queryCep.setToRemove(false);
		
		queryFinal = queryDTO.getQueryFrom() + " " + queryDTO.getQueryInto() + " " + queryDTO.getQueryAs() + " " + queryDTO.getQueryGroupBy();
		queryCep.setQueryFinal(queryFinal);
		sQueryCep.add(queryCep);
	    }
	    

	    metricaOnLine.setHsQueryCep(sQueryCep);
	    metricaOnLine.setStreamCep(streamCep);
	    metricaOnLine.setOnLineMetricDesc(form.getOnLineMetricDesc());
	    metricaOnLine.setOnLineMetricName(form.getOnLineMetricName());
	    metricaOnLine.setUsuarioCreacion((String) request.getSession().getAttribute("username"));
	    metricaOnLine.setEsCamposId(ES_MAPPING_ID);
	    metricaOnLine.setEsIndex(form.getSelSourceName());
	    metricaOnLine.setEsType(form.getOnLineMetricName());
	    metricaOnLine.setFechaUltModif(new Date());
	    metricaOnLine.setEstado(ESTADO_ONLINE_EN_CREACION);
	    
	    if (isModif.equals("0")) {
		metricaOnLine.setIsCreated(true);
		metricaOnLine.setFechaCreacion(new Date());
		LOG.info("SAVE BBDD running....");
		metricaOnLineService.saveMetricaOnLine(metricaOnLine);
		LOG.info("SAVE BBDD done");
	    }
	    else {
		metricaOnLine.setIsUpdated(true);
		LOG.info("UPDATE BBDD running....");
		metricaOnLineService.updateMetricaOnLine(metricaOnLine);
		LOG.info("UPDATE BBDD done");
	    }
	    form.setId(metricaOnLine.getId().toString());
	}
	catch (Exception e) {
	    form.setId("ERROR");
	    form.setError(e.toString());
	    LOG.error(e);
	}
	return form;
    }    
    
    
    @RequestMapping("/showonline")
    public String showOnLine(Model model, HttpServletRequest request) {
	List<MetricaOnLine> lstMetrics = new ArrayList<MetricaOnLine>();
	for (MetricaOnLine metricaOnLine : metricaOnLineService.findAllMetricaOnLines()){
	    if(metricaOnLine.getStreamCep().getToRemove() == null){
		lstMetrics.add(metricaOnLine);
	    }
	    else if(! metricaOnLine.getStreamCep().getToRemove()){
		lstMetrics.add(metricaOnLine);
	    }
	}
	model.addAttribute("lstMetrics", lstMetrics);
	model.addAttribute("search", request.getParameter("hidSearch"));
	return "/console/showonline";
    }

    @RequestMapping("/refreshOnLine")
    public String refreshOnLineMetrics(@RequestParam String search, Model model) {
	List<MetricaOnLine> lstMetrics = metricaOnLineService.findAllMetricaOnLines();
	model.addAttribute("lstMetrics", lstMetrics);
	model.addAttribute("search", search);
	return "/console/showonline";
    }

    @RequestMapping("/updateOnLineMetric")
    public String updateOnLineMetric(@RequestParam String idMetric, Model model, HttpServletRequest request) {
	List<OrigenEstructurado> lstSources = origenEstructuradoService.findAllOrigenEstructuradoes();
	model.addAttribute("lstSources", lstSources);
	MetricaOnLine metricaOnLine = metricaOnLineService.findMetricaOnLine(new Long(idMetric));
	model.addAttribute("metricaOnLine", metricaOnLine);
	List<QueryDTO> queriesBBDD = new ArrayList<QueryDTO>();
	QueryDTO queryDTO = null;
	for (QueryCep queryCep : metricaOnLine.getHsQueryCep()){
	    queryDTO = new QueryDTO();
	    queryDTO.setId(queryCep.getId().toString());
	    queryDTO.setQueryAs(queryCep.getOutputFieldUser());
	    queryDTO.setQueryFrom(queryCep.getQueryDefinition());
	    queryDTO.setQueryGroupBy(queryCep.getGroupBy());
	    queryDTO.setQueryInto(queryCep.getOutputStream());
	    queryDTO.setQueryName(queryCep.getQueryName());
	    if (queryCep.getHasCallback()){
		queryDTO.setRdCallback("1");
	    }
	    else{
		queryDTO.setRdCallback("0");
	    }
	    
	    queriesBBDD.add(queryDTO);
	}
	model.addAttribute("queriesSession", queriesBBDD);
	request.getSession().setAttribute("queriesSession", queriesBBDD);
	return "/console/createonline";
    }

    @RequestMapping(value = "/deleteOnLineMetric", method = RequestMethod.GET)
    public @ResponseBody String deleteOnLineMetric(@RequestParam String idMetric, Model model) throws Exception {
	MetricaOnLine metricaOnLine = metricaOnLineService.findMetricaOnLine(new Long(idMetric));
	metricaOnLine.setIsUpdated(true) ;
	metricaOnLine.getStreamCep().setToRemove(true);
	metricaOnLineService.updateMetricaOnLine(metricaOnLine);
	for (QueryCep queryCep : metricaOnLine.getHsQueryCep()){
	    queryCep.setToRemove(true);
	    queryCepService.updateQueryCep(queryCep);
	}
	LOG.info("UPDATE BBDD running....");
	LOG.info("UPDATE BBDD done");
	return "";
    }
    
    // ***************** TEST *****************
    
    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public String test(Model model) {
	List<MetricaOnLine> lstMetricaOnLine = null;
	try {/*
	    JobDetail job = new JobDetail();
	    	job.setName("dummyJobName");
	    	job.setJobClass(com.produban.openbus.console.util.ScheduledJob.class);
	 
	    	CronTrigger trigger = new CronTrigger();
	    	trigger.setName("dummyTriggerName");
	    	trigger.setCronExpression("0/30 * * * * ?");
	 
	    	//schedule it
	    	Scheduler scheduler = new StdSchedulerFactory().getScheduler();
	    	scheduler.start();
	    	scheduler.scheduleJob(job, trigger);
	    	*/
		lstMetricaOnLine = metricaOnLineService.findAllMetricaOnLines();
		for (MetricaOnLine metricaOnLine : lstMetricaOnLine){
		    ObjectMapper mapper = new ObjectMapper();
		    try {
			model.addAttribute("json",mapper.writeValueAsString(metricaOnLine));
			MetricaOnLine obj = mapper.readValue(mapper.writeValueAsString(metricaOnLine), MetricaOnLine.class);
			System.out.println(obj);
		    }
		    catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
	}
	catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return "/test";
    }
    
    @RequestMapping(value = "/test2", method = RequestMethod.GET)
    public @ResponseBody List<MetricaOnLine> test2(Model model) {
	List<MetricaOnLine> lstMetricaOnLine = null;
	try {
		lstMetricaOnLine = metricaOnLineService.findAllMetricaOnLines();
		for (MetricaOnLine metricaOnLine : lstMetricaOnLine){
		    ObjectMapper mapper = new ObjectMapper();
		    try {
			model.addAttribute("json",mapper.writeValueAsString(metricaOnLine));
			MetricaOnLine obj = mapper.readValue(mapper.writeValueAsString(metricaOnLine), MetricaOnLine.class);
			System.out.println(obj);
		    }
		    catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
	}
	catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return lstMetricaOnLine;
    }

    
    
    
    
    
    
    // METODOS ANTIGUOS
    @RequestMapping(value = "/createBatchMetric")
    public String createBatchMetric(@Valid MetricaBatch metricaBatch, BindingResult bindingResult, Model model, HttpServletRequest request, HttpSession session) {
	if (!bindingResult.hasErrors()) {
	    try {
		String isModif = request.getParameter("hidModif");
		String isBatch = request.getParameter("rdMetricType");
		metricaBatch.setCreateCode("");
		metricaBatch.setEsCamposId(ES_MAPPING_ID);
		metricaBatch.setEsIndex(request.getParameter("selSourceName"));
		metricaBatch.setEsType(metricaBatch.getBatchMetricName());
		metricaBatch.setFechaUltModif(new Date());
		metricaBatch.setUsuarioCreacion((String) session.getAttribute("username"));
		if (isBatch.equals("1")) {
		    metricaBatch.setIsBatch(true);
		    metricaBatch.setPlanificacion("");
		}
		else {
		    metricaBatch.setIsBatch(false);
		    metricaBatch.setPlanificacion("");
		}
		if (isModif.equals("0")) {
		    metricaBatch.setFechaCreacion(new Date());
		    metricaBatch.setIsCreated(true); // Esto ira cuando se lance
						     // la metrica
		    metricaBatch.setIsUpdated(false);
		    runMetricAtHive(metricaBatch, request);
		    metricaBatchService.saveMetricaBatch(metricaBatch);
		}
		else {
		    metricaBatch.setIsCreated(false);
		    metricaBatch.setIsUpdated(true); // Esto ira cuando se lance
						     // la metrica
		    runMetricAtHive(metricaBatch, request);
		    metricaBatchService.updateMetricaBatch(metricaBatch);
		}
		model.addAttribute("errorCreateBatchMetric", false);
	    }
	    catch (Exception e) {
		model.addAttribute("messageError", e.getMessage());
		model.addAttribute("errorCreateBatchMetric", true);
	    }
	}
	else {
	    model.addAttribute("messageError", "Error de bind: " + bindingResult);
	    model.addAttribute("errorCreateBatchMetric", true);
	}
	return "/menu";
    }

    private void runMetricAtHive(MetricaBatch metricaBatch, HttpServletRequest request) throws Exception {
	try {
	    String externalQuery = null;
	    String insertQuery = null;
	    if (metricaBatch.getIsCreated()) {
		externalQuery = buildCreateExternal(metricaBatch, request);
		insertQuery = buildInsert(metricaBatch, request);
		metricaBatch.setCreateCode(externalQuery);
		metricaBatch.setQueryCode(insertQuery);
	    }
	    else {
		externalQuery = metricaBatch.getCreateCode();
		insertQuery = metricaBatch.getQueryCode();
	    }
	    String dropQuery = "DROP TABLE " + metricaBatch.getEsType();

	    HiveConnector hiveConnector = new HiveConnector();
	    hiveConnector.executeQuery(dropQuery);
	    hiveConnector.executeQuery(externalQuery);
	    hiveConnector.executeQuery(insertQuery);
	}
	catch (Exception e) {
	    e.printStackTrace();
	    throw e;
	}
    }

    private String buildCreateExternal(MetricaBatch metricaBatch, HttpServletRequest request) throws Exception {
	Properties prop = new Properties();
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	InputStream resourceStream = loader.getResourceAsStream("META-INF/spring/environment.properties");
	prop.load(resourceStream);

	/*
	 * String strQuerySelect = (String) request.getParameter("selectQuery");
	 * Map<String, String> hmSelectFields = new LinkedHashMap<String,
	 * String>(); Map<String, String> hmSelectFieldsModif = new
	 * LinkedHashMap<String, String>(); strQuerySelect =
	 * strQuerySelect.substring(strQuerySelect.indexOf("ID,") + 3,
	 * strQuerySelect.length()); String key = null; String value = null; for
	 * (String firstCharacter : strQuerySelect.split(",")) { for (String
	 * secondCharacter : firstCharacter.split(" as ")) { if (key != null &&
	 * value != null) { hmSelectFields.put(key, value); key = null; value =
	 * null; } if (key == null) { key = secondCharacter; } else if (value ==
	 * null) { value = secondCharacter; } } } hmSelectFields.put(key,
	 * value); for (Map.Entry entry : hmSelectFields.entrySet()) { String
	 * keyModif = entry.getKey().toString(); String valueModif =
	 * entry.getValue().toString(); if (keyModif.indexOf("(") != -1) {
	 * keyModif = keyModif.substring(0, keyModif.indexOf("(")); }
	 * hmSelectFieldsModif.put(keyModif, valueModif); }
	 * 
	 * StringBuilder externalQuery = new StringBuilder();
	 * externalQuery.append("CREATE EXTERNAL TABLE ");
	 * externalQuery.append(metricaBatch.getEsType());
	 * externalQuery.append("(");
	 * externalQuery.append(metricaBatch.getEsCamposId() + " STRING,");
	 * String type = null; int cont = 0; for (Map.Entry entry :
	 * hmSelectFieldsModif.entrySet()) { cont++; String keyModif =
	 * entry.getKey().toString().trim(); switch (keyModif) { case "MAX":
	 * type = "STRING"; break; case "MIN": type = "STRING"; break; case
	 * "YEAR": type = "BIGINT"; break; case "MONTH": type = "BIGINT"; break;
	 * case "SUM": type = "BIGINT"; break; case "COUNT": type = "BIGINT";
	 * break; default: type = "STRING"; break; } if
	 * (hmSelectFieldsModif.size() != cont) {
	 * externalQuery.append(entry.getValue().toString() + " " + type + ",");
	 * } else { externalQuery.append(entry.getValue().toString() + " " +
	 * type); } }
	 */
	String strQueryType = (String) request.getParameter("typeQuery");
	String strTimestamp = (String) request.getParameter("esTimestamp");
	String strQuerySelect = (String) request.getParameter("selectQuery");
	StringBuilder externalQuery = new StringBuilder();
	externalQuery.append("CREATE EXTERNAL TABLE ");
	externalQuery.append(metricaBatch.getEsType());
	externalQuery.append("(");
	externalQuery.append(strQueryType);
	externalQuery.append(") ");
	externalQuery.append("STORED BY 'org.elasticsearch.hadoop.hive.EsStorageHandler' TBLPROPERTIES('es.resource' = '");
	externalQuery.append(metricaBatch.getEsIndex() + "/" + metricaBatch.getEsType());
	if (strQuerySelect.indexOf(" as ID") != -1) {
	    externalQuery.append("', 'es.mapping.id' = '");
	    externalQuery.append(metricaBatch.getEsCamposId());
	    externalQuery.append("', 'es.id.field' = '");
	    externalQuery.append(metricaBatch.getEsCamposId());
	}
	externalQuery.append("', 'es.index.auto.create' = 'true','es.nodes' = '");
	externalQuery.append(prop.getProperty("elastic.url.datanode1") + "," + prop.getProperty("elastic.url.datanode2") + "," + prop.getProperty("elastic.url.datanode3"));
	externalQuery.append("', 'es.port' = '" + prop.getProperty("elastic.port.datanodes"));
	if (strTimestamp != null && (!strTimestamp.equals(""))) {
	    externalQuery.append("', 'es.mapping.names' = '" + request.getParameter("txtTimestamp") + ":@timestamp");
	}
	externalQuery.append("')");

	return externalQuery.toString();
    }

    private String buildInsert(MetricaBatch metricaBatch, HttpServletRequest request) throws Exception {
	String strQuerySelect = (String) request.getParameter("selectQuery");
	String strQueryFrom = (String) request.getParameter("fromQuery");
	String strQueryWhere = (String) request.getParameter("whereQuery");

	StringBuilder insertQuery = new StringBuilder();
	insertQuery.append("INSERT OVERWRITE TABLE " + metricaBatch.getEsType() + " ");
	insertQuery.append(strQuerySelect + " ");
	insertQuery.append(strQueryFrom + " ");
	insertQuery.append(strQueryWhere);

	return insertQuery.toString();
    }
}
