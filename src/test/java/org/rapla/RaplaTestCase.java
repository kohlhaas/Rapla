/*--------------------------------------------------------------------------*
 | Copyright (C) 2014 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.rapla.client.internal.RaplaClientServiceImpl;
import org.rapla.client.swing.toolkit.ErrorDialog;
import org.rapla.components.util.IOUtil;
import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.framework.logger.ConsoleLogger;
import org.rapla.framework.logger.Logger;

import junit.framework.TestCase;

public abstract class RaplaTestCase extends TestCase {
    protected RaplaClientServiceImpl raplaContainer;
 	Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_WARN).getChildLogger("test");

 	public static String TEST_SRC_FOLDER_NAME="src/test/resources";
    public static String TEST_FOLDER_NAME="target/test";
    protected RaplaStartupEnvironment env = new RaplaStartupEnvironment();

    public RaplaTestCase(String name) {
        super(name);
        try {
            new File("temp").mkdir();
            File testFolder =new File(TEST_FOLDER_NAME);
            System.setProperty("jetty.home",testFolder.getPath());
            testFolder.mkdir();
            IOUtil.copy( TEST_SRC_FOLDER_NAME +"/test.xconf", TEST_FOLDER_NAME + "/test.xconf" );
            //IOUtil.copy( "test-src/test.xlog", TEST_FOLDER_NAME + "/test.xlog" );
        } catch (IOException ex) {
            throw new RuntimeException("Can't initialize config-files: " + ex.getMessage());
        }
        try
        {
        	Class<?> forName = RaplaTestCase.class.getClassLoader().loadClass("org.slf4j.bridge.SLF4JBridgeHandler");
        	forName.getMethod("removeHandlersForRootLogger", new Class[] {}).invoke(null, new Object[] {});
        	forName.getMethod("install", new Class[] {}).invoke(null, new Object[] {});
        }
        catch (Exception ex)
        {
        	getLogger().warn("Can't install logging bridge  " + ex.getMessage());
        	// Todo bootstrap log
        }

    }

    public void copyDataFile(String testFile) throws IOException {
        try {
             IOUtil.copy( testFile, TEST_FOLDER_NAME + "/test.xml" );
        } catch (IOException ex) {
            throw new IOException("Failed to copy TestFile '" + testFile + "': " + ex.getMessage());
        }
    }
   
    protected <T> T getService(Class<T> role) throws RaplaException {
        return null;
    }
   

    protected SerializableDateTimeFormat formater() {
        return new SerializableDateTimeFormat();
    }

    protected Logger getLogger() {
        return logger;
    }

    protected void setUp(String testFile) throws Exception  {
        ErrorDialog.THROW_ERROR_DIALOG_EXCEPTION = true;
        
//        URL configURL = new URL("file:./" + TEST_FOLDER_NAME + "/test.xconf");
        //env.setConfigURL( configURL);
        copyDataFile(TEST_SRC_FOLDER_NAME + "/" + testFile);
        raplaContainer = null;// FIXME new RaplaClientServiceImpl( env );
        assertNotNull("Container not initialized.",raplaContainer);
        ClientFacade facade = getFacade();
        facade.login("homer", "duffs".toCharArray());
    }

    @Before
    protected void setUp() throws Exception {
        setUp("testdefault.xml");
    }

    protected RaplaClientServiceImpl getClientService() throws RaplaException {
        RaplaClientServiceImpl clientContainer = raplaContainer;
        if ( ! clientContainer.isRunning())
        {
            try {
                clientContainer.start( new ConnectInfo("homer", "duffs".toCharArray()));
            } catch (Exception e) {
                throw new RaplaException( e.getMessage(), e);
            }
        }
        return raplaContainer;
    }

    protected ClientFacade getFacade() throws RaplaException {
        return getService(ClientFacade.class);
    }

    protected RaplaLocale getRaplaLocale() throws RaplaException {
        return getService(RaplaLocale.class);
    }

    protected void tearDown() throws Exception {
        if (raplaContainer != null)
            raplaContainer.dispose();
    }

   
}
