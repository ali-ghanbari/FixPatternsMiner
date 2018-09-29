package brooklyn.entity.group;

import brooklyn.entity.Entity;
import brooklyn.entity.Group;
import brooklyn.entity.basic.ApplicationBuilder;
import brooklyn.entity.basic.BasicGroup;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityInternal;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.trait.Startable;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.policy.basic.AbstractPolicy;
import brooklyn.test.EntityTestUtils;
import brooklyn.test.entity.TestApplication;
import brooklyn.test.entity.TestEntity;
import brooklyn.util.javalang.Boxing;
import com.google.common.base.Objects;
import org.testng.Assert;
import org.testng.annotations.*;

/**
 * tests that a group's membership gets updated using subscriptions
 */
public class GroupPickUpEntitiesTest {

    private TestApplication app;
    private BasicGroup group;

    @BeforeMethod(alwaysRun = true)
    public void setup() throws Exception{
        app = ApplicationBuilder.newManagedApp(TestApplication.class);
        group = app.createAndManageChild(EntitySpec.create(BasicGroup.class));
        
        group.addPolicy(new FindUpServicesWithNameBob());
    }

    @AfterMethod(alwaysRun = true)
    public void teardown() {
        if (app != null) Entities.destroyAll(app.getManagementContext());
        LocalManagementContext.terminateAll();
    }

    @Test
    public void testGroupFindsElement() {
        Assert.assertEquals(group.getMembers().size(), 0);
        EntityTestUtils.assertAttributeEquals(group, BasicGroup.GROUP_SIZE, 0);
        
        TestEntity e1 = app.createAndManageChild(EntitySpec.create(TestEntity.class));

        EntityTestUtils.assertAttributeEquals(group, BasicGroup.GROUP_SIZE, 0);

        e1.setAttribute(Startable.SERVICE_UP, true);
        e1.setAttribute(TestEntity.NAME, "bob");

        EntityTestUtils.assertAttributeEqualsEventually(group, BasicGroup.GROUP_SIZE, 1);
        
        TestEntity e2 = app.createAndManageChild(EntitySpec.create(TestEntity.class));

        EntityTestUtils.assertAttributeEquals(group, BasicGroup.GROUP_SIZE, 1);
        Assert.assertEquals(group.getMembers().size(), 1);
        Assert.assertTrue(group.getMembers().contains(e1));

        e2.setAttribute(Startable.SERVICE_UP, true);
        e2.setAttribute(TestEntity.NAME, "fred");

        EntityTestUtils.assertAttributeEquals(group, BasicGroup.GROUP_SIZE, 1);

        e2.setAttribute(TestEntity.NAME, "BOB");
        EntityTestUtils.assertAttributeEqualsEventually(group, BasicGroup.GROUP_SIZE, 2);
    }


    /**
     * sets the membership of a group to be all up services;
     * callers can subclass and override {@link #checkMembership(Entity)} to add additional membership constraints,
     * and optionally {@link #init()} to apply additional subscriptions
     */
    public static class FindUpServices extends AbstractPolicy {

        @SuppressWarnings({"rawtypes"})
        protected final SensorEventListener handler = new SensorEventListener() {
            @Override
            public void onEvent(SensorEvent event) {
                updateMembership(event.getSource());
            }
        };

        @Override
        public void setEntity(EntityLocal entity) {
            assert entity instanceof Group;
            super.setEntity(entity);
            init();
            for (Entity e : ((EntityInternal) entity).getManagementContext().getEntityManager().getEntities()) {
                if (Objects.equal(e.getApplicationId(), entity.getApplicationId()))
                    updateMembership(e);
            }
        }

        @SuppressWarnings("unchecked")
        protected void init() {
            subscribe(null, Startable.SERVICE_UP, handler);
        }

        protected Group getGroup() {
            return (Group) entity;
        }

        protected void updateMembership(Entity e) {
            boolean isMember = checkMembership(e);
            if (isMember) getGroup().addMember(e);
            else getGroup().removeMember(e);
        }

        protected boolean checkMembership(Entity e) {
            if (!Entities.isManaged(e)) return false;
            if (!Boxing.unboxSafely(e.getAttribute(Startable.SERVICE_UP), false)) return false;
            return true;
        }
    }


    public static class FindUpServicesWithNameBob extends FindUpServices {

        @SuppressWarnings("unchecked")
        protected void init() {
            super.init();
            subscribe(null, TestEntity.NAME, handler);
        }

        protected boolean checkMembership(Entity e) {
            if (!super.checkMembership(e)) return false;
            if (!"Bob".equalsIgnoreCase(e.getAttribute(TestEntity.NAME))) return false;
            return true;
        }
    }

}
