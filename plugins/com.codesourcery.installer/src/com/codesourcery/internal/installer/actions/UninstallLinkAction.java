/*******************************************************************************
 *  Copyright (c) 2014 Mentor Graphics and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     Mentor Graphics - initial API and implementation
 *******************************************************************************/
package com.codesourcery.internal.installer.actions;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;

import com.codesourcery.installer.IInstallMode;
import com.codesourcery.installer.IInstallPlatform;
import com.codesourcery.installer.IInstallProduct;
import com.codesourcery.installer.Installer;
import com.codesourcery.installer.actions.AbstractInstallAction;
import com.codesourcery.internal.installer.IInstallConstants;
import com.codesourcery.internal.installer.InstallMessages;

/**
 * Action to set up the Control panel add/remove on Windows.
 */
public class UninstallLinkAction extends AbstractInstallAction {
	/** Action identifier */
	private static final String ID = "com.codesourcery.installer.uninstallLinkAction";
	/** Uninstall location */
	private IPath uninstallLocation;
	/** Vendor name */
	private String vendor;
	/** Version */
	private String version;
	/** Help link */
	private String helpLink;
	/** Name of uninstaller */
	private String uninstallerName;
	/** Installation size */
	private int size;
	
	/**
	 * Constructor
	 */
	public UninstallLinkAction() {
		super(ID);
	}

	/**
	 * Constructor
	 * 
	 * @param uninstallLocation Uninstall location
	 * @param vendor Vendor name or <code>null</code>
	 * @param version Version or <code>null</code>
	 * @param helpLink Help link or <code>null</code>
	 * @param size Installation size in Kilobytes or <code>-1/</code>
	 */
	public UninstallLinkAction(IPath uninstallLocation, String vendor, String version, String helpLink, String uninstallerName, int size) {
		super(ID);
		this.uninstallLocation = uninstallLocation;
		this.vendor = vendor;
		this.version = version;
		this.helpLink = helpLink;
		this.uninstallerName = uninstallerName;
		this.size = size;
	}
	
	/**
	 * Returns the uninstall location.
	 * 
	 * @return Uninstall location
	 */
	public IPath getUninstallLocation() {
		return uninstallLocation;
	}
	
	/**
	 * Returns the vendor name.
	 * 
	 * @return Vendor name
	 */
	public String getVendor() {
		return vendor;
	}
	
	/**
	 * Returns the version.
	 * 
	 * @return Version
	 */
	public String getVersion() {
		return version;
	}
	
	/**
	 * Returns the help link.
	 * 
	 * @return Help link
	 */
	public String getHelpLink() {
		return helpLink;
	}
	
	/**
	 * Returns the installation size.
	 * 
	 * @return Size
	 */
	public int getSize() {
		return size;
	}

	@Override
	public void run(IProvisioningAgent agent, IInstallProduct product, IInstallMode mode, IProgressMonitor monitor) throws CoreException {
		// Only supported on Windows
		if (!Installer.isWindows())
			return;
		
		try {
			IInstallPlatform platform = Installer.getDefault().getInstallPlatform();
			
			String uninstallKey = "HKEY_CURRENT_USER\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\" + product.getId();

			// Install
			if (mode.isInstall()) {
				// No uninstall location
				if (getUninstallLocation() == null)
					return;

				monitor.beginTask(InstallMessages.CreatingAddRemove, 10);
				monitor.setTaskName(InstallMessages.CreatingAddRemove);
				IPath installerPath = getUninstallLocation().append(uninstallerName);
				if (Installer.isWindows()) {
					installerPath = installerPath.addFileExtension(IInstallConstants.EXTENSION_EXE);
				}
				String installerLocation = installerPath.toOSString();
				
				// Create add/remove entry
				// Registry keys can be found at: https://msdn.microsoft.com/en-us/library/aa372105(v=vs.85).aspx
				
				// Display name
				platform.setWindowsRegistryValue(uninstallKey, "DisplayName", product.getUninstallName());
				monitor.worked(1);
				// Display icon
				platform.setWindowsRegistryValue(uninstallKey, "DisplayIcon", installerLocation);
				monitor.worked(1);
				// Installed date
				SimpleDateFormat dateFormat = new SimpleDateFormat("EEEEEEEE MMMMMMMM dd HH:mm:ss zzzzzzzzzz yyyy");
				platform.setWindowsRegistryValue(uninstallKey, "InstallDate", dateFormat.format(new Date()));
				monitor.worked(1);
				// Install location
				platform.setWindowsRegistryValue(uninstallKey,  "InstallLocation", getUninstallLocation().toOSString());
				monitor.worked(1);
				// Uninstall command
				// Note: Do not add quotes to the installer location or Windows will report
				// insufficient rights to run uninstaller.
				platform.setWindowsRegistryValue(uninstallKey,  "UninstallString", installerLocation);
				monitor.worked(1);
				// Modify is not supported
				platform.setWindowsRegistryValue(uninstallKey,  "NoModify", 1);
				monitor.worked(1);
				// Repair is not supported
				platform.setWindowsRegistryValue(uninstallKey,  "NoRepair", 1);
				monitor.worked(1);
				// Producer
				if (getVendor() != null) {
					platform.setWindowsRegistryValue(uninstallKey,  "Publisher", getVendor());
				}
				monitor.worked(1);
				// Version
				if (getVersion() != null) {
					platform.setWindowsRegistryValue(uninstallKey,  "DisplayVersion", getVersion());
				}
				monitor.worked(1);
				// Help
				if (getHelpLink() != null) {
					platform.setWindowsRegistryValue(uninstallKey,  "HelpLink", getHelpLink());
				}
				// Installation size
				if (getSize() != -1) {
					platform.setWindowsRegistryValue(uninstallKey, "EstimatedSize", getSize());
				}
				
				monitor.worked(1);
			}
			// Uninstall
			else {
				monitor.beginTask(InstallMessages.RemovingAddRemove, 1);
				monitor.setTaskName(InstallMessages.RemovingAddRemove);
				platform.deleteWindowsRegistryKey(uninstallKey);
				monitor.worked(1);
			}
		}
		catch (Exception e) {
			Installer.fail(InstallMessages.Error_AddUninstallLinks, e);
		}
		finally {
			monitor.done();
		}
	}

	@Override
	public boolean isSupported(String platform, String arch) {
		// Only supported on Windows
		return isWindows(platform);
	}
}
