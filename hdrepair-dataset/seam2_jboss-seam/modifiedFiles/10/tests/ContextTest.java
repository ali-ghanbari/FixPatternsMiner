//$Id$
package org.jboss.seam.test;

import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.Component;
import org.jboss.seam.Seam;
import org.jboss.seam.contexts.ApplicationContext;
import org.jboss.seam.contexts.Context;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.EventContext;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.contexts.ServerConversationContext;
import org.jboss.seam.contexts.SessionContext;
import org.jboss.seam.core.ConversationEntries;
import org.jboss.seam.core.Init;
import org.jboss.seam.core.Manager;
import org.jboss.seam.core.ServletContexts;
import org.jboss.seam.core.ServletSession;
import org.jboss.seam.el.EL;
import org.jboss.seam.el.SeamELResolver;
import org.jboss.seam.mock.MockExternalContext;
import org.jboss.seam.mock.MockHttpServletRequest;
import org.jboss.seam.mock.MockHttpSession;
import org.jboss.seam.mock.MockServletContext;
import org.jboss.seam.servlet.ServletRequestMap;
import org.jboss.seam.servlet.ServletRequestSessionMap;
import org.testng.annotations.Test;

public class ContextTest
{
   private void installComponent(Context appContext, Class clazz)
   {
      appContext.set( Seam.getComponentName(clazz) + ".component", new Component(clazz) );
   }
   
   @Test
   public void testContextManagement() throws Exception
   {
      SeamELResolver seamVariableResolver = new SeamELResolver();
      org.jboss.seam.jbpm.SeamVariableResolver jbpmVariableResolver = new org.jboss.seam.jbpm.SeamVariableResolver();
      
      MockServletContext servletContext = new MockServletContext();
      MockExternalContext externalContext = new MockExternalContext(servletContext);
      Context appContext = new ApplicationContext( externalContext.getApplicationMap() );
      //appContext.set( Seam.getComponentName(Init.class), new Init() );
      installComponent(appContext, ConversationEntries.class);
      installComponent(appContext, Manager.class);
      installComponent(appContext, ServletSession.class);
      installComponent(appContext, ServletContexts.class);
      appContext.set( Seam.getComponentName(Init.class), new Init() );
      
      installComponent(appContext, Bar.class);
      installComponent(appContext, Foo.class);
      appContext.set("otherFoo", new Foo());
      
      assert !Contexts.isEventContextActive();
      assert !Contexts.isSessionContextActive();
      assert !Contexts.isConversationContextActive();
      assert !Contexts.isApplicationContextActive();
      
      Lifecycle.beginRequest(externalContext);
      
      assert Contexts.isEventContextActive();
      assert Contexts.isSessionContextActive();
      assert !Contexts.isConversationContextActive();
      assert Contexts.isApplicationContextActive();
      
      Manager.instance().setCurrentConversationId("3");
      Lifecycle.resumeConversation(externalContext);
      Manager.instance().setLongRunningConversation(true);
      
      assert Contexts.isEventContextActive();
      assert Contexts.isSessionContextActive();
      assert Contexts.isConversationContextActive();
      assert Contexts.isApplicationContextActive();
      assert !Contexts.isPageContextActive();
      
      assert Contexts.getEventContext()!=null;
      assert Contexts.getSessionContext()!=null;
      assert Contexts.getConversationContext()!=null;
      assert Contexts.getApplicationContext()!=null;
      assert Contexts.getEventContext() instanceof EventContext;
      assert Contexts.getSessionContext() instanceof SessionContext;
      assert Contexts.getConversationContext() instanceof ServerConversationContext;
      assert Contexts.getApplicationContext() instanceof ApplicationContext;
      
      Contexts.getSessionContext().set("zzz", "bar");
      Contexts.getApplicationContext().set("zzz", "bar");
      Contexts.getConversationContext().set("xxx", "yyy");
      
      Object bar = seamVariableResolver.getValue(EL.EL_CONTEXT, null, "bar");
      assert bar!=null;
      assert bar instanceof Bar;
      assert Contexts.getConversationContext().get("bar")==bar;
      Object foo = Contexts.getSessionContext().get("foo");
      assert foo!=null;
      assert foo instanceof Foo;
      
      Lifecycle.endRequest(externalContext);
      
      assert !Contexts.isEventContextActive();
      assert !Contexts.isSessionContextActive();
      assert !Contexts.isConversationContextActive();
      assert !Contexts.isApplicationContextActive();
      assert ((MockHttpSession)externalContext.getSession(false)).getAttributes().size()==4;
      assert ((MockServletContext)externalContext.getContext()).getAttributes().size()==9;
      
      Lifecycle.beginRequest(externalContext);
      
      assert Contexts.isEventContextActive();
      assert Contexts.isSessionContextActive();
      assert !Contexts.isConversationContextActive();
      assert Contexts.isApplicationContextActive();
      
      Manager.instance().setCurrentConversationId("3");
      Lifecycle.resumeConversation(externalContext);
      
      assert Contexts.isEventContextActive();
      assert Contexts.isSessionContextActive();
      assert Contexts.isConversationContextActive();
      assert Contexts.isApplicationContextActive();
      
      assert Contexts.getEventContext()!=null;
      assert Contexts.getSessionContext()!=null;
      assert Contexts.getConversationContext()!=null;
      assert Contexts.getApplicationContext()!=null;
      assert Contexts.getEventContext() instanceof EventContext;
      assert Contexts.getSessionContext() instanceof SessionContext;
      assert Contexts.getConversationContext() instanceof ServerConversationContext;
      assert Contexts.getApplicationContext() instanceof ApplicationContext;
      
      assert Contexts.getSessionContext().get("zzz").equals("bar");
      assert Contexts.getApplicationContext().get("zzz").equals("bar");
      assert Contexts.getConversationContext().get("xxx").equals("yyy");
      assert Contexts.getConversationContext().get("bar")==bar;
      assert Contexts.getSessionContext().get("foo")==foo;
      
      assert Contexts.getConversationContext().getNames().length==2;
      assert Contexts.getApplicationContext().getNames().length==9;
      assert Contexts.getSessionContext().getNames().length==2;
      
      assert seamVariableResolver.getValue(EL.EL_CONTEXT, null, "zzz").equals("bar");
      assert seamVariableResolver.getValue(EL.EL_CONTEXT, null, "xxx").equals("yyy");
      assert seamVariableResolver.getValue(EL.EL_CONTEXT, null, "bar")==bar;
      assert seamVariableResolver.getValue(EL.EL_CONTEXT, null, "foo")==foo;
      
      assert jbpmVariableResolver.resolveVariable("zzz").equals("bar");
      assert jbpmVariableResolver.resolveVariable("xxx").equals("yyy");
      assert jbpmVariableResolver.resolveVariable("bar")==bar;
      assert jbpmVariableResolver.resolveVariable("foo")==foo;

      Manager.instance().setLongRunningConversation(false);
      Lifecycle.endRequest(externalContext);
      
      assert !Contexts.isEventContextActive();
      assert !Contexts.isSessionContextActive();
      assert !Contexts.isConversationContextActive();
      assert !Contexts.isApplicationContextActive();
      assert ((MockHttpSession)externalContext.getSession(false)).getAttributes().size()==2;
      assert ((MockServletContext)externalContext.getContext()).getAttributes().size()==9;
      
      Lifecycle.endSession( servletContext, new ServletRequestSessionMap( (HttpServletRequest) externalContext.getRequest() ) );
            
      Lifecycle.endApplication(servletContext);
      
   }
   
