/*
 * Copyright 2013 SFB 632.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package annis.ql.parser;

import annis.model.QueryAnnotation;
import annis.model.QueryNode;
import annis.ql.AqlParser;
import annis.ql.AqlParserBaseListener;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Thomas Krause <krauseto@hu-berlin.de>
 */
public class QueryNodeListener extends AqlParserBaseListener
{
  private static final Logger log = LoggerFactory.getLogger(QueryNodeListener.class);
  
  private QueryData data = null;

  private final List<QueryNode> currentAlternative = new ArrayList<QueryNode>();

  private long aliasCount = 0l;
  private String lastVariableDefinition = null;

  private final Multimap<String, QueryNode> localNodes = HashMultimap.create();
  
  private List<Map<Interval, QueryNode>> tokenPositions;
  private final Map<Interval, QueryNode> currentTokenPosition = Maps.newHashMap();
  private final Map<Interval, Long> globalTokenPositions = Maps.newHashMap();
  
  private final List<QueryAnnotation> metaData = new ArrayList<QueryAnnotation>();

  public QueryNodeListener()
  {
  }

  public QueryData getQueryData()
  {
    return data;
  }

  public List<QueryAnnotation> getMetaData()
  {
    return metaData;
  }
  
  @Override
  public void enterOrTop(AqlParser.OrTopContext ctx)
  {
    data = new QueryData();
    tokenPositions = new ArrayList<Map<Interval, QueryNode>>();
  }

  @Override
  public void enterAndExpr(AqlParser.AndExprContext ctx)
  {
    currentAlternative.clear();
    localNodes.clear();
    currentTokenPosition.clear();
  }

  @Override
  public void exitAndExpr(AqlParser.AndExprContext ctx)
  {
    data.addAlternative(new ArrayList<QueryNode>(currentAlternative));
    tokenPositions.add(new HashMap<Interval, QueryNode>(currentTokenPosition));
  }

  

  @Override
  public void enterTokOnlyExpr(AqlParser.TokOnlyExprContext ctx)
  {
    QueryNode target = newNode(ctx);
    target.setToken(true);
  }

  @Override
  public void enterNodeExpr(AqlParser.NodeExprContext ctx)
  {
    newNode(ctx);
  }
  

  @Override
  public void enterTokTextExpr(AqlParser.TokTextExprContext ctx)
  {
    QueryNode target = newNode(ctx);
    target.setToken(true);
    QueryNode.TextMatching txtMatch = textMatchingFromSpec(ctx.textSpec(),
      ctx.NEQ() != null);
    String content = textFromSpec(ctx.textSpec());
    target.setSpannedText(content, txtMatch);
  }

  @Override
  public void enterTextOnly(AqlParser.TextOnlyContext ctx)
  {
    QueryNode target = newNode(ctx);
    target.setSpannedText(textFromSpec(ctx.txt),
      textMatchingFromSpec(ctx.txt, false));
  }

  @Override
  public void enterAnnoOnlyExpr(AqlParser.AnnoOnlyExprContext ctx)
  {
    QueryNode target = newNode(ctx);
    String namespace = ctx.qName().namespace == null ? null : ctx.qName().namespace.getText();
    QueryAnnotation anno = new QueryAnnotation(namespace,
      ctx.qName().name.getText());
    target.addNodeAnnotation(anno);
  }

  @Override
  public void enterAnnoEqTextExpr(AqlParser.AnnoEqTextExprContext ctx)
  {
    QueryNode target = newNode(ctx);
    String namespace = ctx.qName().namespace == null ? 
      null : ctx.qName().namespace.getText();
    String name = ctx.qName().name.getText();
    String value = textFromSpec(ctx.txt);
    QueryNode.TextMatching matching = textMatchingFromSpec(ctx.txt,
      ctx.NEQ() != null);
    QueryAnnotation anno = new QueryAnnotation(namespace, name, value, matching);
    target.addNodeAnnotation(anno);
  }

  
  
  
  
