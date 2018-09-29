/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Holger Staudacher - initial API and implementation
 *    Dragos Dascalita  - disbaled autodiscovery
 *    Lars Pfannenschmidt  - made WADL generation configurable
 ******************************************************************************/
package com.eclipsesource.jaxrs.publisher.internal;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ServerProperties;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


public class JerseyContext {

  private final RootApplication application;
  private final HttpService httpService;
  private final String rootPath;
  private boolean isApplicationRegistered;
  private final ServletContainerBridge servletContainerBridge;

  public JerseyContext( HttpService httpService, String rootPath, boolean isWadlDisabled, long publishInterval ) {
    this.httpService = httpService;
    this.rootPath = rootPath == null ? "/services" : rootPath;
    this.application = new RootApplication();
    disableAutoDiscovery();
    disableWadl( isWadlDisabled );
    this.servletContainerBridge = new ServletContainerBridge( application );
    scheduleContainerBridge( publishInterval );
  }

  private void scheduleContainerBridge( long publishInterval ) {
    Executors.newSingleThreadScheduledExecutor( new ThreadFactory() {
      
      @Override
      public Thread newThread( Runnable runnable ) {
        Thread thread = new Thread( runnable, "ServletContainerBridge" );
        thread.setUncaughtExceptionHandler( new UncaughtExceptionHandler() {
          
          @Override
          public void uncaughtException( Thread t, Throwable e ) {
            throw new IllegalStateException( e );
          }
        } );
        return thread;
      }
    } ).scheduleAtFixedRate( servletContainerBridge, 1000, publishInterval, TimeUnit.MILLISECONDS );
  }

  private void disableAutoDiscovery() {
    // don't look for implementations described by META-INF/services/*
    this.application.addProperty(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE, false );
    // disable auto discovery on server, as it's handled via OSGI
    this.application.addProperty(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true );
  }

  /**
   * WADL generation is enabled in Jersey by default. This means that OPTIONS methods are added by
   * default to each resource and an auto-generated /application.wadl resource is deployed too. In
   * case you want to disable that you can set this property to true.
   * 
   * @param disableWadl <code>true</code> to disable WADL feature 
   */
  private void disableWadl( boolean disableWadl ) {
    this.application.addProperty( ServerProperties.WADL_FEATURE_DISABLE, disableWadl );
  }

  public void addResource( Object resource ) {
    getRootApplication().addResource( resource );
    registerServletWhenNotAlreadyRegistered();
  }

  void registerServletWhenNotAlreadyRegistered() {
    if( !isApplicationRegistered ) {
      isApplicationRegistered = true;
      registerApplication();
    }
  }

  private void registerApplication() {
    ClassLoader loader = getContextClassloader();
    setContextClassloader();
    try {
      registerServlet();
    } catch( ServletException shouldNotHappen ) {
      throw new IllegalStateException( shouldNotHappen );
    } catch( NamespaceException shouldNotHappen ) {
      throw new IllegalStateException( shouldNotHappen );
    } finally {
      resetContextClassloader( loader );
    }
  }

  private ClassLoader getContextClassloader() {
    return Thread.currentThread().getContextClassLoader();
  }

  private void setContextClassloader() {
    Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );
  }

  private void registerServlet() throws ServletException, NamespaceException {
    ClassLoader original = getContextClassloader();
    try {
      Thread.currentThread().setContextClassLoader( Application.class.getClassLoader() );
      httpService.registerServlet( rootPath, 
                                   servletContainerBridge.getServletContainer(), 
                                   null, 
                                   null );
    } finally {
      resetContextClassloader( original );
    }
  }

  private void resetContextClassloader( ClassLoader loader ) {
    Thread.currentThread().setContextClassLoader( loader );
  }
  
  public void removeResource( Object resource ) {
    getRootApplication().removeResource( resource );
    unregisterServletWhenNoresourcePresents();
  }

  private void unregisterServletWhenNoresourcePresents() {
    if( !getRootApplication().hasResources() ) {
      httpService.unregister( rootPath );
      servletContainerBridge.reset();
      isApplicationRegistered = false;
    }
  }

  public List<Object> eliminate() {
    servletContainerBridge.destroy();
    try {
      httpService.unregister( rootPath );
    } catch( Exception jerseyShutdownException ) {
      // do nothing because jersey sometimes throws an exception during shutdown
    }
    return new ArrayList<Object>( getRootApplication().getSingletons() );
  }

  // For testing purpose
  RootApplication getRootApplication() {
    return application;
  }

}
