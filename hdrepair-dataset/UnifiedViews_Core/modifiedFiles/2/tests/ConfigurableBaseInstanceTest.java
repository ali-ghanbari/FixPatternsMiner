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
public class ConfigurableBaseInstanceTest extends ConfigurableBase<ConfigDummy> {

	public ConfigurableBaseInstanceTest() {
		super(ConfigDummy.class);
	}
	
	/**
	 * Test that initial configuration has been set properly.
	 */
	@Test 
	public void initialConfigNotNull() {
		assertNotNull(config);
	}
	
	/**
	 * Configuration is not changed on configure(null).
	 */
	@Test
	public void nullSet() throws ConfigException {
		DPUConfigObject oldConfig = config;
		assertNotNull(oldConfig);
		byte[] nullByteConfig = null;
		this.configure(nullByteConfig);
		
		assertEquals(oldConfig, config);
	}

	@Override
	public void execute(DPUContext context)
			throws DPUException,
				DataUnitException,
				InterruptedException {
	
	}
	
}
