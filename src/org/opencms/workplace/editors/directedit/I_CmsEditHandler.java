/*
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (c) Alkacon Software GmbH & Co. KG (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * For further information about Alkacon Software, please see the
 * company website: http://www.alkacon.com
 *
 * For further information about OpenCms, please see the
 * project website: http://www.opencms.org
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.opencms.workplace.editors.directedit;

import org.opencms.ade.containerpage.shared.CmsDialogOptions;
import org.opencms.file.CmsObject;
import org.opencms.util.CmsUUID;
import org.opencms.xml.containerpage.CmsContainerElementBean;

/**
 * Edit handlers are optional and can be configured within the XSD-schema of a resource type.<p>
 * Edit handlers may be used to enhance content editing within the container page editor.
 * They allow edit pre-processing, and specific delete operations.<p>
 */
public interface I_CmsEditHandler {

    /**
     * Returns a map of delete options. The value being the option description displayed to the user.<p>
     *
     * @param cms the cms context
     * @param elementBean the container element to be deleted
     *
     * @return the available delete options
     */
    CmsDialogOptions getDeleteOptions(CmsObject cms, CmsContainerElementBean elementBean);

    /**
     * Returns a map of edit options. The value being the option description displayed to the user.<p>
     *
     * @param cms the cms context
     * @param elementBean the container element to be edited
     *
     * @return the available edit options
     */
    CmsDialogOptions getEditOptions(CmsObject cms, CmsContainerElementBean elementBean);

    /**
     * Executes the actual delete.<p>
     *
     * @param cms the cms context
     * @param elementBean the container element to delete
     * @param deleteOption the selected delete option
     */
    void handleDelete(CmsObject cms, CmsContainerElementBean elementBean, String deleteOption);

    /**
     * Prepares the resource to be edited.<p>
     *
     * @param cms the cms context
     * @param elementBean the container element to be edited
     * @param editOption the selected edit option
     *
     * @return the structure id of the resource to be edited, may differ from the original element id
     */
    CmsUUID prepareForEdit(CmsObject cms, CmsContainerElementBean elementBean, String editOption);

}