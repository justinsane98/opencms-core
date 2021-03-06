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

package org.opencms.ui.apps.sessions;

import org.opencms.configuration.CmsVariablesConfiguration;
import org.opencms.db.CmsLoginMessage;
import org.opencms.main.CmsLog;
import org.opencms.main.OpenCms;
import org.opencms.ui.A_CmsUI;
import org.opencms.ui.CmsVaadinUtils;
import org.opencms.ui.apps.Messages;
import org.opencms.ui.components.CmsBasicDialog;
import org.opencms.ui.components.CmsDateField;

import java.util.Date;

import org.apache.commons.logging.Log;

import com.vaadin.data.Binder;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.Window;

/**
 * Class for the Edit Login View.<p>
 */
public class CmsEditLoginView extends CmsBasicDialog {

    /** The logger for this class. */
    static Log LOG = CmsLog.getLog(CmsEditLoginView.class.getName());

    /**vaadin serial ok.*/
    private static final long serialVersionUID = -1053691437033852491L;

    /**vaadin component.*/
    private Button m_cancel;

    /**vaadin component.*/
    private CheckBox m_enabled;

    /**date field.*/
    private CmsDateField m_endTime;

    /** The form field binder. */
    private Binder<CmsLoginMessage> m_formBinder;

    /**vaadin component.*/
    private CheckBox m_logout;

    /**vaadin component.*/
    private TextArea m_message;

    /**vaadin component.*/
    private Button m_ok;

    /**date field.*/
    private CmsDateField m_startTime;

    /**
     * Public constructor.<p>
     *
     * @param window to be closed
     */
    public CmsEditLoginView(final Window window) {

        CmsVaadinUtils.readAndLocalizeDesign(this, CmsVaadinUtils.getWpMessagesForCurrentLocale(), null);

        bindFields();

        CmsLoginMessage message = OpenCms.getLoginManager().getLoginMessage();
        if (message == null) {
            message = new CmsLoginMessage();
        }
        m_formBinder.readBean(message);

        m_cancel.addClickListener(new ClickListener() {

            private static final long serialVersionUID = 4425001638229366505L;

            public void buttonClick(ClickEvent event) {

                window.close();
            }

        });

        m_ok.addClickListener(new Button.ClickListener() {

            private static final long serialVersionUID = 5512397920545155478L;

            public void buttonClick(ClickEvent event) {

                if (isFormValid()) {
                    submit();
                    window.close();
                }

            }
        });
        m_ok.setEnabled(true);

        m_enabled.addValueChangeListener(event -> {
            setFieldsEnabled();
        });

        setFieldsEnabled();
    }

    /**
     * Gets end time from formular.<p>
     *
     * @return time as long
     */
    protected long getEnd() {

        if (m_endTime.getValue() == null) {
            return 0;
        }
        return m_endTime.getDate().getTime();
    }

    /**
     * Gets start time from formular.<p>
     *
     * @return time as long
     */
    protected long getStart() {

        if (m_startTime.getValue() == null) {
            return 0;
        }
        return m_startTime.getDate().getTime();
    }

    /**
     * Checks if formular is valid.<p>
     *
     * @return true if all fields are ok
     */
    protected boolean isFormValid() {

        // return m_startTime.isValid() & m_endTime.isValid() & m_message.isValid();
        return !m_formBinder.validate().hasErrors();
    }

    /**
     * Set the enable status of fields.<p>
     */
    protected void setFieldsEnabled() {

        boolean enabled = m_enabled.getValue().booleanValue();
        m_startTime.setEnabled(enabled);
        m_endTime.setEnabled(enabled);
        m_logout.setEnabled(enabled);
        m_message.setEnabled(enabled);
    }

    /**
     * Saves the settings.<p>
     */
    protected void submit() {

        CmsLoginMessage loginMessage = new CmsLoginMessage();
        try {
            m_formBinder.writeBean(loginMessage);
            OpenCms.getLoginManager().setLoginMessage(A_CmsUI.getCmsObject(), loginMessage);
            // update the system configuration
            OpenCms.writeConfiguration(CmsVariablesConfiguration.class);
            m_ok.setEnabled(false);
        } catch (Exception e) {
            LOG.error("Unable to save Login Message", e);
        }
    }

    /**
     * Checks whether the entered start end end times are valid.<p>
     *
     * @return <code>true</code> in case the times are valid
     */
    boolean hasValidTimes() {

        return ((getEnd() == 0) | (getStart() == 0)) || (getEnd() >= getStart());
    }

    /**
     * Binds the form fields.<p>
     */
    private void bindFields() {

        m_formBinder = new Binder<>();
        m_formBinder.bind(m_message, CmsLoginMessage::getMessage, CmsLoginMessage::setMessage);

        m_formBinder.bind(
            m_enabled,
            loginMessage -> Boolean.valueOf(loginMessage.isEnabled()),
            (loginMessage, enabled) -> loginMessage.setEnabled(enabled.booleanValue()));

        m_formBinder.bind(
            m_logout,
            loginMessage -> Boolean.valueOf(loginMessage.isLoginCurrentlyForbidden()),
            (loginMessage, forbidden) -> loginMessage.setLoginForbidden(forbidden.booleanValue()));

        m_formBinder.forField(m_endTime).withValidator(
            endTime -> hasValidTimes(),
            CmsVaadinUtils.getMessageText(Messages.GUI_MESSAGES_LOGINMESSAGE_VAL_DATE_0)).bind(
                loginMessage -> loginMessage.getTimeEnd() != CmsLoginMessage.DEFAULT_TIME_END
                ? CmsDateField.dateToLocalDateTime(new Date(loginMessage.getTimeEnd()))
                : null,
                (loginMessage, endTime) -> loginMessage.setTimeEnd(
                    endTime != null
                    ? CmsDateField.localDateTimeToDate(endTime).getTime()
                    : CmsLoginMessage.DEFAULT_TIME_END));
        m_formBinder.forField(m_startTime).withValidator(
            startTime -> hasValidTimes(),
            CmsVaadinUtils.getMessageText(Messages.GUI_MESSAGES_LOGINMESSAGE_VAL_DATE_0)).bind(
                loginMessage -> loginMessage.getTimeStart() != CmsLoginMessage.DEFAULT_TIME_START
                ? CmsDateField.dateToLocalDateTime(new Date(loginMessage.getTimeStart()))
                : null,
                (loginMessage, startTime) -> loginMessage.setTimeStart(
                    startTime != null
                    ? CmsDateField.localDateTimeToDate(startTime).getTime()
                    : CmsLoginMessage.DEFAULT_TIME_START));
    }
}