  @Override
  public void enterMetaTermExpr(AqlParser.MetaTermExprContext ctx)
  {
    // TODO: we have to disallow OR expressions with metadata, how can we
    // achvieve that?
    String namespace = ctx.id.namespace == null ? 
      null : ctx.id.namespace.getText();
    String name = ctx.id.name.getText();
    String value = textFromSpec(ctx.txt);
    QueryNode.TextMatching textMatching = textMatchingFromSpec(ctx.txt, ctx.NEQ() != null);
    
    QueryAnnotation anno = new QueryAnnotation(namespace,
      name, value, textMatching);
    metaData.add(anno);
  }

  @Override
  public void enterNamedVariableTermExpr(AqlParser.NamedVariableTermExprContext ctx)
  {
    lastVariableDefinition = null;
    if(ctx != null)
    {
      String text = ctx.VAR_DEF().getText();
      // remove the trailing "#"
      if(text.endsWith("#"))
      {
        lastVariableDefinition = text.substring(0, text.length()-1);
      }
      else
      {
        lastVariableDefinition = text;
      }
    }
  }

  @Override
  public void enterReferenceNode(AqlParser.ReferenceNodeContext ctx)
  {
    if(ctx != null && ctx.VAR_DEF() != null)
    {
      lastVariableDefinition = null;
    
      String text = ctx.VAR_DEF().getText();
      // remove the trailing "#"
      if(text.endsWith("#"))
      {
        lastVariableDefinition = text.substring(0, text.length()-1);
      }
      else
      {
        lastVariableDefinition = text;
      }
    
    }
  }
  
  
  

  protected static String textFromSpec(AqlParser.TextSpecContext txtCtx)
  {
    if (txtCtx instanceof AqlParser.EmptyExactTextSpecContext || txtCtx instanceof AqlParser.EmptyRegexTextSpecContext)
    {
      return "";
    }
    else if (txtCtx instanceof AqlParser.ExactTextSpecContext)
    {
      return ((AqlParser.ExactTextSpecContext) txtCtx).content.getText();
    }
    else if (txtCtx instanceof AqlParser.RegexTextSpecContext)
    {
      return ((AqlParser.RegexTextSpecContext) txtCtx).content.getText();
    }
    return null;
  }
  
  protected static QueryNode.TextMatching textMatchingFromSpec(
    AqlParser.TextSpecContext txt, boolean not)
  {
    if (txt instanceof AqlParser.ExactTextSpecContext 
      || txt instanceof AqlParser.EmptyExactTextSpecContext)
    {
      return not ? QueryNode.TextMatching.EXACT_NOT_EQUAL : 
        QueryNode.TextMatching.EXACT_EQUAL;
    }
    else if (txt instanceof AqlParser.RegexTextSpecContext
      || txt instanceof AqlParser.EmptyRegexTextSpecContext)
    {
     return  not ? QueryNode.TextMatching.REGEXP_NOT_EQUAL : 
       QueryNode.TextMatching.REGEXP_EQUAL;
    }
    return null;
  }

  private QueryNode newNode(ParserRuleContext ctx)
  {
    Long existingID = globalTokenPositions.get(ctx.getSourceInterval());
    
    if(existingID == null)
    {
      existingID = ++aliasCount;
    }
    
    QueryNode n = new QueryNode(existingID);
    if(lastVariableDefinition == null)
    {
      n.setVariable("" + n.getId());
    }
    else
    {
      n.setVariable(lastVariableDefinition);
    }
    lastVariableDefinition = null;
    
    currentAlternative.add(n);
    localNodes.put(n.getVariable(), n);
    currentTokenPosition.put(ctx.getSourceInterval(), n);
    globalTokenPositions.put(ctx.getSourceInterval(), n.getId());
    
    return n;
  }

  public List<Map<Interval, QueryNode>> getTokenPositions()
  {
    return tokenPositions;
  }
  
  

  
}
