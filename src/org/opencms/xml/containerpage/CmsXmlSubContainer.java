/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/xml/containerpage/Attic/CmsXmlSubContainer.java,v $
 * Date   : $Date: 2009/12/11 08:27:48 $
 * Version: $Revision: 1.1 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Management System
 *
 * Copyright (C) 2002 - 2009 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.xml.containerpage;

import org.opencms.file.CmsFile;
import org.opencms.file.CmsObject;
import org.opencms.file.CmsResourceFilter;
import org.opencms.i18n.CmsEncoder;
import org.opencms.i18n.CmsLocaleManager;
import org.opencms.main.CmsException;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.relations.CmsLink;
import org.opencms.util.CmsMacroResolver;
import org.opencms.util.CmsUUID;
import org.opencms.xml.CmsXmlContentDefinition;
import org.opencms.xml.CmsXmlException;
import org.opencms.xml.CmsXmlGenericWrapper;
import org.opencms.xml.CmsXmlUtils;
import org.opencms.xml.content.CmsXmlContent;
import org.opencms.xml.content.CmsXmlContentMacroVisitor;
import org.opencms.xml.page.CmsXmlPage;
import org.opencms.xml.types.CmsXmlNestedContentDefinition;
import org.opencms.xml.types.I_CmsXmlContentValue;
import org.opencms.xml.types.I_CmsXmlSchemaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;

import org.dom4j.Document;
import org.dom4j.Element;
import org.xml.sax.EntityResolver;

/**
 * Implementation of a object used to access and manage the xml data of a sub container.<p>
 * 
 * In addition to the XML content interface. It also provides access to more comfortable beans.<p>
 * 
 * @author Tobias Herrmann
 * 
 * @version $Revision: 1.1 $
 * 
 * @since 7.9.1
 */
public class CmsXmlSubContainer extends CmsXmlContent {

    /** XML node name constants. */
    public enum XmlNode {

        /** Main node name. */
        SUBCONTAINER("SubContainers"),
        /** Container elements node name. */
        ELEMENT("Element"),
        /** Container type node name. */
        TYPE("Type");

        /** Property name. */
        private String m_name;

        /** Constructor.<p> */
        private XmlNode(String name) {

            m_name = name;
        }

