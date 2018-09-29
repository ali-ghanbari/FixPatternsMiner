package com.qcadoo.mes.orders.states;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.mes.basic.ShiftsServiceImpl;
import com.qcadoo.mes.orders.constants.OrdersConstants;
import com.qcadoo.model.api.DataDefinitionService;
import com.qcadoo.model.api.Entity;
import com.qcadoo.security.api.SecurityService;

@Service
public class OrderStateChangingService {

    @Autowired
    private DataDefinitionService dataDefinitionService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ShiftsServiceImpl shiftsServiceImpl;

    public void saveLogging(final Entity order, final String previousState, final String currentState) {
        Entity logging = dataDefinitionService.get(OrdersConstants.PLUGIN_IDENTIFIER, OrdersConstants.MODEL_LOGGING).create();

        logging.setField("order", order);
        logging.setField("previousState", previousState);
        logging.setField("currentState", currentState);
        Date dateTime = new Date();
        Entity shift = shiftsServiceImpl.getShiftFromDate(dateTime);
        if (shift != null)
            logging.setField("shift", shift);
        else
            logging.setField("shift", null);
        logging.setField("worker", securityService.getCurrentUserName());
        logging.setField("dateAndTime", dateTime);

        logging.getDataDefinition().save(logging);
    }

    public ChangeOrderStateError validationPending(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        List<String> references = Arrays.asList("product", "plannedQuantity");
        return checkValidation(references, entity);
    }

    public ChangeOrderStateError validationAccepted(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        List<String> references = Arrays.asList("product", "plannedQuantity", "dateTo", "dateFrom", "technology");
        return checkValidation(references, entity);
    }

    public ChangeOrderStateError validationInProgress(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        return validationAccepted(entity);
    }

    public ChangeOrderStateError validationCompleted(final Entity entity) {
        checkArgument(entity != null, "entity is null");
        List<String> references = Arrays.asList("product", "plannedQuantity", "dateTo", "dateFrom", "technology", "doneQuantity");
        return checkValidation(references, entity);
    }

    private ChangeOrderStateError checkValidation(final List<String> references, final Entity entity) {
        checkArgument(entity != null, "entity is null");
        ChangeOrderStateError error = null;
        for (String reference : references)
            if (entity.getField(reference) == null) {
                error = new ChangeOrderStateError();
                error.setMessage("orders.order.orderStates.fieldRequired");
                error.setReferenceToField(reference);
                return error;
            }
        return null;
    }
}
