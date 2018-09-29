package cz.cuni.mff.xrg.odcs.commons.module.dpu;

import org.junit.Test;

import cz.cuni.mff.xrg.odcs.commons.configuration.ConfigException;
import cz.cuni.mff.xrg.odcs.commons.configuration.DPUConfigObject;
import cz.cuni.mff.xrg.odcs.commons.data.DataUnitException;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUContext;
import cz.cuni.mff.xrg.odcs.commons.dpu.DPUException;
import cz.cuni.mff.xrg.odcs.commons.module.dpu.ConfigurableBase;

import static org.junit.Assert.*;


/**
 * Test suite for {@link ConfigurableBase} class.
 * 
 * @author Petyr
 *
 */
public class ConfigurableBaseTest {
	
	/**
	 * Dummy implementation of {@link ConfigurableBase}
	 * @author Petyr
	 *
	 */
	private class ConfigurableDummy extends ConfigurableBase<ConfigDummy> {
		public ConfigurableDummy() {
			super(ConfigDummy.class);
		}

		@Override
		public void execute(DPUContext context)
				throws DPUException,
					DataUnitException,
					InterruptedException {
			
		}		
	}
	
	/**
	 * Test not null default configuration.
	 */
	@Test
	public void notNullInit() throws ConfigException {
		ConfigurableDummy configurable = new ConfigurableDummy();
		assertNotNull(configurable.getConf());		
	}
	
	/**
	 * When null is set the configuration should not change.
	 */	
	@Test
	public void nullSet() throws ConfigException {
		ConfigurableDummy configurable = new ConfigurableDummy();
		byte[] oldConfig = configurable.getConf();
		assertNotNull(oldConfig);
		byte[] nullByteConfig = null;
		configurable.configure(nullByteConfig);
		assertNotNull(configurable.getConf());
		byte[] newConfig = configurable.getConf();
		// configuration is unchanged
		assertArrayEquals(oldConfig, newConfig);
	}
	
}
