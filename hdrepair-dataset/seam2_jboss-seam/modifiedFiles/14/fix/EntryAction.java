package actions;

import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.RequestParameter;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.HttpError;

import domain.Blog;
import domain.BlogEntry;

/**
 * Processes a request for a particular entry,
 * and sends a 404 if none is found.
 * 
 * @author Gavin King
 */
@Name("entryAction")
@Scope(ScopeType.STATELESS)
public class EntryAction
{
   @In(create=true) 
   private Blog blog;
   
   @RequestParameter("blogEntryId")
   private String id;
   
   @Out(scope=ScopeType.EVENT, required=false)
   private BlogEntry blogEntry;

   
   public void getBlogEntry()
   {
      blogEntry = blog.getBlogEntry(id);
      if (blogEntry==null)
      {
         HttpError.instance().send(HttpServletResponse.SC_NOT_FOUND);
      }
   }
   
}
