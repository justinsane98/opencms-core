/*
 * File   : $Source: /alkacon/cvs/opencms/src/org/opencms/search/CmsIndexingThreadManager.java,v $
 * Date   : $Date: 2005/06/22 10:38:15 $
 * Version: $Revision: 1.17 $
 *
 * This library is part of OpenCms -
 * the Open Source Content Mananagement System
 *
 * Copyright (C) 2002 - 2005 Alkacon Software (http://www.alkacon.com)
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

package org.opencms.search;

import org.opencms.file.CmsObject;
import org.opencms.i18n.CmsMessageContainer;
import org.opencms.main.CmsLog;
import org.opencms.report.I_CmsReport;

import org.apache.commons.logging.Log;
import org.apache.lucene.index.IndexWriter;

/**
 * Implements the management of indexing threads.<p>
 * 
 * @version $Revision: 1.17 $ $Date: 2005/06/22 10:38:15 $
 * @author Carsten Weinholz 
 * @since 5.3.1
 */
public class CmsIndexingThreadManager extends Thread {

    /** The log object for this class. */
    private static final Log LOG = CmsLog.getLog(CmsIndexingThreadManager.class);

    /** The report. */
    private I_CmsReport m_report;

    /** Timeout for abandoning threads. */
    private long m_timeout;

    /** Overall number of threads started. */
    private int m_fileCounter;

    /** Number of threads abandoned. */
    private int m_abandonedCounter;

    /** Number of thread returned. */
    private int m_returnedCounter;

    /**
     * Creates and starts a thread manager for indexing threads.<p>
     * @param report the report to write out progress information
     * @param timeout timeout after a thread is abandoned
     * @param indexName the name of the index
     */
    public CmsIndexingThreadManager(I_CmsReport report, long timeout, String indexName) {

        super("OpenCms: Search thread watcher for index '" + indexName + "'");

        m_report = report;
        m_timeout = timeout;
        m_fileCounter = 0;
        m_abandonedCounter = 0;
        m_returnedCounter = 0;

        this.start();
    }

    /**
     * Creates and starts a new indexing thread for a resource.<p>
     * 
     * After an indexing thread was started, the manager suspends itself 
     * and waits for an amount of time specified by the <code>timeout</code>
     * value. If the timeout value is reached, the indexing thread is
     * aborted by an interrupt signal.
     * 
     * @param cms the cms object
     * @param writer the write to write the index
     * @param res the resource
     * @param index the index
     */
    public void createIndexingThread(
        CmsObject cms,
        IndexWriter writer,
        A_CmsIndexResource res,
        CmsSearchIndex index) {

        CmsIndexingThread thread = new CmsIndexingThread(cms, writer, res, index, m_report, this);

        try {
            m_fileCounter++;
            thread.start();
            thread.join(m_timeout);

            if (thread.isAlive()) {

                if (LOG.isWarnEnabled()) {
                    LOG.warn(Messages.get().key(
                        Messages.LOG_INDEXING_TIMEOUT_1, res.getRootPath()));
                }

                m_report.println();
                m_report.print(org.opencms.report.Messages.get().container(
                    org.opencms.report.Messages.RPT_FAILED_0), I_CmsReport.C_FORMAT_WARNING);
                m_report.println(Messages.get().container(
                    Messages.RPT_SEARCH_INDEXING_TIMEOUT_1,
                    res.getRootPath()), I_CmsReport.C_FORMAT_WARNING);

                m_abandonedCounter++;
                thread.interrupt();
            }
        } catch (InterruptedException exc) {
            // noop
        }
    }

    /**
     * Writes statistical information to the report.<p>
     * 
     * The method reports the total number of threads started
     * (equals to the number of indexed files), the number of returned
     * threads (equals to the number of successfully indexed files),
     * and the number of abandoned threads (hanging threads reaching the timeout). 
     */
    public void reportStatistics() {

        CmsMessageContainer message = Messages.get().container(
            Messages.RPT_SEARCH_INDEXING_STATS_4,
            new Object[] {
                new Integer(m_fileCounter),
                new Integer(m_returnedCounter),
                new Integer(m_abandonedCounter),
                m_report.formatRuntime()});
        if (LOG.isInfoEnabled()) {
            LOG.info(message.key());
        }

        if (m_report != null) {

            m_report.println(
                Messages.get().container(Messages.RPT_SEARCH_INDEXING_END_0),
                I_CmsReport.C_FORMAT_HEADLINE);
            m_report.println(message);
        }
    }

    /**
     * Gets the current thread (file) count.<p>
     * 
     * @return the current thread count
     */
    public int getCounter() {

        return m_fileCounter;
    }

    /**
     * Signals the thread manager that a thread has finished its job and will exit immediately.<p>
     */
    public synchronized void finished() {

        m_returnedCounter++;
    }

    /**
     * Returns if the indexing manager still have indexing threads.<p>
     * 
     * @return true if the indexing manager still have indexing threads
     */
    public boolean isRunning() {

        return (m_returnedCounter + m_abandonedCounter < m_fileCounter);
    }

    /**
     * Starts the thread manager to look for non-terminated threads<p>
     * The thread manager looks all 10 minutes if threads are not returned
     * and reports the number to the log file.
     * 
     * @see java.lang.Runnable#run()
     */
    public void run() {

        int max = 20;

        try {
            // wait 30 seconds for the initial indexing
            Thread.sleep(30000);

            while (m_fileCounter > m_returnedCounter && max-- > 0) {

                Thread.sleep(30000);
                // wait 30 seconds before we start checking for "dead" index threads
                if (LOG.isWarnEnabled()) {
                    LOG.warn(Messages.get().key(
                        Messages.LOG_WAITING_ABANDONED_THREADS_2,
                        new Integer(m_abandonedCounter),
                        new Integer((m_fileCounter - m_returnedCounter))));
                }

            }
        } catch (Exception exc) {
            // noop
        }

        if (max > 0) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(Messages.get().key(Messages.LOG_THREADS_FINISHED_0));
            }
        } else {
            if (LOG.isErrorEnabled()) {
                LOG.error(Messages.get().key(
                    Messages.LOG_THREADS_FINISHED_0,
                    new Integer(m_fileCounter - m_returnedCounter)));
            }
        }
    }
}