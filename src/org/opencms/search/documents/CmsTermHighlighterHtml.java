/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/documents/CmsTermHighlighterHtml.java,v $
 * Date   : $Date: 2005/06/22 10:38:16 $
 * Version: $Revision: 1.2 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the m_terms of the GNU Lesser General Public
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
package org.opencms.search.documents;


/**
 * HTML Highlighter implementation used while generation excerpts.<p>
 * 
 * @version $Revision: 1.2 $ $Date: 2005/06/22 10:38:16 $
 * @author Carsten Weinholz 
 */
public class CmsTermHighlighterHtml implements I_CmsTermHighlighter {

    /**
     * @see org.opencms.search.documents.I_CmsTermHighlighter#highlightTerm(java.lang.String)
     */
    public String highlightTerm(String term) {

        if (term == null) {
            return "";
        }
        StringBuffer result = new StringBuffer(term.length() + 16);
        result.append("<b>");
        result.append(term);
        result.append("</b>");
        return result.toString();
    }

}
