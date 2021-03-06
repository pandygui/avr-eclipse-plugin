/*******************************************************************************
 * Copyright (c) 2008, 2011 Thomas Holland (thomas@innot.de) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *******************************************************************************/
package de.innot.avreclipse.core.toolinfo;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;

import de.innot.avreclipse.core.avrdude.AVRDudeException.Reason;
import de.innot.avreclipse.core.toolinfo.ICommandOutputListener.StreamSource;

public class ExternalCommandLauncherTest {

	@Test
	public void testExternalCommandLauncher() throws IOException {
		String command = "echo";
		List<String> arguments;
		ExternalCommandLauncher testlauncher;

		// Test 1: just a simple launch
		arguments = new ArrayList<String>(1);
		arguments.add("test1");
		testlauncher = new ExternalCommandLauncher(command, arguments);
		int result = testlauncher.launch();
		assertEquals("Launcher return code", 0, result);
		List<String> stdout = testlauncher.getStdOut();
		assertEquals("Wrong stdout value", "test1", stdout.get(0));

		// Test 2: the LogEventListener
		final List<StreamSource> sources = new ArrayList<StreamSource>();
		final List<String> lines = new ArrayList<String>();
		arguments = new ArrayList<String>(1);
		arguments.add("test2");
		testlauncher = new ExternalCommandLauncher(command, arguments);
		testlauncher.setCommandOutputListener(new ICommandOutputListener() {
			public synchronized void handleLine(String line, StreamSource source) {
				sources.add(source);
				lines.add(line);
			}

			public void init(IProgressMonitor monitor) {
			}

			public Reason getAbortReason() {
				return null;
			}

			public String getAbortLine() {
				return null;
			}
		});
		result = testlauncher.launch();
		assertEquals("Launcher return code", 0, result);
		assertEquals("Wrong Stream", StreamSource.STDOUT, sources.get(0));
		assertEquals("Wrong line", "test2", lines.get(0));

		// Test 3: a canceled launch

	}

}
