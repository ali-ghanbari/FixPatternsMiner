package com.continuuity.gateway.apps.wordcount;

import com.continuuity.api.annotation.UseDataSet;
import com.continuuity.api.batch.MapReduce;
import com.continuuity.api.batch.MapReduceContext;
import com.continuuity.api.batch.MapReduceSpecification;
import com.continuuity.api.common.Bytes;
import com.continuuity.api.data.dataset.KeyValueTable;
import org.apache.hadoop.mapreduce.Job;

public final class ClassicWordCount implements MapReduce {
    @UseDataSet("jobConfig")
    private KeyValueTable table;

    @Override
    public MapReduceSpecification configure() {
      return MapReduceSpecification.Builder.with()
        .setName("ClassicWordCount")
        .setDescription("WordCount job from Hadoop examples")
        .build();
    }

    @Override
    public void beforeSubmit(MapReduceContext context) throws Exception {

      String inputPath = Bytes.toString(table.read(Bytes.toBytes("inputPath")));
      String outputPath = Bytes.toString(table.read(Bytes.toBytes("outputPath")));
      WordCountMR.configureJob((Job) context.getHadoopJob(), inputPath, outputPath);
    }

    @Override
    public void onFinish(boolean succeeded, MapReduceContext context) throws Exception {
      System.out.println("Action taken on MapReduce job " + (succeeded ? "" : "un") + "successful completion");
    }
  }
