package org.ovirt.engine.core.dao;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.BaseDisk;
import org.ovirt.engine.core.common.businessentities.DiskInterface;
import org.ovirt.engine.core.common.businessentities.PropagateErrors;
import org.ovirt.engine.core.compat.Guid;

/**
 * Unit tests to validate {@link BaseDiskDao}.
 */
public class BaseDiskDaoTest extends BaseGenericDaoTestCase<Guid, BaseDisk, BaseDiskDao> {

    private static final Guid EXISTING_DISK_ID = new Guid("1b26a52b-b60f-44cb-9f46-3ef333b04a34");
    private static final int TOTAL_DISKS = 3;

    @Override
    protected Guid generateNonExistingId() {
        return Guid.NewGuid();
    }

    @Override
    protected int getEneitiesTotalCount() {
        return TOTAL_DISKS;
    }

    @Override
    protected BaseDisk generateNewEntity() {
        return new BaseDisk(Guid.NewGuid(),
                1,
                DiskInterface.SCSI,
                true,
                PropagateErrors.Off,
                "DiskName",
                "",
                false, false, true);
    }

    @Override
    protected void updateExistingEntity() {
        existingEntity.setDiskInterface(DiskInterface.IDE);
    }

    @Override
    protected BaseDiskDao prepareDao() {
        return prepareDAO(dbFacade.getBaseDiskDao());
    }

    @Override
    protected Guid getExistingEntityId() {
        return EXISTING_DISK_ID;
    }

    @Test
    public void existsForExistingDisk() throws Exception {
        assertTrue(dao.exists(EXISTING_DISK_ID));
    }

    @Test
    public void existsForNonExistingDisk() throws Exception {
        assertFalse(dao.exists(new Guid()));
    }
}
