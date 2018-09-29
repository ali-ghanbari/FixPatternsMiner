/*
 * Copyright © 2013 VillageReach.  All Rights Reserved.  This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package org.openlmis.web.controller;

import lombok.NoArgsConstructor;
import org.openlmis.core.exception.DataException;
import org.openlmis.core.message.OpenLmisMessage;
import org.openlmis.db.service.DbService;
import org.openlmis.upload.RecordHandler;
import org.openlmis.upload.exception.UploadException;
import org.openlmis.upload.model.AuditFields;
import org.openlmis.upload.model.ModelClass;
import org.openlmis.upload.parser.CSVParser;
import org.openlmis.web.model.UploadBean;
import org.openlmis.web.response.OpenLmisResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.openlmis.web.response.OpenLmisResponse.response;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.TEXT_HTML_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@Controller
@NoArgsConstructor
public class UploadController extends BaseController {

  public static final String SELECT_UPLOAD_TYPE = "upload.select.type";
  public static final String INCORRECT_FILE = "upload.incorrect.file";
  public static final String FILE_IS_EMPTY = "upload.file.empty";
  public static final String INCORRECT_FILE_FORMAT = "upload.incorrect.file.format";
  public static final String UPLOAD_FILE_SUCCESS = "upload.file.successfull";
  public static final String SUCCESS = "success";
  public static final String ERROR = "error";
  public static final String SUPPORTED_UPLOADS = "supportedUploads";

  @Autowired
  private CSVParser csvParser;
  @Autowired
  DbService dbService;

  @Resource
  private Map<String, UploadBean> uploadBeansMap;

  public UploadController(CSVParser csvParser, Map<String, UploadBean> uploadBeansMap, DbService dbService) {
    this.csvParser = csvParser;
    this.uploadBeansMap = uploadBeansMap;
    this.dbService = dbService;
  }

  @RequestMapping(value = "/upload", method = POST)
  @PreAuthorize("@permissionEvaluator.hasPermission(principal,'UPLOADS')")
  public ResponseEntity<OpenLmisResponse> upload(MultipartFile csvFile, String model, HttpServletRequest request) {
    try {
      OpenLmisMessage errorMessage = validateFile(model, csvFile);
      if (errorMessage != null) {
        return errorResponse(errorMessage);
      }

      int initialRecordCount = dbService.getCount(uploadBeansMap.get(model).getTableName());
      Date currentTimestamp = dbService.getCurrentTimestamp();

      RecordHandler recordHandler = uploadBeansMap.get(model).getRecordHandler();
      ModelClass modelClass = new ModelClass(uploadBeansMap.get(model).getImportableClass());
      AuditFields auditFields = new AuditFields(loggedInUserId(request), currentTimestamp);

      int recordsToBeUploaded = csvParser.process(csvFile.getInputStream(), modelClass, recordHandler, auditFields);

      return successResponse(model, initialRecordCount, recordsToBeUploaded);
    } catch (DataException dataException) {
      return errorResponse(dataException.getOpenLmisMessage());
    } catch (UploadException e) {
      return errorResponse(new OpenLmisMessage(messageService.message(e.getCode(), e.getParams())));
    } catch (IOException e) {
      return errorResponse(new OpenLmisMessage(e.getMessage()));
    }
  }

  private ResponseEntity<OpenLmisResponse> successResponse(String model, int initialRecordCount, int recordsToBeUploaded) {
    int finalRecordCount = dbService.getCount(uploadBeansMap.get(model).getTableName());
    int recordsCreated = finalRecordCount - initialRecordCount;

    return successPage(recordsCreated, recordsToBeUploaded - recordsCreated);
  }

  @RequestMapping(value = "/supported-uploads", method = GET, headers = ACCEPT_JSON)
  @PreAuthorize("@permissionEvaluator.hasPermission(principal,'UPLOADS')")
  public ResponseEntity<OpenLmisResponse> getSupportedUploads() {
    return response(SUPPORTED_UPLOADS, uploadBeansMap);
  }

  private OpenLmisMessage validateFile(String model, MultipartFile csvFile) {
    OpenLmisMessage errorMessage = null;
    if (model.isEmpty()) {
      errorMessage = new OpenLmisMessage(SELECT_UPLOAD_TYPE);
    } else if (!uploadBeansMap.containsKey(model)) {
      errorMessage = new OpenLmisMessage(INCORRECT_FILE);
    } else if (csvFile == null || csvFile.isEmpty()) {
      errorMessage = new OpenLmisMessage(FILE_IS_EMPTY);
    } else if (!csvFile.getOriginalFilename().endsWith(".csv")) {
      errorMessage = new OpenLmisMessage(messageService.message(INCORRECT_FILE_FORMAT, uploadBeansMap.get(model).getDisplayName()));
    }
    return errorMessage;
  }

  private ResponseEntity<OpenLmisResponse> successPage(int recordsCreated, int recordsUpdated) {
    Map<String, String> responseMessages = new HashMap<>();
    String message = messageService.message(UPLOAD_FILE_SUCCESS, recordsCreated, recordsUpdated);
    responseMessages.put(SUCCESS, message);
    return response(responseMessages, OK, TEXT_HTML_VALUE);
  }

  private ResponseEntity<OpenLmisResponse> errorResponse(OpenLmisMessage errorMessage) {
    Map<String, String> responseMessages = new HashMap<>();
    String message = messageService.message(errorMessage);
    responseMessages.put(ERROR, message);
    return response(responseMessages, OK, TEXT_HTML_VALUE);
  }

}
