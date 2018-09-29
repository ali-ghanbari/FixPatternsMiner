/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xpn.xwiki.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.velocity.VelocityContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.util.ToString;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameFactory;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.LinkType;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.SyntaxFactory;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.content.Link;
import com.xpn.xwiki.content.parsers.DocumentParser;
import com.xpn.xwiki.content.parsers.LinkParser;
import com.xpn.xwiki.content.parsers.RenamePageReplaceLinkHandler;
import com.xpn.xwiki.content.parsers.ReplacementResultCollection;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.validation.XWikiValidationInterface;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.ObjectAddForm;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;

public class XWikiDocument implements DocumentModelBridge
{
    private static final Log LOG = LogFactory.getLog(XWikiDocument.class);

    /** The default wiki name to use when one isn't specified. */
    private static final String DEFAULT_WIKI_NAME = "xwiki";

    /**
     * Regex Pattern to recognize if there's HTML code in a XWiki page.
     */
    private static final Pattern HTML_TAG_PATTERN =
        Pattern.compile("</?(html|body|img|a|i|b|embed|script|form|input|textarea|object|"
            + "font|li|ul|ol|table|center|hr|br|p) ?([^>]*)>");

    public static final String XWIKI10_SYNTAXID = "xwiki/1.0";

    public static final String XWIKI20_SYNTAXID = "xwiki/2.0";

    private String title;

    private String parent;

    private String space;

    private String name;

    private String content;

    private String meta;

    private String format;

    private String creator;

    private String author;

    private String contentAuthor;

    private String customClass;

    private Date contentUpdateDate;

    private Date updateDate;

    private Date creationDate;

    private Version version;

    private long id = 0;

    private boolean mostRecent = true;

    private boolean isNew = true;

    private String template;

    private String language;

    private String defaultLanguage;

    private int translation;

    private String database;

    private BaseObject tags;

    /**
     * Indicates whether the document is 'hidden', meaning that it should not be returned in public search results.
     * WARNING: this is a temporary hack until the new data model is designed and implemented. No code should rely on or
     * use this property, since it will be replaced with a generic metadata.
     */
    private boolean hidden = false;

    /**
     * Comment on the latest modification.
     */
    private String comment;

    /**
     * Wiki syntax supported by this document. This is used to support different syntaxes inside the same wiki. For
     * example a page can use the Confluence 2.0 syntax while another one uses the XWiki 1.0 syntax. In practice our
     * first need is to support the new rendering component. To use the old rendering implementation specify a
     * "xwiki/1.0" syntaxId and use a "xwiki/2.0" syntaxId for using the new rendering component.
     */
    private String syntaxId;

    /**
     * Is latest modification a minor edit
     */
    private boolean isMinorEdit = false;

    /**
     * Used to make sure the MetaData String is regenerated.
     */
    private boolean isContentDirty = true;

    /**
     * Used to make sure the MetaData String is regenerated
     */
    private boolean isMetaDataDirty = true;

    public static final int HAS_ATTACHMENTS = 1;

    public static final int HAS_OBJECTS = 2;

    public static final int HAS_CLASS = 4;

    private int elements = HAS_OBJECTS | HAS_ATTACHMENTS;

    /**
     * Separator string between database name and space name.
     */
    public static final String DB_SPACE_SEP = ":";

    /**
     * Separator string between space name and page name.
     */
    public static final String SPACE_NAME_SEP = ".";

    // Meta Data
    private BaseClass xWikiClass;

    private String xWikiClassXML;

    /**
     * Map holding document objects grouped by classname (className -> Vector of objects). The map is not synchronized,
     * and uses a TreeMap implementation to preserve className ordering (consistent sorted order for output to XML,
     * rendering in velocity, etc.)
     */
    private Map<String, Vector<BaseObject>> xWikiObjects = new TreeMap<String, Vector<BaseObject>>();

    private List<XWikiAttachment> attachmentList;

    // Caching
    private boolean fromCache = false;

    private ArrayList<BaseObject> objectsToRemove = new ArrayList<BaseObject>();

    /**
     * The view template (vm file) to use. When not set the default view template is used.
     * 
     * @see com.xpn.xwiki.web.ViewAction#render(XWikiContext)
     */
    private String defaultTemplate;

    private String validationScript;

    private Object wikiNode;

    // We are using a SoftReference which will allow the archive to be
    // discarded by the Garbage collector as long as the context is closed (usually during the
    // request)
    private SoftReference<XWikiDocumentArchive> archive;

    private XWikiStoreInterface store;

    /**
     * This is a copy of this XWikiDocument before any modification was made to it. It is reset to the actual values
     * when the document is saved in the database. This copy is used for finding out differences made to this document
     * (useful for example to send the correct notifications to document change listeners).
     */
    private XWikiDocument originalDocument;

    /**
     * The document structure expressed as a tree of Block objects. We store it for performance reasons since parsing is
     * a costly operation that we don't want to repeat whenever some code ask for the XDOM information.
     */
    private XDOM xdom;

    /**
     * Used to convert a string into a proper Document Name.
     */
    private DocumentNameFactory documentNameFactory =
        (DocumentNameFactory) Utils.getComponent(DocumentNameFactory.class);

    /**
     * Used to convert a proper Document Name to string.
     */
    private DocumentNameSerializer compactDocumentNameSerializer =
        (DocumentNameSerializer) Utils.getComponent(DocumentNameSerializer.class, "compact");

    public XWikiStoreInterface getStore(XWikiContext context)
    {
        return context.getWiki().getStore();
    }

    public XWikiAttachmentStoreInterface getAttachmentStore(XWikiContext context)
    {
        return context.getWiki().getAttachmentStore();
    }

    public XWikiVersioningStoreInterface getVersioningStore(XWikiContext context)
    {
        return context.getWiki().getVersioningStore();
    }

    public XWikiStoreInterface getStore()
    {
        return this.store;
    }

    public void setStore(XWikiStoreInterface store)
    {
        this.store = store;
    }

