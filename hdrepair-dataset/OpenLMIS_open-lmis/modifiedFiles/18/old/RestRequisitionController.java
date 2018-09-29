/*
 * This program is part of the OpenLMIS logistics management information system platform software.
 * Copyright © 2013 VillageReach
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License along with this program.  If not, see http://www.gnu.org/licenses.  For additional information contact info@OpenLMIS.org. 
 */

package org.openlmis.restapi.controller;

import lombok.NoArgsConstructor;
import org.openlmis.core.exception.DataException;
import org.openlmis.restapi.domain.Report;
import org.openlmis.restapi.response.RestResponse;
import org.openlmis.restapi.service.RestRequisitionService;
import org.openlmis.rnr.domain.Rnr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

import static org.openlmis.restapi.response.RestResponse.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.web.bind.annotation.RequestMethod.*;

@Controller
@NoArgsConstructor
public class RestRequisitionController extends BaseController {

  public static final String RNR = "requisitionId";

  @Autowired
  private RestRequisitionService restRequisitionService;

  @RequestMapping(value = "/rest-api/requisitions", method = POST, headers = ACCEPT_JSON)
  public ResponseEntity<RestResponse> submitRequisition(@RequestBody Report report, Principal principal) {
    Rnr requisition;

    try {
      requisition = restRequisitionService.submitReport(report, loggedInUserId(principal));
    } catch (DataException e) {
      return error(e.getOpenLmisMessage(), BAD_REQUEST);
    }
    return response(RNR, requisition.getId(), CREATED);
  }

  @RequestMapping(value = "/rest-api/requisitions/{requisitionId}/approve", method = PUT, headers = ACCEPT_JSON)
  public ResponseEntity<RestResponse> approve(@PathVariable Long requisitionId, @RequestBody Report report, Principal principal) {
    try {
      report.validateForApproval();
      restRequisitionService.approve(report, requisitionId, loggedInUserId(principal));
      return success("msg.rnr.approve.success");
    } catch (DataException e) {
      return error(e.getOpenLmisMessage(), BAD_REQUEST);
    }
  }

  @RequestMapping(value = "/rest-api/requisitions/{id}", method = GET, headers = ACCEPT_JSON)
  public ResponseEntity<RestResponse> getReplenishment(@PathVariable Long id) {
    try {
      return response("requisition", restRequisitionService.getReplenishmentDetails(id));
    } catch (DataException e) {
      return error(e.getOpenLmisMessage(), BAD_REQUEST);
    }
  }
}
