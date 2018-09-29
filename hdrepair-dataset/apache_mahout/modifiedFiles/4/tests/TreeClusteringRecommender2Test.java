/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.cf.taste.impl.recommender;

import java.util.List;

import org.apache.mahout.cf.taste.impl.TasteTestCase;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.junit.Ignore;
import org.junit.Test;

/** <p>Tests {@link TreeClusteringRecommender2}.</p> */
public final class TreeClusteringRecommender2Test extends TasteTestCase {

  // Ignore this test since there is an infinite loop in buildClusters:
  // mergeClosestClusters never returns true for this dataset
  @Ignore 
  @Test
  public void testNoRecommendations() throws Exception {
    DataModel dataModel = getDataModel(
            new long[] {1, 2, 3},
            new Double[][] {
                    {0.1},
                    {0.2, 0.6},
                    {0.4, 0.9},
            });
    UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
    ClusterSimilarity clusterSimilarity = new FarthestNeighborClusterSimilarity(similarity);
    Recommender recommender = new TreeClusteringRecommender2(dataModel, clusterSimilarity, 2);
    List<RecommendedItem> recommended = recommender.recommend(1, 1);
    assertNotNull(recommended);
    assertEquals(0, recommended.size());
    recommender.refresh(null);
    assertNotNull(recommended);
    assertEquals(0, recommended.size());
  }

  @Test
  public void testHowMany() throws Exception {
    DataModel dataModel = getDataModel(
            new long[] {1, 2, 3, 4, 5},
            new Double[][] {
                    {0.1, 0.2},
                    {0.2, 0.3, 0.3, 0.6},
                    {0.4, 0.4, 0.5, 0.9},
                    {0.1, 0.4, 0.5, 0.8, 0.9, 1.0},
                    {0.2, 0.3, 0.6, 0.7, 0.1, 0.2},
            });

    UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
    ClusterSimilarity clusterSimilarity = new FarthestNeighborClusterSimilarity(similarity);
    Recommender recommender = new TreeClusteringRecommender2(dataModel, clusterSimilarity, 2);
    List<RecommendedItem> fewRecommended = recommender.recommend(1, 2);
    List<RecommendedItem> moreRecommended = recommender.recommend(1, 4);
    for (int i = 0; i < fewRecommended.size(); i++) {
      assertEquals(fewRecommended.get(i).getItemID(), moreRecommended.get(i).getItemID());
    }
    recommender.refresh(null);
    for (int i = 0; i < fewRecommended.size(); i++) {
      assertEquals(fewRecommended.get(i).getItemID(), moreRecommended.get(i).getItemID());
    }
  }

  @Test
  public void testRescorer() throws Exception {
    DataModel dataModel = getDataModel(
            new long[] {1, 2, 3},
            new Double[][] {
                    {0.1, 0.2},
                    {0.2, 0.3, 0.3, 0.6},
                    {0.4, 0.4, 0.5, 0.9},
            });

    UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
    ClusterSimilarity clusterSimilarity = new FarthestNeighborClusterSimilarity(similarity);
    Recommender recommender = new TreeClusteringRecommender2(dataModel, clusterSimilarity, 2);
    List<RecommendedItem> originalRecommended = recommender.recommend(1, 2);
    List<RecommendedItem> rescoredRecommended =
        recommender.recommend(1, 2, new ReversingRescorer<Long>());
    assertNotNull(originalRecommended);
    assertNotNull(rescoredRecommended);
    assertEquals(2, originalRecommended.size());
    assertEquals(2, rescoredRecommended.size());
    assertEquals(originalRecommended.get(0).getItemID(), rescoredRecommended.get(1).getItemID());
    assertEquals(originalRecommended.get(1).getItemID(), rescoredRecommended.get(0).getItemID());
  }

  @Test
  public void testEstimatePref() throws Exception {
    DataModel dataModel = getDataModel(
            new long[] {1, 2, 3, 4},
            new Double[][] {
                    {0.1, 0.3},
                    {0.2, 0.3, 0.3},
                    {0.4, 0.3, 0.5},
                    {0.7, 0.3, 0.8, 0.9},
            });

    UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
    ClusterSimilarity clusterSimilarity = new FarthestNeighborClusterSimilarity(similarity);
    Recommender recommender = new TreeClusteringRecommender2(dataModel, clusterSimilarity, 2);
    assertEquals(0.9f, recommender.estimatePreference(3, 3), EPSILON);
  }

  @Test
  public void testBestRating() throws Exception {
    DataModel dataModel = getDataModel(
            new long[] {1, 2, 3, 4},
            new Double[][] {
                    {0.1, 0.3},
                    {0.2, 0.3, 0.3},
                    {0.4, 0.3, 0.5},
                    {0.7, 0.3, 0.8},
            });


    UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
    ClusterSimilarity clusterSimilarity = new FarthestNeighborClusterSimilarity(similarity);
    Recommender recommender = new TreeClusteringRecommender2(dataModel, clusterSimilarity, 2);
    List<RecommendedItem> recommended = recommender.recommend(1, 1);
    assertNotNull(recommended);
    assertEquals(1, recommended.size());
    RecommendedItem firstRecommended = recommended.get(0);
    // item one should be recommended because it has a greater rating/score
    assertEquals(2, firstRecommended.getItemID());
    assertEquals(0.3, firstRecommended.getValue(), EPSILON);
  }

}