    public long getId()
    {
        if ((this.language == null) || this.language.trim().equals("")) {
            this.id = getFullName().hashCode();
        } else {
            this.id = (getFullName() + ":" + this.language).hashCode();
        }

        // if (log.isDebugEnabled())
        // log.debug("ID: " + getFullName() + " " + language + ": " + id);
        return this.id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    /**
     * @return the name of the space of the document
     */
    public String getSpace()
    {
        return this.space;
    }

    public void setSpace(String space)
    {
        this.space = space;
    }

    public String getVersion()
    {
        return getRCSVersion().toString();
    }

    public void setVersion(String version)
    {
        if (version != null && !"".equals(version)) {
            this.version = new Version(version);
        }
    }

    public Version getRCSVersion()
    {
        if (this.version == null) {
            return new Version("1.1");
        }
        return this.version;
    }

    public void setRCSVersion(Version version)
    {
        this.version = version;
    }

    public XWikiDocument()
    {
        this("Main", "WebHome");
    }

    /**
     * Constructor that specifies the local document identifier: space name, document name. {@link #setDatabase(String)}
     * must be called afterwards to specify the wiki name.
     * 
     * @param web The space this document belongs to.
     * @param name The name of the document.
     */
    public XWikiDocument(String space, String name)
    {
        this(null, space, name);
    }

    /**
     * Constructor that specifies the full document identifier: wiki name, space name, document name.
     * 
     * @param wiki The wiki this document belongs to.
     * @param web The space this document belongs to.
     * @param name The name of the document.
     */
    public XWikiDocument(String wiki, String web, String name)
    {
        setDatabase(wiki);
        setSpace(web);

        int i1 = name.indexOf(".");
        if (i1 == -1) {
            setName(name);
        } else {
            setSpace(name.substring(0, i1));
            setName(name.substring(i1 + 1));
        }
        this.updateDate = new Date();
        this.updateDate.setTime((this.updateDate.getTime() / 1000) * 1000);
        this.contentUpdateDate = new Date();
        this.contentUpdateDate.setTime((this.contentUpdateDate.getTime() / 1000) * 1000);
        this.creationDate = new Date();
        this.creationDate.setTime((this.creationDate.getTime() / 1000) * 1000);
        this.parent = "";
        this.content = "\n";
        this.format = "";
        this.author = "";
        this.language = "";
        this.defaultLanguage = "";
        this.attachmentList = new ArrayList<XWikiAttachment>();
        this.customClass = "";
        this.comment = "";

        // Note: As there's no notion of an Empty document we don't set the original document
        // field. Thus getOriginalDocument() may return null.
    }

    /**
     * @return the copy of this XWikiDocument instance before any modification was made to it.
     * @see #originalDocument
     */
    public XWikiDocument getOriginalDocument()
    {
        return this.originalDocument;
    }

    /**
     * @param originalDocument the original document representing this document instance before any change was made to
     *            it, prior to the last time it was saved
     * @see #originalDocument
     */
    public void setOriginalDocument(XWikiDocument originalDocument)
    {
        this.originalDocument = originalDocument;
    }

    public XWikiDocument getParentDoc()
    {
        return new XWikiDocument(getSpace(), getParent());
    }

    public String getParent()
    {
        return this.parent != null ? this.parent : "";
    }

    public void setParent(String parent)
    {
        if (parent != null && !parent.equals(this.parent)) {
            setMetaDataDirty(true);
        }
        this.parent = parent;
    }

    public String getContent()
    {
        return this.content;
    }

    public void setContent(String content)
    {
        if (content == null) {
            content = "";
        }
        if (!content.equals(this.content)) {
            setContentDirty(true);
            setWikiNode(null);
        }
        this.content = content;

        // invalidate parsed xdom
        this.xdom = null;
    }

    public String getRenderedContent(XWikiContext context) throws XWikiException
    {
        // Note: We are currently duplicating code from the other getRendered signature since there are
        // some unresolved issues with saving/restoring the context in some cases (need to be investigated),
        // for example in the Admin Import page.

        String renderedContent;
        Object isInRenderingEngine = context.get("isInRenderingEngine");

        try {
            // This tells display() methods that we are inside the rendering engine and thus
            // that they can return wiki syntax and not HTML syntax (which is needed when
            // outside the rendering engine, i.e. when we're inside templates using only
            // Velocity for example).
            context.put("isInRenderingEngine", true);

            // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem. Otherwise use the new one.
            if (is10Syntax()) {
                renderedContent = context.getWiki().getRenderingEngine().renderDocument(this, context);
            } else {
                renderedContent =
                    performSyntaxConversion(getTranslatedContent(context), getSyntaxId(), "xhtml/1.0", true);
            }
        } finally {
            if (isInRenderingEngine != null) {
                context.put("isInRenderingEngine", isInRenderingEngine);
            } else {
                context.remove("isInRenderingEngine");
            }
        }
        return renderedContent;
    }

    /**
     * @param text the text to render
     * @param syntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @param context the XWiki Context object
     * @return the given text rendered in the context of this document using the passed Syntax
     * @since 1.6M1
     */
    public String getRenderedContent(String text, String syntaxId, XWikiContext context)
    {
        String result;
        HashMap<String, Object> backup = new HashMap<String, Object>();
        Object isInRenderingEngine = context.get("isInRenderingEngine");
        try {
            backupContext(backup, context);
            setAsContextDoc(context);

            // This tells display() methods that we are inside the rendering engine and thus
            // that they can return wiki syntax and not HTML syntax (which is needed when
            // outside the rendering engine, i.e. when we're inside templates using only
            // Velocity for example).
            context.put("isInRenderingEngine", true);

            // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem. Otherwise use the new one.
            if (is10Syntax(syntaxId)) {
                result = context.getWiki().getRenderingEngine().renderText(text, this, context);
            } else {
                result = performSyntaxConversion(text, getSyntaxId(), "xhtml/1.0", true);
            }
        } catch (XWikiException e) {
            // Failed to render for some reason. This method should normally throw an exception but this
            // requires changing the signature of calling methods too.
            LOG.warn(e);
            result = "";
        } finally {
            restoreContext(backup, context);
            if (isInRenderingEngine != null) {
                context.put("isInRenderingEngine", isInRenderingEngine);
            } else {
                context.remove("isInRenderingEngine");
            }
        }
        return result;
    }

    public String getEscapedContent(XWikiContext context) throws XWikiException
    {
        CharacterFilter filter = new CharacterFilter();
        return filter.process(getTranslatedContent(context));
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public void setParent(DocumentName parentName)
    {
        this.parent = parentName.toString();
    }

    public String getFullName()
    {
        StringBuffer buf = new StringBuffer();
        buf.append(getSpace());
        buf.append(".");
        buf.append(getName());
        return buf.toString();
    }

    public void setFullName(String name)
    {
        setFullName(name, null);
    }

    /**
     * @return a proper {@link DocumentName} for this document.
     */
    public DocumentName getDocumentName()
    {
        return new DocumentName(getWikiName(), getSpaceName(), getPageName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentModelBridge#getWikiName()
     */
    public String getWikiName()
    {
        return StringUtils.isEmpty(getDatabase()) ? DEFAULT_WIKI_NAME : getDatabase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentModelBridge#getSpaceName()
     */
    public String getSpaceName()
    {
        return this.getSpace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentModelBridge#getSpaceName()
     */
    public String getPageName()
    {
        return this.getName();
    }

    public String getTitle()
    {
        return (this.title != null) ? this.title : "";
    }

    /**
     * @param context the XWiki context used to get access to the XWikiRenderingEngine object
     * @return the document title. If a title has not been provided, look for a section title in the document's content
     *         and if not found return the page name. The returned title is also interpreted which means it's allowed to
     *         use Velocity, Groovy, etc syntax within a title.
     */
    public String getDisplayTitle(XWikiContext context)
    {
        // 1) Check if the user has provided a title
        String title = getTitle();

        // 2) If not, then try to extract the title from the first document section title
        if (title.length() == 0) {
            title = extractTitle();
        }

        // 3) Last if a title has been found renders it as it can contain macros, velocity code,
        // groovy, etc.
        if (title.length() > 0) {
            // This will not completely work for scripting code in title referencing variables
            // defined elsewhere. In that case it'll only work if those variables have been
            // parsed and put in the corresponding scripting context. This will not work for
            // breadcrumbs for example.
            title = context.getWiki().getRenderingEngine().interpretText(title, this, context);
        } else {
            // 4) No title has been found, return the page name as the title
            title = getName();
        }

        return title;
    }

    public String extractTitle()
    {
        String title = "";

        try {
            if (is10Syntax()) {
                title = extractTitle10();
            } else {
                List<HeaderBlock> blocks = getXDOM().getChildrenByType(HeaderBlock.class, true);
                if (blocks.size() > 0) {
                    HeaderBlock header = blocks.get(0);
                    if (header.getLevel().compareTo(HeaderLevel.LEVEL2) <= 0) {
                        title = renderXDOM(new XDOM(header.getChildren()), "plain/1.0");
                    }
                }
            }
        } catch (Exception e) {
        }

        return title;
    }

    /**
     * @return the first level 1 or level 1.1 title text in the document's content or "" if none are found
     * @todo this method has nothing to do in this class and should be moved elsewhere
     */
    public String extractTitle10()
    {
        String content = getContent();
        int i1 = 0;
        int i2;

        while (true) {
            i2 = content.indexOf("\n", i1);
            String title = "";
            if (i2 != -1) {
                title = content.substring(i1, i2).trim();
            } else {
                title = content.substring(i1).trim();
            }
            if ((!title.equals("")) && (title.matches("1(\\.1)?\\s+.+"))) {
                return title.substring(title.indexOf(" ")).trim();
            }
            if (i2 == -1) {
                break;
            }
            i1 = i2 + 1;
        }

        return "";
    }

    public void setTitle(String title)
    {
        if (title != null && !title.equals(this.title)) {
            setContentDirty(true);
        }
        this.title = title;
    }

    public String getFormat()
    {
        return this.format != null ? this.format : "";
    }

    public void setFormat(String format)
    {
        this.format = format;
        if (!format.equals(this.format)) {
            setMetaDataDirty(true);
        }
    }

    public String getAuthor()
    {
        return this.author != null ? this.author.trim() : "";
    }

    public String getContentAuthor()
    {
        return this.contentAuthor != null ? this.contentAuthor.trim() : "";
    }

    public void setAuthor(String author)
    {
        if (!getAuthor().equals(author)) {
            setMetaDataDirty(true);
        }
        this.author = author;
    }

    public void setContentAuthor(String contentAuthor)
    {
        if (!getContentAuthor().equals(contentAuthor)) {
            setMetaDataDirty(true);
        }
        this.contentAuthor = contentAuthor;
    }

    public String getCreator()
    {
        return this.creator != null ? this.creator.trim() : "";
    }

    public void setCreator(String creator)
    {
        if (!getCreator().equals(creator)) {
            setMetaDataDirty(true);
        }
        this.creator = creator;
    }

    public Date getDate()
    {
        if (this.updateDate == null) {
            return new Date();
        } else {
            return this.updateDate;
        }
    }

    public void setDate(Date date)
    {
        if ((date != null) && (!date.equals(this.updateDate))) {
            setMetaDataDirty(true);
        }
        // Make sure we drop milliseconds for consistency with the database
        if (date != null) {
            date.setTime((date.getTime() / 1000) * 1000);
        }
        this.updateDate = date;
    }

    public Date getCreationDate()
    {
        if (this.creationDate == null) {
            return new Date();
        } else {
            return this.creationDate;
        }
    }

    public void setCreationDate(Date date)
    {
        if ((date != null) && (!date.equals(this.creationDate))) {
            setMetaDataDirty(true);
        }

        // Make sure we drop milliseconds for consistency with the database
        if (date != null) {
            date.setTime((date.getTime() / 1000) * 1000);
        }
        this.creationDate = date;
    }

    public Date getContentUpdateDate()
    {
        if (this.contentUpdateDate == null) {
            return new Date();
        } else {
            return this.contentUpdateDate;
        }
    }

    public void setContentUpdateDate(Date date)
    {
        if ((date != null) && (!date.equals(this.contentUpdateDate))) {
            setMetaDataDirty(true);
        }

        // Make sure we drop milliseconds for consistency with the database
        if (date != null) {
            date.setTime((date.getTime() / 1000) * 1000);
        }
        this.contentUpdateDate = date;
    }

    public String getMeta()
    {
        return this.meta;
    }

    public void setMeta(String meta)
    {
        if (meta == null) {
            if (this.meta != null) {
                setMetaDataDirty(true);
            }
        } else if (!meta.equals(this.meta)) {
            setMetaDataDirty(true);
        }
        this.meta = meta;
    }

    public void appendMeta(String meta)
    {
        StringBuffer buf = new StringBuffer(this.meta);
        buf.append(meta);
        buf.append("\n");
        this.meta = buf.toString();
        setMetaDataDirty(true);
    }

    public boolean isContentDirty()
    {
        return this.isContentDirty;
    }

    public void incrementVersion()
    {
        if (this.version == null) {
            this.version = new Version("1.1");
        } else {
            if (isMinorEdit()) {
                this.version = this.version.next();
            } else {
                this.version = this.version.getBranchPoint().next().newBranch(1);
            }
        }
    }

    public void setContentDirty(boolean contentDirty)
    {
        this.isContentDirty = contentDirty;
    }

    public boolean isMetaDataDirty()
    {
        return this.isMetaDataDirty;
    }

    public void setMetaDataDirty(boolean metaDataDirty)
    {
        this.isMetaDataDirty = metaDataDirty;
    }

    public String getAttachmentURL(String filename, XWikiContext context)
    {
        return getAttachmentURL(filename, "download", context);
    }

    public String getAttachmentURL(String filename, String action, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createAttachmentURL(filename, getSpace(), getName(), action, null, getDatabase(),
                context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getExternalAttachmentURL(String filename, String action, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createAttachmentURL(filename, getSpace(), getName(), action, null, getDatabase(),
                context);
        return url.toString();
    }

    public String getAttachmentURL(String filename, String action, String querystring, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createAttachmentURL(filename, getSpace(), getName(), action, querystring,
                getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getAttachmentRevisionURL(String filename, String revision, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createAttachmentRevisionURL(filename, getSpace(), getName(), revision, null,
                getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getAttachmentRevisionURL(String filename, String revision, String querystring, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createAttachmentRevisionURL(filename, getSpace(), getName(), revision, querystring,
                getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getURL(String action, String params, boolean redirect, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createURL(getSpace(), getName(), action, params, null, getDatabase(), context);
        if (redirect) {
            if (url == null) {
                return null;
            } else {
                return url.toString();
            }
        } else {
            return context.getURLFactory().getURL(url, context);
        }
    }

    public String getURL(String action, boolean redirect, XWikiContext context)
    {
        return getURL(action, null, redirect, context);
    }

    public String getURL(String action, XWikiContext context)
    {
        return getURL(action, false, context);
    }

    public String getURL(String action, String querystring, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createURL(getSpace(), getName(), action, querystring, null, getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getURL(String action, String querystring, String anchor, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createURL(getSpace(), getName(), action, querystring, anchor, getDatabase(),
                context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getExternalURL(String action, XWikiContext context)
    {
        URL url =
            context.getURLFactory()
                .createExternalURL(getSpace(), getName(), action, null, null, getDatabase(), context);
        return url.toString();
    }

    public String getExternalURL(String action, String querystring, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createExternalURL(getSpace(), getName(), action, querystring, null, getDatabase(),
                context);
        return url.toString();
    }

    public String getParentURL(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(getParent(), context);
        URL url =
            context.getURLFactory()
                .createURL(doc.getSpace(), doc.getName(), "view", null, null, getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public XWikiDocumentArchive getDocumentArchive(XWikiContext context) throws XWikiException
    {
        loadArchive(context);
        return getDocumentArchive();
    }

    /**
     * Create a new protected {@link com.xpn.xwiki.api.Document} public API to access page information and actions from
     * scripting.
     * 
     * @param customClassName the name of the custom {@link com.xpn.xwiki.api.Document} class of the object to create.
     * @param context the XWiki context.
     * @return a wrapped version of an XWikiDocument. Prefer this function instead of new Document(XWikiDocument,
     *         XWikiContext)
     */
    public com.xpn.xwiki.api.Document newDocument(String customClassName, XWikiContext context)
    {
        if (!((customClassName == null) || (customClassName.equals("")))) {
            try {
                return newDocument(Class.forName(customClassName), context);
            } catch (ClassNotFoundException e) {
                LOG.error("Failed to get java Class object from class name", e);
            }
        }

        return new com.xpn.xwiki.api.Document(this, context);
    }

    /**
     * Create a new protected {@link com.xpn.xwiki.api.Document} public API to access page information and actions from
     * scripting.
     * 
     * @param customClass the custom {@link com.xpn.xwiki.api.Document} class the object to create.
     * @param context the XWiki context.
     * @return a wrapped version of an XWikiDocument. Prefer this function instead of new Document(XWikiDocument,
     *         XWikiContext)
     */
    public com.xpn.xwiki.api.Document newDocument(Class< ? > customClass, XWikiContext context)
    {
        if (customClass != null) {
            try {
                Class< ? >[] classes = new Class[] {XWikiDocument.class, XWikiContext.class};
                Object[] args = new Object[] {this, context};

                return (com.xpn.xwiki.api.Document) customClass.getConstructor(classes).newInstance(args);
            } catch (Exception e) {
                LOG.error("Failed to create a custom Document object", e);
            }
        }

        return new com.xpn.xwiki.api.Document(this, context);
    }

    public com.xpn.xwiki.api.Document newDocument(XWikiContext context)
    {
        String customClass = getCustomClass();
        return newDocument(customClass, context);
    }

    public void loadArchive(XWikiContext context) throws XWikiException
    {
        if (this.archive == null || this.archive.get() == null) {
            XWikiDocumentArchive arch = getVersioningStore(context).getXWikiDocumentArchive(this, context);
            // We are using a SoftReference which will allow the archive to be
            // discarded by the Garbage collector as long as the context is closed (usually during
            // the request)
            this.archive = new SoftReference<XWikiDocumentArchive>(arch);
        }
    }

    public XWikiDocumentArchive getDocumentArchive()
    {
        // We are using a SoftReference which will allow the archive to be
        // discarded by the Garbage collector as long as the context is closed (usually during the
        // request)
        if (this.archive == null) {
            return null;
        } else {
            return this.archive.get();
        }
    }

    public void setDocumentArchive(XWikiDocumentArchive arch)
    {
        // We are using a SoftReference which will allow the archive to be
        // discarded by the Garbage collector as long as the context is closed (usually during the
        // request)
        if (arch != null) {
            this.archive = new SoftReference<XWikiDocumentArchive>(arch);
        }
    }

    public void setDocumentArchive(String sarch) throws XWikiException
    {
        XWikiDocumentArchive xda = new XWikiDocumentArchive(getId());
        xda.setArchive(sarch);
        setDocumentArchive(xda);
    }

    public Version[] getRevisions(XWikiContext context) throws XWikiException
    {
        return getVersioningStore(context).getXWikiDocVersions(this, context);
    }

    public String[] getRecentRevisions(int nb, XWikiContext context) throws XWikiException
    {
        try {
            Version[] revisions = getVersioningStore(context).getXWikiDocVersions(this, context);
            int length = nb;
            // 0 means all revisions
            if (nb == 0) {
                length = revisions.length;
            }

            if (revisions.length < length) {
                length = revisions.length;
            }

            String[] recentrevs = new String[length];
            for (int i = 1; i <= length; i++) {
                recentrevs[i - 1] = revisions[revisions.length - i].toString();
            }
            return recentrevs;
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Get document versions matching criterias like author, minimum creation date, etc.
     * 
     * @param criteria criteria used to match versions
     * @return a list of matching versions
     */
    public List<String> getRevisions(RevisionCriteria criteria, XWikiContext context) throws XWikiException
    {
        List<String> results = new ArrayList<String>();

        Version[] revisions = getRevisions(context);

        XWikiRCSNodeInfo nextNodeinfo = null;
        XWikiRCSNodeInfo nodeinfo = null;
        for (int i = 0; i < revisions.length; i++) {
            nodeinfo = nextNodeinfo;
            nextNodeinfo = getRevisionInfo(revisions[i].toString(), context);

            if (nodeinfo == null) {
                continue;
            }

            // Minor/Major version matching
            if (criteria.getIncludeMinorVersions() || !nextNodeinfo.isMinorEdit()) {
                // Author matching
                if (criteria.getAuthor().equals("") || criteria.getAuthor().equals(nodeinfo.getAuthor())) {
                    // Date range matching
                    Date versionDate = nodeinfo.getDate();
                    if (versionDate.after(criteria.getMinDate()) && versionDate.before(criteria.getMaxDate())) {
                        results.add(nodeinfo.getVersion().toString());
                    }
                }
            }
        }

        nodeinfo = nextNodeinfo;
        if (nodeinfo != null) {
            if (criteria.getAuthor().equals("") || criteria.getAuthor().equals(nodeinfo.getAuthor())) {
                // Date range matching
                Date versionDate = nodeinfo.getDate();
                if (versionDate.after(criteria.getMinDate()) && versionDate.before(criteria.getMaxDate())) {
                    results.add(nodeinfo.getVersion().toString());
                }
            }
        }

        return criteria.getRange().subList(results);
    }

    public XWikiRCSNodeInfo getRevisionInfo(String version, XWikiContext context) throws XWikiException
    {
        return getDocumentArchive(context).getNode(new Version(version));
    }

    /**
     * @return Is this version the most recent one. False if and only if there are newer versions of this document in
     *         the database.
     */
    public boolean isMostRecent()
    {
        return this.mostRecent;
    }

    /**
     * must not be used unless in store system.
     * 
     * @param mostRecent - mark document as most recent.
     */
    public void setMostRecent(boolean mostRecent)
    {
        this.mostRecent = mostRecent;
    }

    public BaseClass getxWikiClass()
    {
        if (this.xWikiClass == null) {
            this.xWikiClass = new BaseClass();
            this.xWikiClass.setName(getFullName());
        }
        return this.xWikiClass;
    }

    public void setxWikiClass(BaseClass xWikiClass)
    {
        this.xWikiClass = xWikiClass;
    }

    public Map<String, Vector<BaseObject>> getxWikiObjects()
    {
        return this.xWikiObjects;
    }

    public void setxWikiObjects(Map<String, Vector<BaseObject>> xWikiObjects)
    {
        this.xWikiObjects = xWikiObjects;
    }

    public BaseObject getxWikiObject()
    {
        return getObject(getFullName());
    }

    public List<BaseClass> getxWikiClasses(XWikiContext context)
    {
        List<BaseClass> list = new ArrayList<BaseClass>();

        // xWikiObjects is a TreeMap, with elements sorted by className
        for (String classname : getxWikiObjects().keySet()) {
            BaseClass bclass = null;
            Vector<BaseObject> objects = getObjects(classname);
            for (BaseObject obj : objects) {
                if (obj != null) {
                    bclass = obj.getxWikiClass(context);
                    if (bclass != null) {
                        break;
                    }
                }
            }
            if (bclass != null) {
                list.add(bclass);
            }
        }
        return list;
    }

    public int createNewObject(String classname, XWikiContext context) throws XWikiException
    {
        BaseObject object = BaseClass.newCustomClassInstance(classname, context);
        object.setWiki(getDatabase());
        object.setName(getFullName());
        object.setClassName(classname);
        Vector<BaseObject> objects = getObjects(classname);
        if (objects == null) {
            objects = new Vector<BaseObject>();
            setObjects(classname, objects);
        }
        objects.add(object);
        int nb = objects.size() - 1;
        object.setNumber(nb);
        setContentDirty(true);
        return nb;
    }

    public int getObjectNumbers(String classname)
    {
        try {
            return getxWikiObjects().get(classname).size();
        } catch (Exception e) {
            return 0;
        }
    }

    public Vector<BaseObject> getObjects(String classname)
    {
        if (classname == null) {
            return new Vector<BaseObject>();
        }
        if (classname.indexOf(".") == -1) {
            classname = "XWiki." + classname;
        }
        return getxWikiObjects().get(classname);
    }

    public void setObjects(String classname, Vector<BaseObject> objects)
    {
        if (classname.indexOf(".") == -1) {
            classname = "XWiki." + classname;
        }
        getxWikiObjects().put(classname, objects);
    }

    public BaseObject getObject(String classname)
    {
        if (classname.indexOf(".") == -1) {
            classname = "XWiki." + classname;
        }
        Vector<BaseObject> objects = getxWikiObjects().get(classname);
        if (objects == null) {
            return null;
        }
        for (int i = 0; i < objects.size(); i++) {
            BaseObject obj = objects.get(i);
            if (obj != null) {
                return obj;
            }
        }
        return null;
    }

    public BaseObject getObject(String classname, int nb)
    {
        try {
            if (classname.indexOf(".") == -1) {
                classname = "XWiki." + classname;
            }
            return getxWikiObjects().get(classname).get(nb);
        } catch (Exception e) {
            return null;
        }
    }

    public BaseObject getObject(String classname, String key, String value)
    {
        return getObject(classname, key, value, false);
    }

    public BaseObject getObject(String classname, String key, String value, boolean failover)
    {
        if (classname.indexOf(".") == -1) {
            classname = "XWiki." + classname;
        }
        try {
            if (value == null) {
                if (failover) {
                    return getObject(classname);
                } else {
                    return null;
                }
            }

            Vector<BaseObject> objects = getxWikiObjects().get(classname);
            if ((objects == null) || (objects.size() == 0)) {
                return null;
            }
            for (int i = 0; i < objects.size(); i++) {
                BaseObject obj = objects.get(i);
                if (obj != null) {
                    if (value.equals(obj.getStringValue(key))) {
                        return obj;
                    }
                }
            }

            if (failover) {
                return getObject(classname);
            } else {
                return null;
            }
        } catch (Exception e) {
            if (failover) {
                return getObject(classname);
            }

            e.printStackTrace();
            return null;
        }
    }

    public void addObject(String classname, BaseObject object)
    {
        if (object != null) {
            object.setWiki(getDatabase());
            object.setName(getFullName());
        }
        Vector<BaseObject> vobj = getObjects(classname);
        if (vobj == null) {
            setObject(classname, 0, object);
        } else {
            setObject(classname, vobj.size(), object);
        }
        setContentDirty(true);
    }

    public void setObject(String classname, int nb, BaseObject object)
    {
        if (object != null) {
            object.setWiki(getDatabase());
            object.setName(getFullName());
        }
        Vector<BaseObject> objects = getObjects(classname);
        if (objects == null) {
            objects = new Vector<BaseObject>();
            setObjects(classname, objects);
        }
        if (nb >= objects.size()) {
            objects.setSize(nb + 1);
        }
        objects.set(nb, object);
        object.setNumber(nb);
        setContentDirty(true);
    }

    /**
     * @return true if the document is a new one (i.e. it has never been saved) or false otherwise
     */
    public boolean isNew()
    {
        return this.isNew;
    }

    public void setNew(boolean aNew)
    {
        this.isNew = aNew;
    }

    public void mergexWikiClass(XWikiDocument templatedoc)
    {
        BaseClass bclass = getxWikiClass();
        BaseClass tbclass = templatedoc.getxWikiClass();
        if (tbclass != null) {
            if (bclass == null) {
                setxWikiClass((BaseClass) tbclass.clone());
            } else {
                getxWikiClass().merge((BaseClass) tbclass.clone());
            }
        }
        setContentDirty(true);
    }

    public void mergexWikiObjects(XWikiDocument templatedoc)
    {
        // TODO: look for each object if it already exist and add it if it doesn't
        for (String name : templatedoc.getxWikiObjects().keySet()) {
            Vector<BaseObject> myObjects = getxWikiObjects().get(name);

            if (myObjects == null) {
                myObjects = new Vector<BaseObject>();
            }
            for (BaseObject otherObject : templatedoc.getxWikiObjects().get(name)) {
                if (otherObject != null) {
                    BaseObject myObject = (BaseObject) otherObject.clone();
                    // BaseObject.clone copies the GUID, so randomize it for the copied object.
                    myObject.setGuid(UUID.randomUUID().toString());
                    myObjects.add(myObject);
                    myObject.setNumber(myObjects.size() - 1);
                }
            }
            getxWikiObjects().put(name, myObjects);
        }
        setContentDirty(true);
    }

    public void clonexWikiObjects(XWikiDocument templatedoc)
    {
        for (String name : templatedoc.getxWikiObjects().keySet()) {
            Vector<BaseObject> tobjects = templatedoc.getObjects(name);
            Vector<BaseObject> objects = new Vector<BaseObject>();
            objects.setSize(tobjects.size());
            for (int i = 0; i < tobjects.size(); i++) {
                BaseObject otherObject = tobjects.get(i);
                if (otherObject != null) {
                    BaseObject myObject = (BaseObject) otherObject.clone();
                    objects.set(i, myObject);
                }
            }
            getxWikiObjects().put(name, objects);
        }
    }

    public String getTemplate()
    {
        return StringUtils.defaultString(this.template);
    }

    public void setTemplate(String template)
    {
        this.template = template;
        setMetaDataDirty(true);
    }

    public String displayPrettyName(String fieldname, XWikiContext context)
    {
        return displayPrettyName(fieldname, false, true, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, XWikiContext context)
    {
        return displayPrettyName(fieldname, showMandatory, true, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before, XWikiContext context)
    {
        try {
            BaseObject object = getxWikiObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }
            return displayPrettyName(fieldname, showMandatory, before, object, context);
        } catch (Exception e) {
            return "";
        }
    }

    public String displayPrettyName(String fieldname, BaseObject obj, XWikiContext context)
    {
        return displayPrettyName(fieldname, false, true, obj, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, BaseObject obj, XWikiContext context)
    {
        return displayPrettyName(fieldname, showMandatory, true, obj, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before, BaseObject obj,
        XWikiContext context)
    {
        try {
            PropertyClass pclass = (PropertyClass) obj.getxWikiClass(context).get(fieldname);
            String dprettyName = "";
            if (showMandatory) {
                dprettyName = context.getWiki().addMandatory(context);
            }
            if (before) {
                return dprettyName + pclass.getPrettyName(context);
            } else {
                return pclass.getPrettyName(context) + dprettyName;
            }
        } catch (Exception e) {
            return "";
        }
    }

    public String displayTooltip(String fieldname, XWikiContext context)
    {
        try {
            BaseObject object = getxWikiObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }
            return displayTooltip(fieldname, object, context);
        } catch (Exception e) {
            return "";
        }
    }

    public String displayTooltip(String fieldname, BaseObject obj, XWikiContext context)
    {
        try {
            PropertyClass pclass = (PropertyClass) obj.getxWikiClass(context).get(fieldname);
            String tooltip = pclass.getTooltip(context);
            if ((tooltip != null) && (!tooltip.trim().equals(""))) {
                String img =
                    "<img src=\"" + context.getWiki().getSkinFile("info.gif", context)
                        + "\" class=\"tooltip_image\" align=\"middle\" />";
                return context.getWiki().addTooltip(img, tooltip, context);
            } else {
                return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    public String display(String fieldname, String type, BaseObject obj, XWikiContext context)
    {
        return display(fieldname, type, "", obj, context.getDoc() != null ? context.getDoc().getSyntaxId()
            : getSyntaxId(), context);
    }

    /**
     * Note: We've introduced this signature taking an extra syntaxId parameter to handle the case where Panels are
     * written in a syntax other than the main document. The problem is that currently the displayPanel() velocity macro
     * in macros.vm calls display() on the main document and not on the panel document. Thus if we don't tell what
     * syntax to use the main document syntax will be used to display panels even if they're written in another syntax.
     */
    public String display(String fieldname, String type, BaseObject obj, String syntaxId, XWikiContext context)
    {
        return display(fieldname, type, "", obj, syntaxId, context);
    }

    public String display(String fieldname, String type, String pref, BaseObject obj, String syntaxId,
        XWikiContext context)
    {
        if (obj == null) {
            return "";
        }
        boolean isInRenderingEngine = BooleanUtils.toBoolean((Boolean) context.get("isInRenderingEngine"));
        HashMap<String, Object> backup = new HashMap<String, Object>();
        try {
            backupContext(backup, context);
            setAsContextDoc(context);

            type = type.toLowerCase();
            StringBuffer result = new StringBuffer();
            PropertyClass pclass = (PropertyClass) obj.getxWikiClass(context).get(fieldname);
            String prefix = pref + obj.getxWikiClass(context).getName() + "_" + obj.getNumber() + "_";

            if (pclass.isCustomDisplayed(context)) {
                pclass.displayCustom(result, fieldname, prefix, type, obj, context);
            } else if (type.equals("view")) {
                pclass.displayView(result, fieldname, prefix, obj, context);
            } else if (type.equals("rendered")) {
                String fcontent = pclass.displayView(fieldname, prefix, obj, context);
                // This mode is deprecated for the new rendering and should also be removed for the old rendering
                // since the way to implement this now is to choose the type of rendering to do in the class itself.
                // Thus for the new rendering we simply make this mode work like the "view" mode.
                if (is10Syntax(syntaxId)) {
                    result.append(getRenderedContent(fcontent, getSyntaxId(), context));
                } else {
                    result.append(fcontent);
                }
            } else if (type.equals("edit")) {
                context.addDisplayedField(fieldname);
                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem and prevent wiki syntax
                // rendering using the pre macro. In the new rendering system it's the XWiki Class itself that does the
                // escaping. For example for a textarea check the TextAreaClass class.
                // If the Syntax id is not "xwiki/1.0", i.e. if the new rendering engine is used then we need to
                // protect the content with a <pre> since otherwise whitespaces will be stripped by the HTML macro
                // used to surround the object property content (see below).
                if (is10Syntax(syntaxId)) {
                    result.append("{pre}");
                } else {
                    result.append("<pre>");
                }
                pclass.displayEdit(result, fieldname, prefix, obj, context);
                if (is10Syntax(syntaxId)) {
                    result.append("{/pre}");
                } else {
                    result.append("</pre>");
                }
            } else if (type.equals("hidden")) {
                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem and prevent wiki syntax
                // rendering using the pre macro. In the new rendering system it's the XWiki Class itself that does the
                // escaping. For example for a textarea check the TextAreaClass class.
                if (is10Syntax(syntaxId)) {
                    result.append("{pre}");
                }
                pclass.displayHidden(result, fieldname, prefix, obj, context);
                if (is10Syntax(syntaxId)) {
                    result.append("{/pre}");
                }
            } else if (type.equals("search")) {
                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem and prevent wiki syntax
                // rendering using the pre macro. In the new rendering system it's the XWiki Class itself that does the
                // escaping. For example for a textarea check the TextAreaClass class.
                if (is10Syntax(syntaxId)) {
                    result.append("{pre}");
                }
                prefix = obj.getxWikiClass(context).getName() + "_";
                pclass.displaySearch(result, fieldname, prefix, (XWikiCriteria) context.get("query"), context);
                if (is10Syntax(syntaxId)) {
                    result.append("{/pre}");
                }
            } else {
                pclass.displayView(result, fieldname, prefix, obj, context);
            }

            // If we're in new rendering engine we want to wrap the HTML returned by displayView() in
            // a {{html/}} macro so that the user doesn't have to do it.
            // We test if we're inside the rendering engine since it's also possible that this display() method is
            // called
            // directly from a template and in this case we only want HTML as a result and not wiki syntax.
            // TODO: find a more generic way to handle html macro because this works only for XWiki 1.0 and XWiki 2.0
            // Add the {{html}}{{/html}} only when result really contains html since it's not needed for pure text
            if (isInRenderingEngine && !is10Syntax(syntaxId) && (result.indexOf("<") != -1 || result.indexOf(">") != -1)) {
                result.insert(0, "{{html clean=\"false\" wiki=\"false\"}}");
                result.append("{{/html}}");
            }

            return result.toString();
        } catch (Exception ex) {
            // TODO: It would better to check if the field exists rather than catching an exception
            // raised by a NPE as this is currently the case here...
            LOG.warn("Failed to display field [" + fieldname + "] in [" + type + "] mode for Object ["
                + (obj == null ? "NULL" : obj.getName()) + "]");
            ex.printStackTrace();
            return "";
        } finally {
            restoreContext(backup, context);
        }
    }

    public String display(String fieldname, BaseObject obj, XWikiContext context)
    {
        String type = null;
        try {
            type = (String) context.get("display");
        } catch (Exception e) {
        }
        if (type == null) {
            type = "view";
        }
        return display(fieldname, type, obj, context);
    }

    public String display(String fieldname, XWikiContext context)
    {
        try {
            BaseObject object = getxWikiObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }
            return display(fieldname, object, context);
        } catch (Exception e) {
            LOG.error("Failed to display field " + fieldname + " of document " + getFullName(), e);
        }
        return "";
    }

    public String display(String fieldname, String mode, XWikiContext context)
    {
        return display(fieldname, mode, "", context);
    }

    public String display(String fieldname, String mode, String prefix, XWikiContext context)
    {
        try {
            BaseObject object = getxWikiObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }
            if (object == null) {
                return "";
            } else {
                return display(fieldname, mode, prefix, object, context.getDoc() != null ? context.getDoc()
                    .getSyntaxId() : getSyntaxId(), context);
            }
        } catch (Exception e) {
            return "";
        }
    }

    public String displayForm(String className, String header, String format, XWikiContext context)
    {
        return displayForm(className, header, format, true, context);
    }

    public String displayForm(String className, String header, String format, boolean linebreak, XWikiContext context)
    {
        Vector<BaseObject> objects = getObjects(className);
        if (format.endsWith("\\n")) {
            linebreak = true;
        }

        BaseObject firstobject = null;
        Iterator<BaseObject> foit = objects.iterator();
        while ((firstobject == null) && foit.hasNext()) {
            firstobject = foit.next();
        }

        if (firstobject == null) {
            return "";
        }

        BaseClass bclass = firstobject.getxWikiClass(context);
        if (bclass.getPropertyList().size() == 0) {
            return "";
        }

        StringBuffer result = new StringBuffer();
        VelocityContext vcontext = new VelocityContext();
        for (String propertyName : bclass.getPropertyList()) {
            PropertyClass pclass = (PropertyClass) bclass.getField(propertyName);
            vcontext.put(pclass.getName(), pclass.getPrettyName());
        }
        result.append(XWikiVelocityRenderer.evaluate(header, context.getDoc().getFullName(), vcontext, context));
        if (linebreak) {
            result.append("\n");
        }

        // display each line
        for (int i = 0; i < objects.size(); i++) {
            vcontext.put("id", new Integer(i + 1));
            BaseObject object = objects.get(i);
            if (object != null) {
                for (String name : bclass.getPropertyList()) {
                    vcontext.put(name, display(name, object, context));
                }
                result
                    .append(XWikiVelocityRenderer.evaluate(format, context.getDoc().getFullName(), vcontext, context));
                if (linebreak) {
                    result.append("\n");
                }
            }
        }
        return result.toString();
    }

    public String displayForm(String className, XWikiContext context)
    {
        Vector<BaseObject> objects = getObjects(className);
        if (objects == null) {
            return "";
        }

        BaseObject firstobject = null;
        Iterator<BaseObject> foit = objects.iterator();
        while ((firstobject == null) && foit.hasNext()) {
            firstobject = foit.next();
        }

        if (firstobject == null) {
            return "";
        }

        BaseClass bclass = firstobject.getxWikiClass(context);
        if (bclass.getPropertyList().size() == 0) {
            return "";
        }

        StringBuffer result = new StringBuffer();
        result.append("{table}\n");
        boolean first = true;
        for (String propertyName : bclass.getPropertyList()) {
            if (first == true) {
                first = false;
            } else {
                result.append("|");
            }
            PropertyClass pclass = (PropertyClass) bclass.getField(propertyName);
            result.append(pclass.getPrettyName());
        }
        result.append("\n");
        for (int i = 0; i < objects.size(); i++) {
            BaseObject object = objects.get(i);
            if (object != null) {
                first = true;
                for (String propertyName : bclass.getPropertyList()) {
                    if (first == true) {
                        first = false;
                    } else {
                        result.append("|");
                    }
                    String data = display(propertyName, object, context);
                    data = data.trim();
                    data = data.replaceAll("\n", " ");
                    if (data.length() == 0) {
                        result.append("&nbsp;");
                    } else {
                        result.append(data);
                    }
                }
                result.append("\n");
            }
        }
        result.append("{table}\n");
        return result.toString();
    }

    public boolean isFromCache()
    {
        return this.fromCache;
    }

    public void setFromCache(boolean fromCache)
    {
        this.fromCache = fromCache;
    }

    public void readDocMetaFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        String defaultLanguage = eform.getDefaultLanguage();
        if (defaultLanguage != null) {
            setDefaultLanguage(defaultLanguage);
        }

        String defaultTemplate = eform.getDefaultTemplate();
        if (defaultTemplate != null) {
            setDefaultTemplate(defaultTemplate);
        }

        String creator = eform.getCreator();
        if ((creator != null) && (!creator.equals(getCreator()))) {
            if ((getCreator().equals(context.getUser()))
                || (context.getWiki().getRightService().hasAdminRights(context))) {
                setCreator(creator);
            }
        }

        String parent = eform.getParent();
        if (parent != null) {
            setParent(parent);
        }

        // Read the comment from the form
        String comment = eform.getComment();
        if (comment != null) {
            setComment(comment);
        }

        // Read the minor edit checkbox from the form
        setMinorEdit(eform.isMinorEdit());

        String tags = eform.getTags();
        if (tags != null) {
            setTags(tags, context);
        }

        // Set the Syntax id if defined
        String syntaxId = eform.getSyntaxId();
        if (syntaxId != null) {
            setSyntaxId(syntaxId);
        }
    }

    /**
     * add tags to the document.
     */
    public void setTags(String tags, XWikiContext context) throws XWikiException
    {
        loadTags(context);

        StaticListClass tagProp =
            (StaticListClass) this.tags.getxWikiClass(context).getField(XWikiConstant.TAG_CLASS_PROP_TAGS);
        tagProp.fromString(tags);
        this.tags.safeput(XWikiConstant.TAG_CLASS_PROP_TAGS, tagProp.fromString(tags));
        setMetaDataDirty(true);
    }

    public String getTags(XWikiContext context)
    {
        ListProperty prop = (ListProperty) getTagProperty(context);
        if (prop != null) {
            return prop.getTextValue();
        }
        return null;
    }

    public List<String> getTagsList(XWikiContext context)
    {
        List<String> tagList = null;

        BaseProperty prop = getTagProperty(context);
        if (prop != null) {
            tagList = (List<String>) prop.getValue();
        }

        return tagList;
    }

    private BaseProperty getTagProperty(XWikiContext context)
    {
        loadTags(context);
        return ((BaseProperty) this.tags.safeget(XWikiConstant.TAG_CLASS_PROP_TAGS));
    }

    private void loadTags(XWikiContext context)
    {
        if (this.tags == null) {
            this.tags = getObject(XWikiConstant.TAG_CLASS, true, context);
        }
    }

    public List getTagsPossibleValues(XWikiContext context)
    {
        loadTags(context);
        String possibleValues =
            ((StaticListClass) this.tags.getxWikiClass(context).getField(XWikiConstant.TAG_CLASS_PROP_TAGS))
                .getValues();
        return ListClass.getListFromString(possibleValues);
        // ((BaseProperty) this.tags.safeget(XWikiConstant.TAG_CLASS_PROP_TAGS)).toString();
    }

    public void readTranslationMetaFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        String content = eform.getContent();
        if (content != null) {
            // Cleanup in case we use HTMLAREA
            // content = context.getUtil().substitute("s/<br class=\\\"htmlarea\\\"\\/>/\\r\\n/g",
            // content);
            content = context.getUtil().substitute("s/<br class=\"htmlarea\" \\/>/\r\n/g", content);
            setContent(content);
        }
        String title = eform.getTitle();
        if (title != null) {
            setTitle(title);
        }
    }

    public void readObjectsFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        for (String name : getxWikiObjects().keySet()) {
            Vector<BaseObject> oldObjects = getObjects(name);
            Vector<BaseObject> newObjects = new Vector<BaseObject>();
            newObjects.setSize(oldObjects.size());
            for (int i = 0; i < oldObjects.size(); i++) {
                BaseObject oldobject = oldObjects.get(i);
                if (oldobject != null) {
                    BaseClass baseclass = oldobject.getxWikiClass(context);
                    BaseObject newobject =
                        (BaseObject) baseclass.fromMap(eform.getObject(baseclass.getName() + "_" + i), oldobject);
                    newobject.setNumber(oldobject.getNumber());
                    newobject.setGuid(oldobject.getGuid());
                    newobject.setName(getFullName());
                    newObjects.set(newobject.getNumber(), newobject);
                }
            }
            getxWikiObjects().put(name, newObjects);
        }
        setContentDirty(true);
    }

    public void readFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        readDocMetaFromForm(eform, context);
        readTranslationMetaFromForm(eform, context);
        readObjectsFromForm(eform, context);
    }

    public void readFromTemplate(EditForm eform, XWikiContext context) throws XWikiException
    {
        String template = eform.getTemplate();
        readFromTemplate(template, context);
    }

    public void readFromTemplate(String template, XWikiContext context) throws XWikiException
    {
        if ((template != null) && (!template.equals(""))) {
            String content = getContent();
            if ((!content.equals("\n")) && (!content.equals("")) && !isNew()) {
                Object[] args = {getFullName()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
                    "Cannot add a template to document {0} because it already has content", null, args);
            } else {
                if (template.indexOf('.') == -1) {
                    template = getSpace() + "." + template;
                }
                XWiki xwiki = context.getWiki();
                XWikiDocument templatedoc = xwiki.getDocument(template, context);
                if (templatedoc.isNew()) {
                    Object[] args = {template, getFullName()};
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
                        "Template document {0} does not exist when adding to document {1}", null, args);
                } else {
                    setTemplate(template);
                    setContent(templatedoc.getContent());

                    // Set the new document syntax as the syntax of the template since the template content
                    // is copied into the new document
                    setSyntaxId(templatedoc.getSyntaxId());

                    if ((getParent() == null) || (getParent().equals(""))) {
                        String tparent = templatedoc.getParent();
                        if (tparent != null) {
                            setParent(tparent);
                        }
                    }

                    if (isNew()) {
                        // We might have received the object from the cache
                        // and the template objects might have been copied already
                        // we need to remove them
                        setxWikiObjects(new TreeMap<String, Vector<BaseObject>>());
                    }
                    // Merge the external objects
                    // Currently the choice is not to merge the base class and object because it is
                    // not
                    // the prefered way of using external classes and objects.
                    mergexWikiObjects(templatedoc);
                }
            }
        }
        setContentDirty(true);
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
        XWikiContext context)
    {
        // Do nothing for the moment..
        // A useful thing here would be to look at any instances of a Notification Object
        // with email addresses and send an email to warn that the document has been modified..

    }

    /**
     * Use the document passed as parameter as the new identity for the current document.
     * 
     * @param document the document containing the new identity
     * @throws XWikiException in case of error
     */
    private void clone(XWikiDocument document) throws XWikiException
    {
        setDatabase(document.getDatabase());
        setRCSVersion(document.getRCSVersion());
        setDocumentArchive(document.getDocumentArchive());
        setAuthor(document.getAuthor());
        setContentAuthor(document.getContentAuthor());
        setContent(document.getContent());
        setContentDirty(document.isContentDirty());
        setCreationDate(document.getCreationDate());
        setDate(document.getDate());
        setCustomClass(document.getCustomClass());
        setContentUpdateDate(document.getContentUpdateDate());
        setTitle(document.getTitle());
        setFormat(document.getFormat());
        setFromCache(document.isFromCache());
        setElements(document.getElements());
        setId(document.getId());
        setMeta(document.getMeta());
        setMetaDataDirty(document.isMetaDataDirty());
        setMostRecent(document.isMostRecent());
        setName(document.getName());
        setNew(document.isNew());
        setStore(document.getStore());
        setTemplate(document.getTemplate());
        setSpace(document.getSpace());
        setParent(document.getParent());
        setCreator(document.getCreator());
        setDefaultLanguage(document.getDefaultLanguage());
        setDefaultTemplate(document.getDefaultTemplate());
        setValidationScript(document.getValidationScript());
        setLanguage(document.getLanguage());
        setTranslation(document.getTranslation());
        setxWikiClass((BaseClass) document.getxWikiClass().clone());
        setxWikiClassXML(document.getxWikiClassXML());
        setComment(document.getComment());
        setMinorEdit(document.isMinorEdit());
        setSyntaxId(document.getSyntaxId());
        setHidden(document.isHidden());

        clonexWikiObjects(document);
        copyAttachments(document);
        this.elements = document.elements;

        this.originalDocument = document.originalDocument;
    }

    @Override
    public Object clone()
    {
        XWikiDocument doc = null;
        try {
            doc = getClass().newInstance();

            doc.setDatabase(getDatabase());
            // use version field instead of getRCSVersion because it returns "1.1" if version==null.
            doc.version = this.version;
            doc.setDocumentArchive(getDocumentArchive());
            doc.setAuthor(getAuthor());
            doc.setContentAuthor(getContentAuthor());
            doc.setContent(getContent());
            doc.setContentDirty(isContentDirty());
            doc.setCreationDate(getCreationDate());
            doc.setDate(getDate());
            doc.setCustomClass(getCustomClass());
            doc.setContentUpdateDate(getContentUpdateDate());
            doc.setTitle(getTitle());
            doc.setFormat(getFormat());
            doc.setFromCache(isFromCache());
            doc.setElements(getElements());
            doc.setId(getId());
            doc.setMeta(getMeta());
            doc.setMetaDataDirty(isMetaDataDirty());
            doc.setMostRecent(isMostRecent());
            doc.setName(getName());
            doc.setNew(isNew());
            doc.setStore(getStore());
            doc.setTemplate(getTemplate());
            doc.setSpace(getSpace());
            doc.setParent(getParent());
            doc.setCreator(getCreator());
            doc.setDefaultLanguage(getDefaultLanguage());
            doc.setDefaultTemplate(getDefaultTemplate());
            doc.setValidationScript(getValidationScript());
            doc.setLanguage(getLanguage());
            doc.setTranslation(getTranslation());
            doc.setxWikiClass((BaseClass) getxWikiClass().clone());
            doc.setxWikiClassXML(getxWikiClassXML());
            doc.setComment(getComment());
            doc.setMinorEdit(isMinorEdit());
            doc.setSyntaxId(getSyntaxId());
            doc.setHidden(isHidden());

            doc.clonexWikiObjects(this);
            doc.copyAttachments(this);
            doc.elements = this.elements;

            doc.originalDocument = this.originalDocument;
        } catch (Exception e) {
            // This should not happen
            LOG.error("Exception while doc.clone", e);
        }
        return doc;
    }

    public void copyAttachments(XWikiDocument xWikiSourceDocument)
    {
        getAttachmentList().clear();
        Iterator<XWikiAttachment> attit = xWikiSourceDocument.getAttachmentList().iterator();
        while (attit.hasNext()) {
            XWikiAttachment attachment = attit.next();
            XWikiAttachment newattachment = (XWikiAttachment) attachment.clone();
            newattachment.setDoc(this);
            if (newattachment.getAttachment_content() != null) {
                newattachment.getAttachment_content().setContentDirty(true);
            }
            getAttachmentList().add(newattachment);
        }
        setContentDirty(true);
    }

    public void loadAttachments(XWikiContext context) throws XWikiException
    {
        for (XWikiAttachment attachment : getAttachmentList()) {
            attachment.loadContent(context);
            attachment.loadArchive(context);
        }
    }

    @Override
    public boolean equals(Object object)
    {
        XWikiDocument doc = (XWikiDocument) object;
        if (!getName().equals(doc.getName())) {
            return false;
        }

        if (!getSpace().equals(doc.getSpace())) {
            return false;
        }

        if (!getAuthor().equals(doc.getAuthor())) {
            return false;
        }

        if (!getContentAuthor().equals(doc.getContentAuthor())) {
            return false;
        }

        if (!getParent().equals(doc.getParent())) {
            return false;
        }

        if (!getCreator().equals(doc.getCreator())) {
            return false;
        }

        if (!getDefaultLanguage().equals(doc.getDefaultLanguage())) {
            return false;
        }

        if (!getLanguage().equals(doc.getLanguage())) {
            return false;
        }

        if (getTranslation() != doc.getTranslation()) {
            return false;
        }

        if (getDate().getTime() != doc.getDate().getTime()) {
            return false;
        }

        if (getContentUpdateDate().getTime() != doc.getContentUpdateDate().getTime()) {
            return false;
        }

        if (getCreationDate().getTime() != doc.getCreationDate().getTime()) {
            return false;
        }

        if (!getFormat().equals(doc.getFormat())) {
            return false;
        }

        if (!getTitle().equals(doc.getTitle())) {
            return false;
        }

        if (!getContent().equals(doc.getContent())) {
            return false;
        }

        if (!getVersion().equals(doc.getVersion())) {
            return false;
        }

        if (!getTemplate().equals(doc.getTemplate())) {
            return false;
        }

        if (!getDefaultTemplate().equals(doc.getDefaultTemplate())) {
            return false;
        }

        if (!getValidationScript().equals(doc.getValidationScript())) {
            return false;
        }

        if (!getComment().equals(doc.getComment())) {
            return false;
        }

        if (isMinorEdit() != doc.isMinorEdit()) {
            return false;
        }

        if ((getSyntaxId() != null && !getSyntaxId().equals(doc.getSyntaxId()))
            || (getSyntaxId() == null && doc.getSyntaxId() != null)) {
            return false;
        }

        if (isHidden() != doc.isHidden()) {
            return false;
        }

        if (!getxWikiClass().equals(doc.getxWikiClass())) {
            return false;
        }

        Set<String> myObjectClassnames = getxWikiObjects().keySet();
        Set<String> otherObjectClassnames = doc.getxWikiObjects().keySet();
        if (!myObjectClassnames.equals(otherObjectClassnames)) {
            return false;
        }

        for (String name : myObjectClassnames) {
            Vector<BaseObject> myObjects = getObjects(name);
            Vector<BaseObject> otherObjects = doc.getObjects(name);
            if (myObjects.size() != otherObjects.size()) {
                return false;
            }
            for (int i = 0; i < myObjects.size(); i++) {
                if ((myObjects.get(i) == null) && (otherObjects.get(i) != null)) {
                    return false;
                }
                if (!myObjects.get(i).equals(otherObjects.get(i))) {
                    return false;
                }
            }
        }

        // We consider that 2 documents are still equal even when they have different original
        // documents (see getOriginalDocument() for more details as to what is an original
        // document).

        return true;
    }

    public String toXML(Document doc, XWikiContext context)
    {
        OutputFormat outputFormat = new OutputFormat("", true);
        if ((context == null) || (context.getWiki() == null)) {
            outputFormat.setEncoding("UTF-8");
        } else {
            outputFormat.setEncoding(context.getWiki().getEncoding());
        }
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, outputFormat);
        try {
            writer.write(doc);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String getXMLContent(XWikiContext context) throws XWikiException
    {
        XWikiDocument tdoc = getTranslatedDocument(context);
        Document doc = tdoc.toXMLDocument(true, true, false, false, context);
        return toXML(doc, context);
    }

    public String toXML(XWikiContext context) throws XWikiException
    {
        Document doc = toXMLDocument(context);
        return toXML(doc, context);
    }

    public String toFullXML(XWikiContext context) throws XWikiException
    {
        return toXML(true, false, true, true, context);
    }

    public void addToZip(ZipOutputStream zos, boolean withVersions, XWikiContext context) throws IOException
    {
        try {
            String zipname = getSpace() + "/" + getName();
            String language = getLanguage();
            if ((language != null) && (!language.equals(""))) {
                zipname += "." + language;
            }
            ZipEntry zipentry = new ZipEntry(zipname);
            zos.putNextEntry(zipentry);
            zos.write(toXML(true, false, true, withVersions, context).getBytes());
            zos.closeEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addToZip(ZipOutputStream zos, XWikiContext context) throws IOException
    {
        addToZip(zos, true, context);
    }

    public String toXML(boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
        boolean bWithVersions, XWikiContext context) throws XWikiException
    {
        Document doc = toXMLDocument(bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions, context);
        return toXML(doc, context);
    }

    public Document toXMLDocument(XWikiContext context) throws XWikiException
    {
        return toXMLDocument(true, false, false, false, context);
    }

    public Document toXMLDocument(boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
        boolean bWithVersions, XWikiContext context) throws XWikiException
    {
        Document doc = new DOMDocument();
        Element docel = new DOMElement("xwikidoc");
        doc.setRootElement(docel);

        Element el = new DOMElement("web");
        el.addText(getSpace());
        docel.add(el);

        el = new DOMElement("name");
        el.addText(getName());
        docel.add(el);

        el = new DOMElement("language");
        el.addText(getLanguage());
        docel.add(el);

        el = new DOMElement("defaultLanguage");
        el.addText(getDefaultLanguage());
        docel.add(el);

        el = new DOMElement("translation");
        el.addText("" + getTranslation());
        docel.add(el);

        el = new DOMElement("parent");
        el.addText(getParent());
        docel.add(el);

        el = new DOMElement("creator");
        el.addText(getCreator());
        docel.add(el);

        el = new DOMElement("author");
        el.addText(getAuthor());
        docel.add(el);

        el = new DOMElement("customClass");
        el.addText(getCustomClass());
        docel.add(el);

        el = new DOMElement("contentAuthor");
        el.addText(getContentAuthor());
        docel.add(el);

        long d = getCreationDate().getTime();
        el = new DOMElement("creationDate");
        el.addText("" + d);
        docel.add(el);

        d = getDate().getTime();
        el = new DOMElement("date");
        el.addText("" + d);
        docel.add(el);

        d = getContentUpdateDate().getTime();
        el = new DOMElement("contentUpdateDate");
        el.addText("" + d);
        docel.add(el);

        el = new DOMElement("version");
        el.addText(getVersion());
        docel.add(el);

        el = new DOMElement("title");
        el.addText(getTitle());
        docel.add(el);

        el = new DOMElement("template");
        el.addText(getTemplate());
        docel.add(el);

        el = new DOMElement("defaultTemplate");
        el.addText(getDefaultTemplate());
        docel.add(el);

        el = new DOMElement("validationScript");
        el.addText(getValidationScript());
        docel.add(el);

        el = new DOMElement("comment");
        el.addText(getComment());
        docel.add(el);

        el = new DOMElement("minorEdit");
        el.addText(String.valueOf(isMinorEdit()));
        docel.add(el);

        el = new DOMElement("syntaxId");
        el.addText(getSyntaxId());
        docel.add(el);

        el = new DOMElement("hidden");
        el.addText(String.valueOf(isHidden()));
        docel.add(el);

        for (XWikiAttachment attach : getAttachmentList()) {
            docel.add(attach.toXML(bWithAttachmentContent, bWithVersions, context));
        }

        if (bWithObjects) {
            // Add Class
            BaseClass bclass = getxWikiClass();
            if (bclass.getFieldList().size() > 0) {
                // If the class has fields, add class definition and field information to XML
                docel.add(bclass.toXML(null));
            }

            // Add Objects (THEIR ORDER IS MOLDED IN STONE!)
            for (Vector<BaseObject> objects : getxWikiObjects().values()) {
                for (BaseObject obj : objects) {
                    if (obj != null) {
                        BaseClass objclass = null;
                        if (StringUtils.equals(getFullName(), obj.getClassName())) {
                            objclass = bclass;
                        } else {
                            objclass = obj.getxWikiClass(context);
                        }
                        docel.add(obj.toXML(objclass));
                    }
                }
            }
        }

        // Add Content
        el = new DOMElement("content");

        // Filter filter = new CharacterFilter();
        // String newcontent = filter.process(getContent());
        // String newcontent = encodedXMLStringAsUTF8(getContent());
        String newcontent = this.content;
        el.addText(newcontent);
        docel.add(el);

        if (bWithRendering) {
            el = new DOMElement("renderedcontent");
            try {
                el.addText(getRenderedContent(context));
            } catch (XWikiException e) {
                el.addText("Exception with rendering content: " + e.getFullMessage());
            }
            docel.add(el);
        }

        if (bWithVersions) {
            el = new DOMElement("versions");
            try {
                el.addText(getDocumentArchive(context).getArchive(context));
                docel.add(el);
            } catch (XWikiException e) {
                LOG.error("Document [" + this.getFullName() + "] has malformed history");
            }
        }

        return doc;
    }

    protected String encodedXMLStringAsUTF8(String xmlString)
    {
        if (xmlString == null) {
            return "";
        }
        int length = xmlString.length();
        char character;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < length; i++) {
            character = xmlString.charAt(i);
            switch (character) {
                case '&':
                    result.append("&amp;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '\n':
                    result.append("\n");
                    break;
                case '\r':
                    result.append("\r");
                    break;
                case '\t':
                    result.append("\t");
                    break;
                default:
                    if (character < 0x20) {
                    } else if (character > 0x7F) {
                        result.append("&#x");
                        result.append(Integer.toHexString(character).toUpperCase());
                        result.append(";");
                    } else {
                        result.append(character);
                    }
                    break;
            }
        }
        return result.toString();
    }

    protected String getElement(Element docel, String name)
    {
        Element el = docel.element(name);
        if (el == null) {
            return "";
        } else {
            return el.getText();
        }
    }

    public void fromXML(String xml) throws XWikiException
    {
        fromXML(xml, false);
    }

    public void fromXML(InputStream is) throws XWikiException
    {
        fromXML(is, false);
    }

    public void fromXML(String xml, boolean withArchive) throws XWikiException
    {
        SAXReader reader = new SAXReader();
        Document domdoc;

        try {
            StringReader in = new StringReader(xml);
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }

        fromXML(domdoc, withArchive);
    }

    public void fromXML(InputStream in, boolean withArchive) throws XWikiException
    {
        SAXReader reader = new SAXReader();
        Document domdoc;

        try {
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }

        fromXML(domdoc, withArchive);
    }

    public void fromXML(Document domdoc, boolean withArchive) throws XWikiException
    {
        Element docel = domdoc.getRootElement();
        setName(getElement(docel, "name"));
        setSpace(getElement(docel, "web"));
        setParent(getElement(docel, "parent"));
        setCreator(getElement(docel, "creator"));
        setAuthor(getElement(docel, "author"));
        setCustomClass(getElement(docel, "customClass"));
        setContentAuthor(getElement(docel, "contentAuthor"));
        if (docel.element("version") != null) {
            setVersion(getElement(docel, "version"));
        }
        setContent(getElement(docel, "content"));
        setLanguage(getElement(docel, "language"));
        setDefaultLanguage(getElement(docel, "defaultLanguage"));
        setTitle(getElement(docel, "title"));
        setDefaultTemplate(getElement(docel, "defaultTemplate"));
        setValidationScript(getElement(docel, "validationScript"));
        setComment(getElement(docel, "comment"));

        String minorEdit = getElement(docel, "minorEdit");
        setMinorEdit(Boolean.valueOf(minorEdit).booleanValue());

        String hidden = getElement(docel, "hidden");
        setHidden(Boolean.valueOf(hidden).booleanValue());

        String strans = getElement(docel, "translation");
        if ((strans == null) || strans.equals("")) {
            setTranslation(0);
        } else {
            setTranslation(Integer.parseInt(strans));
        }

        String archive = getElement(docel, "versions");
        if (withArchive && archive != null && archive.length() > 0) {
            setDocumentArchive(archive);
        }

        String sdate = getElement(docel, "date");
        if (!sdate.equals("")) {
            Date date = new Date(Long.parseLong(sdate));
            setDate(date);
        }

        String contentUpdateDateString = getElement(docel, "contentUpdateDate");
        if (!StringUtils.isEmpty(contentUpdateDateString)) {
            Date contentUpdateDate = new Date(Long.parseLong(contentUpdateDateString));
            setContentUpdateDate(contentUpdateDate);
        }

        String scdate = getElement(docel, "creationDate");
        if (!scdate.equals("")) {
            Date cdate = new Date(Long.parseLong(scdate));
            setCreationDate(cdate);
        }

        String syntaxId = getElement(docel, "syntaxId");
        if ((syntaxId == null) || (syntaxId.length() == 0)) {
            // Documents that don't have syntax ids are considered old documents and thus in
            // XWiki Syntax 1.0 since newer documents always have syntax ids.
            setSyntaxId(XWIKI10_SYNTAXID);
        } else {
            setSyntaxId(syntaxId);
        }

        List atels = docel.elements("attachment");
        for (int i = 0; i < atels.size(); i++) {
            Element atel = (Element) atels.get(i);
            XWikiAttachment attach = new XWikiAttachment();
            attach.setDoc(this);
            attach.fromXML(atel);
            getAttachmentList().add(attach);
        }

        Element cel = docel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel != null) {
            bclass.fromXML(cel);
            setxWikiClass(bclass);
        }

        List objels = docel.elements("object");
        for (int i = 0; i < objels.size(); i++) {
            Element objel = (Element) objels.get(i);
            BaseObject bobject = new BaseObject();
            bobject.fromXML(objel);
            setObject(bobject.getClassName(), bobject.getNumber(), bobject);
        }

        // We have been reading from XML so the document does not need a new version when saved
        setMetaDataDirty(false);
        setContentDirty(false);

        // Note: We don't set the original document as that is not stored in the XML, and it doesn't make much sense to
        // have an original document for a de-serialized object.
    }

    /**
     * Check if provided xml document is a wiki document.
     * 
     * @param domdoc the xml document.
     * @return true if provided xml document is a wiki document.
     */
    public static boolean containsXMLWikiDocument(Document domdoc)
    {
        return domdoc.getRootElement().getName().equals("xwikidoc");
    }

    public void setAttachmentList(List<XWikiAttachment> list)
    {
        this.attachmentList = list;
    }

    public List<XWikiAttachment> getAttachmentList()
    {
        return this.attachmentList;
    }

    public void saveAllAttachments(XWikiContext context) throws XWikiException
    {
        for (int i = 0; i < this.attachmentList.size(); i++) {
            saveAttachmentContent(this.attachmentList.get(i), context);
        }
    }

    public void saveAllAttachments(boolean updateParent, boolean transaction, XWikiContext context)
        throws XWikiException
    {
        for (int i = 0; i < this.attachmentList.size(); i++) {
            saveAttachmentContent(this.attachmentList.get(i), updateParent, transaction, context);
        }
    }

    public void saveAttachmentsContent(List<XWikiAttachment> attachments, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }

            context.getWiki().getAttachmentStore().saveAttachmentsContent(attachments, this, true, context, true);
        } catch (java.lang.OutOfMemoryError e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE,
                "Out Of Memory Exception");
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        saveAttachmentContent(attachment, true, true, context);
    }

    public void saveAttachmentContent(XWikiAttachment attachment, boolean bParentUpdate, boolean bTransaction,
        XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }

            // We need to make sure there is a version upgrade
            setMetaDataDirty(true);

            context.getWiki().getAttachmentStore().saveAttachmentContent(attachment, bParentUpdate, context,
                bTransaction);
        } catch (java.lang.OutOfMemoryError e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE,
                "Out Of Memory Exception");
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }

            context.getWiki().getAttachmentStore().loadAttachmentContent(attachment, context, true);
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    public void deleteAttachment(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        deleteAttachment(attachment, true, context);
    }

    public void deleteAttachment(XWikiAttachment attachment, boolean toRecycleBin, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }
            try {
                // We need to make sure there is a version upgrade
                setMetaDataDirty(true);
                if (toRecycleBin && context.getWiki().hasAttachmentRecycleBin(context)) {
                    context.getWiki().getAttachmentRecycleBinStore().saveToRecycleBin(attachment, context.getUser(),
                        new Date(), context, true);
                }
                context.getWiki().getAttachmentStore().deleteXWikiAttachment(attachment, context, true);
            } catch (java.lang.OutOfMemoryError e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE, "Out Of Memory Exception");
            }
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    /**
     * Get the wiki pages pointing to this page.
     * <p>
     * Theses links are stored to the database when documents are saved. You can use "backlinks" in XWikiPreferences or
     * "xwiki.backlinks" in xwiki.cfg file to enable links storage in the database.
     * 
     * @param context the XWiki context.
     * @return the found wiki pages names.
     * @throws XWikiException error when getting pages names from database.
     */
    public List<String> getBackLinkedPages(XWikiContext context) throws XWikiException
    {
        return getStore(context).loadBacklinks(getFullName(), context, true);
    }

    /**
     * Get a list of unique links from this document to others documents.
     * <p>
     * <ul>
     * <li>1.0 content: get the unique links associated to document from database. This is stored when the document is
     * saved. You can use "backlinks" in XWikiPreferences or "xwiki.backlinks" in xwiki.cfg file to enable links storage
     * in the database.</li>
     * <li>Other content: call {@link #getUniqueLinkedPages(XWikiContext)} and generate the List</li>.
     * </ul>
     * 
     * @param context the XWiki context
     * @return the found wiki links.
     * @throws XWikiException error when getting links from database when 1.0 content.
     * @since 1.9M2
     */
    public Set<XWikiLink> getUniqueWikiLinkedPages(XWikiContext context) throws XWikiException
    {
        Set<XWikiLink> links;

        if (is10Syntax()) {
            links = new LinkedHashSet<XWikiLink>(getStore(context).loadLinks(getId(), context, true));
        } else {
            Set<String> linkedPages = getUniqueLinkedPages(context);
            links = new LinkedHashSet<XWikiLink>(linkedPages.size());
            for (String linkedPage : linkedPages) {
                XWikiLink wikiLink = new XWikiLink();

                wikiLink.setDocId(getId());
                wikiLink.setFullName(getFullName());
                wikiLink.setLink(linkedPage);

                links.add(wikiLink);
            }
        }

        return links;
    }

    /**
     * Extract all the unique static (i.e. not generated by macros) wiki links (pointing to wiki page) from this 1.0
     * document's content to others documents.
     * 
     * @param context the XWiki context.
     * @return the document names for linked pages, if null an error append.
     * @since 1.9M2
     */
    private Set<String> getUniqueLinkedPages10(XWikiContext context)
    {
        Set<String> pageNames;

        try {
            List<String> list = context.getUtil().getUniqueMatches(getContent(), "\\[(.*?)\\]", 1);
            pageNames = new HashSet<String>(list.size());

            DocumentName currentDocumentName = getDocumentName();
            for (String name : list) {
                int i1 = name.indexOf(">");
                if (i1 != -1) {
                    name = name.substring(i1 + 1);
                }
                i1 = name.indexOf("&gt;");
                if (i1 != -1) {
                    name = name.substring(i1 + 4);
                }
                i1 = name.indexOf("#");
                if (i1 != -1) {
                    name = name.substring(0, i1);
                }
                i1 = name.indexOf("?");
                if (i1 != -1) {
                    name = name.substring(0, i1);
                }

                // Let's get rid of anything that's not a real link
                if (name.trim().equals("") || (name.indexOf("$") != -1) || (name.indexOf("://") != -1)
                    || (name.indexOf("\"") != -1) || (name.indexOf("\'") != -1) || (name.indexOf("..") != -1)
                    || (name.indexOf(":") != -1) || (name.indexOf("=") != -1)) {
                    continue;
                }

                // generate the link
                String newname = StringUtils.replace(Util.noaccents(name), " ", "");

                // If it is a local link let's add the space
                if (newname.indexOf(".") == -1) {
                    newname = getSpace() + "." + name;
                }
                if (context.getWiki().exists(newname, context)) {
                    name = newname;
                } else {
                    // If it is a local link let's add the space
                    if (name.indexOf(".") == -1) {
                        name = getSpace() + "." + name;
                    }
                }

                // If the reference is empty, the link is an autolink
                if (!StringUtils.isEmpty(name)) {
                    // The reference may not have the space or even document specified (in case of an empty
                    // string)
                    // Thus we need to find the fully qualified document name
                    DocumentName documentName = this.documentNameFactory.createDocumentName(name);

                    // Verify that the link is not an autolink (i.e. a link to the current document)
                    if (!documentName.equals(currentDocumentName)) {
                        pageNames.add(this.compactDocumentNameSerializer.serialize(documentName));
                    }
                }
            }

            return pageNames;
        } catch (Exception e) {
            // This should never happen
            LOG.error("Failed to get linked documents", e);
            return null;
        }
    }

    /**
     * Extract all the unique static (i.e. not generated by macros) wiki links (pointing to wiki page) from this
     * document's content to others documents.
     * 
     * @param context the XWiki context.
     * @return the document names for linked pages, if null an error append.
     * @since 1.9M2
     */
    public Set<String> getUniqueLinkedPages(XWikiContext context)
    {
        Set<String> pageNames;

        XWikiDocument contextDoc = context.getDoc();
        String contextWiki = context.getDatabase();

        try {
            // Make sure the right document is used as context document
            context.setDoc(this);
            // Make sure the right wiki is used as context document
            context.setDatabase(getDatabase());

            if (is10Syntax()) {
                pageNames = getUniqueLinkedPages10(context);
            } else {
                XDOM dom = getXDOM();

                List<LinkBlock> linkBlocks = dom.getChildrenByType(LinkBlock.class, true);
                pageNames = new LinkedHashSet<String>(linkBlocks.size());

                DocumentName currentDocumentName = getDocumentName();

                for (LinkBlock linkBlock : linkBlocks) {
                    org.xwiki.rendering.listener.Link link = linkBlock.getLink();
                    if (link.getType() == LinkType.DOCUMENT) {
                        // If the reference is empty, the link is an autolink
                        if (!StringUtils.isEmpty(link.getReference())
                            || (StringUtils.isEmpty(link.getAnchor()) && StringUtils.isEmpty(link.getQueryString()))) {
                            // The reference may not have the space or even document specified (in case of an empty
                            // string)
                            // Thus we need to find the fully qualified document name
                            DocumentName documentName =
                                this.documentNameFactory.createDocumentName(link.getReference());

                            // Verify that the link is not an autolink (i.e. a link to the current document)
                            if (!documentName.equals(currentDocumentName)) {
                                pageNames.add(this.compactDocumentNameSerializer.serialize(documentName));
                            }
                        }
                    }
                }
            }
        } finally {
            context.setDoc(contextDoc);
            context.setDatabase(contextWiki);
        }

        return pageNames;
    }

    public List<String> getChildren(XWikiContext context) throws XWikiException
    {
        String query = "select distinct doc.fullName from XWikiDocument as doc";
        Object[][] whereParameters = new Object[][] {{"doc.parent", this.getFullName()}};
        return context.getWiki().search(query, whereParameters, context);
    }

    public void renameProperties(String className, Map fieldsToRename)
    {
        Vector<BaseObject> objects = getObjects(className);
        if (objects == null) {
            return;
        }
        for (BaseObject bobject : objects) {
            if (bobject == null) {
                continue;
            }
            for (Iterator renameit = fieldsToRename.keySet().iterator(); renameit.hasNext();) {
                String origname = (String) renameit.next();
                String newname = (String) fieldsToRename.get(origname);
                BaseProperty origprop = (BaseProperty) bobject.safeget(origname);
                if (origprop != null) {
                    BaseProperty prop = (BaseProperty) origprop.clone();
                    bobject.removeField(origname);
                    prop.setName(newname);
                    bobject.addField(newname, prop);
                }
            }
        }
        setContentDirty(true);
    }

    public void addObjectsToRemove(BaseObject object)
    {
        getObjectsToRemove().add(object);
        setContentDirty(true);
    }

    public ArrayList<BaseObject> getObjectsToRemove()
    {
        return this.objectsToRemove;
    }

    public void setObjectsToRemove(ArrayList<BaseObject> objectsToRemove)
    {
        this.objectsToRemove = objectsToRemove;
        setContentDirty(true);
    }

    public List<String> getIncludedPages(XWikiContext context)
    {
        if (is10Syntax()) {
            return getIncludedPagesForXWiki10Syntax(getContent(), context);
        } else {
            // Find all include macros listed on the page
            XDOM dom = getXDOM();

            List<String> result = new ArrayList<String>();
            for (MacroBlock macroBlock : dom.getChildrenByType(MacroBlock.class, true)) {

                // - Add each document pointed to by the include macro
                // - Also add all the included pages found in the velocity macro when using the deprecated #include*
                // macros
                // This should be removed when we fully drop support for the XWiki Syntax 1.0 but for now we want to
                // play
                // nice with people migrating from 1.0 to 2.0 syntax

                if (macroBlock.getName().equalsIgnoreCase("include")) {
                    String documentName = macroBlock.getParameters().get("document");
                    if (documentName.indexOf(".") == -1) {
                        documentName = getSpace() + "." + documentName;
                    }
                    result.add(documentName);
                } else if (macroBlock.getName().equalsIgnoreCase("velocity")
                    && !StringUtils.isEmpty(macroBlock.getContent())) {
                    // Try to find matching content inside each velocity macro
                    result.addAll(getIncludedPagesForXWiki10Syntax(macroBlock.getContent(), context));
                }
            }

            return result;
        }
    }

    private List<String> getIncludedPagesForXWiki10Syntax(String content, XWikiContext context)
    {
        try {
            String pattern = "#include(Topic|InContext|Form|Macros|parseGroovyFromPage)\\([\"'](.*?)[\"']\\)";
            List<String> list = context.getUtil().getUniqueMatches(content, pattern, 2);
            for (int i = 0; i < list.size(); i++) {
                try {
                    String name = list.get(i);
                    if (name.indexOf(".") == -1) {
                        list.set(i, getSpace() + "." + name);
                    }
                } catch (Exception e) {
                    // This should never happen
                    e.printStackTrace();
                    return null;
                }
            }

            return list;
        } catch (Exception e) {
            // This should never happen
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getIncludedMacros(XWikiContext context)
    {
        return context.getWiki().getIncludedMacros(getSpace(), getContent(), context);
    }

    public String displayRendered(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
        throws XWikiException
    {
        String result = pclass.displayView(pclass.getName(), prefix, object, context);
        return getRenderedContent(result, context);
    }

    public String displayView(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displayView(pclass.getName(), prefix, object, context);
    }

    public String displayEdit(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displayEdit(pclass.getName(), prefix, object, context);
    }

    public String displayHidden(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displayHidden(pclass.getName(), prefix, object, context);
    }

    public String displaySearch(PropertyClass pclass, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displaySearch(pclass.getName(), prefix, criteria, context);
    }

    public XWikiAttachment getAttachment(String filename)
    {
        for (XWikiAttachment attach : getAttachmentList()) {
            if (attach.getFilename().equals(filename)) {
                return attach;
            }
        }
        for (XWikiAttachment attach : getAttachmentList()) {
            if (attach.getFilename().startsWith(filename + ".")) {
                return attach;
            }
        }
        return null;
    }

    public XWikiAttachment addAttachment(String fileName, InputStream iStream, XWikiContext context)
        throws XWikiException, IOException
    {
        ByteArrayOutputStream bAOut = new ByteArrayOutputStream();
        IOUtils.copy(iStream, bAOut);
        return addAttachment(fileName, bAOut.toByteArray(), context);
    }

    public XWikiAttachment addAttachment(String fileName, byte[] data, XWikiContext context) throws XWikiException
    {
        int i = fileName.indexOf("\\");
        if (i == -1) {
            i = fileName.indexOf("/");
        }
        String filename = fileName.substring(i + 1);

        // TODO : avoid name clearing when encoding problems will be solved
        // JIRA : http://jira.xwiki.org/jira/browse/XWIKI-94
        filename = context.getWiki().clearName(filename, false, true, context);

        XWikiAttachment attachment = getAttachment(filename);
        if (attachment == null) {
            attachment = new XWikiAttachment();
            // TODO: Review this code and understand why it's needed.
            // Add the attachment in the current doc
            getAttachmentList().add(attachment);
        }

        attachment.setContent(data);
        attachment.setFilename(filename);
        attachment.setAuthor(context.getUser());
        // Add the attachment to the document
        attachment.setDoc(this);
        return attachment;
    }

    public BaseObject getFirstObject(String fieldname)
    {
        // Keeping this function with context null for compatibility reasons.
        // It should not be used, since it would miss properties which are only defined in the class
        // and not present in the object because the object was not updated
        return getFirstObject(fieldname, null);
    }

    public BaseObject getFirstObject(String fieldname, XWikiContext context)
    {
        Collection<Vector<BaseObject>> objectscoll = getxWikiObjects().values();
        if (objectscoll == null) {
            return null;
        }

        for (Vector<BaseObject> objects : objectscoll) {
            for (BaseObject obj : objects) {
                if (obj != null) {
                    BaseClass bclass = obj.getxWikiClass(context);
                    if (bclass != null) {
                        Set<String> set = bclass.getPropertyList();
                        if ((set != null) && set.contains(fieldname)) {
                            return obj;
                        }
                    }
                    Set<String> set = obj.getPropertyList();
                    if ((set != null) && set.contains(fieldname)) {
                        return obj;
                    }
                }
            }
        }
        return null;
    }

    public void setProperty(String className, String fieldName, BaseProperty value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.safeput(fieldName, value);
        setContentDirty(true);
    }

    public int getIntValue(String className, String fieldName)
    {
        BaseObject obj = getObject(className, 0);
        if (obj == null) {
            return 0;
        }
        return obj.getIntValue(fieldName);
    }

    public long getLongValue(String className, String fieldName)
    {
        BaseObject obj = getObject(className, 0);
        if (obj == null) {
            return 0;
        }
        return obj.getLongValue(fieldName);
    }

    public String getStringValue(String className, String fieldName)
    {
        BaseObject obj = getObject(className);
        if (obj == null) {
            return "";
        }
        String result = obj.getStringValue(fieldName);
        if (result.equals(" ")) {
            return "";
        } else {
            return result;
        }
    }

    public int getIntValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return 0;
        } else {
            return object.getIntValue(fieldName);
        }
    }

    public long getLongValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return 0;
        } else {
            return object.getLongValue(fieldName);
        }
    }

    public String getStringValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return "";
        }

        String result = object.getStringValue(fieldName);
        if (result.equals(" ")) {
            return "";
        } else {
            return result;
        }
    }

    public void setStringValue(String className, String fieldName, String value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setStringValue(fieldName, value);
        setContentDirty(true);
    }

    public List getListValue(String className, String fieldName)
    {
        BaseObject obj = getObject(className);
        if (obj == null) {
            return new ArrayList();
        }
        return obj.getListValue(fieldName);
    }

    public List getListValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return new ArrayList();
        }

        return object.getListValue(fieldName);
    }

    public void setStringListValue(String className, String fieldName, List value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setStringListValue(fieldName, value);
        setContentDirty(true);
    }

    public void setDBStringListValue(String className, String fieldName, List value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setDBStringListValue(fieldName, value);
        setContentDirty(true);
    }

    public void setLargeStringValue(String className, String fieldName, String value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setLargeStringValue(fieldName, value);
        setContentDirty(true);
    }

    public void setIntValue(String className, String fieldName, int value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setIntValue(fieldName, value);
        setContentDirty(true);
    }

    public String getDatabase()
    {
        return this.database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    public void setFullName(String fullname, XWikiContext context)
    {
        DocumentName documentName = this.documentNameFactory.createDocumentName(fullname);
        setDatabase(documentName.getWiki());
        setSpace(documentName.getSpace());
        setName(documentName.getPage());
        setContentDirty(true);
    }

    public String getLanguage()
    {
        if (this.language == null) {
            return "";
        } else {
            return this.language.trim();
        }
    }

    public void setLanguage(String language)
    {
        this.language = language;
    }

    public String getDefaultLanguage()
    {
        if (this.defaultLanguage == null) {
            return "";
        } else {
            return this.defaultLanguage.trim();
        }
    }

    public void setDefaultLanguage(String defaultLanguage)
    {
        this.defaultLanguage = defaultLanguage;
        setMetaDataDirty(true);
    }

    public int getTranslation()
    {
        return this.translation;
    }

    public void setTranslation(int translation)
    {
        this.translation = translation;
        setMetaDataDirty(true);
    }

    public String getTranslatedContent(XWikiContext context) throws XWikiException
    {
        String language = context.getWiki().getLanguagePreference(context);
        return getTranslatedContent(language, context);
    }

    public String getTranslatedContent(String language, XWikiContext context) throws XWikiException
    {
        XWikiDocument tdoc = getTranslatedDocument(language, context);
        String rev = (String) context.get("rev");
        if ((rev == null) || (rev.length() == 0)) {
            return tdoc.getContent();
        }

        XWikiDocument cdoc = context.getWiki().getDocument(tdoc, rev, context);
        return cdoc.getContent();
    }

    public XWikiDocument getTranslatedDocument(XWikiContext context) throws XWikiException
    {
        String language = context.getWiki().getLanguagePreference(context);
        return getTranslatedDocument(language, context);
    }

    public XWikiDocument getTranslatedDocument(String language, XWikiContext context) throws XWikiException
    {
        XWikiDocument tdoc = this;

        if (!((language == null) || (language.equals("")) || language.equals(this.defaultLanguage))) {
            tdoc = new XWikiDocument(getSpace(), getName());
            tdoc.setLanguage(language);
            String database = context.getDatabase();
            try {
                // We might need to switch database to
                // get the translated content
                if (getDatabase() != null) {
                    context.setDatabase(getDatabase());
                }

                tdoc = getStore(context).loadXWikiDoc(tdoc, context);

                if (tdoc.isNew()) {
                    tdoc = this;
                }
            } catch (Exception e) {
                tdoc = this;
            } finally {
                context.setDatabase(database);
            }
        }
        return tdoc;
    }

    public String getRealLanguage(XWikiContext context) throws XWikiException
    {
        return getRealLanguage();
    }

    public String getRealLanguage()
    {
        String lang = getLanguage();
        if ((lang.equals("") || lang.equals("default"))) {
            return getDefaultLanguage();
        } else {
            return lang;
        }
    }

    public List<String> getTranslationList(XWikiContext context) throws XWikiException
    {
        return getStore().getTranslationList(this, context);
    }

    public List<Delta> getXMLDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        return getDeltas(Diff.diff(ToString.stringToArray(fromDoc.toXML(context)), ToString.stringToArray(toDoc
            .toXML(context))));
    }

    public List<Delta> getContentDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        return getDeltas(Diff.diff(ToString.stringToArray(fromDoc.getContent()), ToString.stringToArray(toDoc
            .getContent())));
    }

    public List<Delta> getContentDiff(String fromRev, String toRev, XWikiContext context) throws XWikiException,
        DifferentiationFailedException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);
        return getContentDiff(fromDoc, toDoc, context);
    }

    public List<Delta> getContentDiff(String fromRev, XWikiContext context) throws XWikiException,
        DifferentiationFailedException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);
        return getContentDiff(revdoc, this, context);
    }

    public List<Delta> getLastChanges(XWikiContext context) throws XWikiException, DifferentiationFailedException
    {
        Version version = getRCSVersion();
        try {
            String prev = getDocumentArchive(context).getPrevVersion(version).toString();
            XWikiDocument prevDoc = context.getWiki().getDocument(this, prev, context);
            return getDeltas(Diff.diff(ToString.stringToArray(prevDoc.getContent()), ToString
                .stringToArray(getContent())));
        } catch (Exception ex) {
            LOG.debug("Exception getting differences from previous version: " + ex.getMessage());
        }
        return new ArrayList<Delta>();
    }

    public List<Delta> getRenderedContentDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        String originalContent, newContent;

        originalContent = context.getWiki().getRenderingEngine().renderText(fromDoc.getContent(), fromDoc, context);
        newContent = context.getWiki().getRenderingEngine().renderText(toDoc.getContent(), toDoc, context);

        return getDeltas(Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(newContent)));
    }

    public List<Delta> getRenderedContentDiff(String fromRev, String toRev, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);
        return getRenderedContentDiff(fromDoc, toDoc, context);
    }

    public List<Delta> getRenderedContentDiff(String fromRev, XWikiContext context) throws XWikiException,
        DifferentiationFailedException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);
        return getRenderedContentDiff(revdoc, this, context);
    }

    protected List<Delta> getDeltas(Revision rev)
    {
        List<Delta> list = new ArrayList<Delta>();
        for (int i = 0; i < rev.size(); i++) {
            list.add(rev.getDelta(i));
        }
        return list;
    }

    public List<MetaDataDiff> getMetaDataDiff(String fromRev, String toRev, XWikiContext context) throws XWikiException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);
        return getMetaDataDiff(fromDoc, toDoc, context);
    }

