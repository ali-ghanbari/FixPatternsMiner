package org.ovirt.engine.core.bll;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.ovirt.engine.core.common.action.QuotaCRUDParameters;
import org.ovirt.engine.core.common.businessentities.Quota;
import org.ovirt.engine.core.common.businessentities.QuotaEnforcementTypeEnum;
import org.ovirt.engine.core.common.businessentities.QuotaStorage;
import org.ovirt.engine.core.common.businessentities.QuotaVdsGroup;
import org.ovirt.engine.core.common.businessentities.VM;
import org.ovirt.engine.core.common.businessentities.storage_pool;
import org.ovirt.engine.core.compat.Guid;
import org.ovirt.engine.core.dal.dbbroker.auditloghandling.AuditLogableBaseMockUtils;
import org.ovirt.engine.core.dao.QuotaDAO;
import org.ovirt.engine.core.dao.StoragePoolDAO;
import org.ovirt.engine.core.dao.VmDAO;

@RunWith(MockitoJUnitRunner.class)
public class RemoveQuotaCommandTest {

    private final Guid generalGuidQuota = Guid.NewGuid();
    private final Guid storagePoolUUID = Guid.NewGuid();

    @Mock
    private QuotaDAO quotaDAO;

    @Mock
    private VmDAO vmDAO;

    @Mock
    private StoragePoolDAO storagePoolDAO;

    /**
     * The command under test.
     */
    private RemoveQuotaCommand command;

    @Before
    public void testSetup() {
        mockQuotaDAO();
        mockVmDAO();
        mockStoragePoolDAO();
    }

    private void mockVmDAO() {
        // Mock VM Dao getAllVmsRelatedToQuotaId.
        List<VM> newList = new ArrayList<VM>();
        when(vmDAO.getAllVmsRelatedToQuotaId(generalGuidQuota)).thenReturn(newList);
    }

    private void mockQuotaDAO() {
        when(quotaDAO.getById(any(Guid.class))).thenReturn(mockGeneralStorageQuota());
        List<Quota> quotaList = new ArrayList<Quota>();
        quotaList.add(new Quota());
        quotaList.add(new Quota());
        when(quotaDAO.getQuotaByStoragePoolGuid(storagePoolUUID)).thenReturn(quotaList);
        when(quotaDAO.isQuotaInUse(any(Quota.class))).thenReturn(false);
    }

    private void mockStoragePoolDAO() {
        when(storagePoolDAO.get(any(Guid.class))).thenReturn(mockStoragePool());
    }

    @Test
    public void testExecuteCommand() throws Exception {
        RemoveQuotaCommand removeQuotaCommand = createCommand();
        removeQuotaCommand.executeCommand();
    }

    @Test
    public void testCanDoActionCommand() throws Exception {
        RemoveQuotaCommand removeQuotaCommand = createCommand();
        assertTrue(removeQuotaCommand.canDoAction());
    }

    private RemoveQuotaCommand createCommand() {
        QuotaCRUDParameters param = new QuotaCRUDParameters();
        param.setQuotaId(generalGuidQuota);
        command = spy(new RemoveQuotaCommand(param));
        doReturn(storagePoolDAO).when(command).getStoragePoolDAO();
        doReturn(quotaDAO).when(command).getQuotaDAO();
        AuditLogableBaseMockUtils.mockVmDao(command, vmDAO);
        return command;
    }

    private storage_pool mockStoragePool() {
        storage_pool storagePool = new storage_pool();
        storagePool.setId(storagePoolUUID);
        storagePool.setQuotaEnforcementType(QuotaEnforcementTypeEnum.DISABLED);
        return storagePool;
    }

    private Quota mockGeneralStorageQuota() {
        Quota generalQuota = new Quota();
        generalQuota.setDescription("New Quota to create");
        generalQuota.setQuotaName("New Quota Name");
        QuotaStorage storageQuota = new QuotaStorage();
        storageQuota.setStorageSizeGB(100l);
        storageQuota.setStorageSizeGBUsage(0d);
        generalQuota.setGlobalQuotaStorage(storageQuota);

        QuotaVdsGroup vdsGroupQuota = new QuotaVdsGroup();
        vdsGroupQuota.setVirtualCpu(0);
        vdsGroupQuota.setVirtualCpuUsage(0);
        vdsGroupQuota.setMemSizeMB(0l);
        vdsGroupQuota.setMemSizeMBUsage(0l);
        generalQuota.setGlobalQuotaVdsGroup(vdsGroupQuota);

        generalQuota.setId(generalGuidQuota);
        generalQuota.setStoragePoolId(storagePoolUUID);
        return generalQuota;
    }
}
