package org.basex.api.xqj;

import static org.basex.api.xqj.BXQText.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQResultItem;
import org.basex.data.SAXSerializer;
import org.basex.data.XMLSerializer;
import org.basex.io.ArrayOutput;
import org.basex.query.QueryException;
import org.basex.query.item.AtomType;
import org.basex.query.item.Bln;
import org.basex.query.item.Dbl;
import org.basex.query.item.Flt;
import org.basex.query.item.Item;
import org.basex.query.item.ANode;
import org.basex.query.item.NodeType;
import org.basex.query.item.Type;
import org.basex.query.iter.ItemCache;
import org.basex.util.Token;
import org.basex.util.Util;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

/**
 * Java XQuery API - Item.
 *
 * @author BaseX Team 2005-11, BSD License
 * @author Christian Gruen
 */
final class BXQItem extends BXQAbstract implements XQResultItem {
  /** Connection. */
  private final BXQConnection conn;
  /** Item. */
  final Item it;

  /**
   * Constructor.
   * @param item item
   * @throws XQException exception
   */
  BXQItem(final Item item) throws XQException {
    this(item, new BXQDataFactory(null, null), null);
  }

  /**
   * Constructor.
   * @param item item
   * @param c close reference
   * @param connection connection reference
   */
  BXQItem(final Item item, final BXQAbstract c,
      final BXQConnection connection) {

    super(c);
    conn = connection;
    it = item;
  }

  @Override
  public String getAtomicValue() throws XQException {
    opened();
    if(it.node()) throw new BXQException(ATOM);
    try {
      return Token.string(it.atom(null));
    } catch(final QueryException e) {
      // function item
      throw new BXQException(ATOM);
    }
  }

  @Override
  public boolean getBoolean() throws XQException {
    return ((Bln) check(AtomType.BLN)).bool(null);
  }

  @Override
  public byte getByte() throws XQException {
    return (byte) castItr(AtomType.BYT);
  }

  @Override
  public double getDouble() throws XQException {
    return ((Dbl) check(AtomType.DBL)).dbl(null);
  }

  @Override
  public float getFloat() throws XQException {
    return ((Flt) check(AtomType.FLT)).flt(null);
  }

  @Override
  public int getInt() throws XQException {
    return (int) castItr(AtomType.INT);
  }

  @Override
  public XMLStreamReader getItemAsStream() {
    return new IterStreamReader(new ItemCache(new Item[] { it }, 1));
  }

  @Override
  public String getItemAsString(final Properties props) throws XQException {
    try {
      return it.node() && it.type != NodeType.TXT ? serialize() :
        Token.string(it.atom(null));
    } catch(QueryException e) {
      throw new XQException(e.getMessage(), e.code());
    }
  }

  @Override
  public XQItemType getItemType() throws XQException {
    opened();
    return new BXQItemType(it.type);
  }

  @Override
  public long getLong() throws XQException {
    return castItr(AtomType.LNG);
  }

  @Override
  public Node getNode() throws XQException {
    opened();
    if(!it.node()) throw new BXQException(WRONG, NodeType.NOD, it.type);
    return ((ANode) it).toJava();
  }

  @Override
  public URI getNodeUri() throws XQException {
    opened();
    if(!it.node()) throw new BXQException(NODE);
    final ANode node = (ANode) it;
    try {
      return new URI(Token.string(node.base()));
    } catch(final URISyntaxException ex) {
      throw new BXQException(ex.toString());
    }
  }

  @Override
  public Object getObject() throws XQException {
    opened();
    return it.toJava();
  }

  @Override
  public short getShort() throws XQException {
    return (short) castItr(AtomType.SHR);
  }

  @Override
  public boolean instanceOf(final XQItemType type) throws XQException {
    opened();
    return it.type.instance(((BXQItemType) type).getType());
  }

  @Override
  public void writeItem(final OutputStream os, final Properties props)
      throws XQException {
    valid(os, OutputStream.class);
    serialize(os);
  }

  @Override
  public void writeItemToSAX(final ContentHandler sax) throws XQException {
    valid(sax, ContentHandler.class);
    writeItemToResult(new SAXResult(sax));
  }

  @Override
  public XQConnection getConnection() throws XQException {
    opened();
    return conn;
  }

  @Override
  public void writeItem(final Writer ow, final Properties props)
      throws XQException {
    valid(ow, Writer.class);
    try {
      ow.write(serialize());
    } catch(final IOException ex) {
      throw new BXQException(ex);
    }
  }

  @Override
  public void writeItemToResult(final Result result) throws XQException {
    valid(result, Result.class);

    // evaluate different result types...
    if(result instanceof StreamResult) {
      // StreamResult.. directly write result as string
      writeItem(((StreamResult) result).getWriter(), null);
    } else if(result instanceof SAXResult) {
      try {
        // SAXResult.. serialize result to underlying parser
        final SAXSerializer ser = new SAXSerializer(null);
        ser.setContentHandler(((SAXResult) result).getHandler());
        serialize(it, ser);
        ser.close();
      } catch(final IOException ex) {
        throw new BXQException(ex);
      }
    } else {
      Util.notimplemented();
    }
  }

  /**
   * Serializes the item to the specified output stream.
   * @param os output stream
   * @throws XQException exception
   */
  private void serialize(final OutputStream os) throws XQException {
    try {
      serialize(it, new XMLSerializer(os));
    } catch(final IOException ex) {
      throw new BXQException(ex);
    }
  }

  /**
   * Returns the serialized output.
   * @return cached output
   * @throws XQException exception
   */
  private String serialize() throws XQException {
    opened();
    final ArrayOutput ao = new ArrayOutput();
    serialize(ao);
    return ao.toString();
  }

  /**
   * Checks the specified data type; throws an error if the type is wrong.
   * @param type expected type
   * @return item
   * @throws XQException xquery exception
   */
  private Item check(final Type type) throws XQException {
    opened();
    if(it.type != type) throw new BXQException(WRONG, it.type, type);
    return it;
  }

  /**
   * Casts the current item.
   * @param type expected type
   * @return cast item
   * @throws XQException xquery exception
   */
  private long castItr(final Type type) throws XQException {
    opened();
    try {
      final double d = it.dbl(null);
      if(!it.num() || d != (long) d) throw new BXQException(NUM, d);
      return type.e(it, ctx.ctx, null).itr(null);
    } catch(final QueryException ex) {
      throw new BXQException(ex);
    }
  }
}