    public List<MetaDataDiff> getMetaDataDiff(String fromRev, XWikiContext context) throws XWikiException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);
        return getMetaDataDiff(revdoc, this, context);
    }

    public List<MetaDataDiff> getMetaDataDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        List<MetaDataDiff> list = new ArrayList<MetaDataDiff>();

        if ((fromDoc == null) || (toDoc == null)) {
            return list;
        }

        if (!fromDoc.getParent().equals(toDoc.getParent())) {
            list.add(new MetaDataDiff("parent", fromDoc.getParent(), toDoc.getParent()));
        }
        if (!fromDoc.getAuthor().equals(toDoc.getAuthor())) {
            list.add(new MetaDataDiff("author", fromDoc.getAuthor(), toDoc.getAuthor()));
        }
        if (!fromDoc.getSpace().equals(toDoc.getSpace())) {
            list.add(new MetaDataDiff("web", fromDoc.getSpace(), toDoc.getSpace()));
        }
        if (!fromDoc.getName().equals(toDoc.getName())) {
            list.add(new MetaDataDiff("name", fromDoc.getName(), toDoc.getName()));
        }
        if (!fromDoc.getLanguage().equals(toDoc.getLanguage())) {
            list.add(new MetaDataDiff("language", fromDoc.getLanguage(), toDoc.getLanguage()));
        }
        if (!fromDoc.getDefaultLanguage().equals(toDoc.getDefaultLanguage())) {
            list.add(new MetaDataDiff("defaultLanguage", fromDoc.getDefaultLanguage(), toDoc.getDefaultLanguage()));
        }
        return list;
    }

    public List<List<ObjectDiff>> getObjectDiff(String fromRev, String toRev, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);
        return getObjectDiff(fromDoc, toDoc, context);
    }

    public List<List<ObjectDiff>> getObjectDiff(String fromRev, XWikiContext context) throws XWikiException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);
        return getObjectDiff(revdoc, this, context);
    }

    /**
     * Return the object differences between two document versions. There is no hard requirement on the order of the two
     * versions, but the results are semantically correct only if the two versions are given in the right order.
     * 
     * @param fromDoc The old ('before') version of the document.
     * @param toDoc The new ('after') version of the document.
     * @param context The {@link com.xpn.xwiki.XWikiContext context}.
     * @return The object differences. The returned list's elements are other lists, one for each changed object. The
     *         inner lists contain {@link ObjectDiff} elements, one object for each changed property of the object.
     *         Additionally, if the object was added or removed, then the first entry in the list will be an
     *         "object-added" or "object-removed" marker.
     * @throws XWikiException If there's an error computing the differences.
     */
    public List<List<ObjectDiff>> getObjectDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        ArrayList<List<ObjectDiff>> difflist = new ArrayList<List<ObjectDiff>>();
        // Since objects could have been deleted or added, we iterate on both the old and the new
        // object collections.
        // First, iterate over the old objects.
        for (Vector<BaseObject> objects : fromDoc.getxWikiObjects().values()) {
            for (BaseObject originalObj : objects) {
                // This happens when objects are deleted, and the document is still in the cache
                // storage.
                if (originalObj != null) {
                    BaseObject newObj = toDoc.getObject(originalObj.getClassName(), originalObj.getNumber());
                    List<ObjectDiff> dlist;
                    if (newObj == null) {
                        // The object was deleted.
                        dlist = new BaseObject().getDiff(originalObj, context);
                        ObjectDiff deleteMarker =
                            new ObjectDiff(originalObj.getClassName(), originalObj.getNumber(), originalObj.getGuid(),
                                "object-removed", "", "", "");
                        dlist.add(0, deleteMarker);
                    } else {
                        // The object exists in both versions, but might have been changed.
                        dlist = newObj.getDiff(originalObj, context);
                    }
                    if (dlist.size() > 0) {
                        difflist.add(dlist);
                    }
                }
            }
        }
        // Second, iterate over the objects which are only in the new version.
        for (Vector<BaseObject> objects : toDoc.getxWikiObjects().values()) {
            for (BaseObject newObj : objects) {
                // This happens when objects are deleted, and the document is still in the cache
                // storage.
                if (newObj != null) {
                    BaseObject originalObj = fromDoc.getObject(newObj.getClassName(), newObj.getNumber());
                    if (originalObj == null) {
                        // Only consider added objects, the other case was treated above.
                        originalObj = new BaseObject();
                        originalObj.setClassName(newObj.getClassName());
                        originalObj.setNumber(newObj.getNumber());
                        originalObj.setGuid(newObj.getGuid());
                        List<ObjectDiff> dlist = newObj.getDiff(originalObj, context);
                        ObjectDiff addMarker =
                            new ObjectDiff(newObj.getClassName(), newObj.getNumber(), newObj.getGuid(), "object-added",
                                "", "", "");
                        dlist.add(0, addMarker);
                        if (dlist.size() > 0) {
                            difflist.add(dlist);
                        }
                    }
                }
            }
        }
        return difflist;
    }

    public List<List<ObjectDiff>> getClassDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        ArrayList<List<ObjectDiff>> difflist = new ArrayList<List<ObjectDiff>>();
        BaseClass oldClass = fromDoc.getxWikiClass();
        BaseClass newClass = toDoc.getxWikiClass();

        if ((newClass == null) && (oldClass == null)) {
            return difflist;
        }

        List<ObjectDiff> dlist = newClass.getDiff(oldClass, context);
        if (dlist.size() > 0) {
            difflist.add(dlist);
        }
        return difflist;
    }

    /**
     * @param fromDoc
     * @param toDoc
     * @param context
     * @return
     * @throws XWikiException
     */
    public List<AttachmentDiff> getAttachmentDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        List<AttachmentDiff> difflist = new ArrayList<AttachmentDiff>();
        for (XWikiAttachment origAttach : fromDoc.getAttachmentList()) {
            String fileName = origAttach.getFilename();
            XWikiAttachment newAttach = toDoc.getAttachment(fileName);
            if (newAttach == null) {
                difflist.add(new AttachmentDiff(fileName, origAttach.getVersion(), null));
            } else {
                if (!origAttach.getVersion().equals(newAttach.getVersion())) {
                    difflist.add(new AttachmentDiff(fileName, origAttach.getVersion(), newAttach.getVersion()));
                }
            }
        }
        for (XWikiAttachment newAttach : toDoc.getAttachmentList()) {
            String fileName = newAttach.getFilename();
            XWikiAttachment origAttach = fromDoc.getAttachment(fileName);
            if (origAttach == null) {
                difflist.add(new AttachmentDiff(fileName, null, newAttach.getVersion()));
            }
        }
        return difflist;
    }

    /**
     * Rename the current document and all the backlinks leading to it. See
     * {@link #rename(String, java.util.List, com.xpn.xwiki.XWikiContext)} for more details.
     * 
     * @param newDocumentName the new document name. If the space is not specified then defaults to the current space.
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     */
    public void rename(String newDocumentName, XWikiContext context) throws XWikiException
    {
        rename(newDocumentName, getBackLinkedPages(context), context);
    }

    /**
     * Rename the current document and all the links pointing to it in the list of passed backlink documents. The
     * renaming algorithm takes into account the fact that there are several ways to write a link to a given page and
     * all those forms need to be renamed. For example the following links all point to the same page:
     * <ul>
     * <li>[Page]</li>
     * <li>[Page?param=1]</li>
     * <li>[currentwiki:Page]</li>
     * <li>[CurrentSpace.Page]</li>
     * </ul>
     * <p>
     * Note: links without a space are renamed with the space added.
     * </p>
     * 
     * @param newDocumentName the new document name. If the space is not specified then defaults to the current space.
     * @param backlinkDocumentNames the list of documents to parse and for which links will be modified to point to the
     *            new renamed document.
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     */
    public void rename(String newDocumentName, List<String> backlinkDocumentNames, XWikiContext context)
        throws XWikiException
    {
        // TODO: Do all this in a single DB transaction as otherwise the state will be unknown if
        // something fails in the middle...

        if (isNew()) {
            return;
        }

        // This link handler recognizes that 2 links are the same when they point to the same
        // document (regardless of query string, target or alias). It keeps the query string,
        // target and alias from the link being replaced.
        RenamePageReplaceLinkHandler linkHandler = new RenamePageReplaceLinkHandler();

        // Transform string representation of old and new links so that they can be manipulated.
        Link oldLink = new LinkParser().parse(getFullName());
        Link newLink = new LinkParser().parse(newDocumentName);

        // Verify if the user is trying to rename to the same name... In that case, simply exits
        // for efficiency.
        if (linkHandler.compare(newLink, oldLink)) {
            return;
        }

        // Step 1: Copy the document and all its translations under a new name
        context.getWiki().copyDocument(getFullName(), newDocumentName, false, context);

        // Step 2: For each backlink to rename, parse the backlink document and replace the links
        // with the new name.
        // Note: we ignore invalid links here. Invalid links should be shown to the user so
        // that they fix them but the rename feature ignores them.
        DocumentParser documentParser = new DocumentParser();

        for (String backlinkDocumentName : backlinkDocumentNames) {
            XWikiDocument backlinkDocument = context.getWiki().getDocument(backlinkDocumentName, context);

            // Note: Here we cannot do a simple search/replace as there are several ways to point
            // to the same document. For example [Page], [Page?param=1], [currentwiki:Page],
            // [CurrentSpace.Page] all point to the same document. Thus we have to parse the links
            // to recognize them and do the replace.
            ReplacementResultCollection result =
                documentParser.parseLinksAndReplace(backlinkDocument.getContent(), oldLink, newLink, linkHandler,
                    getSpace());

            backlinkDocument.setContent((String) result.getModifiedContent());
            context.getWiki().saveDocument(backlinkDocument,
                context.getMessageTool().get("core.comment.renameLink", Arrays.asList(new String[] {newDocumentName})),
                true, context);
        }

        // Step 3: Delete the old document
        context.getWiki().deleteDocument(this, context);

        // Step 4: The current document needs to point to the renamed document as otherwise it's
        // pointing to an invalid XWikiDocument object as it's been deleted...
        clone(context.getWiki().getDocument(newDocumentName, context));
    }

    public XWikiDocument copyDocument(String newDocumentName, XWikiContext context) throws XWikiException
    {
        String oldname = getFullName();

        loadAttachments(context);
        loadArchive(context);

        /*
         * if (oldname.equals(docname)) return this;
         */

        XWikiDocument newdoc = (XWikiDocument) clone();
        newdoc.setFullName(newDocumentName, context);
        newdoc.setContentDirty(true);
        newdoc.getxWikiClass().setName(newDocumentName);
        Vector<BaseObject> objects = newdoc.getObjects(oldname);
        if (objects != null) {
            for (BaseObject object : objects) {
                object.setName(newDocumentName);
                // Since GUIDs are supposed to be Unique, although this object holds the same data, it is not exactly
                // the same object, so it should have a different identifier.
                object.setGuid(UUID.randomUUID().toString());
            }
        }
        XWikiDocumentArchive archive = newdoc.getDocumentArchive();
        if (archive != null) {
            newdoc.setDocumentArchive(archive.clone(newdoc.getId(), context));
        }
        return newdoc;
    }

    public XWikiLock getLock(XWikiContext context) throws XWikiException
    {
        XWikiLock theLock = getStore(context).loadLock(getId(), context, true);
        if (theLock != null) {
            int timeout = context.getWiki().getXWikiPreferenceAsInt("lock_Timeout", 30 * 60, context);
            if (theLock.getDate().getTime() + timeout * 1000 < new Date().getTime()) {
                getStore(context).deleteLock(theLock, context, true);
                theLock = null;
            }
        }
        return theLock;
    }

    public void setLock(String userName, XWikiContext context) throws XWikiException
    {
        XWikiLock lock = new XWikiLock(getId(), userName);
        getStore(context).saveLock(lock, context, true);
    }

    public void removeLock(XWikiContext context) throws XWikiException
    {
        XWikiLock lock = getStore(context).loadLock(getId(), context, true);
        if (lock != null) {
            getStore(context).deleteLock(lock, context, true);
        }
    }

    public void insertText(String text, String marker, XWikiContext context) throws XWikiException
    {
        setContent(StringUtils.replaceOnce(getContent(), marker, text + marker));
        context.getWiki().saveDocument(this, context);
    }

    public Object getWikiNode()
    {
        return this.wikiNode;
    }

    public void setWikiNode(Object wikiNode)
    {
        this.wikiNode = wikiNode;
    }

    public String getxWikiClassXML()
    {
        return this.xWikiClassXML;
    }

    public void setxWikiClassXML(String xWikiClassXML)
    {
        this.xWikiClassXML = xWikiClassXML;
    }

    public int getElements()
    {
        return this.elements;
    }

    public void setElements(int elements)
    {
        this.elements = elements;
    }

    public void setElement(int element, boolean toggle)
    {
        if (toggle) {
            this.elements = this.elements | element;
        } else {
            this.elements = this.elements & (~element);
        }
    }

    public boolean hasElement(int element)
    {
        return ((this.elements & element) == element);
    }

    /**
     * @return "inline" if the document should be edited in inline mode by default or "edit" otherwise.
     * @throws XWikiException if an error happens when computing the edit mode
     */
    public String getDefaultEditMode(XWikiContext context) throws XWikiException
    {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        if (is10Syntax()) {
            if (getContent().indexOf("includeForm(") != -1) {
                return "inline";
            }
        } else {
            // Algorithm: look in all include macro and for all document included check if one of them
            // has an SheetClass object attached to it. If so then the edit mode is inline.

            // Find all include macros and extract the document names
            for (MacroBlock macroBlock : getXDOM().getChildrenByType(MacroBlock.class, false)) {
                // TODO: Is there a good way not to hardcode the macro name? The macro itself shouldn't know
                // its own name since it's a deployment time concern.
                if ("include".equals(macroBlock.getName())) {
                    String documentName = macroBlock.getParameter("document");
                    if (documentName != null) {
                        XWikiDocument includedDocument = xwiki.getDocument(documentName, context);
                        if (!includedDocument.isNew()) {
                            BaseObject sheetClassObject = includedDocument.getObject(XWikiConstant.SHEET_CLASS);
                            if (sheetClassObject != null) {
                                // Use the user-defined default edit mode if set.
                                String defaultEditMode = sheetClassObject.getStringValue("defaultEditMode");
                                if (StringUtils.isBlank(defaultEditMode)) {
                                    return "inline";
                                } else {
                                    return defaultEditMode;
                                }
                            }
                        }
                    }
                }
            }
        }

        return "edit";
    }

    public String getDefaultEditURL(XWikiContext context) throws XWikiException
    {
        String editMode = getDefaultEditMode(context);

        if ("inline".equals(editMode)) {
            return getEditURL("inline", "", context);
        } else {
            com.xpn.xwiki.XWiki xwiki = context.getWiki();
            String editor = xwiki.getEditorPreference(context);
            return getEditURL("edit", editor, context);
        }
    }

    public String getEditURL(String action, String mode, XWikiContext context) throws XWikiException
    {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        String language = "";
        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        String realLang = tdoc.getRealLanguage(context);
        if ((xwiki.isMultiLingual(context) == true) && (!realLang.equals(""))) {
            language = realLang;
        }
        return getEditURL(action, mode, language, context);
    }

    public String getEditURL(String action, String mode, String language, XWikiContext context)
    {
        StringBuffer editparams = new StringBuffer();
        if (!mode.equals("")) {
            editparams.append("xpage=");
            editparams.append(mode);
        }

        if (!language.equals("")) {
            if (!mode.equals("")) {
                editparams.append("&");
            }
            editparams.append("language=");
            editparams.append(language);
        }
        return getURL(action, editparams.toString(), context);
    }

    public String getDefaultTemplate()
    {
        if (this.defaultTemplate == null) {
            return "";
        } else {
            return this.defaultTemplate;
        }
    }

    public void setDefaultTemplate(String defaultTemplate)
    {
        this.defaultTemplate = defaultTemplate;
        setMetaDataDirty(true);
    }

    public Vector<BaseObject> getComments()
    {
        return getComments(true);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentModelBridge#getSyntaxId()
     */
    public String getSyntaxId()
    {
        // Can't be initialized in the XWikiDocument constructor because #getDefaultDocumentSyntax() need to create a
        // XWikiDocument object to get preferences and generate an infinite loop
        if (isNew() && this.syntaxId == null) {
            this.syntaxId = getDefaultDocumentSyntax();
        }

        return this.syntaxId;
    }

    /**
     * @param syntaxId the new syntax id to set (eg "xwiki/1.0", "xwiki/2.0", etc)
     * @see #getSyntaxId()
     */
    public void setSyntaxId(String syntaxId)
    {
        this.syntaxId = syntaxId;
    }

    public Vector<BaseObject> getComments(boolean asc)
    {
        if (asc) {
            return getObjects("XWiki.XWikiComments");
        } else {
            Vector<BaseObject> list = getObjects("XWiki.XWikiComments");
            if (list == null) {
                return list;
            }
            Vector<BaseObject> newlist = new Vector<BaseObject>();
            for (int i = list.size() - 1; i >= 0; i--) {
                newlist.add(list.get(i));
            }
            return newlist;
        }
    }

    public boolean isCurrentUserCreator(XWikiContext context)
    {
        return isCreator(context.getUser());
    }

    public boolean isCreator(String username)
    {
        if (username.equals("XWiki.XWikiGuest")) {
            return false;
        }
        return username.equals(getCreator());
    }

    public boolean isCurrentUserPage(XWikiContext context)
    {
        String username = context.getUser();
        if (username.equals("XWiki.XWikiGuest")) {
            return false;
        }
        return context.getUser().equals(getFullName());
    }

    public boolean isCurrentLocalUserPage(XWikiContext context)
    {
        String username = context.getLocalUser();
        if (username.equals("XWiki.XWikiGuest")) {
            return false;
        }
        return context.getUser().equals(getFullName());
    }

    public void resetArchive(XWikiContext context) throws XWikiException
    {
        boolean hasVersioning = context.getWiki().hasVersioning(getFullName(), context);
        if (hasVersioning) {
            getVersioningStore(context).resetRCSArchive(this, true, context);
        }
    }

    // This functions adds an object from an new object creation form
    public BaseObject addObjectFromRequest(XWikiContext context) throws XWikiException
    {
        // Read info in object
        ObjectAddForm form = new ObjectAddForm();
        form.setRequest((HttpServletRequest) context.getRequest());
        form.readRequest();

        String className = form.getClassName();
        BaseObject object = newObject(className, context);
        BaseClass baseclass = object.getxWikiClass(context);
        baseclass.fromMap(form.getObject(className), object);
        return object;
    }

    // This functions adds an object from an new object creation form
    public BaseObject addObjectFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return addObjectFromRequest(className, "", 0, context);
    }

    // This functions adds an object from an new object creation form
    public BaseObject addObjectFromRequest(String className, String prefix, XWikiContext context) throws XWikiException
    {
        return addObjectFromRequest(className, prefix, 0, context);
    }

    // This functions adds multiple objects from an new objects creation form
    public List<BaseObject> addObjectsFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return addObjectsFromRequest(className, "", context);
    }

    // This functions adds multiple objects from an new objects creation form
    public List<BaseObject> addObjectsFromRequest(String className, String pref, XWikiContext context)
        throws XWikiException
    {
        Map map = context.getRequest().getParameterMap();
        List<Integer> objectsNumberDone = new ArrayList<Integer>();
        List<BaseObject> objects = new ArrayList<BaseObject>();
        Iterator it = map.keySet().iterator();
        String start = pref + className + "_";

        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.startsWith(start)) {
                int pos = name.indexOf("_", start.length() + 1);
                String prefix = name.substring(0, pos);
                int num = Integer.decode(prefix.substring(prefix.lastIndexOf("_") + 1)).intValue();
                if (!objectsNumberDone.contains(new Integer(num))) {
                    objectsNumberDone.add(new Integer(num));
                    objects.add(addObjectFromRequest(className, pref, num, context));
                }
            }
        }
        return objects;
    }

    // This functions adds object from an new object creation form
    public BaseObject addObjectFromRequest(String className, int num, XWikiContext context) throws XWikiException
    {
        return addObjectFromRequest(className, "", num, context);
    }

    // This functions adds object from an new object creation form
    public BaseObject addObjectFromRequest(String className, String prefix, int num, XWikiContext context)
        throws XWikiException
    {
        BaseObject object = newObject(className, context);
        BaseClass baseclass = object.getxWikiClass(context);
        baseclass.fromMap(Util.getObject(context.getRequest(), prefix + className + "_" + num), object);
        return object;
    }

    // This functions adds an object from an new object creation form
    public BaseObject updateObjectFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return updateObjectFromRequest(className, "", context);
    }

    // This functions adds an object from an new object creation form
    public BaseObject updateObjectFromRequest(String className, String prefix, XWikiContext context)
        throws XWikiException
    {
        return updateObjectFromRequest(className, prefix, 0, context);
    }

    // This functions adds an object from an new object creation form
    public BaseObject updateObjectFromRequest(String className, String prefix, int num, XWikiContext context)
        throws XWikiException
    {
        int nb;
        BaseObject oldobject = getObject(className, num);
        if (oldobject == null) {
            nb = createNewObject(className, context);
            oldobject = getObject(className, nb);
        } else {
            nb = oldobject.getNumber();
        }
        BaseClass baseclass = oldobject.getxWikiClass(context);
        BaseObject newobject =
            (BaseObject) baseclass.fromMap(Util.getObject(context.getRequest(), prefix + className + "_" + nb),
                oldobject);
        newobject.setNumber(oldobject.getNumber());
        newobject.setGuid(oldobject.getGuid());
        newobject.setName(getFullName());
        setObject(className, nb, newobject);
        return newobject;
    }

    // This functions adds an object from an new object creation form
    public List<BaseObject> updateObjectsFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return updateObjectsFromRequest(className, "", context);
    }

    // This functions adds multiple objects from an new objects creation form
    public List<BaseObject> updateObjectsFromRequest(String className, String pref, XWikiContext context)
        throws XWikiException
    {
        Map map = context.getRequest().getParameterMap();
        List<Integer> objectsNumberDone = new ArrayList<Integer>();
        List<BaseObject> objects = new ArrayList<BaseObject>();
        Iterator it = map.keySet().iterator();
        String start = pref + className + "_";

        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.startsWith(start)) {
                int pos = name.indexOf("_", start.length() + 1);
                String prefix = name.substring(0, pos);
                int num = Integer.decode(prefix.substring(prefix.lastIndexOf("_") + 1)).intValue();
                if (!objectsNumberDone.contains(new Integer(num))) {
                    objectsNumberDone.add(new Integer(num));
                    objects.add(updateObjectFromRequest(className, pref, num, context));
                }
            }
        }
        return objects;
    }

    public boolean isAdvancedContent()
    {
        String[] matches =
            {"<%", "#set", "#include", "#if", "public class", "/* Advanced content */", "## Advanced content",
            "/* Programmatic content */", "## Programmatic content"};
        String content2 = this.content.toLowerCase();
        for (int i = 0; i < matches.length; i++) {
            if (content2.indexOf(matches[i].toLowerCase()) != -1) {
                return true;
            }
        }

        if (HTML_TAG_PATTERN.matcher(content2).find()) {
            return true;
        }

        return false;
    }

    public boolean isProgrammaticContent()
    {
        String[] matches =
            {"<%", "\\$xwiki.xWiki", "$context.context", "$doc.document", "$xwiki.getXWiki()", "$context.getContext()",
            "$doc.getDocument()", "WithProgrammingRights(", "/* Programmatic content */", "## Programmatic content",
            "$xwiki.search(", "$xwiki.createUser", "$xwiki.createNewWiki", "$xwiki.addToAllGroup",
            "$xwiki.sendMessage", "$xwiki.copyDocument", "$xwiki.copyWikiWeb", "$xwiki.parseGroovyFromString",
            "$doc.toXML()", "$doc.toXMLDocument()",};
        String content2 = this.content.toLowerCase();
        for (int i = 0; i < matches.length; i++) {
            if (content2.indexOf(matches[i].toLowerCase()) != -1) {
                return true;
            }
        }

        return false;
    }

    public boolean removeObject(BaseObject bobj)
    {
        Vector<BaseObject> objects = getObjects(bobj.getClassName());
        if (objects == null) {
            return false;
        }
        if (objects.elementAt(bobj.getNumber()) == null) {
            return false;
        }
        objects.set(bobj.getNumber(), null);
        addObjectsToRemove(bobj);
        return true;
    }

    /**
     * Remove all the objects of a given type (XClass) from the document.
     * 
     * @param className The class name of the objects to be removed.
     */
    public boolean removeObjects(String className)
    {
        Vector<BaseObject> objects = getObjects(className);
        if (objects == null) {
            return false;
        }
        Iterator<BaseObject> it = objects.iterator();
        while (it.hasNext()) {
            BaseObject bobj = it.next();
            if (bobj != null) {
                objects.set(bobj.getNumber(), null);
                addObjectsToRemove(bobj);
            }
        }
        return true;
    }

    /**
     * @return the sections in the current document
     * @throws XWikiException
     */
    public List<DocumentSection> getSections() throws XWikiException
    {
        if (is10Syntax()) {
            return getSections10();
        } else {
            List<DocumentSection> splitSections = new ArrayList<DocumentSection>();
            List<HeaderBlock> headers = getXDOM().getChildrenByType(HeaderBlock.class, true);

            int sectionNumber = 1;
            for (HeaderBlock header : headers) {
                // put -1 as index since there is no way to get the position of the header in the source
                int documentSectionIndex = -1;

                // Need to do the same thing than 1.0 content here
                String documentSectionLevel = StringUtils.repeat("1.", header.getLevel().getAsInt() - 1) + "1";

                DocumentSection docSection =
                    new DocumentSection(sectionNumber++, documentSectionIndex, documentSectionLevel, renderXDOM(
                        new XDOM(header.getChildren()), getSyntaxId()));
                splitSections.add(docSection);
            }

            return splitSections;
        }
    }

    /**
     * @return the sections in the current document
     */
    public List<DocumentSection> getSections10()
    {
        // Pattern to match the title. Matches only level 1 and level 2 headings.
        Pattern headingPattern = Pattern.compile("^[ \\t]*+(1(\\.1){0,1}+)[ \\t]++(.++)$", Pattern.MULTILINE);
        Matcher matcher = headingPattern.matcher(getContent());
        List<DocumentSection> splitSections = new ArrayList<DocumentSection>();
        int sectionNumber = 0;
        // find title to split
        while (matcher.find()) {
            ++sectionNumber;
            String sectionLevel = matcher.group(1);
            String sectionTitle = matcher.group(3);
            int sectionIndex = matcher.start();
            // Initialize a documentSection object.
            DocumentSection docSection = new DocumentSection(sectionNumber, sectionIndex, sectionLevel, sectionTitle);
            // Add the document section to list.
            splitSections.add(docSection);
        }
        return splitSections;
    }

    /**
     * Return a Document section with parameter is sectionNumber.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @return
     * @throws XWikiException error when extracting sections from document
     */
    public DocumentSection getDocumentSection(int sectionNumber) throws XWikiException
    {
        // return a document section according to section number
        return getSections().get(sectionNumber - 1);
    }

    /**
     * Return the content of a section.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @return the content of a section or null if the section can't be found.
     * @throws XWikiException error when trying to extract section content
     */
    public String getContentOfSection(int sectionNumber) throws XWikiException
    {
        String content = null;

        if (is10Syntax()) {
            content = getContentOfSection10(sectionNumber);
        } else {
            List<HeaderBlock> headers = getXDOM().getChildrenByType(HeaderBlock.class, true);

            if (headers.size() >= sectionNumber) {
                SectionBlock section = headers.get(sectionNumber - 1).getSection();
                content = renderXDOM(new XDOM(Collections.<Block> singletonList(section)), getSyntaxId());
            }
        }

        return content;
    }

    /**
     * Return the content of a section.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @return the content of a section
     * @throws XWikiException error when trying to extract section content
     */
    public String getContentOfSection10(int sectionNumber) throws XWikiException
    {
        List<DocumentSection> splitSections = getSections();
        int indexEnd = 0;
        // get current section
        DocumentSection section = splitSections.get(sectionNumber - 1);
        int indexStart = section.getSectionIndex();
        String sectionLevel = section.getSectionLevel();
        // Determine where this section ends, which is at the start of the next section of the
        // same or a higher level.
        for (int i = sectionNumber; i < splitSections.size(); i++) {
            DocumentSection nextSection = splitSections.get(i);
            String nextLevel = nextSection.getSectionLevel();
            if (sectionLevel.equals(nextLevel) || sectionLevel.length() > nextLevel.length()) {
                indexEnd = nextSection.getSectionIndex();
                break;
            }
        }
        String sectionContent = null;
        if (indexStart < 0) {
            indexStart = 0;
        }
        if (indexEnd == 0) {
            sectionContent = getContent().substring(indexStart);
        } else {
            sectionContent = getContent().substring(indexStart, indexEnd);
        }

        return sectionContent;
    }

    /**
     * Update a section content in document.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @param newSectionContent the new section content.
     * @return the new document content.
     * @throws XWikiException error when updating content
     */
    public String updateDocumentSection(int sectionNumber, String newSectionContent) throws XWikiException
    {
        String content;
        if (is10Syntax()) {
            content = updateDocumentSection10(sectionNumber, newSectionContent);
        } else {
            XDOM xdom = getXDOM();

            // Get the current section block
            HeaderBlock header = xdom.getChildrenByType(HeaderBlock.class, true).get(sectionNumber - 1);

            // newSectionContent -> Blocks
            List<Block> blocks = parseContent(newSectionContent).getChildren();
            int sectionLevel = header.getLevel().getAsInt();
            for (int level = 1; level < sectionLevel && blocks.size() == 1 && blocks.get(0) instanceof SectionBlock; ++level) {
                blocks = blocks.get(0).getChildren();
            }

            // replace old current SectionBlock with new Blocks
            header.getSection().replace(blocks);

            // render back XDOM to document's content syntax
            content = renderXDOM(xdom, getSyntaxId());
        }

        return content;
    }

    /**
     * Update a section content in document.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @param newSectionContent the new section content.
     * @return the new document content.
     * @throws XWikiException error when updating document content with section content
     */
    public String updateDocumentSection10(int sectionNumber, String newSectionContent) throws XWikiException
    {
        StringBuffer newContent = new StringBuffer();
        // get document section that will be edited
        DocumentSection docSection = getDocumentSection(sectionNumber);
        int numberOfSections = getSections().size();
        int indexSection = docSection.getSectionIndex();
        if (numberOfSections == 1) {
            // there is only a sections in document
            String contentBegin = getContent().substring(0, indexSection);
            newContent = newContent.append(contentBegin).append(newSectionContent);
            return newContent.toString();
        } else if (sectionNumber == numberOfSections) {
            // edit lastest section that doesn't contain subtitle
            String contentBegin = getContent().substring(0, indexSection);
            newContent = newContent.append(contentBegin).append(newSectionContent);
            return newContent.toString();
        } else {
            String sectionLevel = docSection.getSectionLevel();
            int nextSectionIndex = 0;
            // get index of next section
            for (int i = sectionNumber; i < numberOfSections; i++) {
                DocumentSection nextSection = getDocumentSection(i + 1); // get next section
                String nextSectionLevel = nextSection.getSectionLevel();
                if (sectionLevel.equals(nextSectionLevel)) {
                    nextSectionIndex = nextSection.getSectionIndex();
                    break;
                } else if (sectionLevel.length() > nextSectionLevel.length()) {
                    nextSectionIndex = nextSection.getSectionIndex();
                    break;
                }
            }
            if (nextSectionIndex == 0) {// edit the last section
                newContent = newContent.append(getContent().substring(0, indexSection)).append(newSectionContent);
                return newContent.toString();
            } else {
                String contentAfter = getContent().substring(nextSectionIndex);
                String contentBegin = getContent().substring(0, indexSection);
                newContent = newContent.append(contentBegin).append(newSectionContent).append(contentAfter);
            }
            return newContent.toString();
        }
    }

    /**
     * Computes a document hash, taking into account all document data: content, objects, attachments, metadata... TODO:
     * cache the hash value, update only on modification.
     */
    public String getVersionHashCode(XWikiContext context)
    {
        MessageDigest md5 = null;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            LOG.error("Cannot create MD5 object", ex);
            return this.hashCode() + "";
        }

        try {
            String valueBeforeMD5 = toXML(true, false, true, false, context);
            md5.update(valueBeforeMD5.getBytes());

            byte[] array = md5.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(b));
            }
            return sb.toString();
        } catch (Exception ex) {
            LOG.error("Exception while computing document hash", ex);
        }
        return this.hashCode() + "";
    }

    public static String getInternalPropertyName(String propname, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();
        String cpropname = StringUtils.capitalize(propname);
        return (msg == null) ? cpropname : msg.get(cpropname);
    }

    public String getInternalProperty(String propname)
    {
        String methodName = "get" + StringUtils.capitalize(propname);
        try {
            Method method = getClass().getDeclaredMethod(methodName, (Class[]) null);
            return (String) method.invoke(this, (Object[]) null);
        } catch (Exception e) {
            return null;
        }
    }

    public String getCustomClass()
    {
        if (this.customClass == null) {
            return "";
        }
        return this.customClass;
    }

    public void setCustomClass(String customClass)
    {
        this.customClass = customClass;
        setMetaDataDirty(true);
    }

    public void setValidationScript(String validationScript)
    {
        this.validationScript = validationScript;
        setMetaDataDirty(true);
    }

    public String getValidationScript()
    {
        if (this.validationScript == null) {
            return "";
        } else {
            return this.validationScript;
        }
    }

    public String getComment()
    {
        if (this.comment == null) {
            return "";
        }
        return this.comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public boolean isMinorEdit()
    {
        return this.isMinorEdit;
    }

    public void setMinorEdit(boolean isMinor)
    {
        this.isMinorEdit = isMinor;
    }

    // methods for easy table update. It is need only for hibernate.
    // when hibernate update old database without minorEdit field, hibernate will create field with
    // null in despite of notnull in hbm.
    // (http://opensource.atlassian.com/projects/hibernate/browse/HB-1151)
    // so minorEdit will be null for old documents. But hibernate can't convert null to boolean.
    // so we need convert Boolean to boolean
    protected Boolean getMinorEdit1()
    {
        return Boolean.valueOf(this.isMinorEdit);
    }

    protected void setMinorEdit1(Boolean isMinor)
    {
        this.isMinorEdit = (isMinor != null && isMinor.booleanValue());
    }

    public BaseObject newObject(String classname, XWikiContext context) throws XWikiException
    {
        int nb = createNewObject(classname, context);
        return getObject(classname, nb);
    }

    public BaseObject getObject(String classname, boolean create, XWikiContext context)
    {
        try {
            BaseObject obj = getObject(classname);

            if ((obj == null) && create) {
                return newObject(classname, context);
            }

            if (obj == null) {
                return null;
            } else {
                return obj;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validate(XWikiContext context) throws XWikiException
    {
        return validate(null, context);
    }

    public boolean validate(String[] classNames, XWikiContext context) throws XWikiException
    {
        boolean isValid = true;
        if ((classNames == null) || (classNames.length == 0)) {
            for (String classname : getxWikiObjects().keySet()) {
                BaseClass bclass = context.getWiki().getClass(classname, context);
                Vector<BaseObject> objects = getObjects(classname);
                for (BaseObject obj : objects) {
                    if (obj != null) {
                        isValid &= bclass.validateObject(obj, context);
                    }
                }
            }
        } else {
            for (int i = 0; i < classNames.length; i++) {
                Vector<BaseObject> objects = getObjects(classNames[i]);
                if (objects != null) {
                    for (BaseObject obj : objects) {
                        if (obj != null) {
                            BaseClass bclass = obj.getxWikiClass(context);
                            isValid &= bclass.validateObject(obj, context);
                        }
                    }
                }
            }
        }

        String validationScript = "";
        XWikiRequest req = context.getRequest();
        if (req != null) {
            validationScript = req.get("xvalidation");
        }
        if ((validationScript == null) || (validationScript.trim().equals(""))) {
            validationScript = getValidationScript();
        }
        if ((validationScript != null) && (!validationScript.trim().equals(""))) {
            isValid &= executeValidationScript(context, validationScript);
        }
        return isValid;
    }

    private boolean executeValidationScript(XWikiContext context, String validationScript) throws XWikiException
    {
        try {
            XWikiValidationInterface validObject =
                (XWikiValidationInterface) context.getWiki().parseGroovyFromPage(validationScript, context);
            return validObject.validateDocument(this, context);
        } catch (Throwable e) {
            XWikiValidationStatus.addExceptionToContext(getFullName(), "", e, context);
            return false;
        }
    }

    public static void backupContext(Map<String, Object> backup, XWikiContext context)
    {
        backup.put("doc", context.getDoc());
        VelocityManager velocityManager = (VelocityManager) Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = velocityManager.getVelocityContext();
        if (vcontext != null) {
            backup.put("vdoc", vcontext.get("doc"));
            backup.put("vcdoc", vcontext.get("cdoc"));
            backup.put("vtdoc", vcontext.get("tdoc"));
        }
        Map gcontext = (Map) context.get("gcontext");
        if (gcontext != null) {
            backup.put("gdoc", gcontext.get("doc"));
            backup.put("gcdoc", gcontext.get("cdoc"));
            backup.put("gtdoc", gcontext.get("tdoc"));
        }
    }

    public static void restoreContext(Map<String, Object> backup, XWikiContext context)
    {
        if (backup.get("doc") != null) {
            context.setDoc((XWikiDocument) backup.get("doc"));
        }
        VelocityManager velocityManager = (VelocityManager) Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = velocityManager.getVelocityContext();
        Map gcontext = (Map) context.get("gcontext");
        if (vcontext != null) {
            if (backup.get("vdoc") != null) {
                vcontext.put("doc", backup.get("vdoc"));
            }
            if (backup.get("vcdoc") != null) {
                vcontext.put("cdoc", backup.get("vcdoc"));
            }
            if (backup.get("vtdoc") != null) {
                vcontext.put("tdoc", backup.get("vtdoc"));
            }
        }
        if (gcontext != null) {
            if (backup.get("gdoc") != null) {
                gcontext.put("doc", backup.get("gdoc"));
            }
            if (backup.get("gcdoc") != null) {
                gcontext.put("cdoc", backup.get("gcdoc"));
            }
            if (backup.get("gtdoc") != null) {
                gcontext.put("tdoc", backup.get("gtdoc"));
            }
        }
    }

    public void setAsContextDoc(XWikiContext context)
    {
        try {
            context.setDoc(this);
            com.xpn.xwiki.api.Document apidoc = this.newDocument(context);
            com.xpn.xwiki.api.Document tdoc = apidoc.getTranslatedDocument();
            VelocityManager velocityManager = (VelocityManager) Utils.getComponent(VelocityManager.class);
            VelocityContext vcontext = velocityManager.getVelocityContext();
            Map gcontext = (Map) context.get("gcontext");
            if (vcontext != null) {
                vcontext.put("doc", apidoc);
                vcontext.put("tdoc", tdoc);
            }
            if (gcontext != null) {
                gcontext.put("doc", apidoc);
                gcontext.put("tdoc", tdoc);
            }
        } catch (XWikiException ex) {
            LOG.warn("Unhandled exception setting context", ex);
        }
    }

    public String getPreviousVersion()
    {
        return getDocumentArchive().getPrevVersion(this.version).toString();
    }

    @Override
    public String toString()
    {
        return getFullName();
    }

    /**
     * Indicates whether the document should be 'hidden' or not, meaning that it should not be returned in public search
     * results. WARNING: this is a temporary hack until the new data model is designed and implemented. No code should
     * rely on or use this property, since it will be replaced with a generic metadata.
     * 
     * @param hidden The new value of the {@link #hidden} property.
     */
    public void setHidden(Boolean hidden)
    {
        if (hidden == null) {
            this.hidden = false;
        } else {
            this.hidden = hidden;
        }
    }

    /**
     * Indicates whether the document is 'hidden' or not, meaning that it should not be returned in public search
     * results. WARNING: this is a temporary hack until the new data model is designed and implemented. No code should
     * rely on or use this property, since it will be replaced with a generic metadata.
     * 
     * @return <code>true</code> if the document is hidden and does not appear among the results of
     *         {@link com.xpn.xwiki.api.XWiki#searchDocuments(String)}, <code>false</code> otherwise.
     */
    public Boolean isHidden()
    {
        return this.hidden;
    }

    /**
     * Convert the current document content from its current syntax to the new syntax passed as parameter.
     * 
     * @param targetSyntaxId the syntax to convert to (eg "xwiki/2.0", "xhtml/1.0", etc)
     * @throws XWikiException if an exception occurred during the conversion process
     */
    public void convertSyntax(String targetSyntaxId, XWikiContext context) throws XWikiException
    {
        // convert content
        setContent(performSyntaxConversion(getContent(), getSyntaxId(), targetSyntaxId, false));

        // convert objects
        Map<String, Vector<BaseObject>> objectsByClass = getxWikiObjects();

        for (Vector<BaseObject> objects : objectsByClass.values()) {
            for (BaseObject bobject : objects) {
                if (bobject != null) {
                    BaseClass bclass = bobject.getxWikiClass(context);
                    for (Object fieldClass : bclass.getProperties()) {
                        if (fieldClass instanceof TextAreaClass && ((TextAreaClass) fieldClass).isWikiContent()) {
                            TextAreaClass textAreaClass = (TextAreaClass) fieldClass;
                            LargeStringProperty field = (LargeStringProperty) bobject.getField(textAreaClass.getName());

                            if (field != null) {
                                field.setValue(performSyntaxConversion(field.getValue(), getSyntaxId(), targetSyntaxId,
                                    false));
                            }
                        }
                    }
                }
            }
        }

        // change syntax id
        setSyntaxId(targetSyntaxId);
    }

    /**
     * @return the XDOM conrrexponding to the document's string content.
     */
    public XDOM getXDOM()
    {
        if (this.xdom == null) {
            try {
                this.xdom = parseContent(getContent());
            } catch (XWikiException e) {
                LOG.error("Failed to parse document content to XDOM", e);
            }
        }

        return this.xdom.clone();
    }

    /**
     * Convert the passed content from the passed syntax to the passed new syntax.
     * 
     * @param content the content to convert
     * @param currentSyntaxId the syntax of the current content to convert
     * @param targetSyntaxId the new syntax after the conversion
     * @param transform indicate if transformations has to be applied or not
     * @return the converted content in the new syntax
     * @throws XWikiException if an exception occurred during the conversion process
     */
    private String performSyntaxConversion(String content, String currentSyntaxId, String targetSyntaxId,
        boolean transform) throws XWikiException
    {
        try {
            XDOM dom = parseContent(currentSyntaxId, content);

            return performSyntaxConversion(dom, currentSyntaxId, targetSyntaxId, transform);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to convert document to syntax [" + targetSyntaxId + "]", e);
        }
    }

    /**
     * Convert the passed content from the passed syntax to the passed new syntax.
     * 
     * @param content the XDOM content to convert, the XDOM can be modified during the transformation
     * @param currentSyntaxId the syntax of the current content to convert
     * @param targetSyntaxId the new syntax after the conversion
     * @param transform indicate if transformations has to be applied or not
     * @return the converted content in the new syntax
     * @throws XWikiException if an exception occurred during the conversion process
     */
    private String performSyntaxConversion(XDOM content, String currentSyntaxId, String targetSyntaxId,
        boolean transform) throws XWikiException
    {
        try {
            if (transform) {
                // Transform XDOM
                TransformationManager transformations =
                    (TransformationManager) Utils.getComponent(TransformationManager.class);
                SyntaxFactory syntaxFactory = (SyntaxFactory) Utils.getComponent(SyntaxFactory.class);
                transformations
                    .performTransformations(content, syntaxFactory.createSyntaxFromIdString(currentSyntaxId));
            }

            // Render XDOM
            return renderXDOM(content, targetSyntaxId);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to convert document to syntax [" + targetSyntaxId + "]", e);
        }
    }

    /**
     * Render privided XDOM into content of the provided syntax identifier.
     * 
     * @param content the XDOM content to render
     * @param targetSyntaxId the syntax identifier of the rendered content
     * @return the rendered content
     * @throws XWikiException if an exception occurred during the rendering process
     */
    private String renderXDOM(XDOM content, String targetSyntaxId) throws XWikiException
    {
        try {
            SyntaxFactory syntaxFactory = (SyntaxFactory) Utils.getComponent(SyntaxFactory.class);
            PrintRendererFactory factory = (PrintRendererFactory) Utils.getComponent(PrintRendererFactory.class);

            WikiPrinter printer = new DefaultWikiPrinter();

            content.traverse(factory.createRenderer(syntaxFactory.createSyntaxFromIdString(targetSyntaxId), printer));

            return printer.toString();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to render document to syntax [" + targetSyntaxId + "]", e);
        }

    }

    private XDOM parseContent(String content) throws XWikiException
    {
        return parseContent(getSyntaxId(), content);
    }

    private XDOM parseContent(String syntaxId, String content) throws XWikiException
    {
        try {
            Parser parser = (Parser) Utils.getComponent(Parser.class, syntaxId);
            return parser.parse(new StringReader(content));
        } catch (ParseException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to parse content of syntax [" + syntaxId + "]", e);
        }
    }

    private String getDefaultDocumentSyntax()
    {
        return ((CoreConfiguration) Utils.getComponent(CoreConfiguration.class)).getDefaultDocumentSyntax();
    }

    /**
     * @return true if the document has a xwiki/1.0 syntax content
     */
    public boolean is10Syntax()
    {
        return is10Syntax(getSyntaxId());
    }

    /**
     * @return true if the document has a xwiki/1.0 syntax content
     */
    public boolean is10Syntax(String syntaxId)
    {
        return XWIKI10_SYNTAXID.equalsIgnoreCase(syntaxId);
    }
}
