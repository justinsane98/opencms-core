package com.opencms.file;

/*
 * File   : $Source: /alkacon/cvs/opencms/src/com/opencms/file/Attic/CmsRequestContext.java,v $
 * Date   : $Date: 2000/09/25 10:50:08 $
 * Version: $Revision: 1.29 $
 *
 * Copyright (C) 2000  The OpenCms Group 
 * 
 * This File is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * For further information about OpenCms, please see the
 * OpenCms Website: http://www.opencms.com
 * 
 * You should have received a copy of the GNU General Public License
 * long with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.util.*;
import javax.servlet.http.*;

import com.opencms.core.*;

/**
 * This class provides access to the CmsRequestContext. 
 * <br>
 * In the CmsRequestContext class are all methods bundled, which can inform about the
 * current request properties, like  the url or uri of the request.
 * <p>
 * 
 * @author Andreas Schouten
 * @author Michael Emmerich
 * @author Anders Fugmann
 *
 * @version $Revision: 1.29 $ $Date: 2000/09/25 10:50:08 $
 * 
 */
public class CmsRequestContext implements I_CmsConstants {

	/**
	 * The rb to get access to the OpenCms.
	 */
	private I_CmsResourceBroker m_rb;
	
	/**
	 * The current CmsRequest.
	 */
	private I_CmsRequest m_req;
	
	/**
	 * The current CmsResponse.
	 */
	private I_CmsResponse m_resp;
	
	/**
	 * The current user.
	 */
	private CmsUser m_user;
	
	/**
	 * The current group of the user.
	 */
	private CmsGroup m_currentGroup;
	
	/**
	 * The current project.
	 */
	private CmsProject m_currentProject;

/**
 * Returns the current folder object.
 * 
 * @return the current folder object.
 *
 * @exception CmsException if operation was not successful.
 */
public CmsFolder currentFolder() throws CmsException {
	// truncate the filename from the pathinformation
	String folderName = getUri().substring(0, getUri().lastIndexOf("/") + 1);
	return (m_rb.readFolder(currentUser(), currentProject(), folderName, ""));
}
	/**
	 * Returns the current group of the current user.
	 * 
	 * @return the current group of the current user.
	 */
	public CmsGroup currentGroup() {
		return(m_currentGroup);
	}
	/**
	 * Returns the current project of the current user.
	 * 
	 * @return the current project of the current user.
	 */
	public CmsProject currentProject() {
		return m_currentProject;
	}
	/**
	 * Returns the current user object.
	 * 
	 * @return the current user object.
	 */
	public CmsUser currentUser() {
		return(m_user);
	}
   /** 
	* Gets the name of the requested file without any path-information.
	* 
	* @return the requested filename.
	*/
	public String getFileUri() { 
		String uri = m_req.getRequestedResource();
		uri=uri.substring(uri.lastIndexOf("/")+1);
		return uri;
	}
	/**
	 * Gets the current request, if availaible.
	 * 
	 * @return the current request, if availaible.
	 */
	public I_CmsRequest getRequest() {
		return( m_req );
	}
	/**
	 * Gets the current response, if availaible.
	 * 
	 * @return the current response, if availaible.
	 */
	public I_CmsResponse getResponse() {
		return( m_resp );
	}
	/**
	 * Gets the Session for this request.
	 * <br>
	 * This method should be used instead of the originalRequest.getSession() method.
	 * @param value indicates, if a session should be created when a session for the particular client does not already exist. 
	 * @return the CmsSession, or <code>null</code> if no session already exists and value was set to <code>false</code>
	 * 
	 */
	public I_CmsSession getSession(boolean value) {
			return (I_CmsSession) new CmsSession(((HttpServletRequest)m_req.getOriginalRequest()).getSession(value));
	}
	/**
	 * Gets the uri for the requested resource.
	 * <p>
	 * For a http request, the name of the resource is extracted as follows:<br>
	 * <CODE>http://{servername}/{servletpath}/{path to the cms resource}</CODE><br>
	 * In the following example:<br>
	 * <CODE>http://my.work.server/servlet/opencms/system/def/explorer</CODE><br>
	 * the requested resource is <CODE>/system/def/explorer</CODE>.
	 * </P>
	 * 
	 * @return the path to the requested resource.
	 */
	public String getUri() {
		if( m_req != null ) {
			return( m_req.getRequestedResource() );
		} else {
			return( C_ROOT );
		}
	}
	/**
	 * Initializes this RequestContext.
	 * 
	 * @param req the CmsRequest.
	 * @param resp the CmsResponse.
	 * @param user the current user for this request.
	 * @param currentGroup the current group for this request.
	 * @param currentProjectId the id of the current project for this request.
	 *
	 * @exception CmsException if operation was not successful.
	 */
	void init(I_CmsResourceBroker rb, I_CmsRequest req, I_CmsResponse resp, 
			  String user, String currentGroup, int currentProjectId) 
		throws CmsException {
		m_rb = rb;
		m_req = req;
		m_resp = resp;
		
		m_user = m_rb.readUser(null, null, user);

		// check, if the user is disabled
		if( m_user.getDisabled() == true ) {
			m_user = null;
		}

		// set current project and group for this request
		System.out.println("CmsRequestContent.init: currentProjectId = " + currentProjectId);
		setCurrentProject(currentProjectId);
		m_currentGroup = m_rb.readGroup(m_user, m_currentProject, currentGroup);
	}
	/**
	 * Determines if the users is in the admin-group.
	 * 
	 * @return <code>true</code> if the users current group is the admin-group; <code>false</code> otherwise.
	 * 
	 * @exception CmsException if operation was not successful.
	 */	
	public boolean isAdmin() 
		throws CmsException {
		return( m_rb.isAdmin(m_user, m_currentProject) );
	}
	/**
	 * Determines if the users current group is the projectmanager-group.
	 * <BR>
	 * All projectmanagers can create new projects, or close their own projects.
	 * 
	 * @return <code>true</code> if the users current group is the projectleader-group; <code>false</code> otherwise.
	 * 
	 * @exception CmsException if operation was not successful.
	 */	
	public  boolean isProjectManager() 
		throws CmsException	{
		return( m_rb.isProjectManager(m_user, m_currentProject) );
	}
	/**
	 * Sets the current group of the current user.
	 *
	 * @param groupname the name of the group to be set as current group.
	 *
	 * @exception CmsException if operation was not successful.
	 */
	public void setCurrentGroup(String groupname) 
		throws CmsException {
		
		// is the user in that group?
		if(m_rb.userInGroup(m_user, m_currentProject, m_user.getName(), groupname)) {
			// Yes - set it to the current Group.
			m_currentGroup = m_rb.readGroup(m_user, m_currentProject, groupname);
		} else {
			// No - throw exception.
			throw new CmsException("[" + this.getClass().getName() + "] " + groupname,
				CmsException.C_NO_ACCESS);
		}
	}
	/**
	 * Sets the current project for the user.
	 * 
	 * @param projectId the id of the project to be set as current project.
	 * @exception CmsException if operation was not successful.
	 */
	public CmsProject setCurrentProject(int projectId)
		throws CmsException  {
		CmsProject newProject = m_rb.readProject(m_user, 
												   m_currentProject, 
												   projectId);
		if( newProject != null ) {
			m_currentProject = newProject;
		}
		return( m_currentProject );
	}
/**
 * The default constructor.
 * 
 */
public CmsRequestContext() {
	super();
}
}
