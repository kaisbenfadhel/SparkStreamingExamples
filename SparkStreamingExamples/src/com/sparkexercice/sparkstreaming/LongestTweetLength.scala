package com.sparkexercice.sparkstreaming

import org.apache.spark._
import org.apache.spark.SparkContext._
import org.apache.spark.streaming._
import org.apache.spark.streaming.twitter._
import org.apache.spark.streaming.StreamingContext._
import Utilities._
//For multi-threading concepts we use some java stuff
import java.util.concurrent._
//helps in accessing + manipulating a long Integer value
import java.util.concurrent.atomic._
import java.lang.Math

/** Uses thread-safe counters to keep track of the average length of
 *  Tweets in a stream.
 */
object LongestTweetLength {
  /** Our main function where the action happens */
  def main(args: Array[String]) {

    // Configure Twitter credentials using twitter.txt
    setupTwitter()
    
    // Set up a Spark streaming context named "AverageTweetLength" that runs locally using
    // all CPU cores and one-second batches of data
    val ssc = new StreamingContext("local[*]", "AverageTweetLength", Seconds(1))
    
    // Get rid of log spam (should be called after the context is set up)
    setupLogging()

    // Create a DStream from Twitter using our streaming context
    val tweets = TwitterUtils.createStream(ssc, None)
    
    // Now extract the text of each status update into DStreams using map()
    val statuses = tweets.map(status => status.getText())
   
    // Map this to tweet character lengths.
    val lengths = statuses.map(status => status.length())       
    
    // As we could have multiple processes adding into these running totals
    // at the same time, we'll just Java's AtomicLong class to make sure
    // these counters are thread-safe.    
    
    var longestTweetSeen = new AtomicLong(0)
    var rddMaxVal = new AtomicLong(0)
    
    // In Spark 1.6+, you  might also look into the mapWithState function, which allows
    // you to safely and efficiently keep track of global state with key/value pairs.
    // We'll do that later in the course.
    
    lengths.foreachRDD((rdd, time) => {
      
      var count = rdd.count()
      if (count > 0) {               
        
        val repartition = rdd.repartition(1).cache()        
        rddMaxVal.set(repartition.max())       
                
        println("Longest tweet in RDD " + rddMaxVal.get())       
        
        //Longest Tweet encountered since the beginning of time 
        println("Before: Longest Tweet Seen:" + longestTweetSeen.get())
        if (rddMaxVal.get() >= longestTweetSeen.get()) {
          longestTweetSeen.set(rddMaxVal.get())
          println("Inside: Longest Tweet Seen:" + longestTweetSeen.get())          
        }             
      println("After: Longest tweet seen so far: " + longestTweetSeen.get())
      println("--------------------------------")
      }
    })
    
    // Set a checkpoint directory, and kick it all off    
    //ssc.checkpoint("C:/checkpoint/")
    ssc.start()
    ssc.awaitTermination()
  }  
}