   @Test
   public void testContexts()
   {
      MockServletContext servletContext = new MockServletContext();
      MockHttpSession session = new MockHttpSession(servletContext);
      MockHttpServletRequest request = new MockHttpServletRequest(session);
      ExternalContext externalContext = new MockExternalContext(servletContext, request);
      Map sessionAdaptor = new ServletRequestSessionMap(request);
      Map requestAdaptor = new ServletRequestMap(request);
      Context appContext = new ApplicationContext( externalContext.getApplicationMap() );
      installComponent(appContext, ConversationEntries.class);
      installComponent(appContext, Manager.class);
      appContext.set( Seam.getComponentName(Init.class), new Init() );
      Lifecycle.beginRequest(externalContext);
      Manager.instance().setLongRunningConversation(true);
      testContext( new ApplicationContext( externalContext.getApplicationMap() ) );
      testContext( new SessionContext(sessionAdaptor) );
      testContext( new EventContext(requestAdaptor) );
      testContext( new ServerConversationContext(sessionAdaptor, "1") );
      testEquivalence( new ServerConversationContext(sessionAdaptor, "1"), new ServerConversationContext(sessionAdaptor, "1") );
      testEquivalence( new SessionContext(sessionAdaptor), new SessionContext(sessionAdaptor) );
      testEquivalence( new ApplicationContext( externalContext.getApplicationMap() ), new ApplicationContext( externalContext.getApplicationMap() ) );
      testIsolation( new ServerConversationContext(sessionAdaptor, "1"), new ServerConversationContext(sessionAdaptor, "2") );
      // testIsolation( new WebSessionContext(externalContext), new WebSessionContext( new MockExternalContext()) );
      
      Lifecycle.endApplication(servletContext);
   }
   
   private void testEquivalence(Context ctx, Context cty)
   {
      ctx.set("foo", "bar");
      ctx.flush();
      assert cty.get("foo").equals("bar");
      ctx.remove("foo");
      ctx.flush();
      assert !cty.isSet("foo");
   }
   
   private void testIsolation(Context ctx, Context cty)
   {
      ctx.set("foo", "bar");
      ctx.flush();
      assert !cty.isSet("foo");
      cty.set("foo", "bar");
      ctx.remove("foo");
      ctx.flush();
      assert cty.get("foo").equals("bar");
   }
   
   private void testContext(Context ctx)
   {
      assert !ctx.isSet("foo");
      ctx.set("foo", "bar");
      assert ctx.isSet("foo");
      assert ctx.get("foo").equals("bar");
      ctx.remove("foo");
      assert !ctx.isSet("foo");
   }
}
