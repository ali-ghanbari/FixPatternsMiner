package org.openlmis.web.controller;

import lombok.NoArgsConstructor;
import org.openlmis.core.domain.Facility;
import org.openlmis.core.domain.RequisitionHeader;
import org.openlmis.core.service.FacilityService;
import org.openlmis.core.service.ProgramService;
import org.openlmis.web.model.ReferenceData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

import static org.openlmis.authentication.web.UserAuthenticationSuccessHandler.USER;

@Controller
@NoArgsConstructor
public class FacilityController extends BaseController {

    private FacilityService facilityService;
    private ProgramService programService;

    @Autowired
    public FacilityController(FacilityService facilityService, ProgramService programService) {
        this.facilityService = facilityService;
        this.programService = programService;
    }

    @RequestMapping(value = "admin/facilities", method = RequestMethod.GET, headers = "Accept=application/json")
    public List<Facility> getAll() {
        return facilityService.getAll();
    }

    @RequestMapping(value = "logistics/user/facilities", method = RequestMethod.GET, headers = "Accept=application/json")
    public List<Facility> getAllByUser(HttpServletRequest httpServletRequest) {
        return facilityService.getAllForUser(loggedInUser(httpServletRequest));
    }

    @RequestMapping(value = "logistics/facility/{facilityId}/requisition-header", method = RequestMethod.GET, headers = "Accept=application/json")
    public RequisitionHeader getRequisitionHeader(@PathVariable(value = "facilityId") Long facilityId) {
        return facilityService.getRequisitionHeader(facilityId);
    }

    @RequestMapping(value = "admin/facility/reference-data", method = RequestMethod.GET, headers = "Accept=application/json")
    public Map getReferenceData() {
        ReferenceData referenceData = new ReferenceData();
        return referenceData.addFacilityTypes(facilityService.getAllTypes()).
                addFacilityOperators(facilityService.getAllOperators()).
                addGeographicZones(facilityService.getAllZones()).
                addPrograms(programService.getAll()).get();
    }

    @RequestMapping(value = "admin/facility", method = RequestMethod.POST, headers = "Accept=application/json")
    public ResponseEntity<ModelMap> addOrUpdate(@RequestBody Facility facility, HttpServletRequest request) {
        ModelMap modelMap = new ModelMap();
        String modifiedBy = (String) request.getSession().getAttribute(USER);
        facility.setModifiedBy(modifiedBy);
        boolean createFlag = facility.getId()==null?true:false;
        try {
            facilityService.save(facility);
        } catch (RuntimeException exception) {
            modelMap.put("error", exception.getMessage());
            return new ResponseEntity<>(modelMap, HttpStatus.BAD_REQUEST);
        }
        if (createFlag) {
            modelMap.put("success", facility.getName() + " created successfully");
        } else {
            modelMap.put("success", facility.getName() + " updated successfully");
        }
        return new ResponseEntity<>(modelMap, HttpStatus.OK);
    }

    @RequestMapping(value = "admin/facility/{id}", method = RequestMethod.GET, headers = "Accept=application/json")
    public ResponseEntity<ModelMap> getFacility(@PathVariable(value = "id") Long id) {
        ModelMap modelMap = new ModelMap();
        modelMap.put("facility", facilityService.getFacility(id));
        return new ResponseEntity<>(modelMap, HttpStatus.OK);
    }

    @RequestMapping(value = "admin/facility/update/{operation}",  method = RequestMethod.POST,headers = "Accept=application/json")
    public ResponseEntity<ModelMap> updateDataReportableAndActive(@RequestBody Facility facility, @PathVariable(value = "operation") String operation,
                                                                  HttpServletRequest request) {
        ModelMap modelMap = new ModelMap();
        String modifiedBy = (String) request.getSession().getAttribute(USER);
        facility.setModifiedBy(modifiedBy);
        String message;
        if("delete".equalsIgnoreCase(operation)){
            facility.setDataReportable(false);
            facility.setActive(false);
            message = "deleted";
        } else {
            facility.setDataReportable(true);
            message = "restored";
        }
        try {
            facilityService.updateDataReportableAndActiveFor(facility);
        } catch (RuntimeException exception) {
            modelMap.put("error", exception.getMessage());
            modelMap.put("facility", facility);
            return new ResponseEntity<ModelMap>(modelMap, HttpStatus.BAD_REQUEST);
        }
        modelMap.put("facility", facility);
        modelMap.put("success", facility.getName() + " / " + facility.getCode() +" "+ message + " successfully");
        return new ResponseEntity<ModelMap>(modelMap, HttpStatus.OK);
    }

}
