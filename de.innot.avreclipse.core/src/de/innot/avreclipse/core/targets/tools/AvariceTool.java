/*******************************************************************************
 * 
 * Copyright (c) 2009 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id$
 *     
 *******************************************************************************/

package de.innot.avreclipse.core.targets.tools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.innot.avreclipse.core.avrdude.AVRDudeException;
import de.innot.avreclipse.core.targets.IGDBServerTool;
import de.innot.avreclipse.core.targets.IProgrammer;
import de.innot.avreclipse.core.targets.IProgrammerTool;
import de.innot.avreclipse.core.targets.ITargetConfiguration;
import de.innot.avreclipse.core.targets.ITargetConfiguration.ValidationResult;
import de.innot.avreclipse.core.toolinfo.ICommandOutputListener;

/**
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class AvariceTool extends AbstractTool implements IProgrammerTool, IGDBServerTool {

	public final static String			ID					= "avreclipse.avarice";

	private final static String			NAME				= "AVaRICE";

	public final static String			ATTR_CMD_NAME		= ID + ".command";
	private final static String			DEF_CMD_NAME		= "avarice";

	public final static String			ATTR_USE_CONSOLE	= ID + ".useconsole";
	public final static boolean			DEF_USE_CONSOLE		= true;								// TODO:
	// Change to false for release

	private Map<String, String>			fDefaults;

	private ICommandOutputListener		fOutputListener		= new AvariceOutputListener();

	private Set<String>					fProgrammerIds;

	/** Cache of all Name/Version strings, mapped to their respective command name. */
	private Map<String, String>			fNameVersionMap		= new HashMap<String, String>();

	/** Cache of all MCU Sets, mapped to their respective command name */
	private Map<String, Set<String>>	fMCUMap				= new HashMap<String, Set<String>>();

	/*
	 * (non-Javadoc)
	 * @see de.innot.avreclipse.core.targets.ITargetConfigurationTool#getDefaults()
	 */
	public Map<String, String> getDefaults() {
		if (fDefaults == null) {
			fDefaults = new HashMap<String, String>();

			fDefaults.put(ATTR_CMD_NAME, DEF_CMD_NAME);
			fDefaults.put(ATTR_USE_CONSOLE, Boolean.toString(DEF_USE_CONSOLE));
		}

		return fDefaults;
	}

	/*
	 * (non-Javadoc)
	 * @see de.innot.avreclipse.core.targets.ITargetConfigurationTool#getId()
	 */
	public String getId() {
		return ID;
	}

	/*
	 * (non-Javadoc)
	 * @see de.innot.avreclipse.core.targets.ITargetConfigurationTool#getName()
	 */
	public String getName() {
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.core.targets.tools.AbstractTool#getCommand(de.innot.avreclipse.core.targets
	 * .ITargetConfiguration)
	 */
	public String getCommand(ITargetConfiguration tc) {
		String command = tc.getAttribute(ATTR_CMD_NAME);
		if (command == null) {
			command = DEF_CMD_NAME;
		}
		return command;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.core.targets.ITargetConfigurationTool#getVersion(de.innot.avreclipse.
	 * core.targets.ITargetConfiguration)
	 */
	public String getVersion(ITargetConfiguration tc) throws AVRDudeException {

		String cmd = getCommand(tc);

		// Check if we already have the version in the cache
		if (fNameVersionMap.containsKey(cmd)) {
			return fNameVersionMap.get(cmd);
		}

		// Execute avarice without any options
		// The name / version are in the first full line of the output in the format
		// "AVaRICE version 2.8, Nov  7 2008 22:02:05"
		String name = null;
		List<String> stdout = runCommand(tc, "");

		if (stdout != null) {
			// look for a line matching "*Version TheVersionNumber *"
			Pattern mcuPat = Pattern.compile(".*version\\s+([\\w\\.]+).*");
			Matcher m;
			for (String line : stdout) {
				m = mcuPat.matcher(line);
				if (!m.matches()) {
					continue;
				}
				name = getName() + " " + m.group(1);
				break;
			}
		}
		if (name == null) {
			// could not read the version from the output, probably the regex has a
			// mistake. Return a reasonable default.
			return getName() + " ?.?";
		}

		fNameVersionMap.put(cmd, name);
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see de.innot.avreclipse.core.targets.tools.AbstractTool#getOutputListener()
	 */
	@Override
	protected ICommandOutputListener getOutputListener() {
		return fOutputListener;
	}

	/*
	 * (non-Javadoc)
	 * @see de.innot.avreclipse.core.targets.ITargetConfigurationTool#getMCUs()
	 */
	public Set<String> getMCUs(ITargetConfiguration tc) throws AVRDudeException {

		String cmd = getCommand(tc);

		if (fMCUMap.containsKey(cmd)) {
			return fMCUMap.get(cmd);
		}

		Set<String> allmcus = new HashSet<String>();
		List<String> stdout;

		stdout = runCommand(tc, "--known-devices");

		if (stdout != null) {
			// look for a line matching alphanumeric characters (the mcu id) followed by whitespaces
			// and
			// "0x" (the beginning of the device id field)
			Pattern mcuPat = Pattern.compile("(\\w+)\\s+0x.+");
			Matcher m;
			for (String line : stdout) {
				m = mcuPat.matcher(line);
				if (!m.matches()) {
					continue;
				}
				allmcus.add(m.group(1));
			}
		}
		fMCUMap.put(cmd, allmcus);
		return allmcus;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.core.targets.ITargetConfigurationTool#getProgrammers(de.innot.avreclipse
	 * .core.targets.ITargetConfiguration)
	 */
	public Set<String> getProgrammers(ITargetConfiguration tc) throws AVRDudeException {
		if (fProgrammerIds == null) {
			fProgrammerIds = new HashSet<String>();
			for (AvariceProgrammers progger : AvariceProgrammers.values()) {
				fProgrammerIds.add(progger.getId());
			}
		}
		return fProgrammerIds;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.core.targets.ITargetConfigurationTool#getProgrammer(de.innot.avreclipse
	 * .core.targets.ITargetConfiguration, java.lang.String)
	 */
	public IProgrammer getProgrammer(ITargetConfiguration tc, String id) throws AVRDudeException {

		// Quick check if the programmer id is actually supported by avarice
		if (!getProgrammers(tc).contains(id)) {
			return null;
		}

		IProgrammer progger = AvariceProgrammers.valueOf(id);

		return progger;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * de.innot.avreclipse.core.targets.ITargetConfigurationTool#validate(de.innot.avreclipse.core
	 * .targets.ITargetConfiguration, java.lang.String)
	 */
	public ValidationResult validate(ITargetConfiguration tc, String attr) {
		// TODO Auto-generated method stub
		return null;
	}

}