        /** 
         * Returns the name.<p>
         * 
         * @return the name
         */
        public String getName() {

            return m_name;
        }
    }

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsXmlSubContainer.class);

    /** The sub container objects. */
    private Map<Locale, CmsSubContainerBean> m_subContainers;

    /**
     * Hides the public constructor.<p>
     */
    protected CmsXmlSubContainer() {

        // do nothing
    }

    /**
     * Creates a new sub container based on the provided XML document.<p>
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the cms context, if <code>null</code> no link validation is performed 
     * @param document the document to create the container page from
     * @param encoding the encoding of the container page
     * @param resolver the XML entity resolver to use
     */
    protected CmsXmlSubContainer(CmsObject cms, Document document, String encoding, EntityResolver resolver) {

        // must set document first to be able to get the content definition
        m_document = document;
        // for the next line to work the document must already be available
        m_contentDefinition = getContentDefinition(resolver);
        // initialize the XML content structure
        initDocument(cms, m_document, encoding, m_contentDefinition);
    }

    /**
     * Create a new sub container based on the given default content,
     * that will have all language nodes of the default content and ensures the presence of the given locale.<p> 
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the current users OpenCms content
     * @param locale the locale to generate the default content for
     * @param modelUri the absolute path to the container page file acting as model
     * 
     * @throws CmsException in case the model file is not found or not valid
     */
    protected CmsXmlSubContainer(CmsObject cms, Locale locale, String modelUri)
    throws CmsException {

        // init model from given modelUri
        CmsFile modelFile = cms.readFile(modelUri, CmsResourceFilter.ONLY_VISIBLE_NO_DELETED);
        CmsXmlSubContainer model = CmsXmlSubContainerFactory.unmarshal(cms, modelFile);

        // initialize macro resolver to use on model file values
        CmsMacroResolver macroResolver = CmsMacroResolver.newInstance().setCmsObject(cms);

        // content definition must be set here since it's used during document creation
        m_contentDefinition = model.getContentDefinition();
        // get the document from the default content
        Document document = (Document)model.m_document.clone();
        // initialize the XML content structure
        initDocument(cms, document, model.getEncoding(), m_contentDefinition);
        // resolve eventual macros in the nodes
        visitAllValuesWith(new CmsXmlContentMacroVisitor(cms, macroResolver));
        if (!hasLocale(locale)) {
            // required locale not present, add it
            try {
                addLocale(cms, locale);
            } catch (CmsXmlException e) {
                // this can not happen since the locale does not exist
            }
        }
    }

    /**
     * Create a new container page based on the given content definition,
     * that will have one language node for the given locale all initialized with default values.<p> 
     * 
     * The given encoding is used when marshalling the XML again later.<p>
     * 
     * @param cms the current users OpenCms content
     * @param locale the locale to generate the default content for
     * @param encoding the encoding to use when marshalling the container page later
     * @param contentDefinition the content definition to create the content for
     */
    protected CmsXmlSubContainer(
        CmsObject cms,
        Locale locale,
        String encoding,
        CmsXmlContentDefinition contentDefinition) {

        // content definition must be set here since it's used during document creation
        m_contentDefinition = contentDefinition;
        // create the XML document according to the content definition
        Document document = m_contentDefinition.createDocument(cms, this, locale);
        // initialize the XML content structure
        initDocument(cms, document, encoding, m_contentDefinition);
    }

    /**
     * Returns the sub container bean for the given locale.<p>
     *
     * @param cms the cms context
     * @param locale the locale to use
     *
     * @return the sub container bean
     */
    public CmsSubContainerBean getSubContainer(CmsObject cms, Locale locale) {

        Locale theLocale = locale;
        if (!m_subContainers.containsKey(theLocale)) {
            LOG.warn(Messages.get().container(
            // TODO: change message
                Messages.LOG_CONTAINER_PAGE_LOCALE_NOT_FOUND_2,
                cms.getSitePath(getFile()),
                theLocale.toString()).key());
            theLocale = OpenCms.getLocaleManager().getDefaultLocales(cms, getFile()).get(0);
            if (!m_subContainers.containsKey(theLocale)) {
                // locale not found!!
                LOG.error(Messages.get().container(
                // TODO: change message
                    Messages.LOG_CONTAINER_PAGE_LOCALE_NOT_FOUND_2,
                    cms.getSitePath(getFile()),
                    theLocale).key());
                return null;
            }
        }
        return m_subContainers.get(theLocale);
    }

    /**
     * @see org.opencms.xml.content.CmsXmlContent#isAutoCorrectionEnabled()
     */
    @Override
    public boolean isAutoCorrectionEnabled() {

        return true;
    }

    /**
     * Creates a new bookmark for the given element.<p>
     * 
     * @param element the element to create the bookmark for 
     * @param locale the locale
     * @param parent the parent node of the element
     * @param parentPath the parent's path
     * @param parentDef the parent's content definition
     */
    protected void createBookmark(
        Element element,
        Locale locale,
        Element parent,
        String parentPath,
        CmsXmlContentDefinition parentDef) {

        int elemIndex = CmsXmlUtils.getXpathIndexInt(element.getUniquePath(parent));
        String elemPath = CmsXmlUtils.concatXpath(parentPath, CmsXmlUtils.createXpathElement(
            element.getName(),
            elemIndex));
        I_CmsXmlSchemaType elemSchemaType = parentDef.getSchemaType(element.getName());
        I_CmsXmlContentValue elemValue = elemSchemaType.createValue(this, element, locale);
        addBookmark(elemPath, locale, true, elemValue);
    }

    /**
     * @see org.opencms.xml.A_CmsXmlDocument#initDocument(org.dom4j.Document, java.lang.String, org.opencms.xml.CmsXmlContentDefinition)
     */
    @Override
    protected void initDocument(Document document, String encoding, CmsXmlContentDefinition definition) {

        m_document = document;
        m_contentDefinition = definition;
        m_encoding = CmsEncoder.lookupEncoding(encoding, encoding);
        m_elementLocales = new HashMap<String, Set<Locale>>();
        m_elementNames = new HashMap<Locale, Set<String>>();
        m_locales = new HashSet<Locale>();
        m_subContainers = new HashMap<Locale, CmsSubContainerBean>();
        clearBookmarks();

        // initialize the bookmarks
        for (Iterator<Element> itSubContainers = CmsXmlGenericWrapper.elementIterator(m_document.getRootElement()); itSubContainers.hasNext();) {
            Element cntPage = itSubContainers.next();

            try {
                Locale locale = CmsLocaleManager.getLocale(cntPage.attribute(
                    CmsXmlContentDefinition.XSD_ATTRIBUTE_VALUE_LANGUAGE).getValue());

                addLocale(locale);
                Element subContainer = cntPage.element(XmlNode.SUBCONTAINER.getName());

                // container itself
                int cntIndex = CmsXmlUtils.getXpathIndexInt(subContainer.getUniquePath(cntPage));
                String cntPath = CmsXmlUtils.createXpathElement(subContainer.getName(), cntIndex);
                I_CmsXmlSchemaType cntSchemaType = definition.getSchemaType(subContainer.getName());
                I_CmsXmlContentValue cntValue = cntSchemaType.createValue(this, subContainer, locale);
                addBookmark(cntPath, locale, true, cntValue);
                CmsXmlContentDefinition cntDef = ((CmsXmlNestedContentDefinition)cntSchemaType).getNestedContentDefinition();

                List<String> types = new ArrayList<String>();
                // types
                for (Iterator<Element> itTypes = CmsXmlGenericWrapper.elementIterator(
                    subContainer,
                    XmlNode.TYPE.getName()); itTypes.hasNext();) {
                    Element type = itTypes.next();
                    createBookmark(type, locale, subContainer, cntPath, cntDef);
                    types.add(type.getTextTrim());
                }

                List<CmsContainerElementBean> elements = new ArrayList<CmsContainerElementBean>();
                // Elements
                for (Iterator<Element> itElems = CmsXmlGenericWrapper.elementIterator(
                    subContainer,
                    XmlNode.ELEMENT.getName()); itElems.hasNext();) {
                    Element element = itElems.next();
                    createBookmark(element, locale, subContainer, cntPath, cntDef);
                    Element uriLink = element.element(CmsXmlPage.NODE_LINK);
                    CmsUUID elementId = null;
                    if (uriLink == null) {
                        // this can happen when adding the elements node to the xml content
                        // it is not dangerous since the link has to be set before saving 
                    } else {
                        elementId = new CmsLink(uriLink).getStructureId();
                    }

                    if (elementId != null) {
                        elements.add(new CmsContainerElementBean(elementId, null, null));
                    }
                }
                m_subContainers.put(locale, new CmsSubContainerBean(elements, types));
            } catch (NullPointerException e) {
                LOG.error(org.opencms.xml.content.Messages.get().getBundle().key(
                    org.opencms.xml.content.Messages.LOG_XMLCONTENT_INIT_BOOKMARKS_0), e);
            }
        }
    }

    /**
     * @see org.opencms.xml.content.CmsXmlContent#setFile(org.opencms.file.CmsFile)
     */
    @Override
    protected void setFile(CmsFile file) {

        // just for visibility from the factory
        super.setFile(file);
    }
}
